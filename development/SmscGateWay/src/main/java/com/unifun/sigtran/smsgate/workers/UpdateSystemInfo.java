package com.unifun.sigtran.smsgate.workers;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;

public class UpdateSystemInfo extends Thread {

	private long sleepTime;
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	public UpdateSystemInfo(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	@Override
	public void run() {
		logger.info("UpdateSystemInfo started");
		Thread.currentThread().setName("_UpdateSystemInfo");
		long start;
		long duration;
		while(!Thread.currentThread().isInterrupted()) {
			try {
				start = System.currentTimeMillis();
				//Updating NumberRegularExpressionList
				SmsGateWay.checkReqEx();
				//Updating Access Restrictions
				SmsGateWay.checkAccessRestriction();
				//Updating Access CharSet Restrictions
				SmsGateWay.checkAccessCharSetList();
				//Updating Data Coding List
				SmsGateWay.checkDataCodingList();
				//Updating Data Coding List
				SmsGateWay.checkSourcheSubstitutionList();
				//Updating Routing info for MO messages
				SmsGateWay.checkMoRoutingRulesList();
				duration = System.currentTimeMillis() - start;
				logger.info("duration - " + duration);
				Thread.currentThread().sleep((sleepTime > duration) ? (sleepTime - duration) : 0);			
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}			
		}
		logger.info("UpdateSystemInfo finished");
	}

	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}
}
