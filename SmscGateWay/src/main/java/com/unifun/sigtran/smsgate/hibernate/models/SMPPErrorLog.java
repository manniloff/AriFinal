package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "smpp_error_logs")
public class SMPPErrorLog {
	
	public SMPPErrorLog() {}
	
	
	
	public SMPPErrorLog(int systemId, String sourceAddr, long destAddr, int sequenceNumber, int errorCode,
			String errorMessage, Timestamp occurred) {
		this.systemId = systemId;
		this.sourceAddr = sourceAddr;
		this.destAddr = destAddr;
		this.sequenceNumber = sequenceNumber;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.occurred = occurred; 
	}

	public Timestamp getOccurred() {
		return occurred;
	}



	public void setOccurred(Timestamp occurred) {
		this.occurred = occurred;
	}



	@Id@GeneratedValue
	private long id;
	
	@Column(name = "systemId", nullable = false, columnDefinition = "TINYINT")
	private int systemId;
	
	@Column(name = "sourceAddr", nullable = false, length=15)
	private String sourceAddr;
	
	@Column(name = "destAddr", nullable = false, length=15)
	private long destAddr;
	
	@Column(name = "sequenceNumber", nullable = false)
	private int sequenceNumber;
	
	@Column(name = "errorCode", nullable = false, columnDefinition = "TINYINT")
	private int errorCode;
	
	@Column(name = "errorMessage", nullable = false, length = 256)
	private String errorMessage;
	
	@Column(name = "occurred", nullable = false)
	private Timestamp occurred;
}
