package com.unifun.sigtran.smsgate.hibernate.models;

import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="smpp_clients_groups")
public class SMPPClientsGroups {

	@Id
	@GeneratedValue
	@Column(name="id")
	private int groupId;
	@Column(name="groupName", nullable=false)
	private String groupName;
	@Column(name="priority", nullable=false)
	private int groupPriority;
	@Transient
	private AtomicInteger availableClients;
	private boolean enabled;
	
	public SMPPClientsGroups() {
		// TODO Auto-generated constructor stub
	}
	
	public SMPPClientsGroups(int groupId, String groupName, int groupPriority, AtomicInteger availableClients, boolean enabled) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.groupPriority = groupPriority;
		this.availableClients = availableClients;
		this.enabled = enabled;
	}

	public int getGroupId() {
		return groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public int getGroupPriority() {
		return groupPriority;
	}
	public AtomicInteger getAvailableClients() {
		return availableClients;
	}

	public void setAvailableClients(AtomicInteger availableClients) {
		this.availableClients = availableClients;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
