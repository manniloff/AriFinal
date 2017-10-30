package com.unifun.sigtran.smsgate.workers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;

public class CheckContentProviderAccessSmsQueueSize extends Thread {

	private long sleepTime;
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	public CheckContentProviderAccessSmsQueueSize(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	@Override
	public void run() {
		try {
			Thread.currentThread().sleep(5000);	//TODO config
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
		logger.info("CheckContentProviderAccessSmsQueueSize started");
		Thread.currentThread().setName("_CheckContentProviderAccessSmsQueueSize");
		long start;
		long duration;
		while(!Thread.currentThread().isInterrupted()) {
			try {
				start = System.currentTimeMillis();
				//Updating NumberRegularExpressionList
				SmsGateWay.checkContentProviderAccessSmsQueueSize();
				duration = System.currentTimeMillis() - start;
				TimeUnit.MILLISECONDS.sleep((sleepTime > duration) ? (sleepTime - duration) : 0);			
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}			
		}
		logger.info("CheckContentProviderAccessSmsQueueSize finished");
	}

	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

}
