package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.unifun.sigtran.smsgate.enums.Direction;

@Entity
@Table(name="msisdn_black_list")
public class MsisdnBlackList {

	@Id
	private long msisdn;
	@Enumerated(EnumType.STRING)
	private Direction direction;
	@Column(name="started", columnDefinition="TIMESTAMP")
	private Timestamp started;
	
	public MsisdnBlackList() {
		
	}

	public MsisdnBlackList(long msisdn, Direction direction, Timestamp started) {
		super();
		this.msisdn = msisdn;
		this.direction = direction;
		this.started = started;
	}

	public long getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(long msisdn) {
		this.msisdn = msisdn;
	}

	@Enumerated(EnumType.STRING)
	public Direction getDirection() {
		return direction;
	}
	@Enumerated(EnumType.STRING)
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Timestamp getStarted() {
		return started;
	}

	public void setStarted(Timestamp started) {
		this.started = started;
	}

	@Override
	public String toString() {
		return "MsisdnBlackList [msisdn=" + msisdn + ", direction=" + direction + ", started=" + started + "]";
	}
	
}
