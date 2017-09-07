/**
 * 
 */
package com.unifun.sigtran.stack;

import org.mobicents.protocols.ss7.cap.CAPProviderImpl;
import org.mobicents.protocols.ss7.cap.CAPStackImpl;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.CAPStack;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rbabin
 *
 */
public class CapLayer {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[CAP"));
	private CAPStack capStack;
	private CAPProvider capProvider;
	private TCAPProvider tcapProvider;
	
	public void setUp(){
		capStack = new CAPStackImpl("unifun-cap", tcapProvider);
		capProvider = capStack.getCAPProvider();		
	}

}
