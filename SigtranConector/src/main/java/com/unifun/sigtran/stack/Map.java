/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.stack;

import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class Map {
    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[MapLayer"));        
    private MAPStackImpl mapStack;
    private MAPProvider mapProvider;
    private  TCAPStack tcapStack;  


    public Map(TCAPStack tcapStack) {		
		this.tcapStack = tcapStack;        
	}
  //TODO create a shell class that will intiatie map stack
    public boolean init() {
        try {
        	logger.debug("Initializing MAP Stack ....");			
			logger.debug("Initiate Map Stack with tcap provider");
			this.mapStack = new MAPStackImpl("unifun-map", this.tcapStack.getProvider());
			this.mapProvider = this.mapStack.getMAPProvider();			        
			this.mapStack.start();
			logger.debug("Initialized MAP Stack ....");			
			return true;
        } catch (Exception ex) {
            logger.error("[initMap]: " + ex.getMessage());
        }
        return false;
    }

    public void stop() {
        logger.debug("Stopping MAP Stack ....");
        this.mapStack.stop();
        logger.debug("Stopped MAP Stack ....");
    }

    /**
     * @return the mapStack
     */
    public MAPStackImpl getMapStack() {
        return mapStack;
    }

    /**
     * @return the mapProvider
     */
    public MAPProvider getMapProvider() {
        return mapProvider;
    }
}
