package com.unifun.sigtran.smsgate.smpp.common;

import java.sql.Timestamp;

public class MessageContainer {

	private long messageId;
	private String containerId;
	private short smsLen;
	private short currentContainer;
	private short containerReceived;  
	private String[] messagePart;
	private String messageText;
	private short dlrType;
	private Timestamp waitTime;
	private int id;
		
	public MessageContainer(String containerId, short smsLen, short currentContainer
			, String messageText, Timestamp waitTime, int id) {
		this.containerId = containerId;
		this.smsLen = smsLen;
		this.currentContainer = currentContainer;
		this.containerReceived = 1;
		this.messagePart = new String[smsLen];
		this.messageText = messageText;
		this.messagePart[currentContainer - 1] = messageText;
		this.waitTime = waitTime;
		this.id = id;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

	public short getSmsLen() {
		return smsLen;
	}

	public void setSmsLen(short smsLen) {
		this.smsLen = smsLen;
	}

	public short getCurrentContainer() {
		return currentContainer;
	}

	public void setCurrentContainer(short currentContainer) {
		this.currentContainer = currentContainer;
	}

	public short getContainerReceived() {
		return containerReceived;
	}

	public void setContainerReceived(short containerReceived) {
		this.containerReceived = containerReceived;
	}

	public String[] getMessagePart() {
		return messagePart;
	}

	public void setMessagePart(String[] messagePart) {
		this.messagePart = messagePart;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
		
	public short getDlrType() {
		return dlrType;
	}

	public void setDlrType(short dlrType) {
		this.dlrType = dlrType;
	}

	public Timestamp getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(Timestamp waitTime) {
		this.waitTime = waitTime;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "MessageContainer [containerId=" + containerId + ", smsLen=" + smsLen
				+ ", currentContainer=" + currentContainer + ", containerReceived=" + containerReceived
//				+ ", messagePart=" + Arrays.toString(messagePart) + ", messageText=" + messageText + ", waitTime=" + waitTime 
				+ ", id=" + id + "]";
	}
}
