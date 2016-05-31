package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_mo_response_log")
public class SMPPMoResponse {

	@Id
	private long messageId; 
	private int accessId;
	private Timestamp started;
	private Timestamp finished;
	private String errorMessage;
	private String state;
	
	
	public SMPPMoResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public SMPPMoResponse(long messageId, int accessId, Timestamp started, Timestamp finished, String errorMessage,
			String state) {
		super();
		this.messageId = messageId;
		this.accessId = accessId;
		this.started = started;
		this.finished = finished;
		this.errorMessage = errorMessage;
		this.state = state;
	}

	public long getMessageId() {
		return messageId;
	}
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}
	public int getAccessId() {
		return accessId;
	}
	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}
	public Timestamp getStarted() {
		return started;
	}
	public void setStarted(Timestamp started) {
		this.started = started;
	}
	public Timestamp getFinished() {
		return finished;
	}
	public void setFinished(Timestamp finished) {
		this.finished = finished;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
}
