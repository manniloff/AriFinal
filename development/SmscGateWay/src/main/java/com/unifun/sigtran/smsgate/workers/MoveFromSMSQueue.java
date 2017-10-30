package com.unifun.sigtran.smsgate.workers;

import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class MoveFromSMSQueue extends Thread {

	private short queryLimit;
//	private DataBaseLayer dbl;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(MoveFromSMSQueue.class);
	public MoveFromSMSQueue(short sendSpeedPerSec, short queryLimit) {
		this.queryLimit = queryLimit;
		speedLimiter = new SpeedLimiter((double) sendSpeedPerSec);
	}
	@Override
	public void run() {
		logger.info("MoveFromSMSQueue is running");
		currentThread().setName("_THREAD-MoveFromSMSQueue");
		long started;
		while (!Thread.currentThread().isInterrupted()) {
			started = System.nanoTime();
			try {
				SmsGateWay.moveToSMSData(queryLimit);
				speedLimiter.LimitSpeed(System.nanoTime() - started);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info("MoveFromSMSQueue stopped");
	}
	public short getQueryLimit() {
		return queryLimit;
	}
	public void setQueryLimit(short queryLimit) {
		this.queryLimit = queryLimit;
	}
}
