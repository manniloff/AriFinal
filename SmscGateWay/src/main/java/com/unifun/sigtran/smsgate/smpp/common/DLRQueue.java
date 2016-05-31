package com.unifun.sigtran.smsgate.smpp.common;

import java.sql.Timestamp;

public class DLRQueue {
	private long messageId;
	private int systemId;
	private String fromAD;
	private String fromTON;
	private String fromNP;
	private long toAD;
	private String toAN;
	private String toNP;
	private String state;
	private String message;
	private short dcs;
	private Timestamp inserted;
	private Timestamp nextAttempt;
	private short attempts;
	private String dlrResponseType;
	private Timestamp sendDLRUntil;
	private Timestamp receivedDLR;
	
	public DLRQueue() {
		// TODO Auto-generated constructor stub
	}
	
	public DLRQueue(long messageId, int systemId, String fromAD, String fromTON, String fromNP, long toAD,
			String toAN, String toNP, String state, String message, short dcs, Timestamp inserted,
			Timestamp nextAttempt, short attempts, String dlrResponseType, Timestamp sendDLRUntil,
			Timestamp receivedDLR) {
		this.messageId = messageId;
		this.systemId = systemId;
		this.fromAD = fromAD;
		this.fromTON = fromTON;
		this.fromNP = fromNP;
		this.toAD = toAD;
		this.toAN = toAN;
		this.toNP = toNP;
		this.state = state;
		this.message = message;
		this.dcs = dcs;
		this.inserted = inserted;
		this.nextAttempt = nextAttempt;
		this.attempts = attempts;
		this.dlrResponseType = dlrResponseType;
		this.sendDLRUntil = sendDLRUntil;
		this.receivedDLR = receivedDLR;
	}
	public long getMessageId() {
		return messageId;
	}
//	public void setMessageId(long messageId) {
//		this.messageId = messageId;
//	}
	//Hibernate
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}

	public String getFromAD() {
		return fromAD;
	}

	public void setFromAD(String fromAD) {
		this.fromAD = fromAD;
	}

	public String getFromTON() {
		return fromTON;
	}

	public void setFromTON(String fromTON) {
		this.fromTON = fromTON;
	}

	public String getFromNP() {
		return fromNP;
	}

	public void setFromNP(String fromNP) {
		this.fromNP = fromNP;
	}

	public long getToAD() {
		return toAD;
	}

	public void setToAD(long toAD) {
		this.toAD = toAD;
	}

	public String getToAN() {
		return toAN;
	}

	public void setToAN(String toAN) {
		this.toAN = toAN;
	}

	public String getToNP() {
		return toNP;
	}

	public void setToNP(String toNP) {
		this.toNP = toNP;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public short getDcs() {
		return dcs;
	}

	public void setDcs(short dcs) {
		this.dcs = dcs;
	}

	public Timestamp getInserted() {
		return inserted;
	}

	public void setInserted(Timestamp inserted) {
		this.inserted = inserted;
	}

	public Timestamp getNextAttempt() {
		return nextAttempt;
	}

	public void setNextAttempt(Timestamp nextAttempt) {
		this.nextAttempt = nextAttempt;
	}

	public short getAttempts() {
		return attempts;
	}

	public void setAttempts(short attempts) {
		this.attempts = attempts;
	}

	public String getDlrResponseType() {
		return dlrResponseType;
	}

	public void setDlrResponseType(String dlrResponseType) {
		this.dlrResponseType = dlrResponseType;
	}

	public Timestamp getSendDLRUntil() {
		return sendDLRUntil;
	}

	public void setSendDLRUntil(Timestamp sendDLRUntil) {
		this.sendDLRUntil = sendDLRUntil;
	}

	public Timestamp getReceivedDLR() {
		return receivedDLR;
	}

	public void setReceivedDLR(Timestamp receivedDLR) {
		this.receivedDLR = receivedDLR;
	}
}
