package com.unifun.sigtran.smsgate.smpp.workers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.client.SmppClient;
import com.unifun.sigtran.smsgate.smpp.client.SubmitSm;

public class SMPPClientSubmitSMWorker extends Thread {
	private short speed = 10;
	private volatile long sleepTime;
	SmppClient client;
	private Logger logger = LogManager.getLogger(SMPPClientSubmitSMWorker.class);
	
	public SMPPClientSubmitSMWorker(SmppClient client) {
		this.client = client;
		this.setSleepTime(1000/((client.getClientConfig().getSpeedLimit() == 0) 
				? this.speed 
						: client.getClientConfig().getSpeedLimit()));
	}
	
	@Override
	public void run() {
		logger.info("Submit_SM Worker started for client - " + client.getClientConfig().getSystemId());
		Thread.currentThread().setName("_THREAD-Submit_" + client.getClientConfig().getSystemId());
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if(!client.getSmsToSend().isEmpty()) {
					if(client.getSmppSession() != null) {
						ClientController.getEsSendSM().submit(new SubmitSm(client.getSmsToSend().poll()
								, client, sleepTime));
					}
				}
				TimeUnit.MILLISECONDS.sleep(getSleepTime());
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
		logger.info("Submit_SM stopped for client - " + client.getClientConfig().getSystemId());
	}
	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}
	
	
}
