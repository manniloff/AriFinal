/**
 * 
 */
package com.unifun.sigtran.smsgate.smpp.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.PDUStringException;
import org.jsmpp.bean.CancelSm;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.OptionalParameter.Tag;
import org.jsmpp.bean.QuerySm;
import org.jsmpp.bean.ReplaceSm;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.ServerResponseDeliveryAdapter;
import org.jsmpp.session.Session;
import org.jsmpp.session.SessionStateListener;
import org.jsmpp.util.MessageId;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.enums.Direction;
import com.unifun.sigtran.smsgate.enums.ExpiredType;
import com.unifun.sigtran.smsgate.enums.SmsCharSet;
import com.unifun.sigtran.smsgate.enums.SmsType;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccess;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProvidersAccessesRestrictions;
import com.unifun.sigtran.smsgate.hibernate.models.DataCodingList;
import com.unifun.sigtran.smsgate.hibernate.models.MsisdnBlackList;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPServerConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMSQueue;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.smpp.common.MessageContainer;
import com.unifun.sigtran.smsgate.smpp.common.SMPPConnectionInfo;
import com.unifun.sigtran.smsgate.smpp.common.UDHParser;
import com.unifun.sigtran.smsgate.smpp.workers.SMPPDLRWorker;
import com.unifun.sigtran.smsgate.smpp.workers.SMPPMOWorker;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;
import com.unifun.sigtran.smsgate.util.PoolableSpeedLimiter;

public class SmppServer extends ServerResponseDeliveryAdapter implements Runnable, ServerMessageReceiverListener {
	
	private final static String TON_REGEX = "[0-7]";
	private final static String NPI_REGEX = "^[0,1,3-9,15]{1,2}$";
	
	private final ExecutorService execService;
//	private final ExecutorService execSubmitSM;
	
