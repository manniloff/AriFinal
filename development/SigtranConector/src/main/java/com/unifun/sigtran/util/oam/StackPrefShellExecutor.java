/**
 * 
 */
package com.unifun.sigtran.util.oam;

import com.unifun.sigtran.util.StackPreference;

/**
 * @author rbabin
 *
 */
public class StackPrefShellExecutor {
	private StackPreference stackPref;

	public StackPrefShellExecutor(StackPreference stackPref) {
		this.stackPref=stackPref;
	}
/*
 * Available commands
 * stack forwardmode <enable | disable> <hb_interval defaul 30 sec>
 * stack reloadforwardmode
 * stack forwardassociation <association name> <forward association name>
 * 
 */
	//stack forwardmode <enable | disable> <hb_interval defaul 30 sec>
	private String forwardMode(String[] args){
		if (args.length < 3){
			return "Invalid Command";
		}
		int hbInterval =30;
		if (args.length >3)
			if (args[3] != null){
				try{
					hbInterval = Integer.parseInt(args[3],hbInterval);
				}catch(Exception e){
					hbInterval =30;
				}
			}
		return this.stackPref.setForwardMode(args[2],hbInterval);
	}
	
	//stack reloadforwardmode
	private String reloadForwardMode(String[] args){
		stackPref.getM3ua().reloadASPsForwardMode();
		return "Reload settings for forward mode";
	}
	
	//stack forwardassociation <association name> <forward association name>
	private String setForwardAssociation(String[] args){
		if (args.length < 4){
			return "Invalid Command";
		}	
		return stackPref.setForwardAssociation(args[2], args[3]);
		//return args[2].toUpperCase() + "marked as forward association";
	}
	
	public String execute(String[] args) {
        if (args[0].equalsIgnoreCase("stack")) {
            return this.executeStackPref(args);
        }
        return "Invalid Command";
    }
	/**
	 * @param args
	 * @return
	 */
	private String executeStackPref(String[] args) {
		if (args.length < 2) {
            return "Invalid Command";
        }
		if (args[1] == null) {
            return "Invalid Command";
        }
		if(args[1].equalsIgnoreCase("forwardmode")){
        	return forwardMode(args);
        }
		if(args[1].equalsIgnoreCase("reloadforwardmode")){
        	return reloadForwardMode(args);
        }
		if(args[1].equalsIgnoreCase("forwardassociation")){
        	return setForwardAssociation(args);
        }
		return "Invalid Command";
	}
	
}
