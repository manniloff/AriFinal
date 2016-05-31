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

public class CheckNextAttemptQueue extends Thread {
	
	private short queryLimit;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(CheckNextAttemptQueue.class);
	
	public CheckNextAttemptQueue(long sendSpeedPerSec, short queryLimit) {
		speedLimiter = new SpeedLimiter((double) sendSpeedPerSec);
		this.setQueryLimit(queryLimit);
	}
	
	public void run() {
		logger.info("checkNextAttemptQueue started");
		currentThread().setName("_THREAD-checkNextAttemptQueue");
		long started;
		short qty = queryLimit;
		while (!Thread.currentThread().isInterrupted()) {
			started = System.nanoTime();
			try {
				List<Long> messageIds = new ArrayList<>(); 
				Timestamp now = new Timestamp(System.currentTimeMillis());
				SmsGateWay.getNextAttemptList().forEach(next-> {
					if(next.getNextAttempt().before(now)) {
						if(queryLimit != 0) {
							messageIds.add(next.getMessageId());
							logger.info(next.toString());
							SmsGateWay.getNextAttemptList().remove(next);
							queryLimit--;
						} else {
							queryLimit = qty;
							return;
						}
					}
				});
				if(!messageIds.isEmpty())
					SmsGateWay.processNextAttempt(messageIds);
				speedLimiter.LimitSpeed(System.nanoTime() - started);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info("CheckAlertQueue stopped");
	}

	public short getQueryLimit() {
		return queryLimit;
	}

	public void setQueryLimit(short queryLimit) {
		this.queryLimit = queryLimit;
	}
}