	private int port;
	private int timeout;
	private int serverId;
	private boolean runFlag;
	private int processorDegree;
	private short concatenateType;
	private SMPPMOWorker moWorker;
//	private SMPPDLRWorker dlrWorker;
	private static long nextPartWaiting;
	private static boolean serverState;
	private static String serverName;
	private static long smsDefaultLiveTime;
	private static String interfaceVersion;
	private SMPPServerSessionListener sessionListener = null;
	//	String key, MessageContainer - sms part.
	private static ConcurrentHashMap<String, MessageContainer> messageParts;
	private static ConcurrentLinkedQueue<SmsData> moQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentHashMap<String, SMPPServerSession> serverSession = new ConcurrentHashMap();
	private final Logger logger = LogManager.getLogger(SmppServer.class);
	
	
	public SmppServer(SMPPServerConfig cfg, ConcurrentHashMap<String, MessageContainer> messageParts) {
		execService = Executors.newFixedThreadPool(cfg.getWaitbind()
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_Waitbind_" + cfg.getServerName()).setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
		
//		execSubmitSM = Executors.newFixedThreadPool(cfg.getPoolSize()
//				, new CustomThreadFactoryBuilder()
//				.setNamePrefix("_SMPP-SubmitSM_" + cfg.getServerName()).setDaemon(false)
//				.setPriority(Thread.MAX_PRIORITY).build());
		serverId = cfg.getId();
		serverName = cfg.getServerName();
		SmppServer.messageParts = messageParts;
		runFlag = true;
		port = cfg.getServerPort();
		timeout = cfg.getTimeout() * 1000;
		processorDegree = cfg.getProcessorDegree();
		nextPartWaiting = cfg.getNextPartWaiting() * 1000;
		interfaceVersion = cfg.getInerfaceVersion();
		concatenateType = Short.valueOf(cfg.getConcatinateType());
		//send DLR 
//		dlrWorker = new SMPPDLRWorker(cfg.getServerName() + "_DLRWorker", cfg.getDlrQueryLimit(), cfg.getDlrSleepTime(), cfg.getServiceType(), cfg.getId());
//		dlrWorker.start();
		//send MO
		setMoWorker(new SMPPMOWorker(cfg.getServerName() + "_MOWorker", cfg.getSendDLRPerSec(), Short.valueOf(cfg.getConcatinateType()), cfg.getServiceType()));
		getMoWorker().start();
	}
	
	public void stop() {
		runFlag = false;
		try {
			sessionListener.close();
			logger.info(getServerName() + " SessionListener stopped" );
		} catch (IOException e) {
			logger.error(getServerName() + " could not stop SessionListener!!!");
			logger.error(e);
		}
		getMoWorker().interrupt();
		for (SMPPServerSession session : serverSession.values()) {
			serverSession.remove(session.getSessionId());
			ServerController.removeConnection(session, false);	
		}
		
		try {
			execService.shutdown();
			execService.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Handled error stopping smpp server ExectorService");
			logger.error(e);
		}
	}
	
	//CPA
	private String byteToHexString(byte[] response) {
    	char[] hexArray = "0123456789ABCDEF".toCharArray();
    	char[] hexChars = new char[response.length * 2];
        for ( int j = 0; j < response.length; j++ ) {
            int v = response[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
	
	private ContentProviderAccess validation(SubmitSm submitSm, SMPPConnectionInfo info) throws ProcessRequestException {
		try {
//			if(info.getWaitingQueueIsFull()) {
//				throw new ProcessRequestException("Invalid scheduled delivery time. SmsQueue is Full", 0x00000061);
//			}
			long started = System.currentTimeMillis();
			Integer errorCode = null;
			String errorMessage = null;
			//check from idiot starts
			if(submitSm.getSourceAddr() == null) {
				submitSm.setSourceAddr("NULL");
				errorCode = 0x0000000A;
				errorMessage = "Invalid source address";
			}
			if(submitSm.getDestAddress() == null) {
				submitSm.setDestAddress("0");
				if(errorCode == null) {
					errorCode = 0x0000000B;
					errorMessage = "Invalid destination address";	
				}
			}
			if(errorCode != null) {
				throw new ProcessRequestException(errorMessage, errorCode);
			}
			
			if(submitSm.getSourceAddr().length() > 15) {
				errorCode = 0x0000000A;
				errorMessage = "Invalid source address";
				submitSm.setSourceAddr("Too Long");
			}
			try {
				Long.valueOf(submitSm.getDestAddress());
			} catch (NumberFormatException e) {
				submitSm.setDestAddress("1");
				if(errorCode == null) {
					errorCode = 0x0000000B;
					errorMessage = "Invalid destination address";
				}
			}
			if(errorCode != null) {
				throw new ProcessRequestException(errorMessage, errorCode);
			}
			//check from idiot ends
			
			//check blackList
			MsisdnBlackList row = SmsGateWay.getBlackLists().get(Long.valueOf(submitSm.getDestAddress()));
			if(row != null) {
				if(row.getDirection().equals(Direction.MT) 
						|| row.getDirection().equals(Direction.ANY)) {
					throw new ProcessRequestException("Invalid dest address", 0x0000000B);
				}
			}
//			if(SmsGateWay.getBlackLists().stream()
//					.filter(bad -> bad.getMsisdn() 
//							== Long.valueOf(submitSm.getDestAddress())
//							&& (bad.getDirection().equals(Direction.MT) 
//									|| bad.getDirection().equals(Direction.ANY))
//					).findFirst().isPresent()) {
//				logger.warn(submitSm.getDestAddress() +  " in black list");
//				throw new ProcessRequestException("Invalid dest address", 0x0000000B);
//			}
			//Access restrictions starts
			//getting all restrictions for the access;
			List<ContentProvidersAccessesRestrictions> restrictions = new ArrayList<>();
			SmsGateWay.getAccessesRestrictions().forEach(accessRest -> {
				 if(accessRest.getAccessId() == info.getAccessId()) {
					 restrictions.add(accessRest);
				 }
			});
			if(restrictions.isEmpty()) {
				  logger.error("Access Restriction was not found for " + info.toString());
				  throw new ProcessRequestException("Submitting message has failed", 0x00000045); 
			}				
			
			//checkSourece address
			Optional<ContentProvidersAccessesRestrictions> oRestriction 
				= restrictions.stream().filter(rest -> (rest.getSourceAddress() != null 
						&& rest.getSourceAddress().equals(submitSm.getSourceAddr())
					|| rest.getSourceAddress() == null)).findFirst();
			
			if(!oRestriction.isPresent()) {
				logger.warn("invalid source address " + submitSm.getSourceAddr());
				throw new ProcessRequestException("Invalid source address", 0x0000000A);
			}
			//checkSourece ton
			if((oRestriction.get().getSourceTon() != null 
					&& !oRestriction.get().getSourceTon()
						.equals(String.valueOf(submitSm.getSourceAddrTon())))
				&& (oRestriction.get().getSourceTon() == null 
					&& String.valueOf(submitSm.getSourceAddrTon()).matches(TON_REGEX))) {
				logger.warn("invalid source TON. address " + submitSm.getSourceAddr() + "ton - " + submitSm.getSourceAddrTon());
				throw new ProcessRequestException("	Invalid source address type of number", 0x00000048);
			}
			//checkSourece np
			if((oRestriction.get().getSourceNp() != null 
					&& !oRestriction.get().getSourceNp()
						.equals(String.valueOf(submitSm.getSourceAddrNpi())))
				&& (oRestriction.get().getSourceNp() == null 
					&& String.valueOf(submitSm.getSourceAddrNpi()).matches(NPI_REGEX))) {
				logger.warn("invalid source NP. address " + submitSm.getSourceAddr() + "NP - " + submitSm.getSourceAddrNpi());
				throw new ProcessRequestException("	Invalid source address numbering plan", 0x00000049);
			}
			
			//checkDest ton
			if((oRestriction.get().getDestTon() != null 
					&& !oRestriction.get().getDestTon()
						.equals(String.valueOf(submitSm.getDestAddrTon())))
				&& (oRestriction.get().getDestTon() == null 
					&& String.valueOf(submitSm.getDestAddrTon()).matches(TON_REGEX))) {
				logger.warn("invalid source TON. address " + submitSm.getDestAddress() + "ton - " + submitSm.getDestAddrTon());
				throw new ProcessRequestException("	Invalid dest address type of number", 0x00000050);
			}
			
			//checkDest np
			if((oRestriction.get().getDestNp() != null 
					&& !oRestriction.get().getDestNp()
						.equals(String.valueOf(submitSm.getDestAddrNpi())))
				&& (oRestriction.get().getDestNp() == null 
					&& String.valueOf(submitSm.getDestAddrNpi()).matches(NPI_REGEX))) {
				logger.warn("invalid dest NP. address " + submitSm.getDestAddress() + "NP - " + submitSm.getDestAddrNpi());
				throw new ProcessRequestException("	Invalid dest address numbering plan", 0x00000051);
			}
			//Access restrictions ends
			
			//Regular checks
			if(SmsGateWay.getRegExs().isEmpty()) {
				logger.warn("Regular expressions list is empty");
			} else {
				//Source address
				
				String replace = SmsGateWay.getSourcheSubstitutionLists().get(submitSm.getSourceAddr());
				if(replace != null) {
					submitSm.setSourceAddr(replace);
				}
				if(!SmsGateWay.getRegExs().stream()
					.filter(reg -> 
						   reg.getNp().equals(String.valueOf(submitSm.getSourceAddrNpi()))
						&& reg.getTon().equals(String.valueOf(submitSm.getSourceAddrTon()))
						&& submitSm.getSourceAddr().matches(reg.getExpression())
						&& reg.isCheckForSourceAdd() == true
					).findFirst().isPresent()) {
					logger.warn("SOURCHEADDRES. Regular check failed - " + submitSm.getSourceAddr() 
							+ "; ton " + submitSm.getSourceAddrTon()
							+ "; np " + submitSm.getSourceAddrNpi()); 
					throw new ProcessRequestException("Invalid source address", 0x0000000A);
				}
				//Dest address
				if(!SmsGateWay.getRegExs().stream()
						.filter(reg -> 
							   reg.getNp().equals(String.valueOf(submitSm.getDestAddrNpi()))
							&& reg.getTon().equals(String.valueOf(submitSm.getDestAddrTon()))
							&& submitSm.getDestAddress().matches(reg.getExpression())
							&& reg.isCheckForSourceAdd() == false
						).findFirst().isPresent()) {
						logger.warn("DESTADDRES. Regular check failed - " + submitSm.getDestAddress() 
								+ "; ton " + submitSm.getDestAddrTon()
								+ "; np " + submitSm.getDestAddrNpi()); 
						throw new ProcessRequestException("Invalid dest address", 0x0000000B);
				}
			}
			logger.debug("validation duration - " + (System.currentTimeMillis() - started));
			return SmsGateWay.getContentProviderAccesses().get(info.getAccessId());
		} catch (Exception e) {
			logger.error(e);
			if(e instanceof ProcessRequestException) {
				ProcessRequestException pr = (ProcessRequestException)e;
				throw new ProcessRequestException(pr.getMessage(), pr.getErrorCode());
			}
			throw new ProcessRequestException("App Error Code: -1", 0x00000064);
		}
	}

	private void processSM(SubmitSm submitSM, short dcs, short pid, boolean payLoad, long messageId, Charset cs
			, ContentProviderAccess accessInfo, short segmentCount) throws ProcessRequestException, PDUStringException {
		String messageText = null; 
		if(payLoad) {
			byte[] message = submitSM.getOptionalParameter(Tag.MESSAGE_PAYLOAD).serialize();
			messageText = (dcs != 246 ) ? new String(Arrays.copyOfRange(message, Integer.valueOf(message[0]), message.length), cs)
					: byteToHexString(message);
		} else {
			messageText = (dcs != 246 ) ? new String(submitSM.getShortMessage(), cs) 
					: byteToHexString(submitSM.getShortMessage());
		}
//		short segmentCount = ServerController.getSmsContainers(dcs, messageText.length());
		if(!AddInSMSQueue(submitSM, messageText, dcs, pid, messageId, segmentCount, cs, accessInfo)) {
			throw new ProcessRequestException("App Error Code: -100", 0x00000064);	
		}
	}
	
	private void processUDH(SubmitSm submitSM, short dcs, short pid, long messageId, Charset cs
			, ContentProviderAccess accessInfo, short segmentCount, Map<Integer, byte[]> parsedUdh) throws ProcessRequestException {
		try {
			short refId = 0;
			short segmentId = 1;
//			byte[] udh = Arrays.copyOfRange(submitSM.getShortMessage(), 2, submitSM.getShortMessage()[0] + 1);
//			short udhLength = (short)udh.length;
			byte[] udh = parsedUdh.get(0); // get Concatenated short messages, 8-bit reference number
			if(udh == null) {
				throw new ProcessRequestException("Expected UDH parameter is missing", 0x000000C3);
			}

			short udhLength = (short)udh.length;
			if(dcs == 246) {  // hard coded for ICB
				logger.info(udh);
				refId = (short)((udh[0] < 0) ? 256 - udh[0] : udh[0]);
				segmentId = (short)((udh[3] < 0) ? 256 - udh[3] : udh[3]);
			} else {
				refId = (short)((udh[udhLength - 3] < 0) ? 256 - udh[udhLength - 3] : udh[udhLength - 3]);
				segmentId = (short)((udh[udhLength - 1] < 0) ? 256 - udh[udhLength - 1] : udh[udhLength - 1]);	
			}
			
//			short _segmentCount = segmentCount;
			byte[] message = Arrays.copyOfRange(submitSM.getShortMessage(), submitSM.getShortMessage()[0] + 1, submitSM.getShortMessage().length);
			String key = accessInfo.getId() + "_" + refId + "_" + submitSM.getSourceAddr()+ "_" + submitSM.getDestAddress();
			MessageContainer data = new MessageContainer(key, segmentCount, segmentId
					, (dcs == 246) ? byteToHexString(submitSM.getShortMessage()) 
							: new String(message, cs)
					, new Timestamp(System.currentTimeMillis() + SmppServer.getNextPartWaiting()), accessInfo.getId());
			SmppServer.getMessageParts().merge(key, data, (oldPart, newPart) -> {
				newPart.setMessagePart(oldPart.getMessagePart());
				newPart.getMessagePart()[newPart.getCurrentContainer() -1] = newPart.getMessageText();
				newPart.setContainerReceived((short)(oldPart.getContainerReceived() + 1));
				if(newPart.getContainerReceived() == newPart.getSmsLen()) {
					String text = "";
					for (int i = 0; i < newPart.getMessagePart().length; i++) {
						text += newPart.getMessagePart()[i];
					}
					SmppServer.getMessageParts().remove(key);
					try {
						if(!AddInSMSQueue(submitSM, text, dcs, pid, messageId, segmentCount, cs, accessInfo)) {
							throw new ProcessRequestException("App Error Code: -100", 0x00000064);
						}
					} catch (ProcessRequestException e) {
						throw new ConcurrentModificationException(e.fillInStackTrace());
					}
				}
				return newPart;
				});
		} catch (Exception e) {
			logger.error("Error Message " + e.toString() + "; StackTrace" +  e.getStackTrace());
			if (e.getCause() instanceof ProcessRequestException) {
				ProcessRequestException ps = (ProcessRequestException) e.getCause();
				throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
			} else 
				throw new ProcessRequestException("App Error Code: -1", 0x00000064);
		}
	}
	
	private void processSAR(SubmitSm submitSM, byte[] tRefId, byte[] tSegmentId, short segmentCount, short dcs
			, short pid, long messageId, Charset cs, ContentProviderAccess access) throws ProcessRequestException {
		try {
			short refId = tRefId[tRefId.length - 1];
			short segmentId = tSegmentId[tSegmentId.length - 1];
			logger.debug("refId - " + refId + ";segmentId -" + segmentId + ";segmentCount -" + segmentCount);
			String key = access.getId() + "_" + refId + "_" + submitSM.getSourceAddr()+ "_" + submitSM.getDestAddress();
			MessageContainer data = new MessageContainer(key, segmentCount, segmentId
					, new String(submitSM.getShortMessage(), cs)
					, new Timestamp(System.currentTimeMillis() + SmppServer.getNextPartWaiting()), access.getId());
			SmppServer.getMessageParts().merge(key, data, (oldPart, newPart) -> {
				newPart.setMessagePart(oldPart.getMessagePart());
				newPart.getMessagePart()[newPart.getCurrentContainer() -1] = newPart.getMessageText();
				newPart.setContainerReceived((short)(oldPart.getContainerReceived() + 1));
				if(newPart.getContainerReceived() == newPart.getSmsLen()) {
					String text = "";
					for (int i = 0; i < newPart.getMessagePart().length; i++) {
						text += newPart.getMessagePart()[i];
					}
					SmppServer.getMessageParts().remove(key);
					try {
						if(!AddInSMSQueue(submitSM, text, dcs, pid, messageId, segmentCount, cs, access)) {
							throw new ProcessRequestException("App Error Code: -100", 0x00000064);
						}
					} catch (ProcessRequestException e) {
						throw new ConcurrentModificationException(e.fillInStackTrace()); //ps.getMessage(), ps.getErrorCode()
					}
				}
				return newPart;
				});
		} catch (Exception e) {
			logger.error("Error Message " + e.toString() + "; StackTrace" +  e.getStackTrace());
			if (e.getCause() instanceof ProcessRequestException) {
				ProcessRequestException ps = (ProcessRequestException) e.getCause();
				throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
			} else 
				throw new ProcessRequestException("App Error Code: -1", 0x00000064);
		}
	}
	
	private boolean AddInSMSQueue(SubmitSm submitSM, String messageText, short dcs, short pid
			, long messageId, short segmentCount, Charset cs, ContentProviderAccess accessInfo) throws ProcessRequestException {
		boolean result = false;
		try {
			Timestamp sendUntil = ServerController.getExpired(submitSM.getValidityPeriod(), accessInfo.getDefaultSmsLiveTimeInMilSec(), accessInfo.getExpiredType()); 	
			Timestamp schedulerTime = ServerController.getScheduleDeliveryTime(submitSM.getScheduleDeliveryTime()
					, accessInfo.getCanSendSMSFromTime(), accessInfo.getCanSendSMSToTime(), accessInfo.getDaysOfWeek(), accessInfo.getDeliveryType());
			SMSQueue smsQueue = new SMSQueue(messageId, accessInfo.getId(), submitSM.getSourceAddr(), String.valueOf((int)submitSM.getSourceAddrTon())
					, String.valueOf((int)submitSM.getSourceAddrNpi()), Long.valueOf(submitSM.getDestAddress())
					, String.valueOf((int)submitSM.getDestAddrTon()), String.valueOf((int)submitSM.getDestAddrNpi())
					, messageText, segmentCount, dcs, pid
					, new Timestamp(System.currentTimeMillis()), schedulerTime, sendUntil, String.valueOf(submitSM.getRegisteredDelivery())
					, String.valueOf((int)submitSM.getPriorityFlag()), ServerController.getSmsContainerLength(dcs, segmentCount));
			result = ServerController.addSmsToQueue(smsQueue);
		} catch (Exception e) {
			if(e instanceof ProcessRequestException) {
				ProcessRequestException ps = (ProcessRequestException) e;
				throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
			}
			logger.error("saving in SmsQueue. " + Arrays.toString(e.getStackTrace()));
		}
		return result;
	}
	
		
	@Override
	public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	 public MessageId onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source) throws ProcessRequestException {
	  SMPPConnectionInfo info = ServerController.getConnections().get(source.getSessionId());
	  int sysId = 0;
	  try {
		  if(info != null) {
			  sysId = info.getAccessId();
			  PoolableSpeedLimiter limiter = ServerController.getLimmiter().get(info.getLogin());
			  short dcs = (short)((submitSm.getDataCoding() < 0) ? 256 + submitSm.getDataCoding() : submitSm.getDataCoding());
			  if(limiter == null) {
				  logger.warn("Speed Limiter was not found for " + info.toString());
				  throw new ProcessRequestException("Throttling error. limit not found", 0x00000058);
			  }
			  boolean isThrottling  = false;
			  short segmentCount = 1;
			  Map<Integer, byte[]> parsedUdh = new HashMap<>();
			try {
				//check sms length
				if(submitSm.isUdhi()) {
					isThrottling = limiter.isThrottling(1);					//any way increment attempt to send
//					byte[] udh = Arrays.copyOfRange(submitSm.getShortMessage(), 2, submitSm.getShortMessage()[0] + 1);
//					short udhLength = (short)udh.length;
					parsedUdh = UDHParser.parse(Arrays.copyOfRange(submitSm.getShortMessage(), 1, submitSm.getShortMessage()[0] + 1));
					if(parsedUdh.isEmpty()) {
						throw new ProcessRequestException("Expected UDH parameter missing", 0x000000C3);
					}
					byte[] udh = parsedUdh.get(0); // get Concatenated short messages, 8-bit reference number
					if(udh == null) {
						throw new ProcessRequestException("Expected UDH parameter missing", 0x000000C3);
					}

					short udhLength = (short)udh.length;
					if(submitSm.getDataCoding() == -10) { // for ICB
						segmentCount = (short)((udh[2] < 0) ? 256 - udh[2] : udh[2]); 
					} else {
						segmentCount = (short)((udh[udhLength - 2] < 0) ? 256 - udh[udhLength - 2] : udh[udhLength - 2]);	
					}
					if(segmentCount > info.getMessageParts())
						throw new ProcessRequestException("Message too long", 0x00000001);
				} else if(submitSm.getOptionalParameters() != null) {
					if(submitSm.getOptionalParameter(Tag.MESSAGE_PAYLOAD) != null) {
						int smsLen = submitSm.getOptionalParameter(Tag.MESSAGE_PAYLOAD).serialize().length - 4; // 4 TLV parameter 
						int segmentLenInbytes = ServerController.getSegmentLenForPayLoad(dcs, smsLen);
						segmentCount =  (short)((smsLen / segmentLenInbytes) + ((smsLen % segmentLenInbytes == 0) ? 0 : 1));
						isThrottling = limiter.isThrottling(segmentCount);	//any way increment attempt to send
						if(segmentCount > info.getMessageParts())
							throw new ProcessRequestException("Message too long", 0x00000001);
					} else {
						if(submitSm.getOptionalParameter(Tag.SAR_TOTAL_SEGMENTS) != null) {
							byte[] _segmentCount = submitSm.getOptionalParameter(Tag.SAR_TOTAL_SEGMENTS).serialize();
							segmentCount = _segmentCount[_segmentCount.length - 1];
							isThrottling = limiter.isThrottling(1);			//any way increment attempt to send
							if(segmentCount > info.getMessageParts())
								throw new ProcessRequestException("Message too long", 0x00000001);
						}
					}
				} else {
					isThrottling = limiter.isThrottling(1);
				}
			} catch (Exception e) {
				logger.error(e.getMessage() + "; StackTrace: ", e);
				if(e instanceof ProcessRequestException) {
					ProcessRequestException ps = (ProcessRequestException) e;
					throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
				}
				throw new ProcessRequestException("App Error Code: -10", 0x00000064);
			}
			if(isThrottling){
				throw new ProcessRequestException("Throttling error ", 0x00000058);
			}
		  ContentProviderAccess access = validation(submitSm, info);
		  if(access == null) {
			  throw new ProcessRequestException("Submit_sm. ContentProviderAccess not found", 0x00000064);
		  }
		  DataCodingList value = SmsGateWay.getDataCodingList().get(dcs);
		  if(value == null) {
			  logger.info("not supported dcs. DCS - " + dcs);
			  throw new ProcessRequestException("Not supported dcs.", 0x00000104);
		  } else if(!value.isEnabled()) {
			  logger.info("dcs is disabled. DCS - " + dcs);
			  throw new ProcessRequestException("Not supported dcs.", 0x00000104);
		  }
		  //Check Direction
		  if(access.getDirection().equals(Direction.MO)) {
			  throw new ProcessRequestException("Invalid source address", 0x0000000A);
		  }
		  Charset cs = Charset.forName(value.getCharset());
		  //check if charset is available.
		  if(!SmsGateWay.getAccessCharsetLists()
				  .stream().filter(csRow -> csRow.isEnabled() == true
				  && csRow.getAccessId() == access.getId()
				  		//check GS7
				  && ((csRow.getSmsCharset().equals(SmsCharSet.GSM7) //TO ENUMS
						  && (cs.name().equals("ISO-8859-1")
								  || cs.name().equals("US-ASCII")))
						  //check GS8
						  || (csRow.getSmsCharset().equals(SmsCharSet.GSM8)
								  && cs.name().equals("UTF-8"))
						  //check UCS2
						  || (csRow.getSmsCharset().equals(SmsCharSet.UCS2)
								  && (cs.name().equals("UTF-16")
										  || cs.name().equals("UTF-16BE")
										  || cs.name().equals("UTF-16LE")))
							)
					).findAny().isPresent()) {
			  logger.info("CharSet is not available. System - " + info.toString());
			  throw new ProcessRequestException("CharSet is not available", 0x00000104);
		  }
		  short pid = (short)(submitSm.getProtocolId() < 0 ? 256 + submitSm.getProtocolId() : submitSm.getProtocolId());
		  //check if pid is supported
		  if(!access.getSmsType().equals(SmsType.ANY)) {
			  if((access.getSmsType().equals(SmsType.HIDDEN) && pid != 64)
					  || (access.getSmsType().equals(SmsType.REGULAR) && pid == 64)) {
					logger.info("pid not supperted. Allowed - " + access.getSmsType() 
					+ " recieved - " + (pid == 64 ? SmsType.HIDDEN : SmsType.REGULAR));  
					throw new ProcessRequestException("Invalid data coding scheme", 0x00000104);
			  }
		  }
		  long messageId = SmsGateWay.getNextMessageId();
		  int messageLen;
		  if(submitSm.isUdhi()) {
			  logger.debug("Recieve UDH Data");
			  //check segment length
			  messageLen = Arrays.copyOfRange(submitSm.getShortMessage(), submitSm.getShortMessage()[0] + 1, submitSm.getShortMessage().length).length;
			  if((messageLen > 134
					  && (!cs.name().equals("ISO-8859-1")
							  && !cs.name().equals("US-ASCII")))
					  && messageLen > 153) {
				  throw new ProcessRequestException("UDH. Incorrect Message Len " + submitSm.getShortMessage().length + "; DCS " + dcs, 0x00000001);
			  }
			  processUDH(submitSm, dcs, pid, messageId, cs, access, segmentCount, parsedUdh);
			  return new MessageId(String.valueOf(messageId));
		  } else if(submitSm.getOptionalParameters() == null) {
			  logger.debug("Recieve short SM");
			  if((submitSm.getShortMessage().length > 140
					  && (!cs.name().equals("ISO-8859-1")
							  && !cs.name().equals("US-ASCII")))
					  && submitSm.getShortMessage().length > 160) {
				  throw new ProcessRequestException("SAR. Incorrect Message Len " + submitSm.getShortMessage().length + "; DCS " + dcs, 0x00000001);
			  }
			  processSM(submitSm, dcs, pid, false, messageId, cs, access, segmentCount);
			  return new MessageId(String.valueOf(messageId));
		  } else if(submitSm.getOptionalParameter(Tag.MESSAGE_PAYLOAD) != null){
				logger.debug("Recieve PayLoad Data"); 
				processSM(submitSm, dcs, pid, true, messageId, cs, access, segmentCount);
				return new MessageId(String.valueOf(messageId));
		  } else if(submitSm.getOptionalParameter(Tag.SAR_MSG_REF_NUM) != null) {
				logger.debug("Recieve SAR Data");
				byte[] tRefId = null;
				byte[] tSegmentId = null;
				try {
					tRefId = submitSm.getOptionalParameter(Tag.SAR_MSG_REF_NUM).serialize();
					tSegmentId = submitSm.getOptionalParameter(Tag.SAR_SEGMENT_SEQNUM).serialize();
					if(tRefId == null || tSegmentId == null) {
						throw new Exception("SAR DATA is missing");
					}
				} catch (Exception e) {
					logger.warn("Parsing SAR Data. " + Arrays.toString(e.getStackTrace()));
					throw new ProcessRequestException("Optional Parameters missing.", 195);
				}
				//check segment length
				if((submitSm.getShortMessage().length > 134
						  && (!cs.name().equals("ISO-8859-1")
								  && !cs.name().equals("US-ASCII")))
						  && submitSm.getShortMessage().length > 153) {
					  throw new ProcessRequestException("SAR. Incorrect Message Len " + submitSm.getShortMessage().length + "; DCS " + dcs, 0x00000001);
				  }
				processSAR(submitSm, tRefId, tSegmentId, segmentCount, dcs, pid
						, messageId, cs, access);
				return new MessageId(String.valueOf(messageId));
		  } else {
				logger.debug("Recieve short SM");
				if((submitSm.getShortMessage().length > 140
						  && (!cs.name().equals("ISO-8859-1")
								  && !cs.name().equals("US-ASCII")))
						  && submitSm.getShortMessage().length > 160) {
					  throw new ProcessRequestException("SAR. Incorrect Message Len " + submitSm.getShortMessage().length + "; DCS " + dcs, 0x00000001);
				}
				processSM(submitSm, dcs, pid, false, messageId, cs
						, access, segmentCount);
				return new MessageId(String.valueOf(messageId));
				}
		  }
		  	logger.error("system not found for session - " + source.getSessionId());
			throw new ProcessRequestException("SMPP Client No Connection", 0x00000045);
	  } catch (ProcessRequestException ps) {
		  SmsGateWay.saveSmppErrorLog(sysId, submitSm.getSourceAddr(), Long.valueOf(submitSm.getDestAddress())
				  , submitSm.getSequenceNumber(), ps.getErrorCode(), ps.getMessage(), System.currentTimeMillis(), true);
		  if(ps.getErrorCode() == 0x00000058) {
			  logger.warn(ps.toString() + "; " + Arrays.toString(ps.getStackTrace()));
		  } else {
			  logger.error(ps.toString() + "; " + Arrays.toString(ps.getStackTrace()));  
		  }
		  throw new ProcessRequestException(ps.getMessage(), ps.getErrorCode());
	  } catch (PDUStringException e) {
		  SmsGateWay.saveSmppErrorLog(sysId, submitSm.getSourceAddr(), Long.valueOf(submitSm.getDestAddress())
				  , submitSm.getSequenceNumber(), -1, e.getMessage(), System.currentTimeMillis(), true);
		  throw new ProcessRequestException("Submit_sm", 0x00000064);
	  } catch (Exception e) {
		  SmsGateWay.saveSmppErrorLog(sysId, submitSm.getSourceAddr(), Long.valueOf(submitSm.getDestAddress())
				  , submitSm.getSequenceNumber(), -1, e.getMessage(), System.currentTimeMillis(), true);
		  throw new ProcessRequestException("Submit_sm", 0x00000064);
	  }
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
		replaceSm.getMessageId();
		replaceSm.getRegisteredDelivery();
		replaceSm.getScheduleDeliveryTime();
		replaceSm.getShortMessage();
		replaceSm.getSourceAddr();
		replaceSm.getSourceAddrNpi();
		replaceSm.getSourceAddrTon();
		replaceSm.getValidityPeriod();
	}

	@Override
	public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source) throws ProcessRequestException {
		 SMPPConnectionInfo info = ServerController.getConnections().get(source.getSessionId());
		 String errorMessage = null;
		 if(info != null) {
			 if(SmsGateWay.getContentProviderAccesses().get(info.getAccessId()) != null
//					 && SmsGateWay.getContentProviderAccesses().get(info.getAccessId()). //cancel parameter
					 ) {
				 if(!ServerController.processCancelSmRequest(Long.valueOf(cancelSm.getMessageId()), info.getAccessId()))
					 errorMessage = "Message was not found or sent"; 
			 } else {
				 errorMessage = "Not allowed. System Id - " + info.getAccessId() + "; Login - " + info.getLogin();
			 }
		 } else {
			 errorMessage = "Unknown system";
		 }
		
		if(errorMessage != null) {
			SmsGateWay.saveSmppErrorLog(info.getAccessId(), cancelSm.getSourceAddr(), 0L, cancelSm.getSequenceNumber(), 0x00000011, errorMessage
					, System.currentTimeMillis(), true);
			throw new ProcessRequestException("Cancelling message failed", 0x00000011);
		}
	}
		
	@Override
	public void run() {
		serverState= true;
		Thread.currentThread().setName("_THREAD-SMPPServerSessionListener");
		try {
			setSessionListener(new SMPPServerSessionListener(port));
			getSessionListener().setSessionStateListener(new SessionStateListenerImpl());
			getSessionListener().setPduProcessorDegree(processorDegree);
			getSessionListener().setTimeout(timeout);
			logger.info("Server Name: " + serverName + "Listening on port: " + getSessionListener().getPort() 
						+ "; PduProcessorDegree: " + getSessionListener().getPduProcessorDegree());
			 logger.info("Server " + serverName + " started");

		} catch (IOException e) {
			 logger.error(e.toString() +  Arrays.toString(e.getStackTrace()));
			return;
		}
		while (runFlag) {
			try {
				 while (runFlag) {
					 SMPPServerSession serverSession = getSessionListener().accept();
					 logger.info("##### Accepting connection for session: "+ serverSession.getSessionId());
				     serverSession.setMessageReceiverListener(this);
				     serverSession.setResponseDeliveryListener(this);
				     try{
				    	 getExecService().execute(new SmppWaitBindTask(serverId, serverName, serverSession, timeout));
				     }catch(RuntimeException e) {
				    	 logger.error(e.toString() +  Arrays.toString(e.getStackTrace()));
				     } catch(Exception e) {
				    	 logger.error(e.toString() +  Arrays.toString(e.getStackTrace()));
				     }
				 }
			} catch (SocketTimeoutException e) {
//				 ingnore
	        } catch (IOException e) {
	        	 logger.error(e.toString() +  Arrays.toString(e.getStackTrace()));
	        }  catch (Exception e) {
	        	 logger.error(e.toString() +  Arrays.toString(e.getStackTrace()));
	        }	
		}
		serverState = runFlag;
	}

	private class SessionStateListenerImpl implements SessionStateListener {
        public void onStateChange(SessionState newState, SessionState oldState,
        		Session source) {
            SMPPServerSession session = (SMPPServerSession)source;
            logger.info("New state of " + session.getSessionId() + " is " + newState);
            switch (newState.name()) {
			case "CLOSED":
				serverSession.remove(session.getSessionId());
				ServerController.removeConnection(session, false);
				break;
			case "OPEN":
				serverSession.put(session.getSessionId(), session);
				break;
			default:
				logger.info("Modification of Session " + session.getSessionId() 
					+ "; State - " + newState.name() + "(old state - " + oldState.name() + ")");
				break;
			}
        }
    }
	
	public static long getNextPartWaiting() {
		return nextPartWaiting;
	}

	public static boolean getServerState() {
		return serverState;
	}

	public static void setServerState(boolean serverState) {
		SmppServer.serverState = serverState;
	}

	public boolean getRunFlag() {
		return runFlag;
	}

	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}
	
	public SMPPServerSessionListener getSessionListener() {
		return sessionListener;
	}

	public void setSessionListener(SMPPServerSessionListener sessionListener) {
		this.sessionListener = sessionListener;
	}

	public static ConcurrentHashMap<String, MessageContainer> getMessageParts() {
		return messageParts;
	}

	public static void setMessageParts(ConcurrentHashMap<String, MessageContainer> messageParts) {
		SmppServer.messageParts = messageParts;
	}

	public String getServerName() {
		return serverName;
	}

//	public static long getSmsDefaultLiveTime() {
//		return smsDefaultLiveTime;
//	}
//
//	public static void setSmsDefaultLiveTime(long smsDefaultLiveTime) {
//		SmppServer.smsDefaultLiveTime = smsDefaultLiveTime;
//	}
//	public SMPPDLRWorker getDlrWorker() {
//		return dlrWorker;
//	}

	public static ConcurrentLinkedQueue<SmsData> getMoQueue() {
		return moQueue;
	}

	public static void setMoQueue(ConcurrentLinkedQueue<SmsData> moQueue) {
		SmppServer.moQueue = moQueue;
	}

	public SMPPMOWorker getMoWorker() {
		return moWorker;
	}

	public void setMoWorker(SMPPMOWorker moWorker) {
		this.moWorker = moWorker;
	}

	public ExecutorService getExecService() {
		return execService;
	}
}