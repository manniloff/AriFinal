/**
 * 
 */
package com.unifun.sigtran.checksubscriber.servlets;

import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rbabin
 *
 */
public class MapMessagesCache {
	
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[MapMessagesCache"));
	private static MapMessagesCache instance;	
	private ConcurrentHashMap<Long, MAPMessage> mapMessage;
	private ConcurrentHashMap<Long, MAPErrorMessage> mapErrorMessage;
	
	public synchronized static MapMessagesCache getInstance() {
		if (instance == null){
			instance = new MapMessagesCache();
		}
		return instance;
	}
	/**
	 * 
	 */
	
	public MapMessagesCache() {
		this.mapMessage = new ConcurrentHashMap<Long, MAPMessage>();
		this.mapErrorMessage = new ConcurrentHashMap<Long, MAPErrorMessage>();
	}
	
	public MAPMessage getMapMessage(long dialogId){
		return this.mapMessage.remove(dialogId);
	}
	
	public void addMapMessage(long dialogId, MAPMessage mapMessage){
		MAPMessage resp = this.mapMessage.putIfAbsent(dialogId, mapMessage);
		logger.debug("Write to cache dialogid: "+dialogId);
		if (resp != null){
			logger.error(String.format("MapMessage with dialog id=%d allready exist.",dialogId));
		}
	}

	public MAPErrorMessage getMapErrorMessage(long dialogId) {
		return this.mapErrorMessage.remove(dialogId);
	}
	
	public void addErrorMapMessage(long dialogId, MAPErrorMessage mapErrorMessage){
		MAPErrorMessage resp = this.mapErrorMessage.putIfAbsent(dialogId, mapErrorMessage);
		logger.debug("Write to cache dialogid: "+dialogId);
		if (resp != null){
			logger.error(String.format("MapMessage with dialog id=%d allready exist.",dialogId));
		}
	}
	
}
