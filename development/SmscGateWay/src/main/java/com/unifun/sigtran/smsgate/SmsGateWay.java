package com.unifun.sigtran.smsgate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;

import com.unifun.sigtran.smsgate.enums.AccessType;
import com.unifun.sigtran.smsgate.enums.Direction;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.hibernate.models.AlertWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.ClientDLRWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProvider;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccess;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccessCharsetList;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProvidersAccessesRestrictions;
import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.hibernate.models.DataCodingList;
import com.unifun.sigtran.smsgate.hibernate.models.GateWaySettings;
import com.unifun.sigtran.smsgate.hibernate.models.MOIncoming;
import com.unifun.sigtran.smsgate.hibernate.models.MoRoutingRules;
import com.unifun.sigtran.smsgate.hibernate.models.MsisdnBlackList;
import com.unifun.sigtran.smsgate.hibernate.models.NextAttemptWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.NumberRegEx;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientsGroups;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPMoResponse;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPServerConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMSQueue;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.hibernate.models.SourcheSubstitutionList;
import com.unifun.sigtran.smsgate.hibernate.models.TempContentProvideAccessQueueSize;
import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.workers.CheckAlertQueue;
import com.unifun.sigtran.smsgate.workers.CheckContentProviderAccessSmsQueueSize;
import com.unifun.sigtran.smsgate.workers.CheckNextAttemptQueue;
import com.unifun.sigtran.smsgate.workers.GetSMSToSend;
import com.unifun.sigtran.smsgate.workers.MTFSMWorker;
import com.unifun.sigtran.smsgate.workers.MoveFromSMSQueue;
import com.unifun.sigtran.smsgate.workers.ReportSMDSWorker;
import com.unifun.sigtran.smsgate.workers.SRISMWorker;
import com.unifun.sigtran.smsgate.workers.UpdateSystemInfo;

public class SmsGateWay {
		
	private static short threads = 200;
	private short mapWorekrsQty = 1;
	private short getSmsToSendPerSec = 1;
	private short getSmsToSendQuantity = 50;
	private short sendMapMessagesPerSec = 200;
	private short moveFromSMSQueuePerSec = 1;
	private short moveFromSMSQueueQuantity = 100;
	private short checkAlertQueuePerSec = 1;
	private short checkAlertQueueQuantity = 50;
	private short checkNextAttemptQueuePerSec = 1;
	private short checkNextAttemptQueueQuantity = 50;
	private AtomicBoolean mapWorkersRunning = new AtomicBoolean(false);
    private AtomicBoolean dlrWorkersRunning = new AtomicBoolean(false);
    
    private static MapLayer mapLayer;
    private static DataBaseLayer dbl;
    private static AtomicLong messageId = new AtomicLong();
	private ServerController smppServerController;
	private ClientController smppClientController;
	private static long updateConfigInfo = 60000;
	private static long sendDLRUntil = 5 * 60 * 1000;
	private static long maxSchedulerTime = 480 * 60 * 1000;
	private static long nextAttemptToReSend = 5 * 60 * 1000;
	private static long defaultSMSLiveTime = 1440 * 60 * 1000;
	private static long nextAttemptForAlert = 720 * 60 * 1000;
	private static int contentProviderAccessSmsQueueSize = 50000;
	private static String MSCLocalPrefix = "993";
		
	//MAP WORKERS
	private List<SRISMWorker> srismWorkers = new ArrayList<>();
	private List<MTFSMWorker> mtfsmWorkers = new ArrayList<>();
	private List<ReportSMDSWorker> reportSMDSWorkers = new ArrayList<>();
	
	//WORKERS
	private GetSMSToSend wGetSMSToSend;
	private CheckAlertQueue wCheckAlertQueue;
	private MoveFromSMSQueue wMoveFromSMSQueue;
	private CheckNextAttemptQueue wCheckNextAttemptQueue;
	private static UpdateSystemInfo wUpdateSystemInfo;
	private static CheckContentProviderAccessSmsQueueSize wAccessSmsQueueSize;
	//QUEUES
	private static ConcurrentLinkedQueue<SendData> sriSMQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<SendData> mtfSMQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<SendData> rdsSMQueue = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<AlertList> alertLists = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<NextAttempt> nextAttemptList = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<Long> cancelMessageIdList = new ConcurrentLinkedQueue<>();
	
	//INFO for Content Provider CHECK
	private static ConcurrentLinkedQueue<NumberRegEx> regExs = new ConcurrentLinkedQueue<>();
	private static ConcurrentHashMap<Long, MsisdnBlackList> blackLists = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<ContentProvider> contentProviders = new ConcurrentLinkedQueue<>();
	private static ConcurrentHashMap<Integer, ContentProviderAccess> contentProviderAccesses = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<ContentProvidersAccessesRestrictions> accessesRestrictions = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<ContentProviderAccessCharsetList> accessCharsetLists = new ConcurrentLinkedQueue<>();
	private static ConcurrentHashMap<Short, DataCodingList> dataCodingList = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, String> sourcheSubstitutionLists = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<MoRoutingRules> moRoutingRules = new ConcurrentLinkedQueue<>();
	
//	private ConcurrentHashMap<Integer, String> mtpstatus = new ConcurrentHashMap<Integer, String>();
	
	private static final Logger logger = LogManager.getLogger(SmsGateWay.class);
	private static Map<String, Map<String, String>> appSettings;
	
	private static ExecutorService esMain = Executors.newCachedThreadPool();
	
//	public synchronized static SmsGateWay getInstance() {
//		if (instance == null) {
//			instance = new SmsGateWay();
//		}
//		return instance;
//	}
	
	public SmsGateWay(Map<String, Map<String, String>> appSettings, DataBaseLayer dbl) {
		SmsGateWay.appSettings = appSettings;
		SmsGateWay.dbl = dbl;
	}
	
	public void init() {
		Map<String, String> gateWayConfig = appSettings.get("app");
		long lastMessageId = dbl.getLastSMSQueueId();
		if(lastMessageId > 0)
			messageId.set(lastMessageId);
		else if (lastMessageId == 0)
			messageId.set(10000);
		else
			logger.warn(String.format("could not get last messageId {%s}", lastMessageId));
		esMain.execute(() -> {
			dbl.resetMAPQueues();
			});
		//AlertAttempt Queue
		esMain.execute(new LoadAlertAwaitingQueue());
		//NextAttempt Queue
		esMain.execute(new LoadNextAttemptQueue());
		// load configuration 
		loadGateWayConfig(gateWayConfig);
		//load regular expressions
		updateConfigInfo = Integer.valueOf(appSettings.get("app").get("updateConfigInfoInMin")) * 60 * 1000;
		wUpdateSystemInfo = new UpdateSystemInfo(updateConfigInfo);
		wUpdateSystemInfo.start();
//				wAccessSmsQueueSize = new CheckContentProviderAccessSmsQueueSize(updateConfigInfo);
//				exec.execute(wAccessSmsQueueSize);
	}
	
