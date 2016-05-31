package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="smsgateway_settings")
public class GateWaySettings {
	
	public GateWaySettings() {
		// TODO Auto-generated constructor stub
	}
	
	public GateWaySettings(String type, String name, String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	@Id
	@GeneratedValue
	@Column(name="id", nullable=false)
	private int id;
	@Column(name="st_type", nullable=false)
	private String type;
	@Column(name="name", nullable=false)
	private String name;
	@Column(name="value", nullable=false)
	private String value;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "GateWaySettings [id=" + id + ", type=" + type + ", name=" + name + ", value=" + value + "]";
	}
	
	
}
