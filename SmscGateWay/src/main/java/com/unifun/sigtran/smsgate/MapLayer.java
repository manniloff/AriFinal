package com.unifun.sigtran.smsgate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.m3ua.RouteAs;
import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.NoteSubscriberPresentRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReadyForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReadyForSMResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_MTI;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitReportTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.service.sms.MAPDialogSmsImpl;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ConcatenatedShortMessagesIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

import com.unifun.sigtran.smsgate.hibernate.models.MOIncoming;
import com.unifun.sigtran.smsgate.hibernate.models.MoRoutingRules;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;

public class MapLayer implements MAPDialogListener, MAPServiceSmsListener {

	private static Logger logger = Logger.getLogger(MapLayer.class);
	private MAPStackImpl mapStack;
	protected MAPProvider mapProvider;
	private  TCAPStack tcapStack;
	private SccpStack sccpStack;
	private SccpAddress serviceCenterSCCPAddress = null;
	private SM_RP_OA serviceCentreAddressOA = null;
	private AddressString serviceCenterAddress;
	private ExecutorService dbWorker;
	private Map<String, Map<String,String>> appSettings = null;
	private AtomicLong sriCounterReq = new AtomicLong();
	private AtomicLong sriCounterRes = new AtomicLong();
	private AtomicLong sriCounterBadRes = new AtomicLong();
	private AtomicLong mtfCounterReq = new AtomicLong();
	private AtomicLong mtfCounterRes = new AtomicLong();
	private AtomicLong mtfCounterBadRes = new AtomicLong();
	private AtomicLong rdsCounterReq = new AtomicLong();
	private AtomicLong rdsCounterRes = new AtomicLong();
	private AtomicLong rdsCounterBadRes = new AtomicLong();

	private MAPApplicationContext shortMsgMAPApplicationContext;
	private MAPApplicationContext mtForSMSMAPApplicationContext;

	private ConcurrentHashMap<Long, Timestamp> MSISDNInProgress = new ConcurrentHashMap<Long, Timestamp>();
	// SMS Queue for one MSISDN
	private ConcurrentLinkedQueue<SendData> MSISDNQueue = new ConcurrentLinkedQueue<SendData>();
	private ConcurrentHashMap<Long, SendData> SRIRequestQueue = new ConcurrentHashMap<Long, SendData>();
	private ConcurrentHashMap<Long, SendData> MTFSMQueue = new ConcurrentHashMap<Long, SendData>();
	private ConcurrentHashMap<Long, SendData> RDSRequestQueue = new ConcurrentHashMap<Long, SendData>();
	
