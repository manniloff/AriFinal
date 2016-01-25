package com.unifun.sigtran.ussdgate;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessageType;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPDialogSupplementary;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;

import com.unifun.sigtran.ussdgate.db.MapMsgDbWriter;

public class MapMsgHandler implements Runnable{
	private static final Logger logger = LogManager.getLogger(MapMsgHandler.class);
	private ProcessUnstructuredSSResponse procUnstrResInd;
	private UnstructuredSSRequest unstrReqInd;
	private UnstructuredSSResponse unstrResInd;
	private long initialDialogId;
	private UssdMapLayer ussdMapLayer;
	private MAPProvider mapProvider;
	
	public MapMsgHandler(ProcessUnstructuredSSResponse procUnstrResInd, long initialDialogId, UssdMapLayer ussdMapLayer) {
		this.procUnstrResInd = procUnstrResInd;
		this.initialDialogId = initialDialogId;
		this.ussdMapLayer = ussdMapLayer;
		this.mapProvider = ussdMapLayer.getMapProvider();
	}
	
	public MapMsgHandler(UnstructuredSSRequest unstrReqInd, long initialDialogId, UssdMapLayer ussdMapLayer) {
		this.unstrReqInd = unstrReqInd;
		this.initialDialogId = initialDialogId;
		this.ussdMapLayer = ussdMapLayer;
		this.mapProvider = ussdMapLayer.getMapProvider();
	}
	public MapMsgHandler(UnstructuredSSResponse unstrResInd, long initialDialogId, UssdMapLayer ussdMapLayer) {
		this.unstrResInd = unstrResInd;
		this.initialDialogId=initialDialogId;
		this.ussdMapLayer = ussdMapLayer;
		this.mapProvider = ussdMapLayer.getMapProvider();
	}
	@Override
	public void run() {
		MAPDialogSupplementary dialog = (MAPDialogSupplementary)this.mapProvider.getMAPDialog(initialDialogId);
		if (dialog == null){
			logger.error("Unable to find dialog with id: "+initialDialogId);
			return;
		}
		UssMessage msglog = ussdMapLayer.getUssMessages().get(initialDialogId);
		if (msglog == null){
			logger.error("Unable to obtain dialog in ussd map layer with id: "+initialDialogId);
			return;
		}
		msglog.setInTimeStamp(null);
		msglog.setOutTimeStamp(new Timestamp(new Date().getTime()));				
		if(procUnstrResInd!=null){		
			try {
				long invokeId = msglog.getInvokeId();
				dialog.addProcessUnstructuredSSResponse(invokeId, procUnstrResInd.getDataCodingScheme(),procUnstrResInd.getUSSDString());
				dialog.close(false);				
				byte[] ussdbytes = procUnstrResInd.getUSSDString().getEncodedString();
				if ("UCS2".equalsIgnoreCase(procUnstrResInd.getDataCodingScheme().getCharacterSet().name())){
					msglog.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
					msglog.setCharset("72");
				}else{
					msglog.setCharset("15");
					try {
						msglog.setUssdText(procUnstrResInd.getUSSDString().getString(Charset.forName("UTF-8")));
					} catch (MAPException e) {
						logger.error("Failed to decode ussd message: "+e.getMessage());
						e.printStackTrace();
					}
				}				
				msglog.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());	
			} catch (MAPException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
		if(unstrReqInd!= null || unstrResInd != null){
			try {				
				if(unstrReqInd!= null){
					dialog.addUnstructuredSSRequest(unstrReqInd.getDataCodingScheme(),unstrReqInd.getUSSDString(),unstrReqInd.getAlertingPattern(),unstrReqInd.getMSISDNAddressString());
					byte[] ussdbytes = unstrReqInd.getUSSDString().getEncodedString();
					if ("UCS2".equalsIgnoreCase(unstrReqInd.getDataCodingScheme().getCharacterSet().name())){
						msglog.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
						msglog.setCharset("72");
					}else{
						msglog.setCharset("15");
						try {
							msglog.setUssdText(unstrReqInd.getUSSDString().getString(Charset.forName("UTF-8")));
						} catch (MAPException e) {
							logger.error("Failed to decode ussd message: "+e.getMessage());
							e.printStackTrace();
						}
					}				
					msglog.setMessageType(MAPMessageType.unstructuredSSRequest_Request.name());
					msglog.setIntermediateInvokeId(unstrReqInd.getInvokeId());
				}
				if(unstrResInd != null){
					dialog.addUnstructuredSSRequest(unstrResInd.getDataCodingScheme(),unstrResInd.getUSSDString(), null, null);	
					byte[] ussdbytes = unstrResInd.getUSSDString().getEncodedString();
					if ("UCS2".equalsIgnoreCase(unstrResInd.getDataCodingScheme().getCharacterSet().name())){
						msglog.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
						msglog.setCharset("72");
					}else{
						msglog.setCharset("15");
						try {
							msglog.setUssdText(unstrResInd.getUSSDString().getString(Charset.forName("UTF-8")));
						} catch (MAPException e) {
							logger.error("Failed to decode ussd message: "+e.getMessage());
							e.printStackTrace();
						}
					}				
					msglog.setMessageType(MAPMessageType.unstructuredSSRequest_Response.name());
					msglog.setIntermediateInvokeId(unstrResInd.getInvokeId());
					}
				dialog.send();
				
			} catch (MAPException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}			
		}
		try{
			MapMsgDbWriter dbWriter = new MapMsgDbWriter(ussdMapLayer.getDs(), msglog, this.ussdMapLayer.getAppSettings().get("db").get("mapMsgWrProc"));
			ussdMapLayer.getDbWorker().execute(dbWriter);
		}catch(Exception e){
			logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
			e.printStackTrace();
		}
		
		
	}
	

}
