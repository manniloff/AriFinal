/**
 * 
 */
package com.unifun.sigtran.smsgate.smpp.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.PDUStringException;
import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.BindType;
import org.jsmpp.session.BindRequest;
import org.jsmpp.session.SMPPServerSession;

import com.unifun.sigtran.smsgate.smpp.ServerController;

public class SmppWaitBindTask implements Runnable {
	
	private int timeout;
	private int serverId;
	private String serverName;
	private final SMPPServerSession serverSession;
	private static final Logger logger = LogManager.getLogger(SmppWaitBindTask.class);
	
    public SmppWaitBindTask(int serverId, String serverName, SMPPServerSession serverSession,  int timeout) {
    	this.timeout = timeout;
        this.serverId = serverId;
        this.serverName = serverName;
        this.serverSession = serverSession;
    }
	
	@Override
	public void run() {
        try {
            BindRequest bindRequest = serverSession.waitForBind(timeout);
            logger.info("started conncetion for to " + serverName + " from ip - " + serverSession.getInetAddress().getHostAddress() 
            		+ "; sysId - " + bindRequest.getSystemId() + "; sysType - " + bindRequest.getSystemType()
            				+ "; NP - " + bindRequest.getAddrNpi() + "; TON - " + bindRequest.getAddrTon()
            				+ "; iVersion - " +  bindRequest.getInterfaceVersion().name());
            try {
            	if(!bindRequest.getBindType().equals(BindType.BIND_TRX) && !SmppServer.getServerState()) {
            		bindRequest.reject(SMPPConstant.STAT_ESME_RBINDFAIL);
            	} else {
            		if(ServerController.validateBindingRequset(serverSession.getInetAddress().getHostAddress()
                			, bindRequest.getSystemId(), bindRequest.getPassword(), String.valueOf(bindRequest.getAddrTon().ordinal())
                			, String.valueOf(bindRequest.getAddrNpi().ordinal()), serverSession, serverId, serverName)) {
                		logger.info( String.format("Accepting bind for session {%s}, addressrange %s systemid: %s, password ****",serverSession.getSessionId(),
                        		bindRequest.getAddressRange(), bindRequest.getSystemId()));
                        bindRequest.accept(bindRequest.getSystemId(), bindRequest.getInterfaceVersion());
                	} else
                		bindRequest.reject(SMPPConstant.STAT_ESME_RBINDFAIL);	
            	}
            } catch (PDUStringException e) {
                logger.error("Failed to create session: " + Arrays.toString(e.getStackTrace()));
                bindRequest.reject(SMPPConstant.STAT_ESME_RBINDFAIL);
            }
        } catch (IllegalStateException e) {
        	 logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
        } catch (TimeoutException e) {
            logger.warn("Wait for bind has reach timeout. " + Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            logger.error("Failed accepting bind request for session: "+serverSession.getSessionId() 
            				+ " " + Arrays.toString(e.getStackTrace()));
        } catch (Exception e) {
        	logger.error("handled an exception for session: " + serverSession.getSessionId()
        					+ " " + Arrays.toString(e.getStackTrace()));
        }
	}

//	public ConcurrentHashMap<Integer, SMPPServerSession> getSmppBindSessions() {
//		return smppBindSessions;
//	}
//
//	public void setSmppBindSessions(ConcurrentHashMap<Integer, SMPPServerSession> smppBindSessions) {
//		this.smppBindSessions = smppBindSessions;
//	}
}