	public MapLayer(MAPStack mapStack, TCAPStack tcapStack, SccpStack sccpStack) {
		this.sccpStack = sccpStack;
		this.tcapStack = tcapStack;
		this.mapStack = (MAPStackImpl)mapStack;
		this.mapProvider = this.mapStack.getMAPProvider();
		
		
	}
	public SccpStack getSCCPStack() {
		return sccpStack;
	}
	public TCAPStack getTCAPStack() {
		return tcapStack;
	}
	public void init(){
		logger.debug("Appending SMS Map Listiner to MapStack ....");
		this.mapProvider.addMAPDialogListener(this);
		this.mapProvider.getMAPServiceSms().addMAPServiceListener(this);
		this.mapProvider.getMAPServiceSms().acivate(); 
		dbWorker = Executors.newFixedThreadPool(Integer.parseInt(this.appSettings.get("app").get("threads"))
				, new CustomThreadFactoryBuilder().setNamePrefix("_DBWriter").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
	}
	
	public void stop(){
		this.mapProvider.removeMAPDialogListener(this);
		this.mapProvider.getMAPServiceSms().removeMAPServiceListener(this);
		this.mapProvider.getMAPServiceSms().deactivate();
		dbWorker.shutdown();
		appSettings.clear();
		MSISDNInProgress.clear();
		SRIRequestQueue.clear();
		MTFSMQueue.clear();
		RDSRequestQueue.clear();
	}
	
	private class DBWriter extends Thread {
		short type;
		MAPDialog mapDialog;
		SendData data;
		String state;
		String errorMessage;
		AlertServiceCentreRequest alertServiceCentreReq;
		SendRoutingInfoForSMResponse sriSMResp;
		MtForwardShortMessageResponse mtfSMResp;
		MoForwardShortMessageRequest moForwSmInd;
		ReportSMDeliveryStatusResponse reportSMDSResp;

		public DBWriter(SendRoutingInfoForSMResponse sriSMResp) {
			type = 1;
			this.sriSMResp = sriSMResp;
		}

		public DBWriter(MtForwardShortMessageResponse mtfSMResp) {
			type = 2;
			this.mtfSMResp = mtfSMResp;
		}

		public DBWriter(ReportSMDeliveryStatusResponse reportSMDSResp) {
			type = 3;
			this.reportSMDSResp = reportSMDSResp;
		}

		public DBWriter(AlertServiceCentreRequest alertServiceCentreReq) {
			type = 4;
			this.alertServiceCentreReq = alertServiceCentreReq;
		}

		public DBWriter(final MAPDialog mapDialog, String state, String errorMessage) {
			type = 5;
			this.mapDialog = mapDialog;
			this.state = state;
			this.errorMessage = errorMessage;
		}

		public DBWriter(final MoForwardShortMessageRequest moForwSmInd) {
			type = 6;
			this.moForwSmInd = moForwSmInd;
		}
		
		@Override
		public void run() {
			//data.getMessagePart() starts from zero (0)
			try {
				switch (type) {
				case 1:
					data = SRIRequestQueue.remove(sriSMResp.getMAPDialog().getLocalDialogId());
					if (data == null) {
						logger.warn("Can't find SRISM Data!!! DialogId=" 
								+ sriSMResp.toString());
						return;
					} else {
//						data.setNetworkNodeNumber(sriSMResp.getLocationInfoWithLMSI().getNetworkNodeNumber());
//						data.setImsi(sriSMResp.getIMSI());
//						SmsGateWay.processSRISMResponse(data, null, null);
//						logger.info("received srism response for messageId=" + data.getMessageId());
						boolean numberNotInRoaming = //true; 
								sriSMResp.getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress()
								.startsWith(SmsGateWay.getMSCLocalPrefix());
						logger.info("numberNotInRoaming - " + numberNotInRoaming);
						if(numberNotInRoaming) {	//test
							data.setNetworkNodeNumber(sriSMResp.getLocationInfoWithLMSI().getNetworkNodeNumber());
							data.setImsi(sriSMResp.getIMSI());	
						} else {
							//clear from progress queue, because number in roaming. Send sms over SMSC Operator
							ProcessData(data, "2");
						}
						SmsGateWay.processSRISMResponse(data, null, null, numberNotInRoaming);
						logger.info("received srism response for messageId=" + data.getMessageId());	
					}

					break;
				case 2:
					data = MTFSMQueue.remove(mtfSMResp.getMAPDialog().getLocalDialogId());
					if (data == null) {
						logger.warn("Can't find MTFSM Data!!! DialogId=" + mtfSMResp.toString());
						return;
					}
					logger.info(
							"received mtfsm response for messageId=" + data.getMessageId()
							+ "; shortMessageCount=" + data.getQuantity() + "; segmentId="
							+ (data.getMessagePart() + 1));
					if (data.getQuantity() == (data.getMessagePart() + 1)) {
						SmsGateWay.processMTFSMResponse(data, true, "2", errorMessage);
						ProcessData(data, "2");
						logger.info("Finished sending for messageId=" + data.getMessageId());
					} else {
//						db.saveMTFSMResponse(data, false, "2", errorMessage);
						data.setMessagePart((short)(data.getMessagePart() + 1));
						SmsGateWay.getMtfSMQueue().add(data);
						logger.info("Continue sending for messageId=" + data.getMessageId());
					}
					logger.debug("MT Queue: " + MTFSMQueue.size());
					break;
				case 3:
					data = RDSRequestQueue.remove(reportSMDSResp.getMAPDialog().getLocalDialogId());
					if (data != null) {
						SmsGateWay.processReportSMDSResponse(data, null, null);
						logger.info(
								"received rds response for messageId=" + data.getMessageId());
					} else
						logger.error("RDSRequestQueue. Data not found! DialogId - "
								+ reportSMDSResp.getMAPDialog().getLocalDialogId());
					break;
				case 4:
					SmsGateWay.checkAlertWaitingList(Long.valueOf(alertServiceCentreReq.getMsisdn().getAddress()));
					break;
				case 5:
					//in production
//					if(mapDialog.getRemoteAddress().getSubsystemNumber() == 6) {//HLR
//					} else {
//					}
					data = MTFSMQueue.remove(mapDialog.getLocalDialogId());
					if (data == null) {
						data = SRIRequestQueue.remove(mapDialog.getLocalDialogId());
						if(data == null) {
							data = RDSRequestQueue.remove(mapDialog.getLocalDialogId());
							if(data == null) {
								logger.warn("Process error. Data Not Found!!! dialog - " + mapDialog.getLocalDialogId());
								return;
							}
							else {
								SmsGateWay.processReportSMDSResponse(data, state, errorMessage);
								rdsCounterBadRes.incrementAndGet();
							}
						} else {
							sriCounterBadRes.incrementAndGet();
							SmsGateWay.processSRISMResponse(data, state, errorMessage, false);
							ProcessData(data, state);
						}
					} else {
						mtfCounterBadRes.incrementAndGet();
						SmsGateWay.processMTFSMResponse(data, data.getQuantity() == (data.getMessagePart() + 1), state, errorMessage);
						//ignore 31. resend until subscriber not be able to receive sms or error.
						//ONLY FOR MTFSM CASE
						if(!"31".equals(state)) {
							ProcessData(data, state);	
						}
					}
					mapDialog.release();
					break;
				case 6:
					try {
						SmsSignalInfo smsSignalInfo = moForwSmInd.getSM_RP_UI();
						String msisdnFrom = moForwSmInd.getSM_RP_OA().getMsisdn().getAddress();
						Long msisdnTo = Long.valueOf(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getAddress());
						byte[] message = null; short dcs = 0; short pid = 0; CharacterSet cs = CharacterSet.GSM7;
						UserData userData = null;
						SmsTpdu smsTpdu = smsSignalInfo.decodeTpdu(true);
						// SRISMQueue data = MSISDNInProgress.remove(msisdnFrom);
						switch (smsTpdu.getSmsTpduType()) {
						case SMS_SUBMIT:
							SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
							logger.info("Received SMS_SUBMIT = " + smsSubmitTpdu);
							userData = smsSubmitTpdu.getUserData();
							smsSubmitTpdu.getUserDataLength();
							// userData.decode();
							message = userData.getEncodedData();
							dcs = (short)userData.getDataCodingScheme().getCode();
							cs = userData.getDataCodingScheme().getCharacterSet();
							pid = (short)smsSubmitTpdu.getProtocolIdentifier().getCode();
							break;
						case SMS_DELIVER:
							SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
							logger.info("Received SMS_DELIVER = " + smsDeliverTpdu);
							userData = smsDeliverTpdu.getUserData();
							message = smsDeliverTpdu.getUserData().getEncodedData();
							dcs = (short)userData.getDataCodingScheme().getCode();
							cs = userData.getDataCodingScheme().getCharacterSet();
							pid = (short)smsDeliverTpdu.getProtocolIdentifier().getCode();
							break;
						case SMS_SUBMIT_REPORT:
							SmsSubmitReportTpdu smsSubmitReportTpdu = (SmsSubmitReportTpdu) smsTpdu;
							message = smsSubmitReportTpdu.getUserData().getEncodedData();
							userData = smsSubmitReportTpdu.getUserData();
							dcs = (short)userData.getDataCodingScheme().getCode();
							cs = userData.getDataCodingScheme().getCharacterSet();
							pid = (short)smsSubmitReportTpdu.getProtocolIdentifier().getCode();
						default:
							logger.info("Received DEFAULT = " + smsTpdu);
							break;
						}
						if(message != null) {
							Charset charset = cs.name().equals(CharacterSet.GSM7) ? StandardCharsets.US_ASCII
									: cs.name().equals(CharacterSet.GSM8) ? StandardCharsets.UTF_8
											: StandardCharsets.UTF_16;
							String messageText = new String(message, charset);
							
							moForwSmInd.getSM_RP_OA().getMsisdn().getAddress();
							int sysId = 0;
							Optional<MoRoutingRules> oRule = SmsGateWay.getMoRoutingRules()
									.stream().filter(rule -> 
										rule.getAddress().equals(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getAddress())).findFirst();
							if(oRule.isPresent())
								sysId = oRule.get().getAccessId();
							MOIncoming log = new MOIncoming(SmsGateWay.getNextMessageId(), sysId
									, moForwSmInd.getSM_RP_OA().getMsisdn().getAddress()
									, String.valueOf(moForwSmInd.getSM_RP_OA().getMsisdn().getAddressNature().ordinal())
									, String.valueOf(moForwSmInd.getSM_RP_OA().getMsisdn().getNumberingPlan().ordinal())
									, Long.valueOf(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getAddress())
									, String.valueOf(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getAddressNature().ordinal())
									, String.valueOf(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getNumberingPlan().ordinal())
									, messageText, dcs, pid, new Timestamp(System.currentTimeMillis()));
							SmsGateWay.saveIncomingMOLog(log);
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					break;
				default:
					logger.warn("unsupported action type - " + type);
					break;
				}
			} catch (Exception e) {
				logger.error("handled error: " + e.getMessage());
			}
		}
	}

	private void ProcessData(SendData data, String state) {
		LinkedList<SendData> temp = new LinkedList<>();
		if(MSISDNInProgress.remove(data.getToAD()) != null)
			logger.info("MSISDNInProgress removed for messageId=" + data.getMessageId());
		else
			logger.warn("Couldn't clear MSISDNInProgress for messageId=" + data.getMessageId());
		MSISDNQueue.stream().filter(repeatedValue -> repeatedValue.getToAD().equals(data.getToAD()))
			.forEach(nextSMSInQueue -> temp.add(nextSMSInQueue));
		if(!temp.isEmpty()) {
			logger.info("Waiting List - " + temp.size());
			//if successful send or error during preparation to send. try to send next sms in queue.
			if("2".equals(state) || "100".equals(state)) {
				SendData nextSms = temp.poll();
	 			if(MSISDNQueue.remove(nextSms)) {	//removing next sms in queue
	 				 SmsGateWay.getSriSMQueue().add(nextSms);
	 				logger.info("Next sms inprogress for - " + data.getToAD());
	 			} else {
	 				logger.warn("Couldn't remove from MSISDNQueue. messageId=" + data.getMessageId());
	 			}
			} else {
				temp.forEach(nextSMS -> {	// removing all sms in queue 
					logger.info(nextSMS.toString());
					if(MSISDNQueue.remove(nextSMS)) {
						nextSMS.setDialogId(-1L);
						SmsGateWay.processSRISMResponse(nextSMS, state, "Clear SriSM Queue", false);	
					} else {
						logger.warn("Couldn't remove from MSISDNQueue. messageId=" + data.getMessageId());	
					}
				});
			}
		}
	}
	
	public void CheckMTFInprogressQueue(long millisecond) {
		MTFSMQueue.values().stream()
				.filter(data -> data.getStarted().after(new Timestamp(System.currentTimeMillis() - millisecond)))
				.forEach(bad -> {
					MAPDialog mapDialog = mapProvider.getMAPDialog(bad.getDialogId());
					if (mapDialog != null) {
						dbWorker.submit(new DBWriter(mapDialog, "115", "TimeOut"));
					} else 
						logger.warn("Dialog NOT FOUND");
				});
	}

	public void ClearCounter() {
		sriCounterReq = new AtomicLong();
		sriCounterRes = new AtomicLong();
		sriCounterBadRes = new AtomicLong();
		mtfCounterReq = new AtomicLong();
		mtfCounterRes = new AtomicLong();
		mtfCounterBadRes = new AtomicLong();
		rdsCounterReq = new AtomicLong();
		rdsCounterRes = new AtomicLong();
		rdsCounterBadRes = new AtomicLong();
	}
	
	public String getMapLayerQueue(boolean clear, int type) {
		String result = null;
		 switch (type) {
		 case 1:
			 result = String.format("SRISM: %d , MTFSM: %d , RDS: %d"
					 , SRIRequestQueue.size()
					 , MTFSMQueue.size()
					 , RDSRequestQueue.size());
		 break;
		 default:
			 result = "SriSM Size - " + SRIRequestQueue.size()
			 + ". MtfSM Size - " + MTFSMQueue.size()
			 + ". ReportSMDS Size - " + RDSRequestQueue.size()
			 + "\nTotal:\n" 
			 + "SriSM " + sriCounterReq.get() + "/ OK - " + sriCounterRes.get() + "/ BAD - " + sriCounterBadRes.get()
			 + "\nMtfSM " + mtfCounterReq.get() + "/ OK - " + mtfCounterRes.get() + "/ BAD - " + mtfCounterBadRes.get()
			 + "\nRdsSM " + rdsCounterReq.get() + "/ OK - " + rdsCounterRes.get() + "/ BAD - " + rdsCounterBadRes.get();
		 break;
		 }
//		 if(full){
//		 temp = "MSISDNQueue:\n";
//		 for (SRISMQueue inBox : MSISDNQueue) {
//		 temp += inBox.getMsisdn() + ", ";
//		 }
//		 temp += "\nMSISDNInProgress:\n";
//		 for (String msisdn : MSISDNInProgress.values()) {
//		 temp += inBox.getMsisdn() + ", ";
//		 }
//		 }
//		 = String.format("%s. \nSRIQueue = %d; MTFSMQueue = %d, RDSQueue = %d,
//		 ContinatedSMData = %d \n"
//		 + "MSISDNInProgress = %d, MSISDNQueue = %d\n%s"
//		 , dialogs
//		 , SRIRequestQueue.size(), MTFSMQueue.size(), RDSRequestQueue.size(),
//		 ContinatedSMData.size()
//		 , MSISDNInProgress.size(), MSISDNQueue.size(), temp
//		 );
		 if(clear) {//TODO: save to DATABASE
		 SRIRequestQueue.clear();
		 MTFSMQueue.clear();
		 RDSRequestQueue.clear();
		 MSISDNInProgress.clear();
		 MSISDNQueue.clear();
		 }
		return result;
	}

	private MAPApplicationContext getShortMsgMAPApplicationContext() {
		if (this.shortMsgMAPApplicationContext == null) {
			this.shortMsgMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version3);
		}
		return shortMsgMAPApplicationContext;
	}

	private MAPApplicationContext getMtFoSMSMAPApplicationContext() {
		if (this.mtForSMSMAPApplicationContext == null) {
			this.mtForSMSMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.shortMsgMTRelayContext, MAPApplicationContextVersion.version3);
		}
		return this.mtForSMSMAPApplicationContext;
	}

	private SM_RP_OA getServiceCentreAddressOA() {
		return this.mapProvider.getMAPParameterFactory()
					.createSM_RP_OA_ServiceCentreAddressOA(this.getServiceCenterAddressString());
	}
	
	/**
	 * This is our own number. We are Service Center.
	 *
	 * @return
	 */
	private AddressString getServiceCenterAddressString() {
		Map<String,String> mapSettings = appSettings.get("map");
//		return this.mapProvider.getMAPParameterFactory().createAddressString(
//					AddressNature.valueOf(cfg.getMap().getAddressNature()),
//					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan
//							.valueOf(cfg.getMap().getNumberingPlan()),
//					cfg.getMap().getSc());
		return this.mapProvider.getMAPParameterFactory().createAddressString(
				AddressNature.valueOf(mapSettings.get("gtAddressStringAN")),
				NumberingPlan.valueOf(mapSettings.get("gtAddressNP")),
				mapSettings.get("serviceCenter"));
	}
	
	protected SccpAddress genSccpAddress(String msisdn, boolean isCallingParty) throws Exception {        
		GlobalTitle gtG = null;		
		Map<String,String> mapSettings = appSettings.get("map");
		int dpc=0, ssn=0;
		if (isCallingParty){
			dpc = Integer.parseInt(mapSettings.get("opc"));
			ssn = Integer.parseInt(mapSettings.get("opcssn"));
		}
		else{
			ssn = Integer.parseInt(mapSettings.get("dpcssn"));			
			//TODO investigate the case with forwarding associations 
			dpc = getAvailableDPC();
			if (dpc == -1)
				throw new Exception("All routed remote DPC is in pause state.");
			if (mapSettings.containsKey("dstServiceCenter")){
				msisdn = mapSettings.get("dstServiceCenter");
			}
		}		
		if(msisdn == null){
			msisdn = mapSettings.get("serviceCenter");
		}
		switch (mapSettings.get("gtType")) {
		case "GT0001":
			gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")));
			break;
		case "GT0010":
			gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")));
			break;
		case "GT0011":
			gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")),
					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")),
					null);
			break;
		case "GT0100":
			gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")),
					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")),
					null, NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")));
			break;
		}
		if (gtG == null) {
			throw new Exception("[MAP] GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
		}
		//
		//AddressIndicator aiObj = new AddressIndicator((byte) Integer.parseInt(mapSettings.get("addressIndicator")), SccpProtocolVersion.ITU);
		//SccpAddress sccpAddress = new SccpAddress(RoutingIndicator.valueOf(mapSettings.get("routingIndicator")), dpc, gtG, ssn);
		SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(mapSettings.get("routingIndicator")), gtG, dpc, ssn);
		

		return sccpAddress;
	}
	
