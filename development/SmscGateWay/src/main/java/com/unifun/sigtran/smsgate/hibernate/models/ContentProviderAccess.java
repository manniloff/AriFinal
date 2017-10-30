package com.unifun.sigtran.smsgate.hibernate.models;

import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.unifun.sigtran.smsgate.enums.AccessType;
import com.unifun.sigtran.smsgate.enums.ChangeSubmitData;
import com.unifun.sigtran.smsgate.enums.Direction;
import com.unifun.sigtran.smsgate.enums.ExpiredType;
import com.unifun.sigtran.smsgate.enums.SchedulerDays;
import com.unifun.sigtran.smsgate.enums.SmsType;
import com.unifun.sigtran.smsgate.enums.DeliveryType;

@Entity
@Table(name="content_providers_access")
public class ContentProviderAccess {
	
	@Id
	@GeneratedValue
	private int id;
	@Column(name="content_providers_id", nullable=false)
	private int providerId;
	@Column(name="login", nullable=false, unique=true)
	private String login;
	@Column(name="password", nullable=false)
	private String password;
	@Column(name="host", nullable=false)
	private String host;
	@Column(name="ton", nullable=false)
	private String ton;
	@Column(name="np", nullable=false)
	private String np;
	@Column(name="speed_limit", nullable=false)
	private short speedLimit;
	@Column(name="sms_parts", nullable=false)
	private short smsParts;
	
	@Column(name="change_submit_date_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private ChangeSubmitData changeSubmitDateType;
	
	@Column(name="sms_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private SmsType smsType;
	@Column(name="can_send_sms_from_time", columnDefinition="TIME")
	private Time canSendSMSFromTime;
	@Column(name="can_send_sms_to_time", columnDefinition="TIME")
	private Time canSendSMSToTime;
	
	@Column(name="days_of_week", nullable=false)
	@Enumerated(EnumType.STRING)
	private SchedulerDays daysOfWeek;
	
	@Column(name="direction", nullable=false)
	@Enumerated(EnumType.STRING)
	private Direction direction;
	
	@Column(name="access_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private AccessType accessType;
	
	@Column(name="creation_datetime", columnDefinition="TIMESTAMP")
	private Timestamp creationDate;
	@Column(name="last_modification_datetime", columnDefinition="TIMESTAMP")
	private Timestamp modificationDate;
	
	@Column(name="expired_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private ExpiredType expiredType;
	
	@Column(name="delivery_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private DeliveryType DeliveryType;
	
	@Column(name="default_sms_live_timeInMin")
	private Long defaultSmsLiveTimeInMin;
	@Column(name="queue_size", nullable=false)
	private int queueSize;
	private boolean enabled;
	@Transient
	private Long defaultSmsLiveTimeInMilSec;
	
	public ContentProviderAccess() {
		// TODO Auto-generated constructor stub
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getProviderId() {
		return providerId;
	}

	public void setProviderId(int providerId) {
		this.providerId = providerId;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
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

	public short getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(short speedLimit) {
		this.speedLimit = speedLimit;
	}

	public short getSmsParts() {
		return smsParts;
	}

	public void setSmsParts(short smsParts) {
		this.smsParts = smsParts;
	}
	
	@Enumerated(EnumType.STRING)
	public ChangeSubmitData getChangeSubmitDateType() {
		return changeSubmitDateType;
	}

	@Enumerated(EnumType.STRING)
	public void setChangeSubmitDateType(ChangeSubmitData changeSubmitDateType) {
		this.changeSubmitDateType = changeSubmitDateType;
	}

	@Enumerated(EnumType.STRING)
	public SmsType getSmsType() {
		return smsType;
	}

	public void setSmsType(SmsType smsType) {
		this.smsType = smsType;
	}

	public Time getCanSendSMSFromTime() {
		return canSendSMSFromTime;
	}

	public void setCanSendSMSFromTime(Time canSendSMSFromTime) {
		this.canSendSMSFromTime = canSendSMSFromTime;
	}

	public Time getCanSendSMSToTime() {
		return canSendSMSToTime;
	}

	public void setCanSendSMSToTime(Time canSendSMSToTime) {
		this.canSendSMSToTime = canSendSMSToTime;
	}
	@Enumerated(EnumType.STRING)
	public SchedulerDays getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(SchedulerDays daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}
	@Enumerated(EnumType.STRING)
	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	@Enumerated(EnumType.STRING)
	public AccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
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
	@Enumerated(EnumType.STRING)
	public ExpiredType getExpiredType() {
		return expiredType;
	}

	public void setExpiredType(ExpiredType expiredType) {
		this.expiredType = expiredType;
	}
	
	@Enumerated(EnumType.STRING)
	public DeliveryType getDeliveryType() {
		return DeliveryType;
	}

	public void setDeliveryType(DeliveryType deliveryType) {
		DeliveryType = deliveryType;
	}

	public Long getDefaultSmsLiveTimeInMin() {
		return defaultSmsLiveTimeInMin;
	}

	public void setDefaultSmsLiveTimeInMin(Long defaultSmsLiveTime) {
		this.defaultSmsLiveTimeInMin = defaultSmsLiveTime;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public Long getDefaultSmsLiveTimeInMilSec() {
		return defaultSmsLiveTimeInMilSec;
	}

	public void setDefaultSmsLiveTimeInMilSec(Long defaultSmsLiveTimeInMilSec) {
		this.defaultSmsLiveTimeInMilSec = defaultSmsLiveTimeInMilSec;
	}

}
