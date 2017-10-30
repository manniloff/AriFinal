package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@NamedNativeQueries({
	@NamedNativeQuery(name = "getLastSMSQueueId", query = "CALL spGetLastSMSQueueId();"),
	@NamedNativeQuery(name = "moveToSMSData", query = "CALL spMoveToSMSData(:queryLimit)"),
	@NamedNativeQuery(name = "saveSmsData", query = "CALL spAddSRISMQueue(:messageId, :toAD, :toAN, :toNP, :priority, :until);"),
	@NamedNativeQuery(name = "checkAlertWaitingList", query = "CALL spAlertNextAttempt(:limit)"),
	@NamedNativeQuery(name = "moveToArchive", query = "CALL spMoveToArchive()"),
	@NamedNativeQuery(name = "moveToSRISMQueue", query = "CALL spMoveToSRISMQueue(:messageId, :systemId, :msisdn, :msisdnAN, :msisdnNP, :sendUntil, :dlrType)"),
	@NamedNativeQuery(name = "checkExpired", query = "CALL spCheckExpired(:divider, :remainder, :qLimit);"),
	@NamedNativeQuery(name = "resetQueues", query = "CALL spResetQueues();"),
	@NamedNativeQuery(name = "saveSMPPClientSubmitLog", query = "CALL spSMPPClientSaveSubmitSMReponse(:remoteId, :messageId"
			+ ", :clientId, :errorMessage, :started, :finished, :sendUntil);"),
	@NamedNativeQuery(name = "saveSMPPProcessDLR", query = "CALL spSMPPClientProcessDLRRequest(:remoteId, :state, :clientId, :occurred, :sendDLRUntil);"),
	@NamedNativeQuery(name = "addCancelSMRequest", query = "CALL spProcessCancelSM(:messageId, :accessId, :reqType, :queueType);")
//	@NamedNativeQuery(name = "getQueueStatus", query = "CALL spGetQueueStatus();")
})

@Entity
@Table(name="smsqueue")
public class SMSQueue {

	public SMSQueue() {
		// TODO Auto-generated constructor stub
	}
	
	public SMSQueue(long messageId, int systemId, String fromAD, String fromTON, String fromNP, Long toAD, String toAN,
			String toNP, String message, int quantity, int dcs, int pid, Timestamp inserted, Timestamp scheduledTime,
			Timestamp senduntil, String dlrResponseType, String priority, String segmentLen) {
		this.messageId = messageId;
		this.systemId = systemId;
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
		this.scheduledTime = scheduledTime;
		this.senduntil = senduntil;
		this.dlrResponseType = dlrResponseType;
		this.priority = priority;
		this.segmentLen = segmentLen;
	}

	@Id
	@Column(name = "messageId", nullable = false)
	private long messageId;
	
	@Column(name = "systemId", nullable = false, columnDefinition = "TINYINT")
	private int systemId;
	
	@Column(name = "fromAD", nullable = false, length = 15)
	private String fromAD;
	
	@Column(name = "fromTON", nullable = false)
	private String fromTON;
	
	@Column(name = "fromNP", nullable = false)
	private String fromNP;
	
	@Column(name = "toAD", nullable = false, length = 15)
	private Long toAD;
	
	@Column(name = "toAN", nullable = false)
	private String toAN;
	
	@Column(name = "toNP", nullable = false)
	private String toNP;
	
	@Column(name = "message", nullable = false, length = 8000)
	private String message;
	
	@Column(name = "quantity", nullable = false, columnDefinition = "TINYINT")
	private int quantity;
	
	@Column(name = "dcs", nullable = false, columnDefinition = "SMALLINT")
	private int dcs;
	
	@Column(name = "pid", nullable = false, columnDefinition = "SMALLINT")
	private int pid;
	
	@Column(name = "inserted", nullable = false)
	@Type(type="timestamp")
	private Timestamp inserted;
	
	@Column(name = "scheduledtime", nullable = false)
	@Type(type="timestamp")
	private Timestamp scheduledTime;
	
	@Column(name = "senduntil", nullable = false)
	@Type(type="timestamp")
	private Timestamp senduntil;
	
	@Column(name = "dlrResponseType", nullable = false)
	private String dlrResponseType;
	
	@Column(name = "priority", nullable = false)
	private String priority;
	
	@Column(name = "segmentLen")
	private String segmentLen;
	
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

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
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

	public Timestamp getScheduledTime() {
		return scheduledTime;
	}

	public void setScheduledTime(Timestamp scheduledTime) {
		this.scheduledTime = scheduledTime;
	}
	
	public Timestamp getSenduntil() {
		return senduntil;
	}

	public void setSenduntil(Timestamp senduntil) {
		this.senduntil = senduntil;
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

	public void setSegmetnLen(String segmentLen) {
		this.segmentLen = segmentLen;
	}	

	@Override
	public String toString() {
		return "SMSQueue [messageId=" + messageId + ", systemId=" + systemId + ", fromAD=" + fromAD 
				+ ", toAD=" + toAD + ", quantity=" + quantity + ", dcs=" + dcs + ", pid=" + pid 
				+ ", inserted=" + inserted + ", scheduledTime=" + scheduledTime + ", senduntil=" + senduntil 
				+ ", dlrResponseType=" + dlrResponseType + ", priority=" + priority + ", segmentLen=" + segmentLen + "]";
	}
}
