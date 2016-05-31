package com.unifun.sigtran.smsgate.smpp.common;

import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionLimits {
	public String login;
	public AtomicInteger counter;
	
	public ConnectionLimits(String login, int maxSms) {
		this.login = login;
		this.counter = new AtomicInteger();
	}
}
