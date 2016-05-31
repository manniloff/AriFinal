package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="entity_reset_dlrqueue")
public class DLRQueue {
	@Id
	@Column(name = "messageId", nullable = false)
	private long messageId;
	@Column(name = "systemId", length = 50)
	private int systemId;
	@Column(name = "fromAD", nullable = false, length = 15)
	private String fromAD;
	@Column(name = "fromTON", nullable = false)
//	@Enumerated(EnumType.STRING)
	private String fromTON;
	@Column(name = "fromNP", nullable = false)
//	@Enumerated(EnumType.STRING)
	private String fromNP;
	@Column(name = "toAD", nullable = false, length = 15)
	private Long toAD;
	@Column(name = "toAN", nullable = false)
//	@Enumerated(EnumType.STRING)
	private String toAN;
	@Column(name = "toNP", nullable = false)
//	@Enumerated(EnumType.STRING)
	private String toNP;
	@Column(name = "message", nullable = false, length = 8000)
	private String message;
	@Column(name = "state", nullable = false, length = 2)
	private String state;
	@Column(name = "dcs", nullable = false, columnDefinition = "TINYINT")
	private short dcs;
	@Column(name = "inserted", nullable = false)
	private Timestamp inserted;
	@Column(name = "nextAttempt", nullable = false)
	private Timestamp nextAttempt;
	@Column(name = "attempts", nullable = false, columnDefinition = "SMALLINT")
	private int attempts;
	@Column(name = "dlrResponseType", nullable = false, length = 2)
	private String dlrResponseType;
	@Column(name = "sendDLRUntil", nullable = false)
	private Timestamp sendDLRUntil;
	@Column(name = "receivedDLR", nullable = false)
	private Timestamp receivedDLR;
	
	public DLRQueue() {
		// TODO Auto-generated constructor stub
	}
	
	public DLRQueue(long messageId, int systemId, String fromAD, String fromTON, String fromNP, long toAD,
			String toAN, String toNP, String state, String message, short dcs, Timestamp inserted,
			Timestamp nextAttempt, int attempts, String dlrResponseType, Timestamp sendDLRUntil,
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

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
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
