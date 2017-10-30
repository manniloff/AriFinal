package com.unifun.sigtran.smsgate.smpp.workers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.util.DeliveryReceiptState;

import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.smpp.server.SmppMOReceiptTask;
import com.unifun.sigtran.smsgate.smpp.server.SmppServer;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class SMPPMOWorker extends Thread {
	private String workerName;
	private String serviceType;
	private volatile short concatenateType;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(SMPPMOWorker.class);
	
	public SMPPMOWorker(String workerName, int MOSendSpeedPerSec, short concatenateType, String serviceType) { 
		speedLimiter = new SpeedLimiter((double)MOSendSpeedPerSec); //speed per second
		this.workerName = workerName;
		this.serviceType = serviceType;
		this.concatenateType = concatenateType;
	}
	
	@Override
	public void run() {
		logger.info(workerName + " is runnig");
		currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()) {
			long StartActionPoint = System.nanoTime();
			try {
				if(!ServerController.getSmppBindSessions().isEmpty()) {
					SmsData data = SmppServer.getMoQueue().poll();
					if(data != null) {
						SMPPServerSession session = ServerController.getSmppBindSessions().get(data.getSystemId());
						if(session != null) {
							ServerController.getExecMO().submit(new SmppMOReceiptTask(session , data, concatenateType, serviceType));
						} else {
							logger.warn("SMMP Session not found for sysId - " + data.getSystemId());
							ServerController.SaveMOResponse(data.getMessageId(), new Timestamp(System.currentTimeMillis())
									, new Timestamp(System.currentTimeMillis()), "SMMP Session not found", DeliveryReceiptState.UNDELIV.name(), data.getSystemId());
						}	
					}
				}
				speedLimiter.LimitSpeed(System.nanoTime() - StartActionPoint);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		try {
			ServerController.getExecMO().awaitTermination(2, TimeUnit.SECONDS);
			 logger.info("execMO stopped");
		} catch (InterruptedException e) {
			logger.error("Could not stop execMO");
		}
		logger.info(workerName + " was stopped");
	}
}
