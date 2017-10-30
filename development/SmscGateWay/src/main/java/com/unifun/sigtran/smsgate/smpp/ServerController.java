package com.unifun.sigtran.smsgate.smpp;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.SMPPServerSession;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.enums.AccessType;
import com.unifun.sigtran.smsgate.enums.DeliveryType;
import com.unifun.sigtran.smsgate.enums.ExpiredType;
import com.unifun.sigtran.smsgate.enums.SchedulerDays;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccess;
import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.hibernate.models.MOIncoming;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPServerConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMSQueue;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.smpp.common.ClearSMParts;
import com.unifun.sigtran.smsgate.smpp.common.ConnectionLimits;
import com.unifun.sigtran.smsgate.smpp.common.MessageContainer;
import com.unifun.sigtran.smsgate.smpp.common.ResetCounter;
import com.unifun.sigtran.smsgate.smpp.common.SMPPConnectionInfo;
import com.unifun.sigtran.smsgate.smpp.server.SmppServer;
import com.unifun.sigtran.smsgate.smpp.workers.CheckSMPPAccess;
import com.unifun.sigtran.smsgate.smpp.workers.ClearDLRList;
import com.unifun.sigtran.smsgate.smpp.workers.SMPPDLRWorker;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;
import com.unifun.sigtran.smsgate.util.PoolableSpeedLimiter;


public class ServerController {

	private final static int DEFAUTL_THREAD_POOL = 200;
	private final static int DEFAUTL_CHECK_ACCESS_SMPP_LIST = 1;			//MINUTES
	private final static int DEFAUTL_DLR_SEND_UNTIL = 60;					//MINUTES
	
	private final static int DEFAUTL_DLR_RESEND_MAX_ATTEMPTS = 5;
	private final static int DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT = 300;		//SECONDS
	private final static int DEFAUTL_DLR_SEND_SPEED_PER_SECOND = 100;					
	private static int checkSMPPAccessList;
	private static int dlrReSendMaxAttempts;
	private static int dlrReSendIntervalAttemptSec;
	private static int dlrSendSpeedPerSec;
	private static SMPPDLRWorker dlrWorker;
	private static ClearSMParts clearSMParts;
	private static ClearDLRList clearDLRList;
	private static int sendDLRUntil;
	private static ExecutorService exec;
//	private static ExecutorService execSubmitSm;
	private static ExecutorService execMO;
	private static ExecutorService execDLR;
	private static ResetCounter resetCounter;
	private static CheckSMPPAccess checkSMPPAccess;
	private static ConcurrentLinkedQueue<DLRQueue> dlrQueue = new ConcurrentLinkedQueue<>();
	private static ConcurrentHashMap<Integer, SmppServer> servers = new ConcurrentHashMap<>(); 
	private static ConcurrentHashMap<String, SMPPConnectionInfo> connections = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, SMPPServerSession> smppBindSessions = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, MessageContainer> messageParts = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, PoolableSpeedLimiter> limmiter = new ConcurrentHashMap<>();
	
	private static final Logger logger = LogManager.getLogger(ServerController.class);
	
	public ServerController() {
		this(DEFAUTL_THREAD_POOL, DEFAUTL_DLR_SEND_UNTIL, DEFAUTL_CHECK_ACCESS_SMPP_LIST
				, DEFAUTL_DLR_RESEND_MAX_ATTEMPTS, DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT
				, DEFAUTL_DLR_SEND_SPEED_PER_SECOND);
	}

	public ServerController(int threadPoolSize) {
		this(threadPoolSize, DEFAUTL_DLR_SEND_UNTIL, DEFAUTL_CHECK_ACCESS_SMPP_LIST
				, DEFAUTL_DLR_RESEND_MAX_ATTEMPTS, DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT
				, DEFAUTL_DLR_SEND_SPEED_PER_SECOND);
		
	}
	
	public ServerController(int threadPoolSize, int sendDLRUntil) {
		this(threadPoolSize, sendDLRUntil, DEFAUTL_CHECK_ACCESS_SMPP_LIST
				, DEFAUTL_DLR_RESEND_MAX_ATTEMPTS, DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT
				, DEFAUTL_DLR_SEND_SPEED_PER_SECOND);
		
	}
	
