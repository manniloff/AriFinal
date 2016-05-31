package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="mo_routing_rules")
public class MoRoutingRules {

	@Id
	private String address;
	@Column(name="access_id", nullable=false)
	private int accessId;
	@Column(nullable=false)
	private boolean enabled;
	
	public MoRoutingRules() {
		// TODO Auto-generated constructor stub
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getAccessId() {
		return accessId;
	}
	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}	
}
