/**
 * 
 */
package com.unifun.sigtran.util.oam;

import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.tcap.TCAPProviderImpl;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;

import com.unifun.sigtran.stack.Tcap;

/**
 * @author rbabin
 *
 */
public class TCAPShellExecutor {

	private TCAPStack tcapStack;	
	private SccpStack sccpStack;
	private Tcap tcap;
	/**
	 * @param tcap
	 */
	public TCAPShellExecutor(Tcap tcap) {
		this.tcap = tcap;
	}
	/**
	 * tcap init <ssn>
	 * @param args
	 * @return
	 */
	private String setupStack(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		this.sccpStack =  this.tcap.getSccpStack();
		if(this.sccpStack == null)
			return "Unable to obtain sccpStack";
		int ssn=0;
		try{
			ssn = Integer.parseInt(args[2]);
		}catch(Exception e){
			return "Please provide proper ssn";
		}
		this.tcapStack = new TCAPStackImpl("unifun-tcap", this.sccpStack.getSccpProvider(), ssn);
		tcap.setTcapStack(this.tcapStack);
		return "Tcap Stack was successfully initiated";
	}
	/**
	 * tcap register <ssn>
	 * @param args
	 * @return
	 */
	private String registerSsn(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		this.sccpStack =  this.tcap.getSccpStack();
		if(this.sccpStack == null)
			return "Unable to obtain sccpStack";
		int ssn=0;
		try{
			ssn = Integer.parseInt(args[2]);
		}catch(Exception e){
			return "Please provide proper ssn";
		}
		try{
			this.sccpStack.getSccpProvider().registerSccpListener(ssn, (TCAPProviderImpl)this.tcap.getTcapStack().getProvider());
		}catch(Exception e){
			e.printStackTrace();
			return "Some error ocure while registing ssn";
		}
		return "SSN "+ssn+"register to stack";
	}
	
	

	//
	/**
	 * tcap timeout <value in milliseconds>
	 * @param args
	 * @return
	 */
	private String setTimeOut(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		int timeout=0;
		try{
			timeout = Integer.parseInt(args[2]);
		}catch(Exception e){
			return "Please provide proper timeout";
		}
		try {
			this.tcapStack.setInvokeTimeout(timeout);
		} catch (Exception e) {			
			e.printStackTrace();
			return e.getMessage();
		}
		return String.format("Tcap Invoke timeout seted to %d", timeout);
	}
	
	//
	/**
	 * tcap maxdialogs <value>
	 * @param args
	 * @return
	 */
	private String setMaxDialogs(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		int maxdialogs=0;
		try{
			maxdialogs = Integer.parseInt(args[2]);
		}catch(Exception e){
			return "Please provide proper maxdialogs";
		}
		try {
			this.tcapStack.setMaxDialogs(maxdialogs);
		} catch (Exception e) {			
			e.printStackTrace();
			return e.getMessage();
		}
		return String.format("Tcap MaxDialogs seted to %d", maxdialogs);
	}
	//
	/**
	 * tcap dialogidletimeout <value in milliseconds>
	 * @param args
	 * @return
	 */
	private String setDialogIdleTimeout(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		int dialogidletimeout=0;
		try{
			dialogidletimeout = Integer.parseInt(args[2]);
		}catch(Exception e){
			return "Please provide proper dialogidletimeout";
		}
		try {
			this.tcapStack.setDialogIdleTimeout(dialogidletimeout);
		} catch (Exception e) {		
			e.printStackTrace();
			return e.getMessage();
		}
		return String.format("Tcap DialogIdleTimeout seted to %d", dialogidletimeout);
	}
	
	/**
	 * tcap start
	 * @param args
	 * @return
	 */
	private String startTcap(String[] args){
		if (args.length < 2){
			return "Invalid Command";
		}		
		try {
			this.tcapStack.start();
		} catch (Exception e) {
			return String.format("Failed to start Tcap Stack: %s", e.getMessage());
		}		
		return String.format("Tcap Stack started");
	}
	
	/**
	 * tcap stop
	 * @param args
	 * @return
	 */
	private String stopTcap(String[] args){
		if (args.length < 2){
			return "Invalid Command";
		}		
		try {
			this.tcapStack.stop();
		} catch (Exception e) {
			return String.format("Failed to stop Tcap Stack: %s", e.getMessage());
		}
		//tcap.setTcapStack(this.tcapStack);
		return String.format("Tcap Stack stoped");
	}
	
	public String execute(String[] args) {
        if (args[0].equalsIgnoreCase("tcap")) {
            return this.executeTcap(args);
        }
        return "Invalid Command";
    }

	/**
	 * @param args
	 * @return
	 */
	private String executeTcap(String[] args) {
		if (args.length < 2 ) {
            return "Invalid Command";
        }

        if (args[1] == null) {
            return "Invalid Command";
        }        
        if(args[1].equalsIgnoreCase("init")){
        	try {
        		return setupStack(args);
			} catch (Exception e) {
				return "Unable to initiate TCAP stack";
			}
        }        
        if(args[1].equalsIgnoreCase("timeout")){
        	return setTimeOut(args);
        }       
        if(args[1].equalsIgnoreCase("maxdialogs")){
        	return setMaxDialogs(args);
        }       
        if(args[1].equalsIgnoreCase("dialogidletimeout")){
        	return setDialogIdleTimeout(args);
        } 
        if(args[1].equalsIgnoreCase("start")){
        	return startTcap(args);
        }
        if(args[1].equalsIgnoreCase("stop")){
        	return stopTcap(args);
        }
        if(args[1].equalsIgnoreCase("register")){
        	return registerSsn(args);
        }
        return "Invalid Command";
	}

}