	public void stop() {
		sriSMQueue		.clear();
		mtfSMQueue		.clear();
		rdsSMQueue		.clear();
		alertLists		.clear();
		nextAttemptList .clear();
		
		regExs 					.clear();
		blackLists 				.clear();
		contentProviders 		.clear();
		contentProviderAccesses .clear();
		accessesRestrictions 	.clear();
		accessCharsetLists 		.clear();
		dataCodingList 			.clear();
		sourcheSubstitutionLists.clear();
		moRoutingRules			.clear();
		wUpdateSystemInfo.interrupt();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e.fillInStackTrace());
			e.printStackTrace();
		}
		if(!wUpdateSystemInfo.isInterrupted()) {
			logger.warn("Force shutDown wUpdateSystemInfo");
			wUpdateSystemInfo.stop();
		}
		esMain.shutdownNow();
	}
	
	private static boolean loadGateWayConfig(Map<String, String> gateWayConfig) {
		try {
			Short threads = Short.valueOf(gateWayConfig.get("threads"));
			Integer nextAttemptToReSendInMin = Integer.valueOf(gateWayConfig.get("nextAttemptToReSendInMin"));
			Integer defaultSMSLiveTimeMin = Integer.valueOf(gateWayConfig.get("defaultSMSLiveTimeMin"));
			Integer nextAttemptForAlertMin = Integer.valueOf(gateWayConfig.get("nextAttemptForAlertMin"));
			Integer maxSchedulerTimeInMin = Integer.valueOf(gateWayConfig.get("maxSchedulerTimeInMin"));
			Integer sendDLRUntilInMin = Integer.valueOf(gateWayConfig.get("sendDLRUntilInMin"));
			Integer updateConfigInfoInMin = Integer.valueOf(gateWayConfig.get("updateConfigInfoInMin"));
			String MSCLocalPrefix = gateWayConfig.get("MSCLocalPrefix");
//			Integer contentProviderAccessSmsQueueSize = Integer.valueOf(gateWayConfig.get("contentProviderSmsQueueSize"));
			if(threads != null) {
				SmsGateWay.threads = threads;
			}
			if(nextAttemptToReSendInMin != null) {
				nextAttemptToReSend = nextAttemptToReSendInMin * 60 * 1000;
			}
			if(defaultSMSLiveTimeMin != null) {
				defaultSMSLiveTime = defaultSMSLiveTimeMin * 60 * 1000;
			}
			if(nextAttemptForAlertMin != null) {
				nextAttemptForAlert = nextAttemptForAlertMin * 60 * 1000;
			}
			if(maxSchedulerTimeInMin != null) {
				maxSchedulerTime = maxSchedulerTimeInMin * 60 * 1000;
			}
			if(sendDLRUntilInMin != null) {
				sendDLRUntil = sendDLRUntilInMin * 60 * 1000;
			}
			if(updateConfigInfoInMin != null) {
				updateConfigInfo = updateConfigInfoInMin * 60 * 1000; 
			}
			setMSCLocalPrefix(MSCLocalPrefix);
//			if(contentProviderAccessSmsQueueSize != null) {
//				SmsGateWay.contentProviderAccessSmsQueueSize = contentProviderAccessSmsQueueSize; 
//			}
			return true;
		} catch (Exception e) {
			logger.error("could not load configuration from DB.", e);
			return false;
		}
	}
	
    public static DataBaseLayer getDbl() {
		return dbl;
	}
	    
    public static long getNextMessageId() {
		return messageId.incrementAndGet();
	}
    
    public synchronized void MapWorkers(boolean start) {
		if(start) {//start map workers
			logger.info("starting map workers");
			Map<String, String> mapCfg = appSettings.get("mapWorkers");
			if(mapCfg != null) {
				Short sendPerSecond = Short.valueOf(mapCfg.get("TPSPerSec"));
				if(sendPerSecond != null)
					sendMapMessagesPerSec = sendPerSecond;
//				Short workerQty = Short.valueOf(mapCfg.get("workerQty"));
//				if(workerQty != null)
//					mapWorekrsQty = workerQty;
			} 
			String workerName;
			if(!mapWorkersRunning.get()) { 	//&& !mtpstatus.isEmpty()
				for (short i = 0; i < mapWorekrsQty; i++) {
					workerName = "sriSMWorker-" + i;
					getSrismWorkers().add(new SRISMWorker(sendMapMessagesPerSec, workerName, threads));
					
					workerName = "mtfSMWorker-" + i; 
					getMtfsmWorkers().add(new MTFSMWorker(sendMapMessagesPerSec, workerName, threads));
					
					workerName = "reportSMDSWorker-" + i;
					getReportSMDSWorkers().add(new ReportSMDSWorker(sendMapMessagesPerSec, workerName, threads));	
				}
				mapWorekrsQty = (short)((mapWorekrsQty / 2) + 1);

				for (ReportSMDSWorker worker : getReportSMDSWorkers()) {
					worker.setPriority(Thread.MAX_PRIORITY);
					worker.start();
				}
				for (MTFSMWorker worker : getMtfsmWorkers()) {
					worker.setPriority(Thread.MAX_PRIORITY);
					worker.start();
				}
				for (SRISMWorker worker : getSrismWorkers()) {
					worker.setPriority(Thread.MAX_PRIORITY);
					worker.start();
				}
				//get config for getting sms from smsqueue
				Map<String, String> moveWorkers = appSettings.get("moveFromSMSQueue");
				if(mapCfg != null) {
					Short movePerSec = Short.valueOf(moveWorkers.get("TPSPerSec"));
					if(movePerSec != null)
						moveFromSMSQueuePerSec = movePerSec;
					Short moveQty = Short.valueOf(moveWorkers.get("Quantity"));
					if(moveQty != null)
						moveFromSMSQueueQuantity = moveQty;
				}
				wMoveFromSMSQueue = new MoveFromSMSQueue(moveFromSMSQueuePerSec
	    				, moveFromSMSQueueQuantity);
				//get config for getting sms from alertList
	    		moveWorkers = appSettings.get("checkAlertQueue");
				if(mapCfg != null) {
					Short movePerSec = Short.valueOf(moveWorkers.get("TPSPerSec"));
					if(movePerSec != null)
						checkAlertQueuePerSec = movePerSec;
					Short moveQty = Short.valueOf(moveWorkers.get("Quantity"));
					if(moveQty != null)
						checkAlertQueueQuantity = moveQty;
				}
	    		wCheckAlertQueue = new CheckAlertQueue(checkAlertQueuePerSec
	    				, checkAlertQueueQuantity);
				//get config for getting sms from nextAttempt
	    		moveWorkers = appSettings.get("checkNextAttemptQueue");
				if(mapCfg != null) {
					Short movePerSec = Short.valueOf(moveWorkers.get("TPSPerSec"));
					if(movePerSec != null)
						checkNextAttemptQueuePerSec = movePerSec;
					Short moveQty = Short.valueOf(moveWorkers.get("Quantity"));
					if(moveQty != null)
						checkNextAttemptQueueQuantity = moveQty;
				}
				wCheckNextAttemptQueue = new CheckNextAttemptQueue(checkNextAttemptQueuePerSec
	    				, checkNextAttemptQueueQuantity);
				//get config for getting sms from sms_awating_send
				moveWorkers = appSettings.get("getSMSToSend");
				if(mapCfg != null) {
					Short movePerSec = Short.valueOf(moveWorkers.get("TPSPerSec"));
					if(movePerSec != null)
						getSmsToSendPerSec = movePerSec;
					Short moveQty = Short.valueOf(moveWorkers.get("Quantity"));
					if(moveQty != null)
						getSmsToSendQuantity = moveQty;
				}
	    		wGetSMSToSend = new GetSMSToSend(getSmsToSendQuantity, getSmsToSendPerSec);
	    		//starting threads
	    		wMoveFromSMSQueue		.start();
	    		wCheckAlertQueue		.start();
	    		wCheckNextAttemptQueue	.start();
	    		wGetSMSToSend			.start();
	    		mapWorkersRunning.set(true);
	    		logger.info("map workers started");
			}
		} else {	//stop map workers 
			if(mapWorkersRunning.get()) {
				logger.info("stopping map workers");
				wMoveFromSMSQueue.interrupt();
				wGetSMSToSend.interrupt();
				wCheckAlertQueue.interrupt();
				wCheckNextAttemptQueue.interrupt();
				getSrismWorkers().forEach(worker -> {
					worker.interrupt();
				});
				
				getMtfsmWorkers().forEach(worker -> {
					worker.interrupt();
				});
				
				getReportSMDSWorkers().forEach(worker -> {
					worker.interrupt();
				});
				try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
				if(!wMoveFromSMSQueue.isInterrupted()) {
					logger.warn("Force shutDown wMoveFromSMSQueue");
					wMoveFromSMSQueue.stop();
				}
				if(!wGetSMSToSend.isInterrupted()) {
					logger.warn("Force shutDown wGetSMSToSend");
					wGetSMSToSend.stop();
				}
				if(!wCheckAlertQueue.isInterrupted()) {
					logger.warn("Force shutDown wCheckAlertQueue");
					wCheckAlertQueue.stop();
				}
				if(!wCheckNextAttemptQueue.isInterrupted()) {
					logger.warn("Force shutDown wCheckNextAttemptQueue");
					wCheckNextAttemptQueue.stop();
				}
				
				getSrismWorkers().forEach(worker -> {
					if(!worker.isInterrupted()) {
						logger.warn("Force shutDown " + worker.getName());
						worker.stop();
					}
				});
				
				getMtfsmWorkers().forEach(worker -> {
					if(!worker.isInterrupted()) {
						logger.warn("Force shutDown " + worker.getName());
						worker.stop();
					}
				});
				
				getReportSMDSWorkers().forEach(worker -> {
					if(!worker.isInterrupted()) {
						logger.warn("Force shutDown " + worker.getName());
						worker.stop();
					}
				});
				
				getSrismWorkers().clear();
				getMtfsmWorkers().clear();
				getReportSMDSWorkers().clear();
				mapWorkersRunning.set(false);
				logger.info("map workers stopped");
			}
		}
	}
    
	public static ContentProviderAccess validateSmppConnect(String hostAddress, String systemId, String password
			, String ton, String np) {
		ContentProviderAccess cpa = dbl.getSmppConnection(hostAddress, systemId, password, ton, np);
		if(cpa != null)
			contentProviderAccesses.putIfAbsent(cpa.getId(), cpa);
		return cpa;
	}
	
	public static void saveSmppErrorLog(int sysId, String sourceAddr, long destAddr, int sequenceNumber, int errorCode,
			String errorMessage, long occurred, boolean serverError) {
//		SMPPErrorLog log = new SMPPErrorLog(sysId, sourceAddr, destAddr, sequenceNumber, errorCode, errorMessage
//				, new Timestamp(occurred));
		dbl.saveSmppErrorLog(sysId, sourceAddr, destAddr, sequenceNumber, errorCode, errorMessage, occurred, serverError);
	}

	public static void saveSmppMoResponse(long messageId, Timestamp started, Timestamp finished
			, String errorMessage, String state, int accessId) {
		dbl.saveSmppMoResponse(new SMPPMoResponse(messageId, accessId, started, finished, errorMessage, state));
	}
	
	public static void saveIncomingMOLog(MOIncoming data) {
		dbl.saveIncomingMOLog(data);
		if(data.getSystemId() != 0) {
			ServerController.addMORequest(data);	
		} else {
			logger.info("Received MO SMS for unknown system. MO data - " + data.toString());
		}
	}
	
	public static boolean addSubmitSM(SmsData smsData) {
		if(dbl.addSubmitSM(smsData)) {
			logger.info("addSubmitSM successful processed");
			short messagePart = 0;
			short sendAttempts = 0;
			return sriSMQueue.add(new SendData(smsData.getMessageId(), smsData.getFromAD(), smsData.getFromTON()
					, smsData.getFromNP(), smsData.getToAD(), smsData.getToAN(), smsData.getToNP(), smsData.getMessage()
					, smsData.getQuantity(), smsData.getDcs(), smsData.getPid(), smsData.getInserted(), smsData.getSendUntil()
					, smsData.getSystemId(), smsData.getDlrResponseType(), smsData.getPriority(), smsData.getSegmentLen()
					, null, null, messagePart, sendAttempts));
		}
		logger.warn("Could not add SubmitSM to Queue");
		return false;
	}
	
	public static boolean addSmsToQueue(SMSQueue smsQueue) {
		return dbl.saveSMSQueue(smsQueue);
	}
	
	public static List<SendData> getSmsData(short qLimit) {
		return dbl.getSmsData(qLimit);		
	}
	public static void moveToSMSData(short qLimit) {
		dbl.moveToSMSData(qLimit);
	}
	public static void sendSriSm(SendData srism) {
		mapLayer.sendSRISMRequest(srism);
	}
	
	public static void sendMtSM(SendData mtfsm) {
		mapLayer.sendMTFSMRequest(mtfsm);		
	}
	
	public static void sendDSR(SendData rds) {
		mapLayer.sendRDS(rds);		
	}
	
	public static void processSRISMResponse(SendData data, String state, String errorMessage
			, boolean numberNotInRoaming, String mscAddress) {
		dbl.saveSRISMResponse(data, state, errorMessage, numberNotInRoaming, mscAddress);
		Timestamp nextAttempt;
		if(state == null) {
			if(numberNotInRoaming) {
				//send sms over Sigtran
				SmsGateWay.getMtfSMQueue().add(data);
			} else {
				//send sms over SMPP
				if(!ClientController.sendSM(data)) {
					state = "5";
					nextAttempt = new Timestamp(System.currentTimeMillis());
					DLRQueue dlrData = new DLRQueue(data.getMessageId(), data.getSystemId()
							, data.getFromAD(), data.getFromTON(), data.getFromNP(), data.getToAD()
							, data.getToAN(), data.getToNP(), state, "", (short) data.getDcs()
							, data.getInserted(), nextAttempt
							, (short)0, data.getDlrResponseType(), new Timestamp(System.currentTimeMillis() + getSendDLRUntil())
							, nextAttempt);
					ServerController.addDLRRequest(dlrData);
					logger.info("added to DLRQueue for MessageId=" + data.getMessageId() + "; state " + state);
				}
			}
			return;
		}
		if ("5".equals(state) || "8".equals(state)) {
			nextAttempt = new Timestamp(System.currentTimeMillis());
			DLRQueue dlrData = new DLRQueue(data.getMessageId(), data.getSystemId()
					, data.getFromAD(), data.getFromTON(), data.getFromNP(), data.getToAD()
					, data.getToAN(), data.getToNP(), state, "", (short) data.getDcs()
					, data.getInserted(), nextAttempt
					, (short)0, data.getDlrResponseType(), new Timestamp(System.currentTimeMillis() + getSendDLRUntil())
					, nextAttempt);
			ServerController.addDLRRequest(dlrData);
			return;
		} else if ("10".equals(state) || "6".equals(state)) {
			data.setSmDeliveryOutcome("10".equals(state) ? SMDeliveryOutcome.memoryCapacityExceeded : SMDeliveryOutcome.absentSubscriber);
			SmsGateWay.getRdsSMQueue().add(data);
			logger.info("messageId=" + data.getMessageId() + "; Added to RDSQueue");
			return;
		} else {
			nextAttempt = new Timestamp(System.currentTimeMillis() + nextAttemptToReSend);
			NextAttempt attempt = new NextAttempt(data.getMessageId(), data.getSendAttempts()
					, nextAttempt.before(data.getSendUntil()) ? nextAttempt : data.getSendUntil());
			nextAttemptList.add(attempt);
		}
	}

	public static void processMTFSMResponse(SendData data, boolean isLastPart, String state, String errorMessage) {
		dbl.saveMTFSMResponse(data, isLastPart, state, errorMessage);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if("2".equals(state)) {
			DLRQueue dlrData = new DLRQueue(data.getMessageId(), data.getSystemId()
					, data.getFromAD(), data.getFromTON(), data.getFromNP(), data.getToAD()
					, data.getToAN(), data.getToNP(), state, "", (short) data.getDcs()
					, data.getInserted(), now
					, (short)0, data.getDlrResponseType(), new Timestamp(now.getTime() + getSendDLRUntil())
					, now);
			ServerController.addDLRRequest(dlrData);
			return;
		} else if ("5".equals(state) || "8".equals(state)) {
			DLRQueue dlrData = new DLRQueue(data.getMessageId(), data.getSystemId()
					, data.getFromAD(), data.getFromTON(), data.getFromNP(), data.getToAD()
					, data.getToAN(), data.getToNP(), state, "", (short) data.getDcs()
					, data.getInserted(), now
					, (short)0, data.getDlrResponseType(), new Timestamp(now.getTime() + getSendDLRUntil())
					, now);
			ServerController.addDLRRequest(dlrData);
		}
		Timestamp nextAttempt;
		if("31".equals(state)) {
			SmsGateWay.getMtfSMQueue().add(data);
		} else if ("10".equals(state) || "6".equals(state)){
			data.setSmDeliveryOutcome("10".equals(state) ? SMDeliveryOutcome.memoryCapacityExceeded : SMDeliveryOutcome.absentSubscriber);
			SmsGateWay.getRdsSMQueue().add(data);
			logger.info("messageId=" + data.getMessageId() + "; Added to RDSQueue");
		} else {
			nextAttempt = new Timestamp(System.currentTimeMillis() + nextAttemptToReSend);
			logger.info("messageId=" + data.getMessageId() + "; Added to NextAttemptList");
			NextAttempt attempt = new NextAttempt(data.getMessageId(), data.getSendAttempts()
					, nextAttempt.before(data.getSendUntil()) ? nextAttempt : data.getSendUntil());
			nextAttemptList.add(attempt);
		}
	}

	public static void processReportSMDSResponse(SendData data, String errorCode, String errorMessage) {
		dbl.saveReportSMDSResponse(data, errorCode, errorMessage);
		Timestamp nextAttempt;
		if(errorCode == null) {
			nextAttempt = new Timestamp(System.currentTimeMillis() + nextAttemptForAlert);
			alertLists.add(new AlertList(data.getMessageId(), data.getToAD()
					, nextAttempt.before(data.getSendUntil()) ? nextAttempt : data.getSendUntil()));
			logger.info("messageId=" + data.getMessageId() + "; Added to AlertQueue");
		} else {
			nextAttempt = new Timestamp(System.currentTimeMillis() + nextAttemptToReSend);
			nextAttemptList.add(new NextAttempt(data.getMessageId(), data.getSendAttempts()
					, nextAttempt.before(data.getSendUntil()) ? nextAttempt : data.getSendUntil()));
			logger.info("messageId=" + data.getMessageId() + "; Added to NextAttempt");
		}
	}

