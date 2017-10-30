package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.unifun.sigtran.smsgate.enums.SmsCharSet;

@Entity
@Table(name="content_providers_access_charset_restrictions")
public class ContentProviderAccessCharsetList {

	@Id
	@GeneratedValue
	private int id;
	private int accessId;
	@Column(name="charset")
	@Enumerated(EnumType.STRING)
	private SmsCharSet smsCharset;
	private boolean enabled;
	
	public ContentProviderAccessCharsetList() {
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
	
	@Enumerated(EnumType.STRING)
	public SmsCharSet getSmsCharset() {
		return smsCharset;
	}

	public void setSmsCharset(SmsCharSet smsCharset) {
		this.smsCharset = smsCharset;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
