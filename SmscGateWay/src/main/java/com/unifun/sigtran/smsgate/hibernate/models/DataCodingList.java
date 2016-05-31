package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="data_coding_list")
public class DataCodingList {

	@Id
	private short dcsId;
	private String charset;
	private boolean compressed;
	private boolean enabled;
	
	public DataCodingList() {
		// TODO Auto-generated constructor stub
	}

	public short getDcsId() {
		return dcsId;
	}

	public void setDcsId(short dcsId) {
		this.dcsId = dcsId;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isCompressed() {
		return compressed;
	}

	public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
