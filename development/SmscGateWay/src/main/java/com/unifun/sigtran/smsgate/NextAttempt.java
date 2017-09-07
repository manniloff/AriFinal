package com.unifun.sigtran.smsgate;

import java.math.BigInteger;
import java.sql.Timestamp;

public class NextAttempt {
	
	private long messageId;
	private int attempts;
	private Timestamp nextAttempt;
//	private Timestamp sendUntil;
	
	public NextAttempt() {
		// TODO Auto-generated constructor stub
	}
	
	public NextAttempt(long messageId, int attempts, Timestamp nextAttempt/*, Timestamp sendUntil*/) {
		this.messageId = messageId;
		this.attempts = attempts;
		this.nextAttempt = nextAttempt;
//		this.sendUntil = sendUntil;
	}
	
	public long getMessageId() {
		return messageId;
	}
	//hibernate
	public void setMessageId(BigInteger messageId) {
		this.messageId = messageId.longValue();
	}
	public int getAttempts() {
		return attempts;
	}
	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}
	public Timestamp getNextAttempt() {
		return nextAttempt;
	}
	public void setNextAttempt(Timestamp nextAttempt) {
		this.nextAttempt = nextAttempt;
	}
//	public Timestamp getSendUntil() {
//		return sendUntil;
//	}
//	public void setSendUntil(Timestamp sendUntil) {
//		this.sendUntil = sendUntil;
//	}

	@Override
	public String toString() {
		return "NextAttempt [messageId=" + messageId + ", attempts=" + attempts + ", nextAttempt=" + nextAttempt + "]";
	}
	
}
