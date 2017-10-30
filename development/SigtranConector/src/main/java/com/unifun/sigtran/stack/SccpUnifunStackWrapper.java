package com.unifun.sigtran.stack;


import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.protocols.ss7.mtp.Mtp3PausePrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3ResumePrimitive;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SccpUnifunStackWrapper  extends SccpStackImpl{
	private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SccpUnifunStackWrapper"));	
	private ConcurrentHashMap<Long, String> mtpstatus = new ConcurrentHashMap<Long, String>();
	
	public SccpUnifunStackWrapper(String name) {
		super(name);		
	}

	@Override
	public void onMtp3PauseMessage(Mtp3PausePrimitive msg) {		
		super.onMtp3PauseMessage(msg);
		logger.info(String.format("Updating the status of affected PC: %d in cache", msg.getAffectedDpc()));
		updateMtpstatus(msg.getAffectedDpc(), "Pause");		
	}

	@Override
	public void onMtp3ResumeMessage(Mtp3ResumePrimitive msg) {
		super.onMtp3ResumeMessage(msg);
		logger.info(String.format("Updating the status of affected PC: %d in cache", msg.getAffectedDpc()));
		updateMtpstatus(msg.getAffectedDpc(), "Resume");
	}
	public void updateMtpstatus(int affectedDPC, String type) {
		if (this.mtpstatus.contains(affectedDPC)){
			this.mtpstatus.remove(affectedDPC);
		}
		this.mtpstatus.put((long)affectedDPC, type);
	}

	public ConcurrentHashMap<Long, String> getMtpstatus() {
		return mtpstatus;
	}
	
	public void clearMtpstatus() {
		 this.mtpstatus.clear();
	}
}
