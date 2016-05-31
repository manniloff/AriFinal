package com.unifun.sigtran.smsgate.smpp.common;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class ResetCounter extends Thread {
	
	private String workerName;
	private SpeedLimiter speedLimiter;
	private volatile ConcurrentHashMap<String, ConnectionLimits> conLimits;
	private static final Logger logger = LogManager.getLogger(ResetCounter.class);
	
	public ResetCounter(ConcurrentHashMap<String, ConnectionLimits> conLimits, String workerName) {
		this.workerName = workerName;
		this.conLimits = conLimits;
		speedLimiter = new SpeedLimiter((double)1);
		speedLimiter.setCorrelation(50);
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()) {
			try {
			long StartActionPoint = System.nanoTime();
			conLimits.values().forEach(con -> {
				con.counter.set(0);
			});
				speedLimiter.LimitSpeed(System.nanoTime() - StartActionPoint);
				
			} catch (Exception e) {
				logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}		
	}
	
	public ConcurrentHashMap<String, ConnectionLimits> getConnectionLimits() {
		return conLimits;
	}
}
