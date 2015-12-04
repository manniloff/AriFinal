/**
 * 
 */
package com.unifun.sigtran.stack;

import org.mobicents.protocols.ss7.isup.ISUPProvider;
import org.mobicents.protocols.ss7.isup.ISUPStack;
import org.mobicents.protocols.ss7.isup.impl.CircuitManagerImpl;
import org.mobicents.protocols.ss7.isup.impl.ISUPStackImpl;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.mtp.Linkset;
import org.mobicents.protocols.ss7.scheduler.Clock;
import org.mobicents.protocols.ss7.scheduler.DefaultClock;
import org.mobicents.protocols.ss7.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.util.oam.ISUPShellExecutor;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class Isup {
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[ISUP"));
	protected ISUPStack stack;
    protected ISUPProvider provider;
    protected Linkset isupLinkSet;
    protected Clock clock;
    protected Scheduler scheduler;    
    private M3UAManagementImpl clientM3UAMgmt;
    private ISUPShellExecutor isupShellExecutor;
    
    
    public void init(){
    	this.isupShellExecutor = new ISUPShellExecutor(this);    	
    }
    
    /**
	 * 
	 */
	public Isup(M3UAManagementImpl clientM3UAMgmt) {
		this.clientM3UAMgmt = clientM3UAMgmt;
	}
    
    public void setUp(int localSpc, int ni) throws Exception{
    	try {
    		logger.debug("Initializing ISUP Stack ....");
            clock = new DefaultClock();
            scheduler = new Scheduler();
            scheduler.setClock(clock);
            scheduler.start();
            
            this.stack = new ISUPStackImpl(scheduler, localSpc, ni);            
            this.stack.setMtp3UserPart(clientM3UAMgmt);
            this.provider = this.stack.getIsupProvider();
            CircuitManagerImpl cm = new CircuitManagerImpl();
            this.isupShellExecutor.setCircuitManager(cm);
            
		} catch (Exception e) {
			// TODO: handle exception
		}
    }

	public ISUPStack getStack() {
		return stack;
	}

	public ISUPProvider getProvider() {
		return provider;
	}

	public ISUPShellExecutor getIsupShellExecutor() {
		return isupShellExecutor;
	}

}
