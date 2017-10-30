package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;


@NamedNativeQueries({
	  @NamedNativeQuery(name="saveDLRResponse"
		, query="CALL spSmppServerSaveDlrResponse(:messageId, :started, :finished"
				+ ", :errorMessage, :attempts, :doNextAttempt);")
})

@Entity
@Table(name="send_waiting_list")
public class SendData {

	public SendData() {
		// TODO Auto-generated constructor stub
	}
	
	public SendData(long messageId, String fromAD, String fromTON, String fromNP, Long toAD, String toAN, String toNP,
			String message, short quantity, int dcs, int pid, Timestamp inserted, Timestamp senduntil, int systemId,
			String dlrResponseType, String priority, String segmentLen, Long dialogId, Timestamp started,
			short messagePart, int sendAttempts) {
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
		this.senduntil = senduntil;
		this.systemId = systemId;
		this.dlrResponseType = dlrResponseType;
		this.priority = priority;
		this.segmentLen = segmentLen;
		this.dialogId = dialogId;
		this.started = started;
		this.messagePart = messagePart;
		this.sendAttempts = sendAttempts;
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
	private Timestamp senduntil;
	
	@Column(name = "systemId", length = 50)
	private int systemId;
	
	@Column(name = "dlrResponseType", nullable = false)
	private String dlrResponseType;
	
	@Column(name = "priority", nullable = false)
	private String priority;
	
	@Column(name = "segmentLen")
	private String segmentLen;
	 
	@Transient
	private Long dialogId;
	
	@Transient
	private Timestamp started;
	
	@Transient
	private short messagePart = 0;
	
	@Transient
	private ISDNAddressString networkNodeNumber;
	
	@Transient
	private IMSI imsi;
	
	@Transient
	private SMDeliveryOutcome smDeliveryOutcome;
	
	@Transient
	private int sendAttempts = 1;

	@Transient
	private short dlrAttempts = 1;
	
	@Transient
	private boolean expired = false;
	
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
		return senduntil;
	}

	public void setSendUntil(Timestamp senduntil) {
		this.senduntil = senduntil;
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

	public void setSegmetnLen(String segmentLen) {
		this.segmentLen = segmentLen;
	}

	public Long getDialogId() {
		return dialogId;
	}

	public void setDialogId(long dialogId) {
		this.dialogId = dialogId;
	}

	public Timestamp getStarted() {
		return started;
	}

	public void setStarted(Timestamp started) {
		this.started = started;
	}

	public ISDNAddressString getNetworkNodeNumber() {
		return networkNodeNumber;
	}

	public void setNetworkNodeNumber(ISDNAddressString networkNodeNumber) {
		this.networkNodeNumber = networkNodeNumber;
	}

	public IMSI getImsi() {
		return imsi;
	}

	public void setImsi(IMSI imsi) {
		this.imsi = imsi;
	}

	public short getMessagePart() {
		return messagePart;
	}

	public void setMessagePart(short messagePart) {
		this.messagePart = messagePart;
	}

	public int getSendAttempts() {
		return sendAttempts;
	}

	public void setSendAttempts(int sendAttempts) {
		this.sendAttempts = sendAttempts;
	}

	public short getDlrAttempts() {
		return dlrAttempts;
	}

	public void setDlrAttempts(short dlrAttempts) {
		this.dlrAttempts = dlrAttempts;
	}

	public SMDeliveryOutcome getSmDeliveryOutcome() {
		return smDeliveryOutcome;
	}

	public void setSmDeliveryOutcome(SMDeliveryOutcome smDeliveryOutcome) {
		this.smDeliveryOutcome = smDeliveryOutcome;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	@Override
	public String toString() {
		return "SendData [messageId=" + messageId + ", fromAD=" + fromAD + ", toAD=" + toAD + ", quantity=" + quantity
				+ ", dcs=" + dcs + ", pid=" + pid + ", senduntil=" + senduntil + ", systemId=" + systemId
				+ ", priority=" + priority + ", segmentLen=" + segmentLen + "messagePart=" + messagePart + "]";
	}
}
