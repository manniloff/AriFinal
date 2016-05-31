package com.unifun.sigtran.smsgate.hibernate.models;

import java.math.BigInteger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="temp_content_provide_access_queue_size")
public class TempContentProvideAccessQueueSize {

	public TempContentProvideAccessQueueSize() {
		// TODO Auto-generated constructor stub
	}
	@Id
	private int systemId;
	private long size;
	public int getSystemId() {
		return systemId;
	}
	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}
	public long getSize() {
		return size;
	}
	public void setSize(BigInteger size) {
		this.size = size.longValue();
	}	
}
