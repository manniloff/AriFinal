package com.unifun.sigtran.smsgate.smpp.workers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.session.SMPPServerSession;

import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.smpp.server.SmppDeliveryReceiptTask;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class SMPPDLRWorker extends Thread {
	
	private String workerName;
	Optional<DLRQueue> oDlrData;
	private SpeedLimiter speedLimiter;
	
	private Logger logger = LogManager.getLogger(SMPPDLRWorker.class);
	
	public SMPPDLRWorker(String workerName, int DlrSendSpeedPerSec) {
		this.workerName = workerName;
		speedLimiter = new SpeedLimiter((double)DlrSendSpeedPerSec); //speed per second
	}
	
	@Override
	public void run() {
		logger.info(workerName + " is runnig");
		currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()){
			try {
				long StartActionPoint = System.nanoTime();
				if(!ServerController.getSmppBindSessions().isEmpty()) {
					oDlrData = ServerController.getDlrQueue().stream().filter(dlr ->
					dlr.getNextAttempt().before(new Timestamp(System.currentTimeMillis())) 						// check attempt time;
						&& dlr.getSendDLRUntil().after(new Timestamp(System.currentTimeMillis()))				// not expired dlr;
						&& dlr.getAttempts() < ServerController.getDlrReSendMaxAttempts()						// less them max attempts;
						&& ServerController.getSmppBindSessions().get(dlr.getSystemId()) != null).findFirst();	// check active session;
					if(oDlrData.isPresent()) {
						if(!ServerController.getDlrQueue().remove(oDlrData.get()))
							logger.error("Could not delete dlr from queue for TransactionId - " + oDlrData.get().getMessageId());
						oDlrData.get().setAttempts((short)(oDlrData.get().getAttempts() + 1));
						SMPPServerSession session = ServerController.getSmppBindSessions().get(oDlrData.get().getSystemId());
						if(session != null) {
						ServerController.getExecDLR().submit(new SmppDeliveryReceiptTask(session , oDlrData.get()));
						} else {
							logger.warn("SMMP Session not found for sysId - " + oDlrData.get().getSystemId());
							ServerController.getExecDLR().submit(new saveDlrLog(oDlrData.get(), StartActionPoint, System.currentTimeMillis(), "SMMP Session not found"));
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
			ServerController.getExecDLR().shutdown();
			ServerController.getExecDLR().awaitTermination(1, TimeUnit.SECONDS);
			 logger.info("execDLR stopped");
		} catch (InterruptedException e) {
			logger.error(e);
			logger.error("Could not stop execDLR");
		}
		logger.info(workerName + " was stopped");
	}
	
	private class saveDlrLog extends Thread {
		DLRQueue dlrData;
		long started;
		long finished;
		String errorMessage;
		
		public saveDlrLog(DLRQueue dlrData, long started, long finished, String errorMessage) {
			this.dlrData = dlrData;
			this.started = started;
			this.finished = finished;
			this.errorMessage = errorMessage;
		}
		@Override
		public void run() {
			ServerController.SaveDLRResponse(dlrData, started, finished, errorMessage);
		}
	}	
}
