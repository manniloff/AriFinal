package com.unifun.sigtran.smsgate.smpp.workers;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class SMPPClientGetMTSMSToSend extends Thread {
	SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(SMPPClientGetMTSMSToSend.class);
	
	public SMPPClientGetMTSMSToSend(int smsPerSec) {
		speedLimiter = new SpeedLimiter((double) smsPerSec);
	}
	
	@Override
	public void run() {
		logger.info("SMPPClientGetMTSMS started ");
		Thread.currentThread().setName("_THREAD-SMPPClientGetMTSMS");
		while (!Thread.currentThread().isInterrupted()) {
			long stared = System.nanoTime();
			try {
				if(!ClientController.getSmppMTQueue().isEmpty() && ClientController.isAnyClientAvailable()) {
					ClientController.getFromMTSMSQueue();
				}
				speedLimiter.LimitSpeed(System.nanoTime() - stared);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info("SMPPClientGetMTSMS stopped");
	}	
}
