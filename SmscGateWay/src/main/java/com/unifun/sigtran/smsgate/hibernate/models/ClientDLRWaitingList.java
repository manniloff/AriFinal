package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_client_dlr_waiting_list")
public class ClientDLRWaitingList {

	@Id
	private Long remoteId;
	private Long messageId;
	private int clientId;
	private Timestamp waitDlrUntil;
	
	public ClientDLRWaitingList() {
		// TODO Auto-generated constructor stub
	}
	
	public ClientDLRWaitingList(Long remoteId, Long messageId, int clientId, Timestamp waitDlrUntil) {
		super();
		this.remoteId = remoteId;
		this.messageId = messageId;
		this.clientId = clientId;
		this.waitDlrUntil = waitDlrUntil;
	}
	
	public Long getRemoteId() {
		return remoteId;
	}
	public void setRemoteId(Long remoteId) {
		this.remoteId = remoteId;
	}
	public Long getMessageId() {
		return messageId;
	}
	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}
	public int getClientId() {
		return clientId;
	}
	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	public Timestamp getWaitDlrUntil() {
		return waitDlrUntil;
	}
	public void setWaitDlrUntil(Timestamp waitDlrUntil) {
		this.waitDlrUntil = waitDlrUntil;
	}
}
