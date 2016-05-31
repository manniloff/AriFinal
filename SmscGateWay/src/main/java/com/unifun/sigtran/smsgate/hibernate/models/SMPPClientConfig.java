package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smpp_client_config")
public class SMPPClientConfig {
	
	@Id
	@GeneratedValue
	private int id;
	@Column(name="systemId", length=8, nullable=false)
	private String systemId;
	@Column(name="password", length=8, nullable=false)
	private String password;
	@Column(name="groupID", nullable=false)
	private int groupID;
	@Column(name="clientPriority", nullable=false)
	private int clientPriority;
	@Column(name="concatenateType", nullable=false)
	private String concatenateType;
	@Column(name="systemType", nullable=false)
	private String systemType;
	@Column(name="serviceType", nullable=false)
	private String serviceType;
	@Column(name="ton", nullable=false)
	private String ton;
	@Column(name="np", nullable=false)
	private String np;
	@Column(name="host", nullable=false)
	private String host;
	@Column(name="port", nullable=false)
	private int port;
	@Column(name="timeOut", nullable=false)
	private int timeOut;
	@Column(name="pduProcessorDegree", nullable=false)
	private int pduProcessorDegree;
	@Column(name="bindType", nullable=false)
	private String bindType;
	@Column(name="reconnectTries", nullable=false)
	private int reconnectTries;
	@Column(name="reconnectTriesTime", nullable=false)
	private int reconnectTriesTime;
	@Column(name="speedLimit", nullable=false)
	private int speedLimit;
	@Column(name="remoteIdType", nullable=false)
	private String remoteIdType;
	@Column(name="dlrIdType", nullable=false)
	private String dlrIdType;
	
	public SMPPClientConfig() {
		// TODO Auto-generated constructor stub
	}
	
	public SMPPClientConfig(int id, String systemId, String password, Integer groupID, Integer clientPriority,
			String concatenateType, String systemType, String serviceType, String ton, String np, String host, int port,
			int timeOut, int pduProcessorDegree, String bindType, int reconnectTries, int reconnectTriesTime,
			int speedLimit, String remoteIdType, String dlrIdType) {
		this.id = id;
		this.systemId = systemId;
		this.password = password;
		this.groupID = groupID;
		this.clientPriority = clientPriority;
		this.concatenateType = concatenateType;
		this.systemType = systemType;
		this.serviceType = serviceType;
		this.ton = ton;
		this.np = np;
		this.host = host;
		this.port = port;
		this.timeOut = timeOut;
		this.pduProcessorDegree = pduProcessorDegree;
		this.bindType = bindType;
		this.reconnectTries = reconnectTries;
		this.reconnectTriesTime = reconnectTriesTime;
		this.speedLimit = speedLimit;
		this.remoteIdType = remoteIdType;
		this.dlrIdType = dlrIdType;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Integer getGroupID() {
		return groupID;
	}
	public void setGroupID(Integer groupID) {
		this.groupID = groupID;
	}
	public Integer getClientPriority() {
		return clientPriority;
	}
	public void setClientPriority(Integer clientPriority) {
		this.clientPriority = clientPriority;
	}
	public String getConcatenateType() {
		return concatenateType;
	}
	public void setConcatenateType(String concatenateType) {
		this.concatenateType = concatenateType;
	}
	public String getSystemType() {
		return systemType;
	}
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}
	public String getServiceType() {
		return serviceType;
	}
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}
	public String getTon() {
		return ton;
	}
	public void setTon(String ton) {
		this.ton = ton;
	}
	public String getNp() {
		return np;
	}
	public void setNp(String np) {
		this.np = np;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getTimeOut() {
		return timeOut;
	}
	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}
	public int getPduProcessorDegree() {
		return pduProcessorDegree;
	}
	public void setPduProcessorDegree(int pduProcessorDegree) {
		this.pduProcessorDegree = pduProcessorDegree;
	}
	public String getBindType() {
		return bindType;
	}
	public void setBindType(String bindType) {
		this.bindType = bindType;
	}
	public int getReconnectTries() {
		return reconnectTries;
	}
	public void setReconnectTries(int reconnectTries) {
		this.reconnectTries = reconnectTries;
	}
	public int getReconnectTriesTime() {
		return reconnectTriesTime;
	}
	public void setReconnectTriesTime(int reconnectTriesTime) {
		this.reconnectTriesTime = reconnectTriesTime;
	}
	public int getSpeedLimit() {
		return speedLimit;
	}
	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}
	public String getRemoteIdType() {
		return remoteIdType;
	}
	public void setRemoteIdType(String remoteIdType) {
		this.remoteIdType = remoteIdType;
	}
	public String getDlrIdType() {
		return dlrIdType;
	}
	public void setDlrIdType(String dlrIdType) {
		this.dlrIdType = dlrIdType;
	}
}