//	public static void proccessMoRequest(SM_RP_OA sm_RP_OA, SM_RP_DA sm_RP_DA, String message) {
//		
//	}
	
	public static void processServerDLRResponse(long messageId, long started, long finished, int attempts,
			String errorMessage, boolean doNextAttempt) {
		dbl.saveDLRResponse(messageId, started, finished, attempts, errorMessage, doNextAttempt);
	}

	public static void checkAlertWaitingList(Long msisdn) {
		List<Long> messageIds = new ArrayList<>(); 
		alertLists.stream().forEach(alert -> {
			if(alert.getMsisdn() == msisdn) {
				messageIds.add(alert.getMessageId());
				logger.info(alert.toString());
				alertLists.remove(alert);
			}
		});
		if(messageIds.isEmpty())
			return;
		List<AlertWaitingList> alerts = dbl.processAlertRequest(messageIds);
		String textSmsForExpired = "";
		alerts.forEach(sms -> {
			if(sms.isExpired()) {
				logger.info("send dlr started for - " + sms.getMessageId());
				Timestamp now = new Timestamp(System.currentTimeMillis());
				ServerController.addDLRRequest(new DLRQueue(sms.getMessageId(), sms.getSystemId()
						, sms.getFromAD(), sms.getFromTON(), sms.getFromNP(), sms.getToAD()
						, sms.getToAN(), sms.getToNP(), "3", textSmsForExpired, (short) sms.getDcs()
						, sms.getInserted(), now
						, (short)0, sms.getDlrResponseType(), new Timestamp(now.getTime() + (getSendDLRUntil()))
						, now));
			}
			else {
				logger.info("resending sms started for - " + sms.getMessageId());
				sriSMQueue.add(new SendData(sms.getMessageId(), sms.getFromAD(), sms.getFromTON(), sms.getFromNP()
						, sms.getToAD(), sms.getToAN(), sms.getToNP(), sms.getMessage()
						, sms.getQuantity(), sms.getDcs(), sms.getPid(), sms.getInserted()
						, sms.getSendUntil(), sms.getSystemId(), sms.getDlrResponseType()
						, sms.getPriority(), sms.getSegmentLen(), null, null, (short) 0
						, (short)(sms.getSendAttempts() + 1)));
			}
		});
	}

	public static void processAlertNextAttempt(List<Long> messageIds) {
		if(!messageIds.isEmpty()) {
			//dbl.processAlertRequest - save logs to DB
			List<AlertWaitingList> alerts = dbl.processAlertRequest(messageIds);
			alerts.forEach(sms -> {
				if(sms.isExpired()) {
					logger.info("send dlr started for messageId=" + sms.getMessageId());
					Timestamp now = new Timestamp(System.currentTimeMillis());
					logger.info("send dlr started for messageId=" + sms.getMessageId());
					ServerController.addDLRRequest(new DLRQueue(sms.getMessageId(), sms.getSystemId()
							, sms.getFromAD(), sms.getFromTON(), sms.getFromNP(), sms.getToAD()
							, sms.getToAN(), sms.getToNP(), "3", "", (short) sms.getDcs()
							, sms.getInserted(), now
							, (short)0, sms.getDlrResponseType(), new Timestamp(now.getTime() + (getSendDLRUntil()))
							, now));
				}
				else {
					logger.info("resending sms started for messageId=" + sms.getMessageId());
					sriSMQueue.add(new SendData(sms.getMessageId(), sms.getFromAD(), sms.getFromTON(), sms.getFromNP()
							, sms.getToAD(), sms.getToAN(), sms.getToNP(), sms.getMessage()
							, sms.getQuantity(), sms.getDcs(), sms.getPid(), sms.getInserted()
							, sms.getSendUntil(), sms.getSystemId(), sms.getDlrResponseType()
							, sms.getPriority(), sms.getSegmentLen(), null, null, (short) 0
							, (short)(sms.getSendAttempts() + 1)));
				}
			});
		}
	}
	public static void processNextAttempt(List<Long> messageIds) {
		if(!messageIds.isEmpty()) {
			//dbl.processNextAttemptRequest - save logs to DB
			List<NextAttemptWaitingList> reSendList = dbl.processNextAttemptRequest(messageIds);
			reSendList.forEach(sms -> {
				if(sms.isExpired()) {
					logger.info("send dlr started for messageId=" + sms.getMessageId());
					Timestamp now = new Timestamp(System.currentTimeMillis());
					ServerController.addDLRRequest(new DLRQueue(sms.getMessageId(), sms.getSystemId()
							, sms.getFromAD(), sms.getFromTON(), sms.getFromNP(), sms.getToAD()
							, sms.getToAN(), sms.getToNP(), "3", "", (short) sms.getDcs()
							, sms.getInserted(), now
							, (short)0, sms.getDlrResponseType(), new Timestamp(now.getTime() + getSendDLRUntil())
							, now));
				}
				else {
					logger.info("resending sms started for messageId=" + sms.getMessageId());
					sriSMQueue.add(new SendData(sms.getMessageId(), sms.getFromAD(), sms.getFromTON(), sms.getFromNP()
							, sms.getToAD(), sms.getToAN(), sms.getToNP(), sms.getMessage()
							, sms.getQuantity(), sms.getDcs(), sms.getPid(), sms.getInserted()
							, sms.getSendUntil(), sms.getSystemId(), sms.getDlrResponseType()
							, sms.getPriority(), sms.getSegmentLen(), null, null, (short) 0
							, (short)(sms.getSendAttempts() + 1)));
				}
			});
		}
	}
		
	public static List<SMPPServerConfig> getSmppServerList() {
		return dbl.getSmppServerList();
	}
	
	public static List<DLRQueue> resetDlrQueue() {
		return dbl.resetDLRQueue();
	}
	
	public static void checkReqEx() {
		long start = System.currentTimeMillis();
		List<NumberRegEx> regExsList = dbl.getNumberRegEx();	//getting Regular Expression list from DB
		regExsList.forEach(expression -> {
			Optional<NumberRegEx> oRegEx = regExs.stream().filter(regEx -> regEx.getId() == expression.getId()).findFirst();
			if(oRegEx.isPresent()) {							//Check if regular is in queue
				oRegEx.get().setNp(expression.getNp());
				oRegEx.get().setTon(expression.getTon());
				oRegEx.get().setExpression(expression.getExpression());
				oRegEx.get().setCheckForSourceAdd(expression.isCheckForSourceAdd());
			} else {
				regExs.add(expression);
			}
		});
		logger.info("checkReqEx duration - " + (System.currentTimeMillis() - start));
	}
	
	public static void checkAccessRestriction() {
		ConcurrentLinkedQueue<ContentProvidersAccessesRestrictions> accessesRestrictions = new ConcurrentLinkedQueue<>();
		accessesRestrictions.addAll(dbl.loadAccessesRestrictions());
		setAccessesRestrictions(accessesRestrictions);
	}
	
	public static void checkAccessCharSetList() {
		ConcurrentLinkedQueue<ContentProviderAccessCharsetList> accessCharsetLists = new ConcurrentLinkedQueue<>();
		accessCharsetLists.addAll(dbl.loadAccessesCharSetList());
		setAccessCharsetLists(accessCharsetLists);
	}
	
	public static void checkDataCodingList() {
		ConcurrentHashMap<Short, DataCodingList> dataCodingList = new ConcurrentHashMap<>();
		List<DataCodingList> tempList = dbl.getDataCodingList();
		tempList.forEach(row -> {
			dataCodingList.putIfAbsent(row.getDcsId(), row);
		});
		setDataCodingList(dataCodingList);
	}
	
	public static void checkSourcheSubstitutionList() {
		ConcurrentHashMap<String, String> sourcheList = new ConcurrentHashMap<>();
		List<SourcheSubstitutionList> tempList = dbl.loadSourceSubstitutionList();
		tempList.forEach(row -> {
			sourcheList.putIfAbsent(row.getSourcheOriginal(), row.getSourcheSubstitution());
		});
		setSourcheSubstitutionLists(sourcheList);
	}
	
	public static void checkMoRoutingRulesList() {
		ConcurrentLinkedQueue<MoRoutingRules> moRoutingRules = new ConcurrentLinkedQueue<>();
		moRoutingRules.addAll(dbl.loadMoRoutingRulesList());
		setMoRoutingRules(moRoutingRules);
	}
	
	public static List<ContentProviderAccess> checkAccess(AccessType accessType) {
		long start = System.currentTimeMillis();
		List<ContentProviderAccess> result = dbl.getSmppAccessList(accessType);
		for (ContentProviderAccess cpa : result) {
			ContentProviderAccess curretnCPA = contentProviderAccesses.get(cpa.getId());
			if(curretnCPA != null) {	
				// if old access info
				if(curretnCPA.isEnabled()) {
					// if connection is enabled. update info
					curretnCPA.setProviderId(cpa.getProviderId());
					curretnCPA.setLogin(cpa.getLogin());
					curretnCPA.setPassword(cpa.getPassword());
					curretnCPA.setSpeedLimit(cpa.getSpeedLimit());
					curretnCPA.setSmsParts(cpa.getSmsParts());
					curretnCPA.setSmsType(cpa.getSmsType());
					curretnCPA.setChangeSubmitDateType(cpa.getChangeSubmitDateType());
					curretnCPA.setCanSendSMSFromTime(cpa.getCanSendSMSFromTime());
					curretnCPA.setCanSendSMSToTime(cpa.getCanSendSMSToTime());
					curretnCPA.setDaysOfWeek(cpa.getDaysOfWeek());
					curretnCPA.setDirection(cpa.getDirection());
					curretnCPA.setAccessType(cpa.getAccessType());
					curretnCPA.setExpiredType(cpa.getExpiredType());
					curretnCPA.setDeliveryType(cpa.getDeliveryType());
					curretnCPA.setQueueSize(cpa.getQueueSize());					
					if(curretnCPA.getDefaultSmsLiveTimeInMin() != null)
						curretnCPA.setDefaultSmsLiveTimeInMilSec(cpa.getDefaultSmsLiveTimeInMin() * 60 * 1000);
					else
						curretnCPA.setDefaultSmsLiveTimeInMilSec(null);
				} else {
					// if connection is close
					contentProviderAccesses.remove(cpa.getId());
				}
			} else {
				// new connection
				contentProviderAccesses.putIfAbsent(cpa.getId(), cpa);
			}
		}
		logger.info("checkAccess duration - " + (System.currentTimeMillis() - start));
		return result;
	}

	public static void checkContentProviderAccessSmsQueueSize() {
		List<TempContentProvideAccessQueueSize> result = 
				dbl.checkContentProviderAccessSmsQueueSize();
		for (TempContentProvideAccessQueueSize tempCPAQueueSize : result) {
			ServerController.getConnections().values().stream().forEach(connection -> {
				if(connection.getAccessId() == tempCPAQueueSize.getSystemId() && tempCPAQueueSize.getSize() > contentProviderAccessSmsQueueSize) {
					if(!connection.getWaitingQueueIsFull()) {
						connection.setWaitingQueueIsFull(true);
						logger.info("set flag WaitingQueueIsFull - true. " + connection.toString());	
					}
				} else { 
					if(connection.getWaitingQueueIsFull()) {
						connection.setWaitingQueueIsFull(false);
						logger.info("set flag WaitingQueueIsFull - false. " + connection.toString());	
					}
				}
			});
		}
	}
	
	public boolean blackListManipulations(long number, Direction direction, boolean add) throws HibernateException {
		if(dbl.blackListManipulations(number, direction, add)) {
			if(add) {
				MsisdnBlackList row = SmsGateWay.getBlackLists().get(number);
				if(row != null) {
					row.setDirection(direction);
					logger.info("black list was modified for number - " + number); 
				} else {
					if(SmsGateWay.getBlackLists().putIfAbsent(number, new MsisdnBlackList(number, direction, new Timestamp(System.currentTimeMillis()))) == null) {
						logger.info("number added to black list - " + number);
					} else {
						logger.warn("number added to black list - " + number);
					}
				}
			} else {
				if(SmsGateWay.getBlackLists().remove(number) != null) {
					logger.info("number has been removed from black list. number - " + number);
				} else {
					logger.warn("Could not remove number from black list. number - " + number);
				}
			}
			return true;
		}
		return false;
	}
	
	public static boolean reLoadGatewayConfig() {
		List<GateWaySettings> settings = SmsGateWay.getDbl().getGateWaySetting();
		if(settings.isEmpty()) {
			logger.warn("Setting List is empty");
			return false;
		}
		appSettings = fetchSettings(settings);
		return loadGateWayConfig(appSettings.get("app"));
	}
	
	public static Map<String, Map<String, String>> fetchSettings(List<GateWaySettings> settings) {
		Map<String, Map<String, String>> preference = new HashMap<>();
		Set<String> types = null;
		types = settings.stream().collect(Collectors.groupingBy(GateWaySettings::getType)).keySet();
		if(types != null) {
			types.forEach(type -> {
				Map<String, String> params = new HashMap<>();
				settings.forEach(row -> {
					if(row.getType().equals(type)) {
						params.put(row.getName(), row.getValue());
					}
				});
				preference.put(type, params);
			});	
		}
		return preference;
	}
	
	private static class LoadAlertAwaitingQueue extends Thread {
		@Override
		public void run() {
			Thread.currentThread().setName("Load from DB AlertAwatingQueue");
			logger.info("Starting reset Alert Queue");
			List<AlertList> tempList = dbl.resetAlertQueue();
			for (int i = 0; i < tempList.size(); i++) {
				alertLists.add(tempList.get(i));
			}
			logger.info("alertLists Queue size - " + alertLists.size());
		}
	}
	
	private static class LoadNextAttemptQueue extends Thread {
		@Override
		public void run() {
			Thread.currentThread().setName("Load from DB NextAttemptQueue");
			logger.info("Starting reset NextAttempt Queue");
			List<NextAttempt> tempList = dbl.resetNextAttemptQueue();
			for (int i = 0; i < tempList.size(); i++) {
				nextAttemptList.add(tempList.get(i));
			}
			logger.info("nextATtemptList Queue size - " + nextAttemptList.size());
		}
	}
	
	public static List<ClientDLRWaitingList> getClientAwaitingDLRQueue() {
		return dbl.getClientAwaitingDLRQueue();
	}
	
	public static List<SMPPClientConfig> getSmppClientConnectionList(int clientId) {
		return dbl.getSmppClientConnectionList(clientId);
	}

	public static List<SMPPClientsGroups> getSmppClientConnectionGroups() {
		return dbl.getSmppClientConnectionGroups();
	}
		
	public static void processClientDlrRequest(Long remoteId, String state, int clientId) {
		DLRQueue dlr = dbl.processClientDlrRequest(remoteId, state, clientId);
		if(dlr != null) {
			ServerController.addDLRRequest(dlr);
		} else {
			logger.warn("DLR Was not found for: remoteId=" + remoteId + "; state=" + state);
		}
	}
	
	public static void saveClientDlrWaitingList(Long remoteId, int clientId, long messageId, String errorMessage,
			Timestamp started, Timestamp finished, Timestamp sendUntil) {
		dbl.saveClientDlrWaitingList(remoteId, clientId, messageId, errorMessage,
				started, finished, sendUntil);
		
	}

	public static void processNegativeSubmitResponse(SendData data, String state) {
		if(dbl.addSMPPServerIncomingLog(data.getMessageId(), state, System.currentTimeMillis(), 1)) {
			ServerController.addDLRRequest(new DLRQueue(data.getMessageId(), data.getSystemId()
					, data.getFromAD(), data.getFromTON(), data.getFromNP(), data.getToAD()
					, data.getToAN(), data.getToNP(), state, "", (short) data.getDcs()
					, data.getInserted(), new Timestamp(System.currentTimeMillis())
					, 0, data.getDlrResponseType(), new Timestamp(System.currentTimeMillis() + getSendDLRUntil())
					, new Timestamp(System.currentTimeMillis())));
		}
	}
	
	public static boolean processCancelSmRequest(long messageId, AccessType reqType, int accessId) {
		String queueType = "SMSQUEUE";	//sms is waiting for send;
		if(SmsGateWay.getAlertLists().removeIf(row -> row.getMessageId() == messageId))
			queueType = "ALERT";	//sms in alert list
		else if(SmsGateWay.getNextAttemptList().removeIf(row -> row.getMessageId() == messageId))
			queueType = "NEXTATTEMPT";	//sms in nextAttempt list
		return dbl.processCancelSmRequest(messageId, reqType, accessId, queueType);
	}
	
	public static void processCancelMessage(long messageId) {
		
	}
	
	public static void saveSMPPIncomingDLRLog(Long remoteId, String state, int clientId) {
		dbl.saveSMPPIncomingDLRLog(remoteId, state, clientId);
	}
	public AtomicLong getMessageId() {
		return messageId;
	}

	public static void setMessageId(AtomicLong messageId) {
		SmsGateWay.messageId = messageId;
	}

	public static MapLayer getMapLayer() {
		return mapLayer;
	}

	public static void setMapLayer(MapLayer mapLayer) {
		SmsGateWay.mapLayer = mapLayer;
	}

	public ServerController getSmppServerController() {
		return smppServerController;
	}

	public void setSmppServerController(ServerController smppServerController) {
		this.smppServerController = smppServerController;
	}

	public ClientController getSmppClientController() {
		return smppClientController;
	}

	public void setSmppClientController(ClientController smppClientController) {
		this.smppClientController = smppClientController;
	}

	public static long getDefaultSMSLiveTime() {
		return defaultSMSLiveTime;
	}

	public static void setDefaultSMSLiveTime(long defaultSMSLiveTime) {
		SmsGateWay.defaultSMSLiveTime = defaultSMSLiveTime;
	}

	public static long getNextAttemptToReSend() {
		return nextAttemptToReSend;
	}

	public static void setNextAttemptToReSend(long nextAttemptToReSend) {
		SmsGateWay.nextAttemptToReSend = nextAttemptToReSend;
	}

	public static long getNextAttemptForAlert() {
		return nextAttemptForAlert;
	}

	public static void setNextAttemptForAlert(long nextAttemptForAlert) {
		SmsGateWay.nextAttemptForAlert = nextAttemptForAlert;
	}

	public static long getMaxSchedulerTime() {
		return maxSchedulerTime;
	}

	public static void setMaxSchedulerTime(long maxSchedulerTime) {
		SmsGateWay.maxSchedulerTime = maxSchedulerTime;
	}

	public static long getSendDLRUntil() {
		return sendDLRUntil;
	}

	public static void setSendDLRUntil(long sendDLRUntil) {
		SmsGateWay.sendDLRUntil = sendDLRUntil;
	}

	public static ConcurrentLinkedQueue<SendData> getSriSMQueue() {
		return sriSMQueue;
	}

	public static void setSriSMQueue(ConcurrentLinkedQueue<SendData> sriSMQueue) {
		SmsGateWay.sriSMQueue = sriSMQueue;
	}

	public static ConcurrentLinkedQueue<SendData> getMtfSMQueue() {
		return mtfSMQueue;
	}

	public static void setMtfSMQueue(ConcurrentLinkedQueue<SendData> mtfSMQueue) {
		SmsGateWay.mtfSMQueue = mtfSMQueue;
	}

	public static ConcurrentLinkedQueue<SendData> getRdsSMQueue() {
		return rdsSMQueue;
	}

	public static void setRdsSMQueue(ConcurrentLinkedQueue<SendData> rdsSMQueue) {
		SmsGateWay.rdsSMQueue = rdsSMQueue;
	}

	public static ConcurrentLinkedQueue<AlertList> getAlertLists() {
		return alertLists;
	}

	public static void setAlertLists(ConcurrentLinkedQueue<AlertList> alertLists) {
		SmsGateWay.alertLists = alertLists;
	}

	public static ConcurrentLinkedQueue<NextAttempt> getNextAttemptList() {
		return nextAttemptList;
	}

	public static void setNextAttemptList(ConcurrentLinkedQueue<NextAttempt> nextAttemptList) {
		SmsGateWay.nextAttemptList = nextAttemptList;
	}

	public static ConcurrentHashMap<Long, MsisdnBlackList> getBlackLists() {
		return blackLists;
	}

	public static void setBlackLists(ConcurrentHashMap<Long, MsisdnBlackList> blackLists) {
		SmsGateWay.blackLists = blackLists;
	}

	public static ConcurrentLinkedQueue<ContentProvider> getContentProviders() {
		return contentProviders;
	}

	public static void setContentProviders(ConcurrentLinkedQueue<ContentProvider> contentProviders) {
		SmsGateWay.contentProviders = contentProviders;
	}

	public static ConcurrentHashMap<Integer, ContentProviderAccess> getContentProviderAccesses() {
		return contentProviderAccesses;
	}

	public static void setContentProviderAccesses(
			ConcurrentHashMap<Integer, ContentProviderAccess> contentProviderAccesses) {
		SmsGateWay.contentProviderAccesses = contentProviderAccesses;
	}

	public static ConcurrentLinkedQueue<ContentProvidersAccessesRestrictions> getAccessesRestrictions() {
		return accessesRestrictions;
	}

	public static void setAccessesRestrictions(
			ConcurrentLinkedQueue<ContentProvidersAccessesRestrictions> accessesRestrictions) {
		SmsGateWay.accessesRestrictions = accessesRestrictions;
	}

	public static ConcurrentLinkedQueue<ContentProviderAccessCharsetList> getAccessCharsetLists() {
		return accessCharsetLists;
	}

	public static void setAccessCharsetLists(ConcurrentLinkedQueue<ContentProviderAccessCharsetList> accessCharsetLists) {
		SmsGateWay.accessCharsetLists = accessCharsetLists;
	}

	public static ConcurrentHashMap<Short, DataCodingList> getDataCodingList() {
		return dataCodingList;
	}

	public static void setDataCodingList(ConcurrentHashMap<Short, DataCodingList> dataCodingList) {
		SmsGateWay.dataCodingList = dataCodingList;
	}

	public static ConcurrentHashMap<String, String> getSourcheSubstitutionLists() {
		return sourcheSubstitutionLists;
	}

	public static void setSourcheSubstitutionLists(ConcurrentHashMap<String, String> sourcheSubstitutionLists) {
		SmsGateWay.sourcheSubstitutionLists = sourcheSubstitutionLists;
	}

	public static ConcurrentLinkedQueue<MoRoutingRules> getMoRoutingRules() {
		return moRoutingRules;
	}

	public static void setMoRoutingRules(ConcurrentLinkedQueue<MoRoutingRules> moRoutingRules) {
		SmsGateWay.moRoutingRules = moRoutingRules;
	}

	public AtomicBoolean getMapWorkersRunning() {
		return mapWorkersRunning;
	}

	public void setMapWorkersRunning(AtomicBoolean mapWorkersRunning) {
		this.mapWorkersRunning = mapWorkersRunning;
	}

	public AtomicBoolean getDlrWorkersRunning() {
		return dlrWorkersRunning;
	}

	public void setDlrWorkersRunning(AtomicBoolean dlrWorkersRunning) {
		this.dlrWorkersRunning = dlrWorkersRunning;
	}

	public List<SRISMWorker> getSrismWorkers() {
		return srismWorkers;
	}

	public void setSrismWorkers(List<SRISMWorker> srismWorkers) {
		this.srismWorkers = srismWorkers;
	}

	public List<MTFSMWorker> getMtfsmWorkers() {
		return mtfsmWorkers;
	}

	public void setMtfsmWorkers(List<MTFSMWorker> mtfsmWorkers) {
		this.mtfsmWorkers = mtfsmWorkers;
	}

	public List<ReportSMDSWorker> getReportSMDSWorkers() {
		return reportSMDSWorkers;
	}

	public void setReportSMDSWorkers(List<ReportSMDSWorker> reportSMDSWorkers) {
		this.reportSMDSWorkers = reportSMDSWorkers;
	}

