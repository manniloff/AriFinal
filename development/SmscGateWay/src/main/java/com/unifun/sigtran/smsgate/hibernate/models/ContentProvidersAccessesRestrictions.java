package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="content_providers_access_restrictions")
public class ContentProvidersAccessesRestrictions {

	@Id
	@GeneratedValue
	private int id;
	@Column(name="content_providers_access_id")
	private int accessId;
	@Column(name="source_address", nullable=true)
	private String sourceAddress;
	@Column(name="source_ton", nullable=true)
	private String sourceTon;
	@Column(name="source_np", nullable=true)
	private String sourceNp;
	@Column(name="dest_ton", nullable=true)
	private String destTon;
	@Column(name="dest_np", nullable=true)
	private String destNp;
	
	public ContentProvidersAccessesRestrictions() {
		// TODO Auto-generated constructor stub
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAccessId() {
		return accessId;
	}

	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(String sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	public String getSourceTon() {
		return sourceTon;
	}

	public void setSourceTon(String sourceTon) {
		this.sourceTon = sourceTon;
	}

	public String getSourceNp() {
		return sourceNp;
	}

	public void setSourceNp(String sourceNp) {
		this.sourceNp = sourceNp;
	}

	public String getDestTon() {
		return destTon;
	}

	public void setDestTon(String destTon) {
		this.destTon = destTon;
	}

	public String getDestNp() {
		return destNp;
	}

	public void setDestNp(String destNp) {
		this.destNp = destNp;
	}
}
