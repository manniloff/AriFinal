package com.unifun.sigtran.smsgate.smpp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.extra.ProcessRequestException;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.models.ClientDLRWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientsGroups;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.smpp.client.SmppClient;
import com.unifun.sigtran.smsgate.smpp.common.ClearSMParts;
import com.unifun.sigtran.smsgate.smpp.common.ConnectionLimits;
import com.unifun.sigtran.smsgate.smpp.common.MessageContainer;
import com.unifun.sigtran.smsgate.smpp.common.ResetCounter;
import com.unifun.sigtran.smsgate.smpp.workers.ClearDLRList;
import com.unifun.sigtran.smsgate.smpp.workers.SMPPClientGetMTSMSToSend;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;


public class ClientController {

	private final static int DEFAUTL_THREAD_POOL = 200;
	private final static int DEFAUTL_CHECK_DLR_WAITING_LIST 		= 10;	//SECONDS
	private final static int DEFAULT_SMS_LIFE_TIME 					= 1;	//MINUTES
	private final static int DEFAULT_DELAY_TIME 					= 20;	//SECONDS
	private final static int DEFAULT_RESEND_SUBMIT_PERSEC 			= 10;	//SECONDS
	private final static int DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC 	= 20;	//SECONDS
//	private static Timer tResetCounter;
	private static int checkDLRWaitingList;
	private static int smppMaxMOMessageParts;
	private static ClearSMParts clearSMParts;
	private static ClearDLRList clearDLRList;
	private static ResetCounter resetCounter;
	private static SMPPClientGetMTSMSToSend getMTSMSToSend;
//	private static ConcurrentLinkedQueue<ConnectionLimits> speedLimit = new ConcurrentLinkedQueue<>();
	
	private static long defaultDelayTime;
	private static int defaultSmsLiveTime;
	private static int reSendSubmitPerSec;
	private static int increaseWaitDLRFromSMSC;
//	private static AtomicInteger notificationSMCounter;
	private static ExecutorService exec; 					//Controller's pool
	private static ExecutorService esProcessRequset; 		//Pool for incoming requests
	private static ExecutorService esSendSM; 				//Pool for send SMS
	private static LinkedList<SMPPClientsGroups> groupList = new LinkedList<>();
	private static ConcurrentHashMap<Integer, SmppClient> clients = new ConcurrentHashMap<Integer, SmppClient>();
	private static ConcurrentHashMap<String, MessageContainer> messageParts = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<SendData> smppMTQueue = new ConcurrentLinkedQueue<>();
	private static ConcurrentHashMap<Long , ClientDLRWaitingList> dlrWaitingQueue = new ConcurrentHashMap<>();
	private static Logger logger = LogManager.getLogger(ClientController.class);
	
	public ClientController() {
		this(DEFAUTL_THREAD_POOL, DEFAUTL_CHECK_DLR_WAITING_LIST, DEFAULT_SMS_LIFE_TIME
				, DEFAULT_DELAY_TIME, DEFAULT_RESEND_SUBMIT_PERSEC, DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC);
	}
	/**
	 * 
	 * @param threadPoolSize
	 */
	public ClientController(int threadPoolSize) {
		this(threadPoolSize, DEFAUTL_CHECK_DLR_WAITING_LIST, DEFAULT_SMS_LIFE_TIME
				, DEFAULT_DELAY_TIME, DEFAULT_RESEND_SUBMIT_PERSEC, DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC);
	}
	/**
	 * 
	 * @param threadPoolSize
	 * @param smsLiveTime
	 */
	public ClientController(int threadPoolSize, int smsLiveTime) {
		this(threadPoolSize, DEFAUTL_CHECK_DLR_WAITING_LIST, smsLiveTime, DEFAULT_DELAY_TIME
				, DEFAULT_RESEND_SUBMIT_PERSEC, DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC);
	}
	/**
	 * 
	 * @param threadPoolSize
	 * @param checkDLRWaitingList
	 * @param smsLiveTime
	 */
	public ClientController(int threadPoolSize, int checkDLRWaitingList, int smsLiveTime) {
		this(threadPoolSize, checkDLRWaitingList, smsLiveTime, DEFAULT_DELAY_TIME, DEFAULT_RESEND_SUBMIT_PERSEC
				,DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC);
	}
	