	public ServerController(int threadPoolSize, int sendDLRUntil, int checkSMPPAccessList) {
		this(threadPoolSize, sendDLRUntil, checkSMPPAccessList
				, DEFAUTL_DLR_RESEND_MAX_ATTEMPTS, DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT
				, DEFAUTL_DLR_SEND_SPEED_PER_SECOND);
	}
	
	public ServerController(int threadPoolSize, int sendDLRUntil, int checkSMPPAccessList
			, short dlrReSendMaxAttempts) {
		this(threadPoolSize, sendDLRUntil, checkSMPPAccessList
				, dlrReSendMaxAttempts, DEFAUTL_DLR_RESEND_INTERVAL_ATTEMPT
				, DEFAUTL_DLR_SEND_SPEED_PER_SECOND);
	}
	
	public ServerController(int threadPoolSize, int sendDLRUntil, int checkSMPPAccessList
			, int dlrReSendMaxAttempts, int dlrReSendIntervalAttemptSec, int dlrSendSpeedPerSec) {
		if(threadPoolSize < 1 || sendDLRUntil < 1 || checkSMPPAccessList < 1 
				|| dlrReSendMaxAttempts < 1 || dlrReSendIntervalAttemptSec < 1
				|| dlrSendSpeedPerSec < 1) {
			logger.error("any input parameter can't be negative");
			return;
		}
		ServerController.sendDLRUntil = sendDLRUntil * 60 * 1000;
		ServerController.setCheckSMPPAccessList(checkSMPPAccessList * 60 * 1000);
		ServerController.setDlrReSendMaxAttempts(dlrReSendMaxAttempts);
		ServerController.setDlrReSendIntervalAttemptSec(dlrReSendIntervalAttemptSec);
		ServerController.setDlrSendSpeedPerSec(dlrSendSpeedPerSec);
		exec = Executors.newCachedThreadPool(new CustomThreadFactoryBuilder()
				.setNamePrefix("_SmppServerContreller").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
//		execSubmitSm = Executors.newFixedThreadPool(threadPoolSize
//				, new CustomThreadFactoryBuilder()
//				.setNamePrefix("_SMPP_SubmitSM").setDaemon(false)
//				.setPriority(Thread.MAX_PRIORITY).build());
		execDLR = Executors.newFixedThreadPool(threadPoolSize
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_SMPP_DLR").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
		execMO = Executors.newFixedThreadPool(threadPoolSize
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_SMPP_MO").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
	}
	
	public boolean initSmppServers() {
		List<SMPPServerConfig> cServers = SmsGateWay.getSmppServerList();
		if(cServers.size() == 0) {
			logger.error("initialized info is null. Servers quantity = 0");
			return false;
		}
		try {
			for (SMPPServerConfig smppServerConfig : cServers) {
				servers.putIfAbsent(smppServerConfig.getId(), new SmppServer(smppServerConfig, messageParts));
			}
			logger.info("adding servres complited");
			//Cleaner for not complete sms
			clearSMParts = new ClearSMParts("Server_ClearSMParts", messageParts, true);
			clearSMParts.start();
			//Clear DLR waiting List
//			clearDLRList = new ClearDLRList(checkDLRWaitingList, "Server_ClearDLRList", false);
			clearDLRList = new ClearDLRList(1000, getDlrReSendIntervalAttemptSec() * 1000, "Server_ClearExpiredDLR", false);
			clearDLRList.start();
			//Reset incoming counters
//			tResetCounter = new Timer();
			resetCounter = new ResetCounter(new ConcurrentHashMap<String, ConnectionLimits>(), "ServerResetCounter");
			resetCounter.start();
//			tResetCounter.schedule(getResetCounter(), 10L, 1000);
			dlrWorker = new SMPPDLRWorker("Main_DLR_WORKER", dlrSendSpeedPerSec);
			dlrWorker.start();
			//Check SMPP Access to Server
			checkSMPPAccess = new CheckSMPPAccess(checkSMPPAccessList);
			checkSMPPAccess.start();
			//Reset DLR QUEUE
			List<DLRQueue> resetDLRList = SmsGateWay.resetDlrQueue();
			resetDLRList.forEach(dlr -> dlrQueue.add(dlr));
			logger.info("Reset counters started");
			for (SmppServer server : servers.values()) {
				logger.info("starting " + server.getServerName());
				exec.submit(server);
			}
		} catch (Exception e) {
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
			return false;
		}
		return true;
	}
	
	public boolean stopSmppServers() {
		logger.info("ClearDLRList shutdown was started");
		clearDLRList.interrupt();
		logger.info("ClearSMParts shutdown was started");
		clearSMParts.interrupt();
		logger.info("ResetCounter shutdown was started");
		resetCounter.interrupt();
		logger.info("DLRWorker shutdown was started");
		dlrWorker.interrupt();
		logger.info("CheckSMPPAccess shutdown was started");
		checkSMPPAccess.interrupt();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e.fillInStackTrace());
			e.printStackTrace();
		}
		if(!clearDLRList.isInterrupted()) {
			logger.warn("force stop clearDLRList");
			clearDLRList.stop();
		}
		if(!clearSMParts.isInterrupted()) {
			logger.warn("force stop clearSMParts");
			clearSMParts.stop();	
		}
		if(!resetCounter.isInterrupted()) {
			logger.warn("force stop resetCounter");
			resetCounter.stop();
		}
		if(!dlrWorker.isInterrupted()) {
			logger.warn("force stop dlrWorker");
			dlrWorker.stop();
		}
		if(!checkSMPPAccess.isInterrupted())
			logger.warn("force stop checkSMPPAccess");
			checkSMPPAccess.stop();
			
		for (SmppServer server : servers.values()) {
			logger.info("stoping " + server.getServerName());
			server.stop();
			logger.info("Smmp server " + server.getServerName() + " stopped");
		}
		exec.shutdown();
		execDLR.shutdown();
		execMO.shutdown();
//		execSubmitSm.shutdown();
		return true;
	}
	
	public static boolean isAnyServerAvailable() {
		return servers.values().stream().filter(server -> server.getServerState() == true).findFirst().isPresent();
	}
	
	public static List<Integer> checkServerIsDown() {
		List<Integer> serverList = new ArrayList<>();
		servers.entrySet().forEach(server -> {
			if(!server.getValue().getServerState()) {
				logger.warn("Server " + server.getValue().getServerName() + " is down");
				serverList.add(server.getKey());
			}
		});
		return serverList;
	}
	
	public static int getServersActiveSessions() {
		return connections.size();
	}

	public static boolean validateBindingRequset(String hostAddress, String systemId, String password, String ton,
			String np, SMPPServerSession session, int serverId, String serverName) {
			ContentProviderAccess result = SmsGateWay.validateSmppConnect(hostAddress, systemId, password, ton, np);
			if(result == null) {
				logger.warn(String.format("Unknown System! SystemId - " + systemId));
				return false;
			}
			logger.info("starting to add info for system - " + systemId);
			
			Optional<SMPPConnectionInfo> oInfo = getConnections().values().stream().filter(c -> c.getLogin().equals(systemId)).findAny();
			if(oInfo.isPresent()) {
				logger.info("new connection for " + systemId);
				if(getConnections().remove(oInfo.get().getCurrentSMPPSessionId()) != null) {
					logger.info(systemId + " is reconnecting");
					PoolableSpeedLimiter limit = getLimmiter().remove(oInfo.get().getLogin());
					if(limit != null) {
						limit.interrupt();
						logger.info("Limiter was stopped for systemId - " + oInfo.get().getLogin());
					}
					SMPPServerSession oldSession = getSmppBindSessions().remove(oInfo.get().getAccessId());
					if(oldSession != null) {
						oldSession.unbindAndClose();
						logger.info("Session " + oldSession.getSessionId() + " was removed and closed, from Bind Session List; systemId " + systemId);
					} else {
						logger.warn("Session was not found in Bind Session List; systemId " + systemId);	
					}
				} else {
					logger.info("new Connection");
				}
			}
//			if(SmsGateWay.smppConnectionManipulations(clientId, serverId, true)) {
//				logger.info(systemId + " was successfully added on DBLayer");
				getConnections().putIfAbsent(session.getSessionId(), new SMPPConnectionInfo(result.getProviderId(), result.getId(), result.getLogin(), result.getSmsParts(), session.getSessionId()));
				PoolableSpeedLimiter limit = new PoolableSpeedLimiter(systemId, result.getSpeedLimit(), result.getQueueSize());
				getLimmiter().putIfAbsent(systemId, limit);
				limit.start();
				getSmppBindSessions().putIfAbsent(result.getId(), session);
				logger.info("add info for system - " + systemId + "Successfully compleated");
				return true;
//			} else {
//				logger.error(systemId + " was not added on DBLayer");	
//				return false;
//			}
	}
	
	public static synchronized boolean removeConnection(SMPPServerSession session, boolean isOpen) {
		try {
			if(session == null) {
				logger.warn("SMPP session is null");
				return false;
			}
			if(!isOpen) {
				if(session.getSessionState().isBound() && session.getSessionState().isTransmittable())
					return true;		
			} else {
				logger.info("closing session - " + session.getSessionId());
				session.unbindAndClose();
			}
			Optional<SMPPConnectionInfo> info = getConnections().values().stream()
					.filter(x -> x.getCurrentSMPPSessionId().equals(session.getSessionId())).findFirst();
			if (info.isPresent()) {
				String sessionId = session.getSessionId();
				logger.info("Starting removing session info. session - " + sessionId);
//				if(SmsGateWay.smppConnectionManipulations(info.get().getId(), 0, false)) {
					if(getConnections().remove(info.get().getCurrentSMPPSessionId()) != null) {
						logger.info("Connection list was cleared. session - " + sessionId);
						if(getSmppBindSessions().remove(info.get().getAccessId()) != null) {
							logger.info("Bind Sessions list was cleared. session - " + sessionId);
							PoolableSpeedLimiter limit = getLimmiter().remove(info.get().getLogin());
							if(limit != null) {
								limit.interrupt();
								logger.info("Limiter was stopped for systemId - " + info.get().getLogin());
								return true;
							} else 
								logger.info("Could not remove Limiter for systemId - " + info.get().getLogin());
						} else {
							logger.warn("Bind Sessions list was not cleared. session - " + sessionId);
						}
					} else {
						logger.warn("Connection list was not cleared. session - " + sessionId);	
					}
//				} else {
//					logger.warn("Could not remove session from DB. session - " + sessionId);
//				}
			} else {
				logger.warn("Could not found info about session - " + session.getSessionId());
			}
		} catch (Exception e) {
		}
		return false;	
	}
	
	public static void checkSMPPConncetions() {
		List<ContentProviderAccess> result = SmsGateWay.checkAccess(AccessType.SMPP);
		if(result.isEmpty()) {
			getConnections().values().forEach(connect -> {
				if(removeConnection(getSmppBindSessions().get(connect.getClientId()), true)) {
					logger.error("could not process closing connection for id - " + connect.getClientId());
				}
			});
		} else {
			result.forEach(con -> {
				if(con.isEnabled()) {
					PoolableSpeedLimiter limit = limmiter.get(con.getLogin());
					if(limit != null) {
						limit.setMaxSpeed(con.getSpeedLimit());
						limit.setMaxQueueSize(con.getQueueSize());
					}
					//check if Content Provider Access in ConnectionInfo List
					Optional<SMPPConnectionInfo> oInfo = getConnections().values().stream().filter(info -> info.getAccessId() == con.getId()).findFirst();
					if(oInfo.isPresent()) {
						oInfo.get().setMessageParts(con.getSmsParts());
						logger.debug("Message Parts changed to " + con.getSmsParts() + " for access id - " + con.getLogin());
					} else {
						logger.debug("AccessId - " + con.getId() + " is not connected");
					}
				} else {
					if(removeConnection(getSmppBindSessions().get(con.getId()), true)) {
						logger.error("could not process closing connection for id - " + con.getId());
					}
				}
			});
		}	
	}
	
	public static boolean addSubmitSM(SmsData smsData) {
		return SmsGateWay.addSubmitSM(smsData);
	}
	
	public static boolean addSmsToQueue(SMSQueue smsQueue) {
		return SmsGateWay.addSmsToQueue(smsQueue);
	}
	
	public static boolean addDLRRequest(DLRQueue dlrData) {
		if(dlrData != null) {
			getDlrQueue().add(dlrData);
			logger.info("DLR Added - " + dlrData.getMessageId() + "; state: " + dlrData.getState());
			return true;
		}	
		logger.warn("");
		return false;
	}
	
	public static void SaveDLRResponse(DLRQueue deliverSm, long started, long finished
			, String errorMessage) {
		boolean doNextAttempt = (deliverSm.getSendDLRUntil().after(new Timestamp(System.currentTimeMillis()))
				&& deliverSm.getAttempts() < ServerController.getDlrReSendMaxAttempts())
				&& errorMessage != null;
		logger.info("process dlr for messageId - " + deliverSm.getMessageId() + "; doNextAttempt - " + doNextAttempt 
				+ "; sendDLRUntil - " + deliverSm.getSendDLRUntil());
		SmsGateWay.processServerDLRResponse(deliverSm.getMessageId(), started, finished
				, deliverSm.getAttempts(), errorMessage, doNextAttempt);
		if(doNextAttempt) {
			logger.info("trying to resend DLR for transaction - " + deliverSm.getMessageId() 
					+ "| current Attempt - " + deliverSm.getAttempts() + "| Send DLR Until - " + deliverSm.getSendDLRUntil());
			deliverSm.setNextAttempt(new Timestamp(System.currentTimeMillis() + ServerController.getDlrReSendIntervalAttemptSec() * 1000));
			ServerController.getDlrQueue().add(deliverSm);
		}
	}
	
	public static void addMORequest(MOIncoming moData) {
		logger.info("received mo request for messageId=" + moData.getMessageId());
		SmppServer.getMoQueue().add(new SmsData(moData.getMessageId(), moData.getFromAD(), moData.getFromTON()
				, moData.getFromNP(), moData.getToAD(), moData.getToAN(), moData.getToNP(), moData.getMessage()
				, (short)1, moData.getDcs(), moData.getPid(), moData.getOccurred(), null
				, moData.getSystemId(), "0", "0", "140"));
	}

	public static void SaveMOResponse(long messageId, Timestamp started, Timestamp finished
			, String errorMessage, String state, int accessId) {
		SmsGateWay.saveSmppMoResponse(messageId, started, finished
			, errorMessage, state, accessId);
	}
	
	public static void clearDLRList(Timestamp expiredTime) {

	}
	
	public static void saveDLRAwaitingRespose(DLRQueue deliverSm) {
		
	}
	
//	public static Timestamp getExpiredIn(String time, Long accessDefaultLiveTime) throws ProcessRequestException {
//		try {
//			long now = System.currentTimeMillis() - 1000; //correlation
//			/*
//			 * if access defaul live time is null and validity period is null
//			 * set Global default live Time
//			 */
//			if(time == null && accessDefaultLiveTime == null) 
//				return new Timestamp(System.currentTimeMillis() + SmsGateWay.getDefaultSMSLiveTime());
//			if(time == null) {
//				/*
//				 * Global live time can not be less then access validity period!
//				 */
//				if((now + accessDefaultLiveTime > (now + SmsGateWay.getDefaultSMSLiveTime())))
//					return new Timestamp(now + SmsGateWay.getDefaultSMSLiveTime());
//				else
//					return new Timestamp(now + accessDefaultLiveTime);
//			}
//			long expired = fromTimeInToLong(time);
//			//check validity less then Global live time
//		 	if(expired < (now + SmsGateWay.getDefaultSMSLiveTime()))
//		 		return new Timestamp(expired);
//		 	else
//		 		return new Timestamp(System.currentTimeMillis() + SmsGateWay.getDefaultSMSLiveTime());
//		} catch (Exception e) {
//			logger.warn("Validty Period wrong format - " + time);
//			throw new ProcessRequestException("Invalid Validty Period value.", 0x00000062);
//		}
//	}
	
	public static Timestamp getExpired(String time, Long accessDefaultLiveTime, ExpiredType expiredType) throws ProcessRequestException {
		try {
			long now = System.currentTimeMillis() - 1000; //correlation
			/*
			 * if access default live time is null and validity period is null
			 * set Global default live Time
			 */
			if(time == null && accessDefaultLiveTime == null) 
				return new Timestamp(System.currentTimeMillis() + SmsGateWay.getDefaultSMSLiveTime());
			if(time == null) {
				/*
				 * Global live time can not be less then access validity period!
				 */
				if((now + accessDefaultLiveTime > (now + SmsGateWay.getDefaultSMSLiveTime())))
					return new Timestamp(now + SmsGateWay.getDefaultSMSLiveTime());
				else
					return new Timestamp(now + accessDefaultLiveTime);
			}
			long parsedDate = now;
			if(expiredType.equals(ExpiredType.EXPIREDAT)) {
				String a = time.substring(0, 6) + " " + time.substring(6, 12);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd HHmmss");
				parsedDate = dateFormat.parse(a).getTime();
			} else {
				parsedDate = fromTimeInToLong(time);
			}
		 	//check validity less then Global live time
		 	if(parsedDate < (now + SmsGateWay.getDefaultSMSLiveTime()))
		 		return new Timestamp(parsedDate);
		 	else
		 		return new Timestamp(System.currentTimeMillis() + SmsGateWay.getDefaultSMSLiveTime());
		} catch (Exception e) {
			logger.warn("Validty Period wrong format - " + time);
			logger.error(e.getMessage(), e);
			throw new ProcessRequestException("Invalid Validty Period value.", 0x00000062);
		}
	}

	public static Timestamp getScheduleDeliveryTime(String time, Time canSendFromTime, Time canSendToTime
			, SchedulerDays days, DeliveryType deliveryType) throws ProcessRequestException {
		try {
			long now = System.currentTimeMillis();
			long schedulerTime = now;
			Calendar cal = Calendar.getInstance();
			Calendar calFrom = Calendar.getInstance();
			Calendar calTo = Calendar.getInstance();
			int dayOfWeek;
			String[] timeParts;
			if(time != null) { 														// check scheduler time
				//150505202000000
				if(deliveryType.equals(DeliveryType.DELIVERYAT)) {
					String a = time.substring(0, 6) + " " + time.substring(6, 12);  
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd HHmmss");
					schedulerTime = dateFormat.parse(a).getTime();	
				} else {
					schedulerTime = fromTimeInToLong(time);	
				}				
			}
			if(schedulerTime > now + SmsGateWay.getMaxSchedulerTime()) {	// check schedulerTime less Max Scheduler Time
				logger.warn("schedulerTime schedulerTime more then MaxSchedulerTime ");
				throw new ProcessRequestException("Invalid scheduled delivery time. schedulerTime more then MaxSchedulerTime(canSendFromTime is null)", 0x00000061);
			}
			// restrictions 
			if(canSendFromTime == null && canSendToTime == null) {				// there is no any scheduler restrictions
				dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
				if(days.equals(SchedulerDays.ANY) 										// valid day of week
						|| (days.equals(SchedulerDays.WORKINGDAYS) && (dayOfWeek > 1 && dayOfWeek < 7))
						|| (days.equals(SchedulerDays.WEEKEND) && (dayOfWeek == 1 || dayOfWeek == 7))
						|| days.ordinal() == dayOfWeek) {
					return new Timestamp(schedulerTime);	
				} else {
					logger.warn("scheduler day allow " + days.ordinal() + "; now " + dayOfWeek);  
					throw new ProcessRequestException("Invalid scheduled delivery time. Send today is not allowed", 0x00000061);	
				}
			}
			//TODAY CHECK
			cal.setTime(new Date(schedulerTime));
			dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			if(days.equals(SchedulerDays.ANY) 										// valid day of week
					|| (days.equals(SchedulerDays.WORKINGDAYS) && (dayOfWeek > 1 && dayOfWeek < 7))
					|| (days.equals(SchedulerDays.WEEKEND) && (dayOfWeek == 1 || dayOfWeek == 7))
					|| days.ordinal() == dayOfWeek) {					
				if(canSendFromTime != null) {										// check send from limit 
					timeParts = canSendFromTime.toString().split(":");
					calFrom.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
					calFrom.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
					calFrom.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
					if(calFrom.getTimeInMillis() < schedulerTime) {
						if(schedulerTime > now + SmsGateWay.getMaxSchedulerTime()) {	// check schedulerTime less Max Scheduler Time
							logger.warn("schedulerTime schedulerTime more then MaxSchedulerTime. Scheduler Time - " + schedulerTime 
									+ "; Max Value - " + (now + SmsGateWay.getMaxSchedulerTime()));
							throw new ProcessRequestException("Invalid scheduled delivery time. Scheduler Time - " + schedulerTime 
									+ "; Max Value - " + (now + SmsGateWay.getMaxSchedulerTime()), 0x00000061);	
						}
						if(canSendToTime != null) {									// check send to limit
							timeParts = canSendToTime.toString().split(":");
							calTo.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
							calTo.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
							calTo.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
							if(calTo.getTimeInMillis() > schedulerTime)				//today not too late
								return new Timestamp(schedulerTime);
						} else
							return new Timestamp(schedulerTime);	
					} else { //send today use from limit time
						return new Timestamp(calFrom.getTimeInMillis());
					}
				} else if(canSendToTime != null) {											// check send to limit
					timeParts = canSendToTime.toString().split(":");
					calTo.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
					calTo.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
					calTo.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
					if(calTo.getTimeInMillis() > schedulerTime) {
						return new Timestamp(schedulerTime);	
					}
				}	
			}
			//TRY TO ADD TO NEXT DAY CHECK
			calFrom.add(Calendar.DATE, 1);
			if(canSendFromTime != null) {	
				timeParts = canSendFromTime.toString().split(":");
				calFrom.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
				calFrom.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
				calFrom.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
				logger.info("MAX SCHEDULER - " + new Timestamp(now + SmsGateWay.getMaxSchedulerTime()));
				logger.info("MAX calFrom - " + new Timestamp(calFrom.getTime().getTime()));
				if(calFrom.getTime().getTime() < (now + SmsGateWay.getMaxSchedulerTime())) {
					dayOfWeek = calFrom.get(Calendar.DAY_OF_WEEK);
					if(days.equals(SchedulerDays.ANY) 										// valid day of week
							|| (days.equals(SchedulerDays.WORKINGDAYS) && (dayOfWeek > 1 && dayOfWeek < 7))
							|| (days.equals(SchedulerDays.WEEKEND) && (dayOfWeek == 1 || dayOfWeek == 7))
							|| days.ordinal() == dayOfWeek) {
						
						return new Timestamp(calFrom.getTimeInMillis());
					}
				}
			}
			logger.warn("Invalid scheduled delivery time. NEXT DAY invalid scheduler time - " + new Timestamp(schedulerTime) 
					+ " Limit from " + canSendFromTime + " to - " + canSendToTime);
			throw new ProcessRequestException("Invalid scheduled delivery time. NEXT DAY invalid scheduler time - " + new Timestamp(schedulerTime) 
					+ " Limit from " + canSendFromTime + " to - " + canSendToTime, 0x00000061);	
		} catch (ParseException e) {
			logger.info("scheduleDeliveryTime wrong format - " + time);
			logger.error(e.getMessage(), e);
			throw new ProcessRequestException("Invalid scheduled Period value.", 0x00000061);
		}  catch (Exception e) {
			logger.info("scheduleDeliveryTime - " + time + "; Delivery Type - " + deliveryType 
					+ "; From - " + canSendFromTime + ", To - " + canSendToTime);
			logger.error(e.getMessage(), e);
			throw new ProcessRequestException("Invalid scheduled Period value.", 0x00000061);
		}
	}
	//get from Delivery||Expired AT
	private static long fromTimeInToLong(String time) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		if(time == null) {
			return c.getTimeInMillis();
		}
		if(!"00".equals(time.substring(0,2)))
			c.add(Calendar.YEAR, Integer.valueOf(time.substring(0,2)));
		if(!"00".equals(time.substring(2,4)))
			c.add(Calendar.MONTH, Integer.valueOf(time.substring(2,4)));
		if(!"00".equals(time.substring(4,6)))
			c.add(Calendar.DAY_OF_MONTH, Integer.valueOf(time.substring(4,6)));
		if(!"00".equals(time.substring(6,8)))
			c.add(Calendar.HOUR, Integer.valueOf(time.substring(6,8)));
		if(!"00".equals(time.substring(8,10)))
			c.add(Calendar.MINUTE, Integer.valueOf(time.substring(8,10)));
		if(!"00".equals(time.substring(10,12)))
			c.add(Calendar.SECOND, Integer.valueOf(time.substring(10,12)));
		return c.getTimeInMillis();
	}
	
