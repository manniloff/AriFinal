package com.unifun.sigtran.smsgate.util;

public class SpeedLimiter {
	
	private double MaxSpeed;

	private long Correlation;
	
	public SpeedLimiter(double maxSpeed)
	{
		if(maxSpeed<0.001){
			maxSpeed = 1;
		}
		Correlation = 1;
		MaxSpeed = maxSpeed;
	}

	public double getMaxSpeed() {
		return MaxSpeed;
	}

	public void setMaxSpeed(double maxSpeed) {
		MaxSpeed = maxSpeed;
	}
	
	public void LimitSpeed(long millsecondsSpendForOperation) throws InterruptedException{
		
		
		if(1000/MaxSpeed<1){
			return;
		}
		long sleepForSpeedLimit = (long)(1000/MaxSpeed) - ((millsecondsSpendForOperation)/1000000)- Correlation;
		
		if(sleepForSpeedLimit<1){
			return;
		}
		Thread.sleep(sleepForSpeedLimit);
	
		
	}
	public long getCorrelation() {
		return Correlation;
	}
	public void setCorrelation(long correlation) {
		Correlation = correlation;
	}
	
	/*	//1 time in 60 sec 1000x0.016666~=60
	 *  SpeedLimiter limiter =  new SpeedLimiter(0.016666);
		  while(true)
		  { long StartActionPoint= System.nanoTime();
		   logger.info(new Date());
		   long StoptActionPoint= System.nanoTime();
		   
		   try {
		    limiter.LimitSpeed(action2-action);
		   } catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		   }
		  }
	 * 
	 * 
	 * */
}
