package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="content_providers")
public class ContentProvider {
	
	@Id
	@GeneratedValue
	private int id;
	@Column(name="provider_name", nullable=false, unique=true)
	private String providerName;
	@Column(name="creation_date", columnDefinition="TIMESTAMP")
	private Timestamp creationDate;
	@Column(name="last_modification_date", columnDefinition="TIMESTAMP")
	private Timestamp modificationDate;
	private boolean enabled;
	
	public ContentProvider() {
		// TODO Auto-generated constructor stub
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Timestamp getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Timestamp creationDate) {
		this.creationDate = creationDate;
	}

	public Timestamp getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Timestamp modificationDate) {
		this.modificationDate = modificationDate;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "ContentProvider [id=" + id + ", providerName=" + providerName + ", creationDate=" + creationDate
				+ ", modificationDate=" + modificationDate + ", enabled=" + enabled + "]";
	}
	
}