	public static boolean processCancelSmRequest(long messageId, int accessId) {
		return SmsGateWay.processCancelSmRequest(messageId, AccessType.SMPP, accessId);
	}
	
	public static String getSmsContainerLength(short dcs, short quantity) {
		if(dcs == 246) {// for ICB - Project. receive byte code with UDH  
			return "140";
		}
		if((dcs > -1 && dcs < 4) || (dcs > 15 && dcs < 20)
				|| (dcs > 31 && dcs < 36) || (dcs > 47 && dcs < 52)
				|| (dcs > 63 && dcs < 68) || (dcs > 79 && dcs < 84)) {
			if(quantity > 1)
				return "153";
			else
				return "160";
		} else if((dcs > 7 && dcs < 12) || (dcs > 23 && dcs < 28)
				|| (dcs > 39 && dcs < 44) || (dcs > 55 && dcs < 60)
				|| (dcs > 71 && dcs < 76) || (dcs > 87 && dcs < 92)) {
			if(quantity > 1)
				return "67";
			else
				return "70";
		} else {
			if(quantity > 1)
				return "134";
			else
				return "140";
		}
	}
	
	public static int getSegmentLenForPayLoad(short dcs , int messageLen) {
		if((dcs > -1 && dcs < 4) || (dcs > 15 && dcs < 20)
				|| (dcs > 31 && dcs < 36) || (dcs > 47 && dcs < 52)
				|| (dcs > 63 && dcs < 68) || (dcs > 79 && dcs < 84)
				|| (dcs > 95 && dcs < 100) || (dcs > 111 && dcs < 116)
				|| (dcs > 207 && dcs < 224) || (dcs > 239 && dcs < 244)
				|| (dcs > 247 && dcs < 252)) {
			return messageLen > 160 ? 153 : 160;
		}
		return messageLen > 140 ? 134 : 140;
	}
	
