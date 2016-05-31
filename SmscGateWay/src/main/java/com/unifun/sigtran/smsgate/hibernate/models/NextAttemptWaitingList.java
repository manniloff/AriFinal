package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

@Entity
@Table(name="next_attempt_waiting_list")
public class NextAttemptWaitingList {

	public NextAttemptWaitingList() {
		// TODO Auto-generated constructor stub
	}
	
	public NextAttemptWaitingList(long messageId, String fromAD, String fromTON, String fromNP, Long toAD, String toAN,
			String toNP, String message, short quantity, int dcs, int pid, Timestamp inserted, Timestamp sendUntil,
			int systemId, String dlrResponseType, String priority, String segmentLen, int sendAttempts,
			String handledType, String errorState, Timestamp nextAttempt) {
		super();
		this.messageId = messageId;
		this.fromAD = fromAD;
		this.fromTON = fromTON;
		this.fromNP = fromNP;
		this.toAD = toAD;
		this.toAN = toAN;
		this.toNP = toNP;
		this.message = message;
		this.quantity = quantity;
		this.dcs = dcs;
		this.pid = pid;
		this.inserted = inserted;
		this.sendUntil = sendUntil;
		this.systemId = systemId;
		this.dlrResponseType = dlrResponseType;
		this.priority = priority;
		this.segmentLen = segmentLen;
		this.sendAttempts = sendAttempts;
		this.handledType = handledType;
		this.errorState = errorState;
		this.nextAttempt = nextAttempt;
	}

	@Id
	@Column(name = "messageId", nullable = false)
	private long messageId;
	
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
	
	@Column(name = "quantity", nullable = false, columnDefinition = "TINYINT")
	private short quantity;
	
	@Column(name = "dcs", nullable = false, columnDefinition = "TINYINT")
	private int dcs;
	
	@Column(name = "pid", nullable = false, columnDefinition = "TINYINT")
	private int pid;
	
	@Column(name = "inserted", nullable = false)
	@Type(type="timestamp")
	private Timestamp inserted;
	
	@Column(name = "senduntil", nullable = false)
	@Type(type="timestamp")
	private Timestamp sendUntil;
	
	@Column(name = "systemId", length = 50)
	private int systemId;
	
	@Column(name = "dlrResponseType", nullable = false)
	private String dlrResponseType;
	
	@Column(name = "priority", nullable = false)
	private String priority;
	
	@Column(name = "segmentLen")
	private String segmentLen;
	 
	@Column(name = "sendSMAttempts", nullable = false, columnDefinition = "TINYINT")
	private int sendAttempts = 1;

	@Column(name = "handledType", nullable = false)
	private String handledType;
	
	@Column(name = "errorState", nullable = false)
	private String errorState;
	
	@Column(name = "nextAttempt", nullable = false)
	private Timestamp nextAttempt;
	
	@Transient
	private boolean expired;
	
	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
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

	public Long getToAD() {
		return toAD;
	}

	public void setToAD(Long toAD) {
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public short getQuantity() {
		return quantity;
	}

	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}

	public int getDcs() {
		return dcs;
	}

	public void setDcs(int dcs) {
		this.dcs = dcs;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public Timestamp getInserted() {
		return inserted;
	}

	public void setInserted(Timestamp inserted) {
		this.inserted = inserted;
	}

	public Timestamp getSendUntil() {
		return sendUntil;
	}

	public void setSenduntil(Timestamp sendUntil) {
		this.sendUntil = sendUntil;
	}

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}

	public String getDlrResponseType() {
		return dlrResponseType;
	}

	public void setDlrResponseType(String dlrResponseType) {
		this.dlrResponseType = dlrResponseType;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getSegmentLen() {
		return segmentLen;
	}

	public void setSegmentLen(String segmentLen) {
		this.segmentLen = segmentLen;
	}

	public int getSendAttempts() {
		return sendAttempts;
	}

	public void setSendAttempts(int sendAttempts) {
		this.sendAttempts = sendAttempts;
	}

	public String getHandledType() {
		return handledType;
	}

	public void setHandledType(String handledType) {
		this.handledType = handledType;
	}

	public Timestamp getNextAttempt() {
		return nextAttempt;
	}

	public void setNextAttempt(Timestamp nextAttempt) {
		this.nextAttempt = nextAttempt;
	}

	public String getErrorState() {
		return errorState;
	}

	public void setErrorState(String errorState) {
		this.errorState = errorState;
	}
	
	@Override
	public String toString() {
		return "NextAttemptWaitingList [messageId=" + messageId + ", systemId=" + systemId + ", msisdn=" + toAD
				+ ", nextAttempt=" + nextAttempt + ", errorState=" + errorState
				+ ", sendUntil=" + sendUntil+ ", dlrResponseType=" + dlrResponseType + ", handledType=" + handledType
				+ "]";
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}
}
