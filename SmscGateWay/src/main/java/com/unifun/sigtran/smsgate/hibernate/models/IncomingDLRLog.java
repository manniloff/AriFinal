package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_incoming_dlr_log")
public class IncomingDLRLog {

	@Id
	private long messageId;
	@Column(name="receivedDLR", columnDefinition="TIMESTAMP")
	private Timestamp receivedDLR;
	@Column(name="sendDLRUntil", columnDefinition="TIMESTAMP")
	private Timestamp sendDLRUntil;
	@Column(name="state", nullable=false)
	private String state;
	@Column(name="sendSMAttempts", nullable=false, columnDefinition="SMALLINT")
	private int sendSMAttempts;
	
	public IncomingDLRLog() {
	}
	
	public IncomingDLRLog(long messageId, Timestamp receivedDLR, Timestamp sendDLRUntil, String state, int sendSMAttempts) {
		this.state = state;
		this.messageId = messageId;
		this.receivedDLR = receivedDLR;
		this.sendDLRUntil = sendDLRUntil;
		this.sendSMAttempts = sendSMAttempts;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public Timestamp getReceivedDLR() {
		return receivedDLR;
	}

	public void setReceivedDLR(Timestamp receivedDLR) {
		this.receivedDLR = receivedDLR;
	}

	public Timestamp getSendDLRUntil() {
		return sendDLRUntil;
	}

	public void setSendDLRUntil(Timestamp sendDLRUntil) {
		this.sendDLRUntil = sendDLRUntil;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getSendSMAttempts() {
		return sendSMAttempts;
	}

	public void setSendSMAttempts(int sendSMAttempts) {
		this.sendSMAttempts = sendSMAttempts;
	}
}
