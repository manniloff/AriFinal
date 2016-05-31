package com.unifun.sigtran.smsgate.workers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class CheckAlertQueue extends Thread {
	
	private short queryLimit;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(CheckAlertQueue.class);
	
	public CheckAlertQueue(long sendSpeedPerSec, short queryLimit) {
		speedLimiter = new SpeedLimiter((double) sendSpeedPerSec);
		this.setQueryLimit(queryLimit);
	}
	
	public void run() {
		logger.info("CheckAlertQueue started");
		currentThread().setName("_THREAD-checkAlertQueue");
		long started;
		short qty = queryLimit;
		while (!Thread.currentThread().isInterrupted()) {
			started = System.nanoTime();
			try {
				List<Long> messageIds = new ArrayList<>(); 
				Timestamp now = new Timestamp(System.currentTimeMillis());
				SmsGateWay.getAlertLists().stream()
				.forEach(alert -> {
					if(alert.getNextAttempt().before(now)) {
						if(queryLimit != 0) {
							messageIds.add(alert.getMessageId());
							logger.info(alert.toString());
							SmsGateWay.getAlertLists().remove(alert);
							queryLimit--;
						} else {
							queryLimit = qty;
							return;
						}
					}
				});
				if(!messageIds.isEmpty())
					SmsGateWay.processAlertNextAttempt(messageIds);
				speedLimiter.LimitSpeed(System.nanoTime() - started);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info("checkNextAttemptQueue stopped");
	}

	public short getQueryLimit() {
		return queryLimit;
	}

	public void setQueryLimit(short queryLimit) {
		this.queryLimit = queryLimit;
	}
}
