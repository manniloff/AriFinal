package com.unifun.sigtran.smsgate.smpp.common;

public class SMPPConnectionInfo {

	private int clientId;
	private int accessId;
	private String login;
	private short messageParts;
	private String currentSMPPSessionId;
	private volatile boolean waitingQueueIsFull;
	
	public SMPPConnectionInfo(int clientId, int accessId, String login, short messageParts,
			String currentSMPPSessionId) {
		this.clientId = clientId;
		this.accessId = accessId;
		this.login = login;
		this.messageParts = messageParts;
		this.currentSMPPSessionId = currentSMPPSessionId;
	}
	
	public int getClientId() {
		return clientId;
	}
	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	public int getAccessId() {
		return accessId;
	}
	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public short getMessageParts() {
		return messageParts;
	}
	public void setMessageParts(short messageParts) {
		this.messageParts = messageParts;
	}
	public String getCurrentSMPPSessionId() {
		return currentSMPPSessionId;
	}
	public void setCurrentSMPPSessionId(String currentSMPPSessionId) {
		this.currentSMPPSessionId = currentSMPPSessionId;
	}
	@Override
	public String toString() {
		return "SMPPConnectionInfo [clientId=" + clientId + ", accessId=" + accessId + ", login=" + login
				+ "messageParts=" + messageParts + ", currentSMPPSessionId=" + currentSMPPSessionId + "]";
	}

	public boolean getWaitingQueueIsFull() {
		return waitingQueueIsFull;
	}

	public void setWaitingQueueIsFull(boolean waitingQueueIsFull) {
		this.waitingQueueIsFull = waitingQueueIsFull;
	}
}
