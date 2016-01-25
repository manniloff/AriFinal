/**
 * 
 */
package com.unifun.sigtran.ussdgate;

/**
 * @author rbabin
 *
 */

public class SsRouteRules {
	private long id;	
	private String ussdText;	
	private String serviceCode;	
	private String protocolType;	
	private String destAddress;	
	private String connps;	
	private String ussdsc;	
	private int proxy_mode;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUssdText() {
		return ussdText;
	}

	public void setUssdText(String ussdText) {
		this.ussdText = ussdText;
	}

	public String getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(String protocolType) {
		this.protocolType = protocolType;
	}

	public String getDestAddress() {
		return destAddress;
	}

	public void setDestAddress(String destAddress) {
		this.destAddress = destAddress;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public String getConnps() {
		return connps;
	}

	public void setConnps(String connps) {
		this.connps = connps;
	}

	public String getUssdsc() {
		return ussdsc;
	}

	public void setUssdsc(String ussdsc) {
		this.ussdsc = ussdsc;
	}
	
	@Override
	public String toString() {		
		return "Ss Route Rules: "+this.getId()+" ussdtext: "+this.getUssdText()+" sc: "+this.getServiceCode()+" ussdc"+this.getUssdsc();
	}

	public int getProxy_mode() {
		return proxy_mode;
	}

	public void setProxy_mode(int proxy_mode) {
		this.proxy_mode = proxy_mode;
	}
}
