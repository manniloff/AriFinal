/**
 * 
 */
package com.unifun.sigtran.stack;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.impl.oam.SccpExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class Sccp {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[Sccp"));    
    private final M3UAManagementImpl serverM3UAMgmt;
    //private SccpStackImpl sccpStack;
    private SccpUnifunStackWrapper sccpStack;
    private SccpExecutor sccpShellExecuter;
    private String configPath =  null;

    public Sccp(M3UAManagementImpl serverM3UAMgmt,String configPath) {        
        this.serverM3UAMgmt = serverM3UAMgmt;
        this.configPath=configPath;
    }

    public boolean init() {
        return initSccp();
    }

    public void stop() throws Exception {
        logger.debug("Stopping SCCP Stack ....");
        this.sccpStack.clearMtpstatus();
        this.sccpStack.removeAllResourses();
        this.sccpStack.stop();
        logger.debug("Stopped SCCP Stack ....");
    }

    private boolean initSccp() {
        try {
            logger.info("Initializing SCCP Stack ....");
            this.sccpStack = new SccpUnifunStackWrapper("unifun-sccp");
            this.sccpStack.setPersistDir(this.configPath);            
            this.sccpStack.setMtp3UserPart(1, this.serverM3UAMgmt);            
            this.sccpStack.start();
            this.sccpStack.removeAllResourses();
            this.sccpShellExecuter = new SccpExecutor();
            Map<String, SccpStackImpl> sccpStacksTemp = new HashMap<>();
            sccpStacksTemp.put("unifun", sccpStack);
            this.sccpShellExecuter.setSccpStacks(sccpStacksTemp);
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return false;
        }        
    }

    /**
     * @return the sccpStack
     */
    public SccpStack getSccpStack() {
        return sccpStack;
    }

    /**
     * @param sccpStack the sccpStack to set
     */
    public void setSccpStack(SccpUnifunStackWrapper sccpStack) {
        this.sccpStack = sccpStack;
    }

	public SccpExecutor getSccpShellExecuter() {
		return sccpShellExecuter;
	}
}
