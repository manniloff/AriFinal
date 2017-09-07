package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_server_config")
public class SMPPServerConfig {
	@Id@GeneratedValue
	private int id;
	@Column(name="server_name", nullable=false, length=50)
	private String serverName;
	@Column(name="server_port", nullable=false)
	private int serverPort;
	@Column(name="waitbind", nullable=false)
	private int waitbind;
	@Column(name="poolSize", nullable=false)
	private int poolSize;
	@Column(name="timeout", nullable=false)
	private int timeout;
	@Column(name="next_part_waiting", nullable=false)
	private int nextPartWaiting;
	@Column(name="processor_degree", nullable=false)
	private int processorDegree;
	@Column(name="inerface_version", nullable=false, length=6)
	private String InerfaceVersion;
	@Column(name="service_type", nullable=false, length=24)
	private String serviceType;
	@Column(name="concatinate_type", nullable=false, length=2)
	private String concatinateType;
	@Column(name="send_dlr_per_sec", nullable=false)
	private int sendDLRPerSec;
	
	public SMPPServerConfig() {
		// TODO Auto-generated constructor stub
	}
	
	public SMPPServerConfig(int id, String serverName, int serverPort, int waitbind, int poolSize, int timeout,
			int nextPartWaiting, int processorDegree, String inerfaceVersion, String serviceType
			, String concatinateType, short moSleepTime, int sendDLRPerSec) {
		super();
		this.id = id;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.waitbind = waitbind;
		this.poolSize = poolSize;
		this.timeout = timeout;
		this.nextPartWaiting = nextPartWaiting;
		this.processorDegree = processorDegree;
		InerfaceVersion = inerfaceVersion;
		this.serviceType = serviceType;
		this.concatinateType = concatinateType;
		this.sendDLRPerSec = sendDLRPerSec;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	public int getWaitbind() {
		return waitbind;
	}
	public void setWaitbind(int waitbind) {
		this.waitbind = waitbind;
	}
	public int getPoolSize() {
		return poolSize;
	}
	public void setResponseThreads(int poolSize) {
		this.poolSize = poolSize;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public int getNextPartWaiting() {
		return nextPartWaiting;
	}
	public void setNextPartWaiting(int nextPartWaiting) {
		this.nextPartWaiting = nextPartWaiting;
	}
	public String getInerfaceVersion() {
		return InerfaceVersion;
	}
	public void setInerfaceVersion(String inerfaceVersion) {
		InerfaceVersion = inerfaceVersion;
	}
	public int getProcessorDegree() {
		return processorDegree;
	}
	public void setProcessorDegree(int processorDegree) {
		this.processorDegree = processorDegree;
	}
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getConcatinateType() {
		return concatinateType;
	}

	public void setConcatinateType(String concatinateType) {
		this.concatinateType = concatinateType;
	}

	public int getSendDLRPerSec() {
		return sendDLRPerSec;
	}

	public void setSendDLRPerSec(int sendDLRPerSec) {
		this.sendDLRPerSec = sendDLRPerSec;
	}
}
