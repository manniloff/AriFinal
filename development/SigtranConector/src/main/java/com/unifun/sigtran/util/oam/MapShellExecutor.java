/**
 * 
 */
package com.unifun.sigtran.util.oam;

import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPProvider;

import com.unifun.sigtran.stack.MapStack;

/**
 * @author rbabin
 *
 */
public class MapShellExecutor {
	private MapStack mapInstance;
	private MAPStackImpl mapStack;
    private MAPProvider mapProvider;
	
	public MapShellExecutor(MapStack mapInstance) {
		this.mapInstance=mapInstance;
//		this.mapStack = mapInstance.getMapStack();
//		this.mapProvider = mapInstance.getMapProvider();
	}
	
	/**
	 * map start
	 * @param args
	 * @return
	 */
	private String setupMapStack(String[] args){
		this.mapInstance.setTcapStack(this.mapInstance.getTcap().getTcapStack());
		if (this.mapInstance.init_old()){
			return "Map Stack started";
		}
		return "Failed to start Map Stack";
	}
	/**
	 * map sstop
	 * @param args
	 * @return
	 */
	private String stopMapStack(String[] args){
		this.mapInstance.stop();		
		return "Map Stack Stoped";
	}
	
	public String execute(String[] args) {
        if (args[0].equalsIgnoreCase("map")) {
            return this.executeMap(args);
        }
        return "Invalid Command";
    }

	/**
	 * @param args
	 * @return
	 */
	private String executeMap(String[] args) {
		if (args[1] == null) {
			return "Invalid Command";
		}
		if (args[1].equalsIgnoreCase("start")) {
			return setupMapStack(args);
		}
		if (args[1].equalsIgnoreCase("stop")) {
			return stopMapStack(args);
		}
		return "Invalid Command";

	}
}
