package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="erroneous_requets_log")
public class ErroneousRequests {
		
	public ErroneousRequests() {
		// TODO Auto-generated constructor stub
	}
	
	public ErroneousRequests(long messageId, int systemId, String methodtype, String errorMessage) {
		super();
		this.messageId = messageId;
		this.systemId = systemId;
		this.methodtype = methodtype;
		this.errorMessage = errorMessage;
		this.occurred = new Timestamp(System.currentTimeMillis());
	}
	@Id
	@Column(name = "messageId", nullable = false)
	private long messageId;
	
	@Column(name = "systemId", nullable = false, columnDefinition = "TINYINT")
	private int systemId;
	
	@Column(name = "methodtype", nullable = false)
	private String methodtype;
	
	@Column(name = "errormessage", nullable = false, length = 1024)
	private String errorMessage;
	
	@Column(name = "occurred", nullable = false, columnDefinition = "TIMESTAMP")
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

	public String getMethodtype() {
		return methodtype;
	}

	public void setMethodtype(String methodtype) {
		this.methodtype = methodtype;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Timestamp getOccurred() {
		return occurred;
	}

	public void setOccurred(Timestamp occurred) {
		this.occurred = occurred;
	}

	@Override
	public String toString() {
		return "ErroneousRequests [messageId=" + messageId + ", systemId=" + systemId + ", methodtype=" + methodtype
				+ ", errorMessage=" + errorMessage + ", occurred=" + occurred + "]";
	}
}