	/**
	 * 
	 * @param threadPoolSize
	 * @param checkDLRWaitingList
	 * @param smsLiveTime
	 * @param reSendSubmitPerSec
	 */
	public ClientController(int threadPoolSize, int checkDLRWaitingList, int smsLiveTime, int reSendSubmitPerSec) {
		this(threadPoolSize, checkDLRWaitingList, smsLiveTime, DEFAULT_DELAY_TIME, reSendSubmitPerSec
				, DEFAULT_INCREASE_WAIT_DLR_FROM_SMSC);
	}
	
	public ClientController(int threadPoolSize, int checkDLRWaitingList, int smsLiveTime, int reSendSubmitPerSec
			, int increaseWaitDLRFromSMSC) {
		this(threadPoolSize, checkDLRWaitingList, smsLiveTime, DEFAULT_DELAY_TIME, reSendSubmitPerSec
				, increaseWaitDLRFromSMSC);
	}
	
	/**
	 * 
	 * @param threadPoolSize		- ExecuterServices poll size
	 * @param checkDLRWaitingList	- repetition frequency of checks DLRWaitingList
	 * @param defaultSmsLiveTime	- default live of sms
	 * @param defaultDelayTime		- default delay time of starts worker    
	 */
	public ClientController(int threadPoolSize, int checkDLRWaitingList, int defaultSmsLiveTime
			, int defaultDelayTime, int reSendSubmitPerSec, int increaseWaitDLRFromSMSC) {
		this.checkDLRWaitingList = checkDLRWaitingList * 1000; 
		this.defaultSmsLiveTime = defaultSmsLiveTime * 60 * 1000;
		this.defaultDelayTime = defaultDelayTime * 1000;
		this.reSendSubmitPerSec = reSendSubmitPerSec;
		this.increaseWaitDLRFromSMSC = increaseWaitDLRFromSMSC * 1000;
		exec = Executors.newCachedThreadPool( 
				 new CustomThreadFactoryBuilder()
				.setNamePrefix("_SmppClientContreller").setDaemon(false)
				.setPriority(Thread.NORM_PRIORITY).build());
		esProcessRequset = Executors.newFixedThreadPool(threadPoolSize
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_SmppClientDBWriter").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
		esSendSM = Executors.newFixedThreadPool(threadPoolSize
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_SmppClientSendSM").setDaemon(false)
				.setPriority(Thread.MAX_PRIORITY).build());
	}
	
	public boolean initSmppClient() {
		List<SMPPClientConfig> clientList = SmsGateWay.getSmppClientConnectionList(0);
		List<SMPPClientsGroups> groupList = SmsGateWay.getSmppClientConnectionGroups();
		if(clientList.isEmpty() || groupList.isEmpty()) {
			logger.error("initialized info is empty. Client list - " + clientList.isEmpty() + "; Group list - " + groupList.isEmpty());
			return false;	
		}
		clientList.forEach(config -> {
			ConnectionLimits cLimit = new ConnectionLimits(config.getSystemId(), config.getSpeedLimit());
			getClients().putIfAbsent(config.getId(), new SmppClient(config, messageParts, cLimit));
		});
		groupList.forEach(group -> group.setAvailableClients(new AtomicInteger(0)));
		this.groupList.addAll(groupList);
		//get all awaiting dlr messages.
		List<ClientDLRWaitingList> recoveryQueue = SmsGateWay.getClientAwaitingDLRQueue();
		recoveryQueue.forEach(row -> dlrWaitingQueue.putIfAbsent(row.getRemoteId(), row));
		//Cleaner for not complete sms
		clearSMParts = new ClearSMParts("ClientController_ClearSMParts", messageParts, false);
		clearSMParts.start();
		//Clear DLR waiting List
		clearDLRList = new ClearDLRList(defaultDelayTime, checkDLRWaitingList, "ClientController_ClearDLRList", true);
		clearDLRList.start();
		//Reset incoming counters
		resetCounter = new ResetCounter(new ConcurrentHashMap<String, ConnectionLimits>(), "ClientController_ResetCounter");
		resetCounter.start();
		getMTSMSToSend = new SMPPClientGetMTSMSToSend(reSendSubmitPerSec);
		getMTSMSToSend.start();
		logger.info("Reset counters started");
		for (SmppClient client : getClients().values()) {
			exec.submit(client);
			resetCounter.getConnectionLimits().putIfAbsent(client.getClientConfig().getSystemId(), client.getConLimit());
		}
		return true;
	}
	
	public boolean stopSmppClient() {
		clearSMParts.interrupt();
		clearDLRList.interrupt();
		resetCounter.interrupt();
		getMTSMSToSend.interrupt();
		for (SmppClient client : getClients().values()) {
			logger.info("stoping " + client.getClientConfig().getSystemId());
			client.stop();
		}
		exec.shutdown();
		esSendSM.shutdown();
		esProcessRequset.shutdown();
		return true;
	}

	public static boolean isAnyClientAvailable() {
		return clients.values().stream().filter(client -> client.getClientAvailable().get() == true).findFirst().isPresent();
	}
	
	public static List<Integer> checkClientIsDown() {
		List<Integer> clientList = new ArrayList<>();
		clients.values().forEach(client -> {
			if(client.getClientAvailable().get() 
					&& client.getReconnectTears().get() == 0)
				clientList.add(client.getClientConfig().getId());
		});
		return clientList;
	}
	
	public static int getClinetsActiveSessions() {
		int result = 0;
		for (SmppClient client : clients.values()) {
			if(client.getSmppSession() != null)
				result++;
		}
		return result;
	}
	
	public synchronized static boolean resetClinet(int clientId) {
		//reload client config info.
		List<SMPPClientConfig> clientList = SmsGateWay.getSmppClientConnectionList(clientId);
		List<SMPPClientsGroups> groupList = SmsGateWay.getSmppClientConnectionGroups();
		if(clientId == 0) {
			for (SmppClient client : getClients().values()) {
				logger.info("stoping " + client.getClientConfig().getSystemId());
				client.setClientIsRuning(false);
				if(resetCounter.getConnectionLimits().remove(client.getClientConfig().getSystemId()) != null) {
					logger.info("resetCounter cleared for clientId - " + client.getClientConfig().getId());
				} else {
					logger.warn("could not clear from resetCounter clientId - " + client.getClientConfig().getId());
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			clients = new ConcurrentHashMap<>();
			ClientController.groupList = new LinkedList<>();
			
			if(clientList.isEmpty() || groupList.isEmpty()) {
				logger.warn("initialized info is empty. Client list - " + clientList.isEmpty() + "; Group list - " + groupList.isEmpty());
				return true;	
			}
			
			clientList.forEach(config -> {
				ConnectionLimits cLimit = new ConnectionLimits(config.getSystemId(), config.getSpeedLimit());
				getClients().putIfAbsent(config.getId(), new SmppClient(config, messageParts, cLimit));
			});
			groupList.forEach(group -> group.setAvailableClients(new AtomicInteger(0)));
			ClientController.groupList.addAll(groupList);
			for (SmppClient client : getClients().values()) {
				exec.submit(client);
				resetCounter.getConnectionLimits().putIfAbsent(client.getClientConfig().getSystemId(), client.getConLimit());
			}	
		} else {
			SmppClient smppClient = clients.get(clientId);
			if(smppClient != null) {
				logger.info("stoping " + smppClient.getClientConfig().getSystemId());
				smppClient.setClientIsRuning(false);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
				clientList.forEach(newClinet -> {
					ConnectionLimits conLimit = new ConnectionLimits(newClinet.getSystemId(), newClinet.getSpeedLimit());
					SmppClient client = new SmppClient(newClinet, messageParts, conLimit);
					exec.submit(client);
					resetCounter.getConnectionLimits().putIfAbsent(newClinet.getSystemId(), conLimit);
					clients.replace(clientId, client);
				});
			} else {
				logger.warn("client was not found - " + clientId);
				return false;
			}
		}
		return true;
	}
		
	public static boolean getFromMTSMSQueue() {
		boolean result = false;
		SmppClient alternativeClient = getAvailableSMPPConncetion();
		if(alternativeClient != null) {
			SendData sms = smppMTQueue.poll();
			if(sms != null) {
				int t = alternativeClient.getConLimit().counter.get();
				alternativeClient.getConLimit().counter.set(t + sms.getQuantity());
				alternativeClient.getSmsToSend().add(sms);
				logger.info("received submitSM request for MessageId - " + sms.getMessageId());	
	
			}
			result = true;
		}
		return result;
	}
			
	public static void sendSM(SendData smsData) {
		SmppClient alternativeClient = getAvailableSMPPConncetion();
		if(alternativeClient != null) {
			int t = alternativeClient.getConLimit().counter.get();
			alternativeClient.getConLimit().counter.set(t + smsData.getQuantity());
			alternativeClient.getSmsToSend().add(smsData);
			logger.info("trying to send submitSM request for MessageId - " + smsData.getMessageId());
		} else {
			logger.warn("Could not found active session");
			ClientController.getSmppMTQueue().add(smsData);
		}
	}
	
	public static void addDlrWaitingList(Long remoteId, int clientId, SendData smsData, String errorMessage
			, Timestamp started, Timestamp finished, boolean needWaitDLR) {
		Timestamp waitDLR = new Timestamp(smsData.getSendUntil().getTime() + increaseWaitDLRFromSMSC);
		SmsGateWay.saveClientDlrWaitingList(remoteId, clientId, smsData.getMessageId(), errorMessage
				, started, finished, waitDLR);
		if(remoteId > 0) {
			dlrWaitingQueue.putIfAbsent(remoteId, new ClientDLRWaitingList(remoteId, smsData.getMessageId(), clientId
					, waitDLR));
			logger.info("Waiting DLR for " + remoteId);	
			if(!needWaitDLR) {										//no need to wait dlr response
				ProcessDlrRequest(remoteId, "6", clientId);			//6 - ACCEPTED 
			}
			return;
		} else if(smsData.getSendUntil().before(new Timestamp(System.currentTimeMillis()))) { //sms is dead;
			SmsGateWay.processNegativeSubmitResponse(smsData, "3");	//3 - EXPIRED
		} else if((remoteId == -4 || remoteId == -6 || remoteId == -8)) {
			//-4 TimeOut; -6 Throttling; -8 IOException
			logger.info("Negative Response from SMSC. messageId - " + smsData.getMessageId());
			sendSM(smsData);
		}  else {
			logger.info("sms was rejected from SMSC. messageId - " + smsData.getMessageId());
			SmsGateWay.processNegativeSubmitResponse(smsData, "5"); //5 - UNDELIVERED
		}
	}

	public static void ProcessDlrRequest(Long remoteId, String state, int clientId) {
		ClientDLRWaitingList temp = ClientController.getDlrWaitingQueue().remove(remoteId);
		if(temp == null) {
			logger.info("Unknown remoteId - " + remoteId);
			SmsGateWay.saveSMPPIncomingDLRLog(remoteId, state, clientId);
		} else {
			SmsGateWay.processClientDlrRequest(remoteId, state, clientId);	
		}
	}
	
	/**
	 * 
	 * @param sms		- smsData	
	 * @param clientId	- SmppClient received MO message
	 * @return boolean
	 */
	public static boolean addMORequest(SmsData sms, int clientId) {
		//TODO
		return false;
	}
	
	/**
	 * MO didn't pass validation
	 * @param sms 				- smsData
	 * @param clientId			- SmppClient received MO message
	 * @param validationResult	- validation result
	 * @param validationMessage	- validation message
	 * @return boolean
	 */
	public static boolean addMORequest(SmsData sms, int clientId, int validationResult
			, String validationMessage) {
		//TODO 
		return false;
	}
	
	/**
	 * 
	 * @return SmppClient - available client to send submitSM request
	 */
	private static SmppClient getAvailableSMPPConncetion() {
		Optional<SMPPClientsGroups> gResult;
		Optional<SmppClient> cResult;
		while ((gResult = getGroupList().stream().filter(group -> group.getAvailableClients().get() > 0)
				.max(new Comparator<SMPPClientsGroups>() {
				public int compare(SMPPClientsGroups g1, SMPPClientsGroups g2) {
					return Integer.compare(g1.getGroupPriority(), g2.getGroupPriority());
				}})).isPresent()) {
			int groupId = gResult.get().getGroupId();
			if((cResult = getClients().values().stream().filter(x -> x.getClientConfig().getGroupID() == groupId && x.getClientAvailable().get() == true)
				.max(new Comparator<SmppClient>() {
					public int compare(SmppClient c1, SmppClient c2) {
						return Integer.compare(c1.getClientConfig().getClientPriority() + (c1.getClientConfig().getSpeedLimit() 
													- c1.getConLimit().counter.get() - c1.getSmsToSend().size())
								, c2.getClientConfig().getClientPriority() + (c2.getClientConfig().getSpeedLimit() 
										- c2.getConLimit().counter.get() - c2.getSmsToSend().size()));						
					}})).isPresent()) {
				return cResult.get();
			}
		}
//		groupList.forEach(x -> System.out.println(x.toString()));
//		Optional<SMPPClientsGroups> gResult = groupList.stream().filter(group -> group.getAvailableClients().get() > 0)
//				.max(new Comparator<SMPPClientsGroups>() {
//					public int compare(SMPPClientsGroups g1, SMPPClientsGroups g2) {
//						return Integer.compare(g1.getGroupPriority(), g2.getGroupPriority());
//					}});
//		if(!gResult.isPresent()) {
//			logger.warn("Active group not found! MessageId = " + MessageId);
//			return null;
//		}
//		Optional<SmppClient> cResult = clients.values().stream().filter(x -> x.getClientConfig().getGroupID() == gResult.get().getGroupId() && x.getClientAvailable().get() == true)
//				.max(new Comparator<SmppClient>() {
//					public int compare(SmppClient c1, SmppClient c2) {
//						return Integer.compare(c1.getClientConfig().getClientPriority() + (c1.getClientConfig().getSpeedLimit() - c1.getSmsSended().get())
//								, c2.getClientConfig().getClientPriority() + (c2.getClientConfig().getSpeedLimit() - c2.getSmsSended().get()));						
//					}});
//		if(!gResult.isPresent()) {
//			logger.warn("Active client not found! MessageId = " + MessageId);
//			return null;
//		}
//		return cResult.get(); 
		return null;
	}
		
	public static String getExpiredAt(Timestamp time) throws ProcessRequestException {
		if(time == null) 
			return null;
		String stringTime = "0000";
		long timeDiff = time.getTime() - System.currentTimeMillis();
		if(timeDiff < 0) {
			logger.info(String.format("Validity time %s less then now %s", time, new Timestamp(System.currentTimeMillis()))); //Validity
			return null;
		}
		long diffDays = timeDiff / (24 * 60 * 60 * 1000);
		if(diffDays != 0) {
			timeDiff = timeDiff - diffDays * (24 * 60 * 60 * 1000);
			stringTime += (diffDays > 9) ? diffDays : "0" + diffDays;
		} else
			stringTime += "00";
		long diffHours = timeDiff / (60 * 60 * 1000);
		if(diffHours != 0) {
			timeDiff = timeDiff - diffHours * (60 * 60 * 1000);
			stringTime += (diffHours > 9) ? diffHours : "0" + diffHours;
		} else 
			stringTime += "00";
		long diffMinutes = timeDiff / (60 * 1000);
		if(diffMinutes != 0) {
			timeDiff = timeDiff - diffMinutes * (60 * 1000);
			stringTime += (diffMinutes > 9) ? diffMinutes : "0" + diffMinutes;
		} else 
			stringTime += "00";
		long diffSeconds = timeDiff / 1000;
		if(diffSeconds != 0) {
//			timeDiff = timeDiff - diffSeconds * 1000;
			stringTime += (diffSeconds > 9) ? diffSeconds : "0" + diffSeconds;
		} else 
			stringTime += "00";
		return stringTime + "000R";		
	}
	
	public static Charset getCharset(int dcs) {
		if((dcs > -1 && dcs < 4) || (dcs > 15 && dcs < 20)
				|| (dcs > 31 && dcs < 36) || (dcs > 47 && dcs < 52)
				|| (dcs > 63 && dcs < 68) || (dcs > 79 && dcs < 84) //) {
				//Compressed 
				|| (dcs > 95 && dcs < 100) || (dcs > 111 && dcs < 116)
				|| (dcs > 207 && dcs < 224) || (dcs > 239 && dcs < 244)
				|| (dcs > 247 && dcs < 100) || (dcs > 111 && dcs < 252)) {
			if(dcs == 3)
				return StandardCharsets.ISO_8859_1;
			return StandardCharsets.US_ASCII;
		}
		if((dcs > 7 && dcs < 12) || (dcs > 23 && dcs < 28)
				|| (dcs > 39 && dcs < 44) || (dcs > 55 && dcs < 60)
				|| (dcs > 71 && dcs < 76) || (dcs > 87 && dcs < 92) //) {
				//Compressed 
				|| (dcs > 103 && dcs < 108) || (dcs > 119 && dcs < 124)
				|| (dcs > 224 && dcs < 240)) {
			return StandardCharsets.UTF_16BE;
		}
		return StandardCharsets.UTF_8;
	}
	
	public static short getSmsContainers(short dcs, int messageLen) {
		boolean needConcatination = false;
		short containerLen = 0;
		short smscount = 1;
		if((dcs > -1 && dcs < 4) || (dcs > 15 && dcs < 20)
				|| (dcs > 31 && dcs < 36) || (dcs > 47 && dcs < 52)
				|| (dcs > 63 && dcs < 68) || (dcs > 79 && dcs < 84)) {
			if(messageLen <= 160)
				containerLen = 160;
			else {
				containerLen = 153;
				needConcatination = true;
			}
		} else if((dcs > 7 && dcs < 12) || (dcs > 23 && dcs < 28)
				|| (dcs > 39 && dcs < 44) || (dcs > 55 && dcs < 60)
				|| (dcs > 71 && dcs < 76) || (dcs > 87 && dcs < 92)) {
			if(messageLen <= 70)
				containerLen = 70;
			else {
				containerLen = 67;
				needConcatination = true;
			}
		} else {
			if(messageLen <= 140)
				containerLen = 140;
			else {
				containerLen = 134;
				needConcatination = true;
			}
		}
		
		if(needConcatination) {
			smscount = (short) (messageLen / containerLen);
			if(messageLen % containerLen > 0)
				smscount++;
		}
		return smscount;
	}
	public static short getContainerLen(int dcs, int messageLen) {
		if((dcs > -1 && dcs < 4) || (dcs > 15 && dcs < 20)
				|| (dcs > 31 && dcs < 36) || (dcs > 47 && dcs < 52)
				|| (dcs > 63 && dcs < 68) || (dcs > 79 && dcs < 84)) {
			return (short)((messageLen <= 160) ? 160 : 153);
		
		} else if((dcs > 7 && dcs < 12) || (dcs > 23 && dcs < 28)
				|| (dcs > 39 && dcs < 44) || (dcs > 55 && dcs < 60)
				|| (dcs > 71 && dcs < 76) || (dcs > 87 && dcs < 92)) {
			return (short)((messageLen <= 70) ? 70 : 67);
		} else
			return (short)((messageLen <= 140) ? 140 : 134);
	}
		
	public static ExecutorService getEsProcessRequset() {
		return esProcessRequset;
	}

	public static void setEsProcessRequset(ExecutorService esProcessRequset) {
		ClientController.esProcessRequset = esProcessRequset;
	}
	
	public static ExecutorService getEsSendSM() {
		return esSendSM;
	}

	public static void setEsSendSM(ExecutorService esSendSM) {
		ClientController.esSendSM = esSendSM;
	}

	public static ConcurrentHashMap<Integer, SmppClient> getClients() {
		return clients;
	}

	public static void setClients(ConcurrentHashMap<Integer, SmppClient> clients) {
		ClientController.clients = clients;
	}

	public static LinkedList<SMPPClientsGroups> getGroupList() {
		return groupList;
	}

	public static void setGroupList(LinkedList<SMPPClientsGroups> groupList) {
		ClientController.groupList = groupList;
	}

	public static int getDefaultSmsLiveTime() {
		return ClientController.defaultSmsLiveTime;
	}

	public static void setDefaultSmsLiveTime(int defaultSmsLiveTime) {
		ClientController.defaultSmsLiveTime = defaultSmsLiveTime;
	}

	public static int getSmppMaxMOMessageParts() {
		return smppMaxMOMessageParts;
	}

	public static void setSmppMaxMOMessageParts(int smppMaxMOMessageParts) {
		ClientController.smppMaxMOMessageParts = smppMaxMOMessageParts;
	}

	public static ConcurrentLinkedQueue<SendData> getSmppMTQueue() {
		return smppMTQueue;
	}

	public static void setSmppMTQueue(ConcurrentLinkedQueue<SendData> smppMTQueue) {
		ClientController.smppMTQueue = smppMTQueue;
	}

	public static ConcurrentHashMap<Long, ClientDLRWaitingList> getDlrWaitingQueue() {
		return dlrWaitingQueue;
	}

	public static void setDlrWaitingQueue(ConcurrentHashMap<Long, ClientDLRWaitingList> dlrWaitingQueue) {
		ClientController.dlrWaitingQueue = dlrWaitingQueue;
	}
	public static int getIncreaseWaitDLRFromSMSC() {
		return increaseWaitDLRFromSMSC;
	}
	public static void setIncreaseWaitDLRFromSMSC(int increaseWaitDLRFromSMSC) {
		ClientController.increaseWaitDLRFromSMSC = increaseWaitDLRFromSMSC;
	}
}