	private int getAvailableDPC(){
		//ConcurrentHashMap<Long, String> mtpstatus =  ((SccpUnifunStackWrapper)this.sccpStack).getMtpstatus();
		//if ("server".equalsIgnoreCase(cfg.getType())){
		//java.util.Map<String, As[]> routes = new HashMap<>();
		java.util.Map<String,  RouteAs> routes = new HashMap<>();
		this.sccpStack.getMtp3UserParts().forEach((id, mtpUserPart)->{
			if (mtpUserPart instanceof M3UAManagement){
				routes.putAll(((M3UAManagement)mtpUserPart).getRoute());
			}
		});	
		if(routes.size()>0){
			//Check dpc from settings
			for (String route : routes.keySet()){
				String rdpc = route.split(":")[0];
				if (rdpc.equals(appSettings.get("map").get("dpc"))){
					//As[] associations = routes.get(route);
					As[] associations = routes.get(route).getAsArray();
					for (As assoc : associations){
						if (assoc.isUp()){
							return Integer.parseInt(appSettings.get("map").get("dpc"));
						}
					}
					break;
				}
			}
			//loop to find another Available PC in case that the main is down.
			for (String route : routes.keySet()){
				String rdpc = route.split(":")[0];				
				As[] associations = routes.get(route).getAsArray();
				for (As assoc : associations){
					if (assoc.isUp()){
						return Integer.parseInt(rdpc);
					}
				}			
			}
		}
		return -1;
	}
	
