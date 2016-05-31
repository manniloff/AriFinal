package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_dlr_sended_log")
public class DLRSendedLog {

	@Id
	private long messageId;
	@Column(name="sendedDLR", nullable=true, columnDefinition="TIMESTAMP")
	private Timestamp sendedDLR;
	@Column(name="attempts", nullable=false, columnDefinition="TINYINT")
	private short attempts;
	
	public DLRSendedLog() {
	}

	public DLRSendedLog(long messageId, Timestamp sendedDLR, short attempts) {
		this.messageId = messageId;
		this.sendedDLR = sendedDLR;
		this.attempts = attempts;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public Timestamp getSendedDLR() {
		return sendedDLR;
	}

	public void setSendedDLR(Timestamp sendedDLR) {
		this.sendedDLR = sendedDLR;
	}

	public short getAttempts() {
		return attempts;
	}

	public void setAttempts(short attempts) {
		this.attempts = attempts;
	}

}
