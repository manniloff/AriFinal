/**
 * 
 */
package com.unifun.sigtran.ussdgate;

import java.sql.Timestamp;

/**
 * @author rbabin
 *
 */
public class UssMessage {
	//dialog_id, ussd_text, msisdn, charset, message_type (TCAP Continue or TCAP End)
	private long invokeId;
	private long dialogId;
	private String ussdText;
	private String msisdn;
	private String charset;
	private String messageType;
	private String serviceCode;
	private int opc;
	private int dpc;
	private Timestamp outTimeStamp;
	private Timestamp inTimeStamp;
	private boolean isSmpp = false;
	private String source="app";
	private long initialDialogId=-1;
	private SsRouteRules routeRule;
	private SsRouteRules maintenanceRouteRule;
	private long intermediateInvokeId;
	
	
	public UssMessage() {
		
	}
	/**
	 * @param ussMessage
	 */
	public UssMessage(UssMessage ussMessage) {
		super();
		this.invokeId = ussMessage.getInvokeId();
		this.dialogId = ussMessage.getDialogId();
		this.ussdText = ussMessage.getUssdText();
		this.msisdn = ussMessage.getMsisdn();
		this.charset = ussMessage.getCharset();
		this.messageType = ussMessage.getMessageType();
		this.serviceCode = ussMessage.getServiceCode();
		this.opc = ussMessage.getOpc();
		this.dpc = ussMessage.getDpc();
		this.outTimeStamp = ussMessage.getOutTimeStamp();
		this.inTimeStamp = ussMessage.getInTimeStamp();
		this.isSmpp = ussMessage.isSmpp;
		this.source = ussMessage.getSource();
		this.initialDialogId = ussMessage.getInitialDialogId();
		this.routeRule = ussMessage.getRouteRule();
		this.maintenanceRouteRule = ussMessage.getMaintenanceRouteRule();
		this.intermediateInvokeId = ussMessage.getIntermediateInvokeId();
	}
	public long getInvokeId() {
		return invokeId;
	}
	public void setInvokeId(long invokeId) {
		this.invokeId = invokeId;
	}
	public int getOpc() {
		return opc;
	}
	public void setOpc(int opc) {
		this.opc = opc;
	}
	public int getDpc() {
		return dpc;
	}
	public void setDpc(int dpc) {
		this.dpc = dpc;
	}
	public boolean isSmpp() {
		return isSmpp;
	}
	public void setSmpp(boolean isSmpp) {
		this.isSmpp = isSmpp;
	}
	public long getDialogId() {
		return dialogId;
	}
	public void setDialogId(long dialogId) {
		this.dialogId = dialogId;
	}
	public String getUssdText() {
		return ussdText;
	}
	public void setUssdText(String ussdText) {
		this.ussdText = ussdText;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}	
	public String getServiceCode() {
		return serviceCode;
	}
	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}	
	public Timestamp getOutTimeStamp() {
		return outTimeStamp;
	}	
	public void setOutTimeStamp(Timestamp outTimeStamp) {
		this.outTimeStamp = outTimeStamp;
	}	
	public Timestamp getInTimeStamp() {
		return inTimeStamp;
	}	
	public void setInTimeStamp(Timestamp inTimeStamp) {
		this.inTimeStamp = inTimeStamp;
	}	
	public String getSource() {
		return source;
	}	
	public void setSource(String source) {
		this.source = source;
	}
	public long getInitialDialogId() {
		return initialDialogId;
	}
	public void setInitialDialogId(long initialDialogId) {
		this.initialDialogId = initialDialogId;
	}
	
	@Override
	public String toString() {		
		return "UssMessage: dialogId="+this.getDialogId()
				+" ussdText="+this.getUssdText()
				+" msisdn="+this.getMsisdn()
				+" charset="+this.getCharset()
				+" message_type="+this.getMessageType()
				+" service_code="+this.getServiceCode();
	}
	public SsRouteRules getRouteRule() {
		return routeRule;
	}
	public void setRouteRule(SsRouteRules routeRule) {
		this.routeRule = routeRule;
	}
	public SsRouteRules getMaintenanceRouteRule() {
		return maintenanceRouteRule;
	}
	public void setMaintenanceRouteRule(SsRouteRules maintenanceRouteRule) {
		this.maintenanceRouteRule = maintenanceRouteRule;
	}
	public long getIntermediateInvokeId() {
		return intermediateInvokeId;
	}
	public void setIntermediateInvokeId(long intermediateInvokeId) {
		this.intermediateInvokeId = intermediateInvokeId;
	}
}
