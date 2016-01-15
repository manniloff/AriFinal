/**
 * 
 */
package com.unifun.sigtran.stack;

import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.impl.oam.M3UAShellExecutor;
import org.mobicents.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class M3UA {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[M3UA"));
    private final Management sctpManagement;
    private UnifunM3UAManagementImpl serverM3UAMgmt;
    protected final ParameterFactoryImpl factory = new ParameterFactoryImpl();
    private M3UAShellExecutor m3uaShellExecuter;
    private String configPath =  null;

    public M3UA(Management sctpManagement, String configPath) {        
        this.configPath=configPath;
    	this.sctpManagement = sctpManagement;
        m3uaShellExecuter = new M3UAShellExecutor();
    }

    public boolean init() {
        return initM3UA();
    }

    /**
     * @return the serverM3UAMgmt
     */
    public UnifunM3UAManagementImpl getServerM3UAMgmt() {
        return serverM3UAMgmt;
    }

    /**
     * @param serverM3UAMgmt the serverM3UAMgmt to set
     */
    public void setServerM3UAMgmt(UnifunM3UAManagementImpl serverM3UAMgmt) {
        this.serverM3UAMgmt = serverM3UAMgmt;
    }

    private boolean initM3UA() {
        try {
            logger.debug("[M3UA] Initializing M3UA Stack ....");
            this.serverM3UAMgmt = new UnifunM3UAManagementImpl("unifun_m3ua");
            this.serverM3UAMgmt.setPersistDir(this.configPath);
            TimeUnit.SECONDS.sleep(2);
            this.serverM3UAMgmt.setTransportManagement(this.sctpManagement);
            this.serverM3UAMgmt.start();
            this.serverM3UAMgmt.removeAllResourses();
            this.m3uaShellExecuter.setM3uaManagement(this.serverM3UAMgmt);
            logger.debug("[M3UA] Initialized M3UA Stack ....");
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return false;
        }        
    }
    public void stop() throws Exception {
    	logger.debug("Stopping M3UA Stack ....");
    	this.serverM3UAMgmt.getAppServers().forEach((value) -> {
    		logger.debug("[M3UA] Stopping ASP " + value.getName());
    		 try {
				this.serverM3UAMgmt.stopAsp(value.getName());
			} catch (Exception e) {
				logger.warn(e.getMessage());
				e.printStackTrace();
			}
             logger.debug("[M3UA] Stopped ASP " + value.getName());
    	});	
        TimeUnit.SECONDS.sleep(2);
        this.serverM3UAMgmt.removeAllResourses();
        this.serverM3UAMgmt.stop();
        logger.debug("Stopped M3UA Stack ....");
    }

	public M3UAShellExecutor getM3uaShellExecuter() {
		return m3uaShellExecuter;
	}
	
	public boolean isM3UAManagementStarted(){
		return this.serverM3UAMgmt.isStarted();
	}
	
	public void setForwardMode(boolean forwardMode){
		this.serverM3UAMgmt.setEnableForward(forwardMode);
	}
	
	public void reloadASPsForwardMode(){
		this.serverM3UAMgmt.aspFactoryReloadForwardMode();
	}
}
