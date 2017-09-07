package com.unifun.sigtran.smsgate.workers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.util.SpeedLimiter;

public class GetSMSToSend extends Thread {
	private short qLimit;
	private SpeedLimiter speedLimiter;
	private Logger logger = LogManager.getLogger(GetSMSToSend.class);
		
	public GetSMSToSend(short qLimit, short sendSpeedPerSec) {
		this.qLimit = qLimit;
		speedLimiter = new SpeedLimiter((double) sendSpeedPerSec);
	}

	@Override
	public void run() {
		logger.info("GetSMSToSend started");
		currentThread().setName("_THREAD-AddToSRISMQueue");
		long started;
		while (!Thread.currentThread().isInterrupted()) {
			started = System.nanoTime();
			try {
				List<SendData> smsToSend = SmsGateWay.getSmsData(qLimit);
				smsToSend.forEach(sms -> {
					if(sms.isExpired()) {
						long now = System.currentTimeMillis();
						logger.debug("send dlr started for - " + sms.getMessageId());
						ServerController.addDLRRequest(new DLRQueue(sms.getMessageId(), sms.getSystemId()
								, sms.getFromAD(), sms.getFromTON(), sms.getFromNP(), sms.getToAD()
								, sms.getToAN(), sms.getToNP(), "3", "", (short) sms.getDcs()
								, sms.getInserted(), new Timestamp(now)
								, (short)0, sms.getDlrResponseType(), new Timestamp(now + SmsGateWay.getSendDLRUntil())
								, new Timestamp(now)));
					} else {
						logger.debug("sending sms started for - " + sms.getMessageId());
						SmsGateWay.getSriSMQueue().add(sms);
					}
				});
				speedLimiter.LimitSpeed(System.nanoTime() - started);
			} catch (Exception e) {
				logger.error(e);
				if(e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
	}
}