	void sendSRISMRequest(SendData data) {
		MAPDialogSms mapDialog = null;
		String errorMessage = null; 
		try {
			Timestamp temp = MSISDNInProgress.putIfAbsent(data.getToAD(), new Timestamp(System.currentTimeMillis()));
            if (temp != null) {
            	MSISDNQueue.add(data);
            	logger.info("Increasing MSISDNQueue - " + data.toString());
            } else {
            	SccpAddress origAddr = genSccpAddress(null,true);
//    			SccpAddress destAddr = genSccpAddress("99366399112", false); //for test
            	SccpAddress destAddr = genSccpAddress(String.valueOf(data.getToAD()), false);
    			mapDialog = this.mapProvider.getMAPServiceSms().createNewDialog(getShortMsgMAPApplicationContext(),
    					origAddr, null, destAddr, null);
    			logger.info("passed");
    			ISDNAddressString msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
    					AddressNature.getInstance(Integer.valueOf(data.getToAN())),
    					NumberingPlan.getInstance(Integer.valueOf(data.getToNP())), String.valueOf(data.getToAD()));
    			mapDialog.addSendRoutingInfoForSMRequest(msisdn, true, getServiceCenterAddressString(), null, true,
    					SM_RP_MTI.SMS_Deliver, null, null);
    			data.setDialogId(mapDialog.getLocalDialogId());
    			SRIRequestQueue.putIfAbsent(mapDialog.getLocalDialogId(), data);
    			mapDialog.send();
    			sriCounterReq.incrementAndGet();
    			logger.info(
    					"srism sended for dialog - " + mapDialog.getLocalDialogId() + "; messageId=" + data.getMessageId());            	
            }
		} catch (MAPException m) {
			logger.error(m.getStackTrace());
			errorMessage = "Sending map message catched an error: " + m.getMessage();
		} catch (Exception e) {
			logger.error(e.getStackTrace());
			errorMessage = e.getMessage();
		}
		if (errorMessage != null) {
			logger.error(errorMessage);
			if(mapDialog != null)
				SRIRequestQueue.remove(mapDialog.getLocalDialogId());
			data.setDialogId(-1);
			SmsGateWay.processSRISMResponse(data, "100", errorMessage, false);
			ProcessData(data, "100");
		}
	}

	void sendMTFSMRequest(SendData data) {
		MAPDialogSms mapDialogSms = null;
		String errorMessage = null;
		boolean canDeliver = true;  
		try {
			boolean createNewDialog = true;
			if (data.getDialogId() != null) {
				if(mapProvider.getMAPDialog(data.getDialogId()) != null) {
					if(mapProvider.getMAPDialog(data.getDialogId()).getTCAPMessageType().getTag() == 0x65) {
						logger.info("Dialog found - " + data.getDialogId());
						mapDialogSms = (MAPDialogSmsImpl) mapProvider.getMAPDialog(data.getDialogId());
						createNewDialog = false;
					}
				}
			}
			if(createNewDialog) {
				mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(getMtFoSMSMAPApplicationContext(),
						this.genSccpAddress(null, true), null, this.genSccpAddress(data.getNetworkNodeNumber().getAddress(), false),
						null);
			}
			SM_RP_DA sm_RP_DA = this.mapProvider.getMAPParameterFactory().createSM_RP_DA(data.getImsi());
			AddressField oAddress = new AddressFieldImpl(TypeOfNumber.getInstance(Integer.valueOf(data.getFromTON())),
					NumberingPlanIdentification.getInstance(Integer.valueOf(data.getFromNP())), data.getFromAD());
			Calendar cal = Calendar.getInstance();
			AbsoluteTimeStampImpl serviceCentreTimeStamp = new AbsoluteTimeStampImpl((cal.get(Calendar.YEAR) % 100),
					(cal.get(Calendar.MONTH)), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
					cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.ZONE_OFFSET));
			boolean moreMessagesToSend = false;
			DataCodingScheme dcs = new DataCodingSchemeImpl(data.getDcs());
			Charset cs = "GSM8".equals(dcs.getCharacterSet().name()) ? Charset.forName("UTF-8") : null;
			UserData ud = null;
			if(data.getQuantity() < 1) {
				throw new Exception("Wrong sms Quantity. SmsData - " + data.toString());	
			} // for ICB
        	if (data.getQuantity() > 1) {
				logger.debug("##### message is concatenated #####");
				UserDataHeader udh = new UserDataHeaderImpl();
				UserDataHeaderElement dataHeaderElement;
				dataHeaderElement = new ConcatenatedShortMessagesIdentifierImpl(false,
						(int) (data.getMessageId() % 256), data.getQuantity(), data.getMessagePart() + 1);
				udh.addInformationElement(dataHeaderElement);
				logger.debug("### " + udh.toString());
				int startIndex = data.getMessagePart() * Integer.valueOf(data.getSegmentLen());
				int lastIndex = startIndex + Integer.valueOf(data.getSegmentLen());
				String messageToSend = data.getMessage().substring(startIndex
						, ( lastIndex > data.getMessage().length()) ? data.getMessage().length() : lastIndex);
					ud = new UserDataImpl(messageToSend , dcs, udh, cs);
					moreMessagesToSend = !(data.getQuantity() == (data.getMessagePart() + 1));	
//						}
			} else {
				ud = new UserDataImpl(data.getMessage(), dcs, null, cs);	
			}
			// 64 Short Message Type 0 (GSM_03.40), 127 - (U)SIM Data download		
			SmsDeliverTpduImpl smsDeliverTpdu = new SmsDeliverTpduImpl(moreMessagesToSend, false, false, true, oAddress,
					new ProtocolIdentifierImpl(data.getPid()), serviceCentreTimeStamp, ud);
			SmsSignalInfo smsSignalInfo = new SmsSignalInfoImpl(smsDeliverTpdu, null);
			mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, getServiceCentreAddressOA(), smsSignalInfo, false,
					null);
			data.setDialogId(mapDialogSms.getLocalDialogId());
			data.setStarted(new Timestamp(System.currentTimeMillis()));
			MTFSMQueue.putIfAbsent(mapDialogSms.getLocalDialogId(), data);
			logger.info(String.format("MessageUserDataLengthOnSend = %d, MaxUserDataLength = %d", mapDialogSms.getMessageUserDataLengthOnSend(), mapDialogSms.getMaxUserDataLength()));
			if(mapDialogSms.getMessageUserDataLengthOnSend() > mapDialogSms.getMaxUserDataLength()) {
				canDeliver = false;
				throw new MAPException("UserDataLength too long. UDL - " + mapDialogSms.getMessageUserDataLengthOnSend() 
										+ "; Max Size - " + mapDialogSms.getMaxUserDataLength());
			}
			mapDialogSms.send();
			mtfCounterReq.incrementAndGet();
			logger.debug(
					"mtfsm sended for dialog - " + mapDialogSms.getLocalDialogId() + "; messageId=" + data.getMessageId() //239
					+ "; quantity=" + data.getQuantity() + "; messagePart=" + (data.getMessagePart() + 1));       
		} catch (MAPException e) {
			errorMessage = "MAPException on sending MTFSMRequest. Error Message - " + e.getMessage();
		} catch (Exception e) {
			errorMessage = "Exception on processing MTFSMRequest. Error Message - " + e.getMessage();
		}
		if (errorMessage != null) {
			logger.error("For data - " + data + ". Error message - " + errorMessage);
			if(mapDialogSms != null)
				MTFSMQueue.remove(mapDialogSms.getLocalDialogId());
			data.setDialogId(-1);
			if(canDeliver) {
				SmsGateWay.processMTFSMResponse(data, data.getQuantity() == data.getMessagePart(), "100", errorMessage);	
			} else {
				SmsGateWay.processMTFSMResponse(data, data.getQuantity() == data.getMessagePart(), "5", errorMessage);
			}
			ProcessData(data, "100");
		}
	}

	void sendRDS(SendData data) {
		MAPDialogSms mapDialogSms = null;
		String errorMessage = null;
		try {
        	SccpAddress origAddr = genSccpAddress(null, true);
//			SccpAddress destAddr = genSccpAddress("99366399112", false); //for test
        	SccpAddress destAddr = genSccpAddress(String.valueOf(data.getToAD()), false);
			mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(getShortMsgMAPApplicationContext(),
					origAddr, null, destAddr, null);
			ISDNAddressString calledPartyAddress = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
					AddressNature.getInstance(Integer.valueOf(data.getToAN())),
					NumberingPlan.getInstance(Integer.valueOf(data.getToNP())), String.valueOf(data.getToAD()));
			mapDialogSms.addReportSMDeliveryStatusRequest(calledPartyAddress, getServiceCenterAddressString(),
					data.getSmDeliveryOutcome(), null, null, false, true, null, null);
			data.setDialogId(mapDialogSms.getLocalDialogId());
			RDSRequestQueue.putIfAbsent(mapDialogSms.getLocalDialogId(), data);
			mapDialogSms.send();
			rdsCounterReq.incrementAndGet();
			logger.info("sendRDS sended for messageId=" + data.getMessageId());
		} catch (MAPException m) {
			errorMessage = "MAPException, Error Message:" + m.getMessage();
		} catch (Exception e) {
			errorMessage = "Exception, Error Message: " + e.getMessage();
		}
		if (errorMessage != null) {
			 logger.error(errorMessage);
			 if(mapDialogSms != null)
				 RDSRequestQueue.remove(mapDialogSms.getLocalDialogId());
			 data.setDialogId(-1);
			SmsGateWay.processReportSMDSResponse(data, "100",errorMessage);
		}
	}
	

	@Override
	public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
		logger.info("[onErrorComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [MAPErrorMessage]: "
				+ mapErrorMessage);
		// logger.info("Error code: " + mapErrorMessage.getErrorCode());
		// SRISMQueue srismQueue; MTFSMQueue mtfsmQueue; ReportSMDSQueue
		// rSMDSQueue;
		String error = null;
		String state = "7";
		String errorCode = (mapErrorMessage.getErrorCode() == null) ? "0"
				: String.valueOf(mapErrorMessage.getErrorCode());
		error = mapErrorMessage.toString();
		switch (errorCode) {
		case "1":
		case "5":
		case "9":
			state = "8";
			break;
		case "6":
			state = "6";
			break;
		case "8":
		case "10":
		case "11":
			state = "5";
			break;
		case "31":
			state = "31";
			break;
		case "32":
			SMEnumeratedDeliveryFailureCause cause = mapErrorMessage.getEmSMDeliveryFailure()
					.getSMEnumeratedDeliveryFailureCause();
			if (cause != null) {
				error += ". DeliveryFailureCause - " + cause.name();
				state = (cause.getCode() == 0) ? "10" : "32";
			} else
				state = "7";
			break;
		case "33":
			state = "33";
			break;
		default:
			break;
		}
		logger.debug(mapDialog.getRemoteAddress());
		dbWorker.submit(new DBWriter(mapDialog, state, error));
		mapDialog.release();
		
	}

	@Override
	public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
		logger.info("[onRejectComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [Problem]: " + problem
				+ " [isLocalOriginated]: " + isLocalOriginated);
		dbWorker.submit(new DBWriter(mapDialog, "114",
				"[Problem]: " + problem + " [isLocalOriginated]: " + isLocalOriginated));
		
	}

	@Override
	public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
		logger.info("[onInvokeTimeout]: " + mapDialog + " [invokeId]: " + invokeId);
		dbWorker.submit(new DBWriter(mapDialog, "115", "onInvokeTimeout"));
		
	}

	@Override
	public void onMAPMessage(MAPMessage mapMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onForwardShortMessageRequest(ForwardShortMessageRequest forwSmInd) {
		logger.info("[onForwardShortMessageRequest]: " + forwSmInd);
	}

	@Override
	public void onForwardShortMessageResponse(ForwardShortMessageResponse forwSmRespInd) {
		logger.info("[onForwardShortMessageResponse]: " + forwSmRespInd);
		logger.info("[TCAPMessageType]: " + forwSmRespInd.getMAPDialog().getTCAPMessageType());
	}

	@Override
	public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwSmInd) {
		logger.info("[onMoForwardShortMessageRequest]: " + moForwSmInd);
		String errorMessage = null;
		String msisdnFrom = null;
		Long msisdnTo = null;
		try {
			SmsSignalInfo smsSignalInfo = moForwSmInd.getSM_RP_UI();
			msisdnFrom = moForwSmInd.getSM_RP_OA().getMsisdn().getAddress();
			msisdnTo = Long.valueOf(moForwSmInd.getSM_RP_DA().getServiceCentreAddressDA().getAddress());
			String message = null;
			SmsTpdu smsTpdu = smsSignalInfo.decodeTpdu(true);
			// SRISMQueue data = MSISDNInProgress.remove(msisdnFrom);
			switch (smsTpdu.getSmsTpduType()) {
			case SMS_SUBMIT:
				SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
				logger.info("Received SMS_SUBMIT = " + smsSubmitTpdu);
				UserData userData = smsSubmitTpdu.getUserData();
				smsSubmitTpdu.getUserDataLength();
				// userData.decode();
				message = userData.getDecodedMessage();

				break;
			case SMS_DELIVER:
				SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
				logger.info("Received SMS_DELIVER = " + smsDeliverTpdu);
				message = smsDeliverTpdu.getUserData().getDecodedMessage();
				break;
			case SMS_SUBMIT_REPORT:
				SmsSubmitReportTpdu smsSubmitReportTpdu = (SmsSubmitReportTpdu) smsTpdu;
				message = smsSubmitReportTpdu.getUserData().getDecodedMessage();
			default:
				logger.info("Received DEFAULT = " + smsTpdu);
				break;
			}
			dbWorker.submit(new DBWriter(moForwSmInd));
			moForwSmInd.getMAPDialog().addMoForwardShortMessageResponse(moForwSmInd.getInvokeId(), moForwSmInd.getSM_RP_UI(), moForwSmInd.getExtensionContainer());
			moForwSmInd.getMAPDialog().close(false);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			logger.error(e);
		}
	}

	@Override
	public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
		logger.info("[onMoForwardShortMessageResponse]: " + moForwSmRespInd);
		
	}

	@Override
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
		logger.info("[onMtForwardShortMessageRequest]: " + mtForwSmInd);
		MAPDialogSms mapDialogSms = mtForwSmInd.getMAPDialog();
		MAPExtensionContainer extensionContainer = null;
		try {
			mapDialogSms.addMtForwardShortMessageResponse(mtForwSmInd.getInvokeId(), mtForwSmInd.getSM_RP_UI(),
					extensionContainer);
			if (mtForwSmInd.getMoreMessagesToSend())
				mapDialogSms.send();
			else
				mapDialogSms.close(false);
		} catch (MAPException e) {
			logger.error("MAP addMtForwardShortMessageResponse MapException: " + e.getStackTrace());
		}		
	}

	@Override
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwSmRespInd) {
		logger.info("[onMtForwardShortMessageResponse]: " + mtForwSmRespInd);
		dbWorker.submit(new DBWriter(mtForwSmRespInd));
		mtfCounterRes.incrementAndGet();
		
	}

	@Override
	public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
		logger.info("[onSendRoutingInfoForSMRequest]: " + sendRoutingInfoForSMInd);
	}

	@Override
	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMRespInd) {
		logger.info("[onSendRoutingInfoForSMResponse]: " + sendRoutingInfoForSMRespInd);
		dbWorker.submit(new DBWriter(sendRoutingInfoForSMRespInd));
		sriCounterRes.incrementAndGet();
		
	}

	@Override
	public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
		logger.info("[onReportSMDeliveryStatusRequest]: " + reportSMDeliveryStatusInd);
		MAPDialogSms mapDialogSms = reportSMDeliveryStatusInd.getMAPDialog();
		MAPExtensionContainer extensionContainer = null;
		try {
			mapDialogSms.addReportSMDeliveryStatusResponse(reportSMDeliveryStatusInd.getInvokeId(),
					reportSMDeliveryStatusInd.getMsisdn(), extensionContainer);
			mapDialogSms.send();
		} catch (MAPException e) {
			logger.error("Error while trying to send ReportSMDeliveryStatusResponse", e);
		}
	}

	@Override
	public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse reportSMDeliveryStatusRespInd) {
		logger.info("[onReportSMDeliveryStatusResponse]: " + reportSMDeliveryStatusRespInd);
		dbWorker.submit(new DBWriter(reportSMDeliveryStatusRespInd));
		rdsCounterRes.incrementAndGet();
		reportSMDeliveryStatusRespInd.getMAPDialog().release();
	}

	@Override
	public void onInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreInd) {
		logger.info("[onInformServiceCentreRequest]: " + informServiceCentreInd);
	}

	@Override
	public void onAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreInd) {
		logger.debug("[onAlertServiceCentreRequest]: " + alertServiceCentreInd);
		String errorMessage = null;
		try {
			if (alertServiceCentreInd.getMsisdn() != null) {
				logger.info("Processing alert message for - " + alertServiceCentreInd.getMsisdn().getAddress());
				dbWorker.submit(new DBWriter(alertServiceCentreInd));
				MAPDialogSms dialogSms = alertServiceCentreInd.getMAPDialog();
				dialogSms.addAlertServiceCentreResponse(alertServiceCentreInd.getInvokeId());
				dialogSms.close(false);
			} else
				errorMessage = "MSISDN info Is NULL";
		} catch (MAPException e) {
			errorMessage = "[onAlertServiceCentreRequest] Sending response" + e.getMessage();
		} catch (Exception e) {
			errorMessage = "[onAlertServiceCentreRequest] Processing request. Error Message - " + e.getMessage();
		} finally {
			if (errorMessage != null)
				logger.warn(errorMessage);
		}
	}

	@Override
	public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
		logger.debug("[onAlertServiceCentreResponse]: " + alertServiceCentreInd);
	}

	@Override
	public void onDialogDelimiter(MAPDialog mapDialog) {
		logger.debug("[onDialogDelimiter]: " + mapDialog);
	}

	@Override
	public void onDialogRequest(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
			MAPExtensionContainer extensionContainer) {
		logger.debug("[onDialogRequest]: " + mapDialog + " [AddressString]: " + destReference + " [AddressString]: "
				+ origReference + " [MAPExtensionContainer]: " + extensionContainer);
		
	}

	@Override
	public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
			IMSI eriImsi, AddressString eriVlrNo) {
		logger.info("[onDialogRequestEricsson]: " + mapDialog + " [AddressString]: " + destReference
				+ " [AddressString]: " + origReference + " [IMSI]: " + eriImsi + " [AddressString]: " + eriVlrNo);
	}

	@Override
	public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
		logger.debug("[onDialogAccept]: " + mapDialog + " [MAPExtensionContainer]: " + extensionContainer);		
	}

	@Override
	public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason refuseReason,
			ApplicationContextName alternativeApplicationContext, MAPExtensionContainer extensionContainer) {
		logger.debug(
				"[onDialogReject]: " + mapDialog + " [MAPRefuseReason]: " + refuseReason + " [ApplicationContextName]: "
						+ alternativeApplicationContext + " [MAPExtensionContainer]: " + extensionContainer);
		dbWorker.submit(new DBWriter(mapDialog, "110",
				" [MAPRefuseReason]: " + refuseReason + " [ApplicationContextName]: " + alternativeApplicationContext
						+ " [MAPExtensionContainer]: " + extensionContainer));
	}

	@Override
	public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice userReason,
			MAPExtensionContainer extensionContainer) {
		logger.debug("[onDialogUserAbort]: " + mapDialog + " [MAPUserAbortChoice]: " + userReason
				+ " [MAPExtensionContainer]: " + extensionContainer);
		dbWorker.submit(new DBWriter(mapDialog, "111",
				" [MAPUserAbortChoice]: " + userReason + " [MAPExtensionContainer]: " + extensionContainer));
		
	}

	@Override
	public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason abortProviderReason,
			MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
		logger.debug("[onDialogProviderAbort]: " + mapDialog + " [MAPAbortProviderReason]: " + abortProviderReason
				+ " [MAPAbortSource]: " + abortSource + " [MAPExtensionContainer]: " + extensionContainer);
		dbWorker.submit(
				new DBWriter(mapDialog, "113", "[MAPAbortProviderReason]: " + abortProviderReason
						+ " [MAPAbortSource]: " + abortSource + " [MAPExtensionContainer]: " + extensionContainer));
	}

	@Override
	public void onDialogClose(MAPDialog mapDialog) {
		logger.debug("[onDialogClose]: " + mapDialog);
		mapDialog.release();
	}

	@Override
	public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
		logger.debug("[onDialogNotice]: " + mapDialog + "[ MAPNoticeProblemDiagnostic ]: " + noticeProblemDiagnostic);		
	}

	@Override
	public void onDialogRelease(MAPDialog mapDialog) {
		logger.debug("[onDialogRelease]: " + mapDialog);
		
	}

	@Override
	public void onDialogTimeout(MAPDialog mapDialog) {
		logger.info("[onDialogTimeout]: " + mapDialog);
		dbWorker.submit(new DBWriter(mapDialog, "116", "onDialogTimeout"));
	}

	public void setDbWorker(ExecutorService dbWorker) {
		this.dbWorker = dbWorker;
	}


	@Override
	public void onReadyForSMRequest(ReadyForSMRequest request) {
		logger.info("[onReadyForSMRequest]: " + request);
	}


	@Override
	public void onReadyForSMResponse(ReadyForSMResponse response) {
		logger.info("[onReadyForSMResponse]: " + response);
	}


	@Override
	public void onNoteSubscriberPresentRequest(NoteSubscriberPresentRequest request) {
		logger.info("[onNoteSubscriberPresentRequest]: " + request);
	}

	public Map<String, Map<String, String>> getAppSettings() {
		return appSettings;
	}

	public void setAppSettings(Map<String, Map<String, String>> appSettings) {
		this.appSettings = appSettings;
	}

	
	
}
