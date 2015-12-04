package com.unifun.sigtran.stack;

import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tcap {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[TCAP"));		
	
	private TCAPStack tcapStack;	
	private SccpStackImpl sccpStack;	
	
	public Tcap(SccpStackImpl sccpStack){		
		this.sccpStack = sccpStack;
	}
	//TODO create a shell class that will intiatie tcap stack
	public void initTCAP() throws Exception {
//		logger.debug("Initializing TCAP Stack ....");
//		
//		this.tcapStack = new TCAPUnifunStackWrapperImpl(this.sccpStack.getSccpProvider(), 0);
//		TcapPreference pref = cfg.getTcap();
//		if (pref.getInvoketimeout() != -1)
//			this.tcapStack.setInvokeTimeout(pref.getInvoketimeout());
//		if(pref.getMaxdialogs()!= -1 )
//			this.tcapStack.setMaxDialogs(pref.getMaxdialogs());
//		if(pref.getTimeout()!= -1)
//			this.tcapStack.setDialogIdleTimeout(pref.getTimeout());		
//		this.tcapStack.start();
//		logger.debug("Initialized TCAP Stack ....");
	}
	public void stopTcap(){
		this.tcapStack.stop();
	}
	
	public TCAPStack getTcapStack() {
		return tcapStack;
	}

}
