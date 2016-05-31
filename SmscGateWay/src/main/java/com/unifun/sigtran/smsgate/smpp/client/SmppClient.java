package com.unifun.sigtran.smsgate.smpp.client;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.CancelSm;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.QuerySm;
import org.jsmpp.bean.ReplaceSm;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.ServerResponseDeliveryAdapter;
import org.jsmpp.session.Session;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.jsmpp.util.MessageId;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.common.ConnectionLimits;
import com.unifun.sigtran.smsgate.smpp.common.MessageContainer;
import com.unifun.sigtran.smsgate.smpp.workers.SMPPClientSubmitSMWorker;



public class SmppClient extends ServerResponseDeliveryAdapter implements Runnable, ServerMessageReceiverListener, MessageReceiverListener {

	private boolean reconnect;
	private boolean clientIsRuning; 
	private int timeOut = 2; 
	private SMPPClientConfig cfg;
	private ConnectionLimits conLimit;
	private SMPPClientSubmitSMWorker submitSMWorker;
	
	private volatile SMPPSession smppSession;
	private AtomicInteger reconnectTears = new AtomicInteger();
	private AtomicBoolean clientAvailable = new AtomicBoolean();
	
	private static ConcurrentHashMap<String, MessageContainer> messageParts;
	private ConcurrentLinkedQueue<SendData> smsToSend = new ConcurrentLinkedQueue<>();
	
	private Logger logger = LogManager.getLogger(SmppClient.class);
	
	public SmppClient(SMPPClientConfig cfg, ConcurrentHashMap<String, MessageContainer> messageParts, ConnectionLimits conLimit) {
		this.setClientConfig(cfg);
		SmppClient.messageParts = messageParts;
		clientIsRuning = true;
		clientAvailable.set(false);
		this.conLimit = conLimit;
		setSubmitSMWorker(new SMPPClientSubmitSMWorker(this));
	}
	
	public void stop() {
		getSubmitSMWorker().interrupt();
		clientIsRuning = false;
		//clear binding sessions
		if(smppSession != null) {
			smppSession.unbindAndClose();
		}
		//Clear from Group
		ClientController.getGroupList().stream().forEach(g -> {
			if(g.getGroupId() == cfg.getGroupID()) {
				logger.info("SystemId - " + cfg.getSystemId() + "; Decrementing active clients for group - " + g.getGroupId()	 
				+ "; Clients - " + g.getAvailableClients().decrementAndGet());
			}
		});
	}

	@Override
	public void run() {
		Thread.currentThread().setName("_SMPPClient_" + cfg.getSystemId());
		logger.info("Client started for - " + cfg.getSystemId());
		setReconnectTears(new AtomicInteger(getClientConfig().getReconnectTries()));
		
		connectAndBind();
		getSubmitSMWorker().start();
		reconnect = true;
		while (clientIsRuning) {
			if(smppSession == null || !smppSession.getSessionState().isBound())
				connectAndBind();
			try { 
				Thread.sleep(getClientConfig().getReconnectTriesTime() * 1000); 
			} catch (InterruptedException e) {
				logger.error("SMPP CLIENT [Host=" + getClientConfig().getHost() + ";Port=" + getClientConfig().getPort() + ";SystemId=" + getClientConfig().getSystemId() + "] - Thread InterruptedException in method connectAndBind().", e);
			}
		}
		logger.info("Client finished for - " + cfg.getSystemId());
	}
	
	private void connectAndBind() {
		if(reconnect && (getReconnectTears().get() == 0)) {
			clientAvailable.set(false);
			smppSession = null;
			return;
		}
		try	{
			if (smppSession != null) {
				ClientController.getGroupList().stream().forEach(g -> {
					if(g.getGroupId() == cfg.getGroupID()) {
						logger.info("SystemId - " + cfg.getSystemId() + "; Decrementing active clients for group - " + g.getGroupId()	 
						+ "; Clients - " + g.getAvailableClients().decrementAndGet());
					}
				});
				clientAvailable.set(false);
				smppSession.close();
			}
			smppSession = new SMPPSession();
			smppSession.setPduProcessorDegree(getClientConfig().getPduProcessorDegree());
			smppSession.setTransactionTimer(((getClientConfig().getTimeOut() == 0) ? timeOut : getClientConfig().getTimeOut()) * 1000);
			int ton = Integer.valueOf(getClientConfig().getTon());
			int np = Integer.valueOf(getClientConfig().getNp());
			// try to connect
			smppSession.connectAndBind(getClientConfig().getHost(), getClientConfig().getPort(), BindType.valueOf(Integer.valueOf(getClientConfig().getBindType()))
					, getClientConfig().getSystemId(), getClientConfig().getPassword(), getClientConfig().getSystemType(), TypeOfNumber.valueOf((byte) ton),
					NumberingPlanIndicator.valueOf((byte) np), null);

			// add DLR listener
			smppSession.setMessageReceiverListener(this);

			logger.info("SMPP CLIENT [Host=" + getClientConfig().getHost() + ";Port=" + getClientConfig().getPort() + ";SystemId=" + getClientConfig().getSystemId() + "] IS BOUNDED TO SERVER");
			setReconnectTears(new AtomicInteger(getClientConfig().getReconnectTries()));
			clientAvailable.set(true);
			ClientController.getGroupList().stream().forEach(g -> {
				if(g.getGroupId() == cfg.getGroupID()) {
					logger.info("SystemId - " + cfg.getSystemId() 
						+ "; Increasing active clients for group - " + g.getGroupId()	 
						+ "; Clients - " + g.getAvailableClients().incrementAndGet());
				}
			});
		}
		catch (Exception e)
		{
			smppSession = null;
			logger.warn("SMPP CLIENT [Host=" + getClientConfig().getHost() + ";Port=" + getClientConfig().getPort() + ";SystemId=" + getClientConfig().getSystemId() + "] - Failed initialize connection or bind. Reconnection after "
					+ (getClientConfig().getReconnectTriesTime()) + " seconds. Remained [" + getReconnectTears().decrementAndGet() + "] tries.");
			// try to reconnect after N seconds
			logger.error(e);
		}
	}
		
