/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.util;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.ss7.m3ua.AspFactory;
import org.mobicents.protocols.ss7.m3ua.impl.oam.M3UAOAMMessages;
import org.mobicents.protocols.ss7.m3ua.impl.oam.M3UAShellExecutor;
import org.mobicents.protocols.ss7.m3ua.impl.oam.SCTPShellExecutor;
import org.mobicents.protocols.ss7.sccp.impl.oam.SccpExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.stack.Isup;
import com.unifun.sigtran.stack.M3UA;
import com.unifun.sigtran.stack.MapStack;
import com.unifun.sigtran.stack.Sccp;
import com.unifun.sigtran.stack.Sctp;
import com.unifun.sigtran.stack.Tcap;
import com.unifun.sigtran.stack.UnifunM3UAManagementImpl;
import com.unifun.sigtran.util.oam.ISUPShellExecutor;
import com.unifun.sigtran.util.oam.MapShellExecutor;
import com.unifun.sigtran.util.oam.StackPrefShellExecutor;
import com.unifun.sigtran.util.oam.TCAPShellExecutor;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 */
public class StackPreference {

    private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[StackPreference"));
    private Sctp sctp;
    private M3UA m3ua;
    private Sccp sccp;
    private Tcap tcap;
    private MapStack map;  
    //
    private Isup isup;
    
    private StackPrefShellExecutor stackPrefShellExecuter;
    private SCTPShellExecutor sctpShellExecuter;
    private M3UAShellExecutor m3uaShellExecuter;
    private SccpExecutor sccpShellExecuter;
    private ISUPShellExecutor isupShellExecuter;
    private TCAPShellExecutor tcapShellExecuter;
    private MapShellExecutor mapShellExecuter;
    
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
    	this.tcap = new Tcap(this.sccp.getSccpStack(),configPath);
    	this.tcap.initTCAP();
    	this.map = new MapStack(this.tcap);
    	this.map.init();
        this.isup = new Isup(this.m3ua.getServerM3UAMgmt());
    	this.isup.init();
    	this.stackPrefShellExecuter = new StackPrefShellExecutor(this);
    	this.sctpShellExecuter = this.sctp.getSctpShellExecuter();
		this.m3uaShellExecuter = this.m3ua.getM3uaShellExecuter();
		this.sccpShellExecuter = this.sccp.getSccpShellExecuter();
		this.isupShellExecuter = this.isup.getIsupShellExecutor();
		this.tcapShellExecuter = this.tcap.getTcapShellExecutor();
		this.mapShellExecuter = this.map.getMapShellExecutor();
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
    								else if (args[0].compareToIgnoreCase("MAP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.mapShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("TCAP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.tcapShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("ISUP") == 0)
    								{
    									logger.info(strLine);
    									respString = this.isupShellExecuter.execute(args);
    									logger.info(respString);
    								}
    								else if (args[0].compareToIgnoreCase("stack") == 0)
    								{
    									logger.info(strLine);
    									respString = this.stackPrefShellExecuter.execute(args);
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
            if(map.getMapStack()!=null)
            	map.stop();
            TimeUnit.MILLISECONDS.sleep(1000);
            if(tcap.getTcapStack()!=null)
            	tcap.stopTcap();
            TimeUnit.MILLISECONDS.sleep(1000);
            if(sccp.getSccpStack()!=null)
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
    public MapStack getMap() {
        return map;
    }

	public Isup getIsup() {
		return isup;
	}

	public void setStackConfiguration(BufferedReader br) {
		this.br = br;
	}

	public String setForwardMode(String arg, int hbInterval) {
		logger.debug("setForwardMode");
		boolean fwMode = false;
		if("enable".equalsIgnoreCase(arg))
			fwMode=true;
		ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
		StringBuffer assocCheckResult = new StringBuffer();
		if(fwMode){
			logger.debug("Enable forward mode");
			this.m3ua.getServerM3UAMgmt().setTcapStack(this.tcap.getTcapStack());
			this.m3ua.getServerM3UAMgmt().setMapProvider(this.map.getMapProvider());
			this.m3ua.getServerM3UAMgmt().setEnableForward(true);
			//Check if forward association was set
			Map<Association, Association> forwardAssocGroup = this.m3ua.getServerM3UAMgmt().getForwardAssocGroup();
			if( forwardAssocGroup.size() < 1)
				return "Please firs set the forward association group:\n stack forwardassociation association_name forward_association_name";
			Map<String, Boolean> raspFwdMode = new HashMap<>();
						
			for (Association lAssoc : forwardAssocGroup.keySet()){
            	Association fwdAssoc = forwardAssocGroup.get(lAssoc);
            	raspFwdMode.put(fwdAssoc.getName(), true);
            	assocCheckResult.append(lAssoc.getName() + " - " +fwdAssoc.getName()+"\n" );
            	}
			//reload forward mode
			m3ua.getServerM3UAMgmt().aspFactoryReloadForwardMode();
			//Start watchdog
			Runnable watchdogTask = new Runnable() {				
				@Override
				public void run() {		
					try {
					forwardAssocGroup.forEach((localAssoc, fwdAssoc)->{
						//Check if association assigned to fw association is up
//						AspFactory asp =  m3ua.getServerM3UAMgmt().getAspForAssoc(fwdAssoc.getName());													
//						try {
//							if (!localAssoc.isUp()){
//								if (!asp.getStatus()) {
//									//logger.debug(String.format("ASP: %s already stoped", asp.getName()));
//								}else{
//									logger.info(String.format("Stop ASP: %s", asp.getName()));
//									m3ua.getServerM3UAMgmt().stopAsp(asp.getName());
//								}
//							}else{
//								if (!asp.getStatus()) {
//									logger.info(String.format("Start ASP: %s", asp.getName()));
//									m3ua.getServerM3UAMgmt().startAsp(asp.getName());
//								}
//							}
//						} catch (Exception e) {
//							logger.error("Some error oured while startin or stoping ASP for forward associations\n"+e.getMessage());
//							e.printStackTrace();
//						}

						if(fwdAssoc.isConnected()){
							//Enable forward mode
							if(!raspFwdMode.get(fwdAssoc.getName())){
								logger.info(fwdAssoc.getName()+ ": is UP, activating back forward mode");
								raspFwdMode.put(fwdAssoc.getName(), true);
								m3ua.getServerM3UAMgmt().aspFactoryReloadForwardMode(fwdAssoc, true);								
							}
						}else{
							//Disable forward mode
							if(raspFwdMode.get(fwdAssoc.getName())){								
								logger.info(fwdAssoc.getName()+ ": is Down, disabling forward mode");
								raspFwdMode.put(fwdAssoc.getName(), false);
								m3ua.getServerM3UAMgmt().aspFactoryReloadForwardMode(fwdAssoc, false);								
							}
						}
					});
					} catch (Exception e) {
						logger.error(e.getMessage());
						e.printStackTrace();
					}
				}
			};
			try{
				logger.debug("Start watchdog");
				// delay 3 seconds after checking the  status of association
				watchdog.scheduleWithFixedDelay(watchdogTask, 0, hbInterval, TimeUnit.SECONDS);
			}catch (Exception e){
				e.printStackTrace();
				logger.error(e.getMessage());
			}	
			
		}else{
			logger.debug("Disable Forward mode");
			//stop watchdog
			try{
				logger.debug("Shutdown watchdog");
				watchdog.shutdown();
			}catch(Exception e){
				logger.error(e.getMessage());
			}
			//set forward mode to false
			// reload the forward mode
			m3ua.getServerM3UAMgmt().setEnableForward(false);
			m3ua.getServerM3UAMgmt().aspFactoryReloadForwardMode();
		}
		return arg.toUpperCase()+" forward mode." + assocCheckResult.toString();		
	}

	public StackPrefShellExecutor getStackPrefShellExecuter() {
		return stackPrefShellExecuter;
	}

	public String setForwardAssociation(String localassoc, String forwardassoc) {
		Association localAssoc = this.m3ua.getServerM3UAMgmt().getAssocName(localassoc);
		if (localAssoc == null)
			return localassoc + " association was not found";
		Association forwardAssoc = this.m3ua.getServerM3UAMgmt().getAssocName(forwardassoc);
		if (forwardAssoc == null)
			return forwardassoc + " association was not found";
		this.m3ua.getServerM3UAMgmt().addForwardAssocs(localAssoc, forwardAssoc);
		return localassoc + " - " + forwardassoc + " marked as forward association";
	}
}
