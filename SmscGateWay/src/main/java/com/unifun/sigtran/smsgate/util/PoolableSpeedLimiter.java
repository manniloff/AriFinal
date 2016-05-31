package com.unifun.sigtran.smsgate.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
/**
 * SpeedLimiter  - calculate event limit with possibility to exceed speed temporary using Pool
 *	limiter = new PoolableSpeedLimiter("LimiterName",MaxEventPerSec,ExceedPoolSize);
	limiter.start();
	boolean isThrottling = limiter.isThrottling(IntegerEventCount);
 * */
public class PoolableSpeedLimiter extends Thread {
	private int MaxSpeed;
	private String name;
	private SpeedLimiter speedLimiter;
	private AtomicInteger internalCounter;
	private Integer MaxQueueSize;
	
	private static final Logger logger = LogManager.getLogger(PoolableSpeedLimiter.class);
	private Lock lock;
	

	public PoolableSpeedLimiter(String name, int MaxSpeed, int queueSize) { 
		this.name = name;
		this.MaxSpeed = MaxSpeed;
		this.internalCounter =  new AtomicInteger(0);
		this.MaxQueueSize = queueSize;
		SpeedLimiter internal_limiter = new SpeedLimiter((double) 1);
		internal_limiter.setCorrelation(1);
		speedLimiter = internal_limiter;
		lock = new ReentrantLock();
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("ThrottlingLimmiter-" + name);
		while (!Thread.currentThread().isInterrupted()) {
			long StartActionPoint = System.nanoTime();
			try {
				
				Integer current =internalCounter.get();
				//System.out.println("Current counter value:"+current);
				if(current>0){
					if(current>=MaxSpeed){
						internalCounter.set(current-MaxSpeed);
					}
					else{
						internalCounter.set(0);
					}
				}
				speedLimiter.LimitSpeed(System.nanoTime() - StartActionPoint);
			} 
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
			}
		}
	}

	public int getMaxSpeed() {
		return MaxSpeed;
	}

	public void setMaxSpeed(int MaxSpeed) {
		this.MaxSpeed = MaxSpeed;
	}
	public boolean isThrottling(int tryReserveAmount){
		boolean isEventThrottling = false;
		lock.lock();
		try { 
			Integer current = internalCounter.get();
			if(current+tryReserveAmount>=MaxSpeed+MaxQueueSize)
			{	
				isEventThrottling = true;
			}
			else{
				internalCounter.set(current+tryReserveAmount);
			}
			
		}
		finally {
		  lock.unlock(); 
		}
		
		return isEventThrottling;
		
	}
	public Integer getMaxQueueSize() {
		return MaxQueueSize;
	}

	public void setMaxQueueSize(Integer maxQueueSize) {
		MaxQueueSize = maxQueueSize;
	}


}