	@Override
	public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	 @Override
	 public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
		 if(deliverSm.getEsmClass() != 0 && deliverSm.getEsmClass() != 64) {
			 ClientController.getEsProcessRequset().submit( new Runnable() {
				 @Override
				 public void run() {
					 try { //DLR
						 Long remoteId = -1L;
						 switch (cfg.getDlrIdType()) {
						 case "HEX":
							 remoteId = new BigInteger(deliverSm.getShortMessageAsDeliveryReceipt().getId(), 16).longValueExact();
							 break;
						 case "LONG":
							 remoteId = Long.valueOf(deliverSm.getShortMessageAsDeliveryReceipt().getId());
							 break;
						 default:
							 logger.error("Unsupported RemoteIdType. REMOTEID: " + deliverSm.getShortMessageAsDeliveryReceipt().getId()
									 + "STATE: " + deliverSm.getShortMessageAsDeliveryReceipt().getFinalStatus().name()
									 + "DestAddress: " + Long.valueOf(deliverSm.getDestAddress())
									 + "SourchAddress: " + Long.valueOf(deliverSm.getSourceAddr()));
							 break;
						}
						if(remoteId < 0)
							return;
						logger.info(deliverSm.getShortMessageAsDeliveryReceipt().toString());
						String state = String.valueOf(deliverSm.getShortMessageAsDeliveryReceipt().getFinalStatus().value() + 1);
						ClientController.ProcessDlrRequest(remoteId
								, state, cfg.getId());
					} catch (NumberFormatException | InvalidDeliveryReceiptException e) {
						logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
						SmsGateWay.saveSmppErrorLog(cfg.getId(), deliverSm.getSourceAddr(), Long.valueOf(deliverSm.getDestAddress())
								, deliverSm.getSequenceNumber(), -1, e.getMessage(), System.currentTimeMillis(), false);
					}
				 }});
			 } else { // MO
//				 try {
//					 ClientController.getEsProcessRequset().submit(new MOProcessLogic(deliverSm, cfg.getId())).get();
//				 } catch (Exception e) {
//					 if(e.getCause() instanceof ProcessRequestException) {
//						 ProcessRequestException ps = (ProcessRequestException)e.getCause();
//						 SmsGateWay.saveSmppErrorLog(cfg.getId(), deliverSm.getSourceAddr(), Long.valueOf(deliverSm.getDestAddress())
//									, deliverSm.getSequenceNumber(), -1, e.getMessage(), System.currentTimeMillis(), false);
//						 if(ps.getErrorCode() < 0 )
//							 logger.warn(e.toString() + "; " + Arrays.toString(e.getStackTrace()));
//						 else
//							 logger.error(e.toString() + "; " + Arrays.toString(e.getStackTrace()));
//						 throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
//					}
//					logger.error(e.toString() + "; " + Arrays.toString(e.getStackTrace()));
//					SmsGateWay.saveSmppErrorLog(cfg.getId(), deliverSm.getSourceAddr(), Long.valueOf(deliverSm.getDestAddress())
//							, deliverSm.getSequenceNumber(), -1, e.getMessage(), System.currentTimeMillis(), false);
//					throw new ProcessRequestException("Deliver_sm", 8191);
//				}
			}
	 	}
	 
	@Override
	public void onAcceptAlertNotification(AlertNotification alertNotification) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MessageId onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession source)
			throws ProcessRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		
	}
	
	public SMPPClientConfig getClientConfig() {
		return cfg;
	}
	public void setClientConfig(SMPPClientConfig cfg) {
		this.cfg = cfg;
	}
	public SMPPSession getSmppSession() {
		return smppSession;
	}

//	public AtomicInteger getSmsSended() {
//		return smsSended;
//	}
//
//	public void setSmsSended(AtomicInteger smsSended) {
//		this.smsSended = smsSended;
//	}

	public AtomicBoolean getClientAvailable() {
		return clientAvailable;
	}

	public void setClientAvailable(AtomicBoolean clientAvailable) {
		this.clientAvailable = clientAvailable;
	}


	public boolean isClientIsRuning() {
		return clientIsRuning;
	}


	public void setClientIsRuning(boolean clientIsRuning) {
		this.clientIsRuning = clientIsRuning;
	}

	public ConcurrentLinkedQueue<SendData> getSmsToSend() {
		return smsToSend;
	}

	public ConnectionLimits getConLimit() {
		return conLimit;
	}


	public void setConLimit(ConnectionLimits conLimit) {
		this.conLimit = conLimit;
	}


	public static ConcurrentHashMap<String, MessageContainer> getMessageParts() {
		return messageParts;
	}


	public static void setMessageParts(ConcurrentHashMap<String, MessageContainer> messageParts) {
		SmppClient.messageParts = messageParts;
	}


	public AtomicInteger getReconnectTears() {
		return reconnectTears;
	}


	public void setReconnectTears(AtomicInteger reconnectTears) {
		this.reconnectTears = reconnectTears;
	}
	
	public SMPPClientSubmitSMWorker getSubmitSMWorker() {
		return submitSMWorker;
	}


	public void setSubmitSMWorker(SMPPClientSubmitSMWorker submitSMWorker) {
		this.submitSMWorker = submitSMWorker;
	}
	
}
