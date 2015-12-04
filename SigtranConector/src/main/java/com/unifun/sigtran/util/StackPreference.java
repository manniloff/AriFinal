/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.util;

import java.io.BufferedReader;
import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.ss7.m3ua.impl.oam.M3UAShellExecutor;
import org.mobicents.protocols.ss7.m3ua.impl.oam.SCTPShellExecutor;
import org.mobicents.protocols.ss7.sccp.impl.oam.SccpExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.stack.Isup;
import com.unifun.sigtran.stack.M3UA;
import com.unifun.sigtran.stack.Map;
import com.unifun.sigtran.stack.Sccp;
import com.unifun.sigtran.stack.Sctp;
import com.unifun.sigtran.stack.Tcap;
import com.unifun.sigtran.util.oam.ISUPShellExecutor;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 */
public class StackPreference {

    private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SctpServer"));
    private Sctp sctp;
    private M3UA m3ua;
    private Sccp sccp;
    private Tcap tcap;
    private Map map;  
    //
    private Isup isup;
    
    private SCTPShellExecutor sctpShellExecuter;
    private M3UAShellExecutor m3uaShellExecuter;
    private SccpExecutor sccpShellExecuter;
    private ISUPShellExecutor isupShellExecuter;
    
    private int workerThreads;
    private String configPath;
    private BufferedReader br;
    /**
	 * 
	 */
	public StackPreference(int workerThreads, String configPath) {
		this.workerThreads = workerThreads;
		this.configPath = configPath;		
	}
	
    public void initializeStack() throws Exception{
    	this.sctp =  new Sctp(workerThreads, configPath);
    	sctp.initSCTP();
    	if (!this.sctp.getSctpManagement().isStarted())
    		throw new Exception("The Sctp Managment is not started");
    	this.m3ua = new M3UA(this.sctp.getSctpManagement(), configPath);
    	if (!m3ua.init())
    		throw new Exception("Failed to initiate M3UA");
    	this.sccp  = new Sccp(this.m3ua.getServerM3UAMgmt(), configPath);
    	if(!this.sccp.init())
    		throw new Exception("Failed to initiate SCCP");
    	//TODO Init sccp, tcap and map layer
        this.isup = new Isup(this.m3ua.getServerM3UAMgmt());
    	this.isup.init();
		this.sctpShellExecuter = this.sctp.getSctpShellExecuter();
		this.m3uaShellExecuter = this.m3ua.getM3uaShellExecuter();
		this.sccpShellExecuter = this.sccp.getSccpShellExecuter();
		this.isupShellExecuter = this.isup.getIsupShellExecutor();
		if(br!=null)
			readConfig(br);
    	
    }
    
    public void readConfig(BufferedReader br) throws Exception{
    	try{
    		String strLine;
    		while ((strLine = br.readLine()) != null)
    		{
    			String respString = null;
    			String[] args = strLine.split(" ");
    			if (args.length > 0)
    			{
    				args[0] = args[0].trim();
    				if (args[0].compareTo("") != 0) {
    					if (args[0].compareTo("#") != 0) {
    						if (args[0].compareTo(" ") != 0) {
    							if (!args[0].trim().startsWith("#")) {
    								if (args[0].compareToIgnoreCase("SCTP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.sctpShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("M3UA") == 0)
    								{
    									logger.info(strLine);
    									respString = this.m3uaShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("SCCP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.sccpShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("ISUP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.isupShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else
    								{
    									throw new Exception("invalid command string found :" + strLine);
    								}
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    	catch (Exception e)
    	{
    		logger.error(e.getStackTrace().toString());
    		throw e;
    	}
    }


    public void stop() {
        try {
            logger.debug("[StackPreference] Stopping...");
            TimeUnit.MILLISECONDS.sleep(1000);
            map.stop();
            TimeUnit.MILLISECONDS.sleep(1000);
            sccp.stop();
            TimeUnit.MILLISECONDS.sleep(1000);
            if (isup.getStack()!=null)
            	isup.getStack().stop();
            m3ua.stop();
            TimeUnit.MILLISECONDS.sleep(1000);
            sctp.stop();
            logger.debug("[StackPreference] Stopped");            
        } catch (InterruptedException ex) {
            logger.error("[StackPreference]" + ex.toString());
        } catch (Exception ex) {
            logger.error("[StackPreference] " + ex.toString());
        }
    }

    
    /**
     * @return the sctp
     */
    public Sctp getSctp() {
        return sctp;
    }

    /**
     * @return the m3ua
     */
    public M3UA getM3ua() {
        return m3ua;
    }

    /**
     * @return the sccp
     */
    public Sccp getSccp() {
        return sccp;
    }

	public Tcap getTcap() {
		return tcap;
	}

    /**
     * @return the map
     */
    public Map getMap() {
        return map;
    }

	public Isup getIsup() {
		return isup;
	}

	public void setStackConfiguration(BufferedReader br) {
		this.br = br;
	}
}
