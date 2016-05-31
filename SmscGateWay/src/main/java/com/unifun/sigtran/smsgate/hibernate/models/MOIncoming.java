package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name="mo_incoming_log")
public class MOIncoming {

	public MOIncoming(long messageId, int systemId, String fromAD, String fromTON, String fromNP, Long toAD, String toAN,
			String toNP, String message, int dcs, int pid, Timestamp occurred) {
		this.messageId = messageId;
		this.systemId = systemId;
		this.fromAD = fromAD;
		this.fromTON = fromTON;
		this.fromNP = fromNP;
		this.toAD = toAD;
		this.toAN = toAN;
		this.toNP = toNP;
		this.message = message;
		this.dcs = dcs;
		this.pid = pid;
		this.occurred = occurred;
	}

	@Id
	@Column(name = "messageId", nullable = false)
	private long messageId;
	@Column(name = "systemId", nullable = false)
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
	
	@Column(name = "dcs", nullable = false, columnDefinition = "SMALLINT")
	private int dcs;
	
	@Column(name = "pid", nullable = false, columnDefinition = "SMALLINT")
	private int pid;
	
	@Column(name = "occurred", nullable = false)
	@Type(type="timestamp")
	private Timestamp occurred;

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

	public Timestamp getOccurred() {
		return occurred;
	}

	public void setOccurred(Timestamp occurred) {
		this.occurred = occurred;
	}

	@Override
	public String toString() {
		return "IncomingMO [messageId=" + messageId + ", systemId=" + systemId + ", fromAD=" + fromAD + ", fromTON="
				+ fromTON + ", fromNP=" + fromNP + ", toAD=" + toAD + ", toAN=" + toAN + ", toNP=" + toNP + "]";
	}
	
	
}