	public static ConcurrentHashMap<String, SMPPConnectionInfo> getConnections() {
		return connections;
	}

	public static void setConnections(ConcurrentHashMap<String, SMPPConnectionInfo> connections) {
		ServerController.connections = connections;
	}

	public static ConcurrentHashMap<Integer, SmppServer> getServers() {
		return servers;
	}

	public static void setServers(ConcurrentHashMap<Integer, SmppServer> servers) {
		ServerController.servers = servers;
	}

	public static ResetCounter getResetCounter() {
		return resetCounter;
	}
	
	public static ConcurrentHashMap<Integer, SMPPServerSession> getSmppBindSessions() {
		return smppBindSessions;
	}

	void setSmppBindSessions(ConcurrentHashMap<Integer, SMPPServerSession> smppBindSessions) {
		ServerController.smppBindSessions = smppBindSessions;
	}

	public static int getSendDLRUntil() {
		return sendDLRUntil;
	}

	public static void setSendDLRUntil(int sendDLRUntil) {
		ServerController.sendDLRUntil = sendDLRUntil;
	}

	public static ExecutorService getExecDLR() {
		return execDLR;
	}
	public static ExecutorService getExecMO() {
		return execMO;
	}

//	public static ExecutorService getExecSubmitSm() {
//		return execSubmitSm;
//	}

