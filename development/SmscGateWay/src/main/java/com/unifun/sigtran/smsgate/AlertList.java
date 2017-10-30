package com.unifun.sigtran.smsgate;

import java.math.BigInteger;
import java.sql.Timestamp;

public class AlertList {

	private long messageId;
	private long msisdn; 
	private Timestamp nextAttempt;
//	private Timestamp sendUntil;
	
	public AlertList() {
		// TODO Auto-generated constructor stub
	}
	
	public AlertList(long messageId, long msisdn, Timestamp nextAttempt/*, Timestamp sendUntil*/) {
		this.messageId = messageId;
		this.msisdn = msisdn;
		this.nextAttempt = nextAttempt;
//		this.sendUntil = sendUntil;
	}

	public long getMessageId() {
		return messageId;
	}

//	public void setMessageId(long messageId) {
//		this.messageId = messageId;
//	}
	//hibernate	
	public void setMessageId(BigInteger messageId) {
		this.messageId = messageId.longValue();
	}

	public long getMsisdn() {
		return msisdn;
	}

//	public void setMsisdn(long msisdn) {
//		this.msisdn = msisdn;
//	}
	//hibernate
	public void setMsisdn(BigInteger msisdn) {
		this.msisdn = msisdn.longValue();
	}

	public Timestamp getNextAttempt() {
		return nextAttempt;
	}

	public void setNextAttempt(Timestamp nextAttempt) {
		this.nextAttempt = nextAttempt;
	}

	@Override
	public String toString() {
		return "AlertList [messageId=" + messageId + ", msisdn=" + msisdn + ", nextAttempt=" + nextAttempt + "]";
	}

//	public Timestamp getSendUntil() {
//		return sendUntil;
//	}
//
//	public void setSendUntil(Timestamp sendUntil) {
//		this.sendUntil = sendUntil;
//	}
	
}
