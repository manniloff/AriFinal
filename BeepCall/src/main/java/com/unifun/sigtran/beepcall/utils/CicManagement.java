/**
 * 
 */
package com.unifun.sigtran.beepcall.utils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.beepcall.utils.Channel.Circuit_states;



/**
 * @author rbabin
 *
 */


public class CicManagement{
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[CicManagement"));
	private static final Object synchCic = new Object();
	protected ConcurrentHashMap<Integer, Channel> channelByCic = new ConcurrentHashMap<Integer, Channel>();

	public CicManagement() {
		logger.info("Isup cic management started");
		channelByCic.clear();
	}
	
	public Channel getChannelByCic(int cic) throws Exception {
		if (channelByCic.containsKey(cic))
			return channelByCic.get(cic);
		else{
			//Allocate channel to CIC
//			Channel ch = new Channel(cic);
//			channelByCic.putIfAbsent(cic, ch);
//			return ch;
			throw new Exception(String.format("No allocated channel with cic: %d", cic));			
		}
	}
	
	public void resetAllChannels(){
		//TODO
	}
	
	public boolean setIdle(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_IDLE);
				return true;
			} catch (Exception e) {
				return false;	// if a channel is unequipped - does not exist 
			}
		}
	}
	
	public boolean setGotIam(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_GOT_IAM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentIam(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_SENT_IAM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setGotAcm(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_GOT_ACM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentAcm(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_SENT_ACM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setGotRel(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_GOT_REL);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentRel(int cic) {
		synchronized (synchCic) {
			try {
				channelByCic.get(cic).setState(Circuit_states.ST_SENT_REL);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	public void remove(int cic){
		channelByCic.remove(cic);
	}
	public int i=1;
	public String testMsg(){
		return "Test msg "+i++;
	}
	
	public int getIdleCic() throws Exception{
		Optional<Integer> cic = this.channelByCic.keySet().stream()
				.filter(value -> this.channelByCic.get(value).getState() == Circuit_states.ST_IDLE)
				.findFirst();
		try{
			return cic.get();
		}catch (Exception e ){
			throw new Exception("Unable to find IDLE Channels");
		}	
	}
	public void addChannel(int cic, int dpc){
		Channel ch = new Channel(cic, dpc);
		this.channelByCic.putIfAbsent(cic, ch);
	}
	
}
