/**
 * 
 */
package com.unifun.sigtran.beepcall.utils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.beepcall.utils.Channel.CircuitStates;




/**
 * @author rbabin
 *
 */


public class CicManagement{
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[CicManagement"));
	private static final Object synchCic = new Object();
	protected ConcurrentHashMap<Long, Channel> channels = new ConcurrentHashMap<Long, Channel>();

	public ConcurrentHashMap<Long, Channel> getChannels() {
		return channels;
	}

	public CicManagement() {
		logger.info("Isup cic management started");
		channels.clear();
	}
	
	public Channel getChannelById(long channelId) throws Exception {
		if (channels.containsKey(channelId))
			return channels.get(channelId);
		else{
			//Allocate channel to CIC
//			Channel ch = new Channel(cic);
//			channels.putIfAbsent(cic, ch);
//			return ch;
			throw new Exception(String.format("No allocated channel: %d", channelId));			
		}
	}
	
	public void resetAllChannels(){
		//TODO
	}
	
	public boolean setIdle(long channelId) {
		synchronized (synchCic) {
			try {
				Channel cn = channels.get(channelId);
				cn.setState(CircuitStates.ST_IDLE);
				cn.setSessionId(0);
				cn.setCauseIndicator(-1);
				cn.setCalledParty("");
				cn.setCallingParty("");
				cn.setStatrtDate(null);
				cn.setEndDate(null);
				
				return true;
			} catch (Exception e) {
				return false;	// if a channel is unequipped - does not exist 
			}
		}
	}
	
	public boolean setGotIam(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_GOT_IAM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentIam(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_SENT_IAM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	public boolean setBussy(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_BUSY);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	public boolean setGotAcm(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_GOT_ACM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentAcm(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_SENT_ACM);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setGotCpg(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_GOT_CPG);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setGotRel(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_GOT_REL);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentRel(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_SENT_REL);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentRsc(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_SENT_RSC);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setSentGrs(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_SENT_GRS);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	public boolean setGotRsc(long channelId) {
		synchronized (synchCic) {
			try {
				channels.get(channelId).setState(CircuitStates.ST_GOT_RSC);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	public void remove(long channelId){
		channels.remove(channelId);
	}
	public int i=1;
	public String testMsg(){
		return "Test msg "+i++;
	}
	
	public long getIdleChannel() throws Exception{
		Optional<Long> channelId = this.channels.keySet().stream()
				.filter(value -> this.channels.get(value).getState() == CircuitStates.ST_IDLE)
				.findFirst();
		try{
			return channelId.get();
		}catch (Exception e ){
			throw new Exception("Unable to find IDLE Channels");
		}	
	}
	
	public void addChannel(int cic, int dpc, long circuitId){
		Channel ch = new Channel(cic, dpc, circuitId);
		this.channels.putIfAbsent(circuitId, ch);
	}
	
}
