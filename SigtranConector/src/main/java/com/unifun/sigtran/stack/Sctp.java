/**
 * 
 */
package com.unifun.sigtran.stack;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.sctp.ManagementImpl;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.mobicents.protocols.ss7.m3ua.impl.oam.SCTPShellExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class Sctp {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[Sctp"));	
	private Management sctpManagement;
	private SCTPShellExecutor sctpShellExecuter = new SCTPShellExecutor();
	private int workerThreads = 1;
	private String configPath =  null;
	private String arg= null;

	public Sctp(int workerThreads, String configPath) {
		this.workerThreads=workerThreads;
		this.configPath=configPath;		
	}

	public void initSCTP() {
		logger.debug("[SCTP] Initializing SCTP Stack....");
			try {
				//this.sctpManagement = new ManagementImpl("Unifun");
				this.sctpManagement = new NettySctpManagementImpl("Unifun");
				this.sctpManagement.setSingleThread(false);
				this.sctpManagement.setWorkerThreads(this.workerThreads);
				this.sctpManagement.setPersistDir(this.configPath);
				this.sctpManagement.start();        		
				this.sctpManagement.removeAllResourses();
				this.sctpManagement.setConnectDelay(10000);
				Map<String, Management> sctpManagementsTemp = new HashMap<>();
				sctpManagementsTemp.put("unifun", sctpManagement);
				this.sctpShellExecuter.setSctpManagements(sctpManagementsTemp);			
			} catch (InterruptedException iex) {				
				logger.error("[SCTP]: " + iex.getMessage());				
			} catch (Exception ex) {			
				logger.error("[SCTP]: " + ex.getMessage());				
			}
	}


	public boolean isIsSCTPStarted() {
		return this.sctpManagement.isStarted();
	}

	public void stop() throws Exception {
		
		logger.debug("Stop SCTP Asociation ....");
		this.sctpManagement.getAssociations().keySet().forEach((value) -> {
			logger.debug("Stop Asociation - " + value);
			try {
				if (sctpManagement.getAssociation(value).isStarted()) 
					sctpManagement.stopAssociation(value);
			} catch (Exception e) {
				logger.warn(e.getMessage());
				e.printStackTrace();
			}
		});
		logger.debug("Stop Servers ....");
		this.sctpManagement.getServers().forEach((value) -> {
			try {
				this.sctpManagement.stopServer(value.getName());
			} catch (Exception e) {
				logger.warn(e.getMessage());
				e.printStackTrace();
			}
		});
		this.sctpManagement.stop();
		logger.debug("Stopped SCTP Stack ....");
	}

	public Management getSctpManagement() {
		return sctpManagement;
	}

	public void setSctpManagement(Management sctpManagement) {
		this.sctpManagement = sctpManagement;
	}

	public SCTPShellExecutor getSctpShellExecuter() {
		return sctpShellExecuter;
	}


}
