package com.unifun.sigtran.smsgate.smpp.workers;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.smpp.ServerController;

public class CheckSMPPAccess extends Thread {

	private int sleepTime;
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	public CheckSMPPAccess(int sleepTime) {
		this.sleepTime = sleepTime;
	}

	@Override
	public void run() {
		logger.info("CheckSMPPAccess started");
		Thread.currentThread().setName("_CheckSMPPAccess");
		long start;
		long duration;
		while(!Thread.currentThread().isInterrupted()) {
			try {
				start = System.currentTimeMillis();
				ServerController.checkSMPPConncetions();
				duration = System.currentTimeMillis() - start;
				TimeUnit.MILLISECONDS.sleep((sleepTime > duration) ? (sleepTime - duration) : 0);		
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}			
		}
		logger.info("CheckSMPPAccess finished");
	}

	public int getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
	}
}
