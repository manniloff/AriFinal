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

public class MTFSMWorker extends Thread {
	
	private ExecutorService es;
	private String workerName;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(MTFSMWorker.class);
	
	public MTFSMWorker(short sendSpeedPerSec, String workerName, short threads) {
		speedLimiter = new SpeedLimiter((double)sendSpeedPerSec);
		this.setWorkerName(workerName);
		es = Executors.newFixedThreadPool(threads
				, new CustomThreadFactoryBuilder()
				.setNamePrefix("_" + workerName).setDaemon(false)
				.setPriority(Thread.NORM_PRIORITY).build());
	}
	
	@Override
	public void run() {
		logger.info(workerName + " is runnig");
		currentThread().setName("_THREAD-" + workerName);
		long started;
		while (!Thread.currentThread().isInterrupted()) {
			started = System.nanoTime();
			try {
				if(!SmsGateWay.getMtfSMQueue().isEmpty())
					es.submit(new SendMTFSM(SmsGateWay.getMtfSMQueue().poll()));	
				speedLimiter.LimitSpeed(System.nanoTime() - started);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		try {
			es.shutdown();
			es.awaitTermination(2, TimeUnit.SECONDS);
			logger.info("Worker stopped");
		} catch (InterruptedException e) {
			logger.error("handled error stopping ExecutorService");
			logger.error(e);
		}
	}
	
	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	private class SendMTFSM implements Runnable {
		SendData mtfsm;
		
		public SendMTFSM(SendData mtfsm) {
			this.mtfsm = mtfsm;
		}

		@Override
		public void run() {
//			SmsGateWay.getDbl().
			SmsGateWay.sendMtSM(mtfsm);
		}
	}	
}
