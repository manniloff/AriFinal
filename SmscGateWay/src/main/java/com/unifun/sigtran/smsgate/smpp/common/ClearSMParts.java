package com.unifun.sigtran.smsgate.smpp.common;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;

public class ClearSMParts extends Thread {

	private String workerName;
	private boolean serverCleaner;
	private ConcurrentHashMap<String, MessageContainer> messageParts;
	private static Logger logger = LogManager.getLogger(ClearSMParts.class);
	
	
	public ClearSMParts(ConcurrentHashMap<String, MessageContainer> messageParts) {
		this.messageParts = messageParts;
	}
	
	public ClearSMParts(String workerName, ConcurrentHashMap<String, MessageContainer> messageParts
			, boolean serverCleaner) {
		this(messageParts);
		this.workerName = workerName;
		this.serverCleaner = serverCleaner;
	}

	@Override
	public void run() {
		logger.info(workerName + " is runnig");
		currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()) {
			try {
				messageParts.values().stream()
				.filter(s -> new Timestamp(System.currentTimeMillis()).after(s.getWaitTime()))
				.forEach(bad -> {
					logger.debug("Before list Size - " + messageParts.size());
					if(messageParts.remove(bad.getContainerId()) != null) {
						logger.info("request cleaned - " + bad.toString());
						SmsGateWay.saveSmppErrorLog(bad.getId(), "", 0, 0, -11, bad.toString().substring(0, 250)
								, System.currentTimeMillis(), serverCleaner);
					}
					else
						logger.warn("Couldn't find request - " + bad.toString());
					logger.debug("After list Size - " + messageParts.size());
				});
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (Exception e) {
				logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info(workerName + " was stopped");
	}
}
