package com.unifun.sigtran.smsgate.workers;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.util.CustomThreadFactoryBuilder;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;


public class ReportSMDSWorker extends Thread {

	private SpeedLimiter speedLimiter; 
	private String workerName;
	private ExecutorService es;
	private Logger logger = LogManager.getLogger(ReportSMDSWorker.class);
	
	public ReportSMDSWorker(short sendSpeedPerSec, String workerName, short threads) {
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
				if(!SmsGateWay.getRdsSMQueue().isEmpty())
					es.execute(new ReportSMDS(SmsGateWay.getRdsSMQueue().poll()));
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
			logger.info("stopped");
		} catch (InterruptedException e) {
			logger.error(e);
		}
	}
	
	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	private class ReportSMDS implements Runnable {

		SendData reportSMDSQueue;
		
		public ReportSMDS(SendData reportSMDSQueue) {
			this.reportSMDSQueue = reportSMDSQueue;
		}
		@Override
		public void run() {
			SmsGateWay.sendDSR(reportSMDSQueue);
		}
	}
	
}