	public static ConcurrentHashMap<String, PoolableSpeedLimiter> getLimmiter() {
		return limmiter;
	}

	public static void setLimmiter(ConcurrentHashMap<String, PoolableSpeedLimiter> limmiter) {
		ServerController.limmiter = limmiter;
	}

	public static ConcurrentLinkedQueue<DLRQueue> getDlrQueue() {
		return dlrQueue;
	}

	public static void setDlrQueue(ConcurrentLinkedQueue<DLRQueue> dlrQueue) {
		ServerController.dlrQueue = dlrQueue;
	}

	public static int getCheckSMPPAccessList() {
		return checkSMPPAccessList;
	}

	public static void setCheckSMPPAccessList(int checkSMPPAccessList) {
		ServerController.checkSMPPAccessList = checkSMPPAccessList;
	}

	public static int getDlrReSendMaxAttempts() {
		return dlrReSendMaxAttempts;
	}

	public static void setDlrReSendMaxAttempts(int dlrReSendMaxAttempts) {
		ServerController.dlrReSendMaxAttempts = dlrReSendMaxAttempts;
	}

	public static int getDlrReSendIntervalAttemptSec() {
		return dlrReSendIntervalAttemptSec;
	}

	public static void setDlrReSendIntervalAttemptSec(int dlrReSendIntervalAttemptSec) {
		ServerController.dlrReSendIntervalAttemptSec = dlrReSendIntervalAttemptSec;
	}

	public static int getDlrSendSpeedPerSec() {
		return dlrSendSpeedPerSec;
	}

	public static void setDlrSendSpeedPerSec(int dlrSendSpeedPerSec) {
		ServerController.dlrSendSpeedPerSec = dlrSendSpeedPerSec;
	}
}
