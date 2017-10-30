/**
 * 
 */
package com.unifun.sigtran.util.oam;

import org.mobicents.protocols.ss7.isup.impl.CircuitManagerImpl;

import com.unifun.sigtran.stack.Isup;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class ISUPShellExecutor {
	private Isup isupInstance;
	private CircuitManagerImpl circuitManager;
	
	/**
	 * 
	 */
	public ISUPShellExecutor(Isup isupInstance) {
		this.isupInstance = isupInstance;
	}

	/**
	 * isup create <localSpc> <ni>
	 * 
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	private String setupIsupStack(String[] args) throws Exception{
		if (args.length < 4){
			return "Invalid Command";
		}
		int localSpc = Integer.parseInt(args[2]);
		int ni = Integer.parseInt(args[3]);
		isupInstance.setUp(localSpc, ni);
		return "Isup Stack succesfully initiated";
		
	}
	
	/**
	 * isup addcircuit <cic> <dpc>
	 * @param args
	 * @return
	 */
	private String addCircuit(String[] args){
		if (args.length < 4){
			return "Invalid Command";
		}
		int cic = Integer.parseInt(args[2]);
		int dpc = Integer.parseInt(args[3]);
		circuitManager.addCircuit(cic, dpc);
		return String.format("cic: %d, dpc: %d added to Circuit Manager", cic, dpc);
		
	}
	
	/**
	 * isup addgroupofcircuit <start cic nb. - end cic nb.> <dpc>
	 * @param args
	 * @return
	 */
	private String addGroupOfCircuit(String[] args){
		if (args.length < 4){
			return "Invalid Command";
		}
		String[] cics = args[2].split("-");
		if (cics.length<2){
			return "Please specifiy start cic and end cic separated by -";
		}
		int startCic = Integer.parseInt(cics[0]);
		int endCic = Integer.parseInt(cics[1]);
		int dpc = Integer.parseInt(args[3]);
		for (int i=startCic; i<=endCic; i++){
			circuitManager.addCircuit(i, dpc);
		}		
		return String.format("cics from %d to %d dpc: %d was added to Circuit Manager", startCic, endCic, dpc);
		
	}
	
	/**
	 * isup rmcircuit <cic> <dpc>
	 * @param args
	 * @return
	 */
	private String rmCircuit(String[] args){
		if (args.length < 4){
			return "Invalid Command";
		}
		int cic = Integer.parseInt(args[2]);
		int dpc = Integer.parseInt(args[3]);
		circuitManager.removeCircuit(cic, dpc);
		return String.format("cic: %d, dpc: %d removed from Circuit Manager", cic, dpc);
		
	}
	
	/**
	 * isup start
	 * @param args
	 * @return
	 */
	private String startIsupStack(String[] args){
		if (args.length < 2){
			return "Invalid Command";
		}
		isupInstance.getStack().setCircuitManager(circuitManager);
		isupInstance.getStack().start();
		return "Isup stack succesfully started";
	}
	
	/**
	 * isup stop
	 * @param args
	 * @return
	 */
	private String stopIsupStack(String[] args){
		if (args.length < 2){
			return "Invalid Command";
		}		
		isupInstance.getStack().stop();
		return "Isup stack succesfully stoped";
	}
	
	public String execute(String[] args) {
        if (args[0].equalsIgnoreCase("isup")) {
            return this.executeISUP(args);
        }
        return "Invalid Command";
    }

	/**
	 * @param args
	 * @return
	 */
	private String executeISUP(String[] args) {
		if (args.length < 2 || args.length > 4) {
            return "Invalid Command";
        }

        if (args[1] == null) {
            return "Invalid Command";
        }
        //isup create <localSpc> <ni>
        if(args[1].equalsIgnoreCase("create")){
        	try {
        		return setupIsupStack(args);
			} catch (Exception e) {
				return "Unable to initiate ISUP stack";
			}
        }
        //isup addcircuit <cic> <dpc>
        if(args[1].equalsIgnoreCase("addcircuit")){
        	return addCircuit(args);
        }
        //isup addgroupofcircuit <start cic nb. - end cic nb.> <dpc>
        if(args[1].equalsIgnoreCase("addgroupofcircuit")){
        	return addGroupOfCircuit(args);
        }
        //isup rmcircuit <cic> <dpc>
        if(args[1].equalsIgnoreCase("rmcircuit")){
        	return rmCircuit(args);
        }
        //isup start
        if(args[1].equalsIgnoreCase("start")){
        	return startIsupStack(args);
        }
        //isup stop
        if(args[1].equalsIgnoreCase("stop")){
        	return startIsupStack(args);
        }
        return "Invalid Command";
	}

	public void setCircuitManager(CircuitManagerImpl cm) {
		this.circuitManager = cm;
	}

}
