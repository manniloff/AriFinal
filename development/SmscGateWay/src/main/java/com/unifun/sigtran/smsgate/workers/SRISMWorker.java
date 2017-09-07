package com.unifun.sigtran.smsgate.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class SRISMWorker extends Thread {
	
	ExecutorService es;
	private String workerName;
	SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(SRISMWorker.class);
	
	public SRISMWorker(short sendSpeedPerSec, String workerName, short threads) {
		this.setWorkerName(workerName);
		speedLimiter = new SpeedLimiter((double) sendSpeedPerSec);		
		es = Executors.newFixedThreadPool(threads
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_" + workerName).setDaemon(false)
				.setPriority(Thread.NORM_PRIORITY).build());
	}
	
	@Override
	public void run() {
		logger.info(getWorkerName() + " is runnig");
		currentThread().setName("_THREAD-" + workerName);
		while (!Thread.currentThread().isInterrupted()) {
			long StartActionPoint = System.nanoTime();
			try {
				if(!SmsGateWay.getSriSMQueue().isEmpty())
					es.execute(new SendSRISM(SmsGateWay.getSriSMQueue().poll()));	
				speedLimiter.LimitSpeed(System.nanoTime() - StartActionPoint);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
		}
		try {
			es.shutdown();
			es.awaitTermination(2, TimeUnit.SECONDS);
			logger.info("worker stopped");
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	private class SendSRISM implements Runnable {
		SendData requset;
		
		public SendSRISM(SendData requset) {
			this.requset = requset;
		}

		public void run() {
			SmsGateWay.sendSriSm(requset);
		}
	}
	
}
