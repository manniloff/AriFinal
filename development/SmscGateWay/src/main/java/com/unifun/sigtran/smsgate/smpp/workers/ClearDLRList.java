package com.unifun.sigtran.smsgate.smpp.workers;

import java.sql.Timestamp;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.ServerController;

public class ClearDLRList extends Thread {
	private String workerName;
	private boolean incomingDLR;
	private long delaytime;
	private volatile long sleepTime;
	
	private Logger logger = LogManager.getLogger(ClearDLRList.class);
	
	public ClearDLRList(long delaytime, long sleepTime, String workerName, boolean incomingDLR) {
		this.delaytime = delaytime;
		this.setSleepTime(sleepTime);
		this.workerName = workerName;
		this.incomingDLR = incomingDLR;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(delaytime);
		} catch (InterruptedException e) {
			logger.error("Could not execute delayTime");
		}
		logger.info("ClearDLRList for " + ((incomingDLR) ? "SMPPClinet " : "SMPPServer ") + "started");
		Thread.currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()) {
			try {
				long started = System.currentTimeMillis();
				Timestamp now = new Timestamp(started);
				if(incomingDLR) {
					ClientController.getDlrWaitingQueue().values().forEach(exDlr -> {
						if(exDlr.getWaitDlrUntil().before(now)) {
//							switch (exDlr.getDlrResponseType()) {
//							case "2":	//dlr on error
//								ClientController.ProcessDlrRequest(exDlr.getRemoteId(), "2", exDlr.getClientId());	
//								break;
//							case "3":	//dlr on delivered 
//								ClientController.ProcessDlrRequest(exDlr.getRemoteId(), "5", exDlr.getClientId());
//								break;
//							default:	//always send DLR
								ClientController.ProcessDlrRequest(exDlr.getRemoteId(), "3", exDlr.getClientId());
//								break;
//							}
							try {
								Thread.sleep(5);
								} catch (Exception e) {
									Thread.currentThread().interrupt();
								}
						}
					});	
				}
				else
					ServerController.getDlrQueue().stream()
					.forEach(exDlr -> {
						if(exDlr.getSendDLRUntil().before(now)) {
							ServerController.SaveDLRResponse(exDlr, started, System.currentTimeMillis(), "DLR EXPIRED");
							if(!ServerController.getDlrQueue().remove(exDlr)) {
								logger.error("could not remove Expired DLR from Server queue for messageId - " + exDlr.getMessageId());	
							}
							try {
								Thread.sleep(5);
							} catch (Exception e) {
								Thread.currentThread().interrupt();
							}	
						}
					});
				long duration = (System.currentTimeMillis() - started);
				Thread.sleep((sleepTime - duration > 0) ? (sleepTime - duration) : 0);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
		}
		logger.info("ClearDLRList for " + ((incomingDLR) ? "SMPPClinet " : "SMPPServer ") + "was stopped");
	}
	
	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}
	
}