//	public SctpServer getSctpServer() {
//		return sctpServer;
//	}
//
//	public void setSctpServer(SctpServer sctpServer) {
//		SmsGateWay.sctpServer = sctpServer;
//	}

//	public ConcurrentHashMap<Integer, String> getMtpstatus() {
//		return mtpstatus;
//	}
//
//	public void setMtpstatus(ConcurrentHashMap<Integer, String> mtpstatus) {
//		this.mtpstatus = mtpstatus;
//	}

	public static ConcurrentLinkedQueue<NumberRegEx> getRegExs() {
		return regExs;
	}

	public static void setRegExs(ConcurrentLinkedQueue<NumberRegEx> regExs) {
		SmsGateWay.regExs = regExs;
	}

	public int getContentProviderAccessSmsQueueSize() {
		return contentProviderAccessSmsQueueSize;
	}

	public void setContentProviderAccessSmsQueueSize(int contentProviderAccessSmsQueueSize) {
		SmsGateWay.contentProviderAccessSmsQueueSize = contentProviderAccessSmsQueueSize;
	}

	public static String getMSCLocalPrefix() {
		return MSCLocalPrefix;
	}

	public static void setMSCLocalPrefix(String mSCLocalPrefix) {
		MSCLocalPrefix = mSCLocalPrefix;
	}

	public static ConcurrentLinkedQueue<Long> getCancelMessageIdList() {
		return cancelMessageIdList;
	}

	public static void setCancelMessageIdList(ConcurrentLinkedQueue<Long> cancelMessageIdList) {
		SmsGateWay.cancelMessageIdList = cancelMessageIdList;
	}
}
