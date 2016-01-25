package com.unifun.sigtran.stack;

import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.util.oam.TCAPShellExecutor;

public class Tcap {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[TCAP"));		
	
	private TCAPStack tcapStack;	
	private SccpStack sccpStack;
	private TCAPShellExecutor tcapShellExecutor;
	
	public Tcap(SccpStack sccpStack){		
		this.sccpStack = sccpStack;
	}

	public void initTCAP() throws Exception {
		tcapShellExecutor = new TCAPShellExecutor(this);
	}
	 
	public void stopTcap(){
		this.tcapStack.stop();
	}
	
	public TCAPStack getTcapStack() {
		return tcapStack;
	}
	public TCAPShellExecutor getTcapShellExecutor() {
		return tcapShellExecutor;
	}
	public SccpStack getSccpStack() {
		return sccpStack;
	}
	public void setTcapStack(TCAPStack tcapStack) {
		this.tcapStack = tcapStack;
	}

}
