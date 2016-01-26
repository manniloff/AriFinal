/**
 * 
 */
package com.unifun.sigtran.ussdgate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessageType;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.SupplementaryMessage;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.ussdgate.db.MapMsgDbWriter;

/**
 * @author rbabin
 *
 */
public class SupplementaryMessageProcessor implements Runnable{
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SupplementaryMessageProcessor"));
	private SupplementaryMessage message;
	private Map<String, SsRouteRules> rR;
	private Map<String, SsRouteRules> prR;
	private boolean isEmap = false;
	private DataSource ds;
	private ExecutorService dbWrk;
	private boolean maintenancemode = false;
	private UssdMapLayer mapLayer;
	private String mapWriterProcName;
	private int resptimeout;
	

	public SupplementaryMessageProcessor(
			UssdMapLayer mapLayer, 
			DataSource ds, ExecutorService dbWrk, 
			Map<String, SsRouteRules> rR,
			Map<String, SsRouteRules> prR,
			SupplementaryMessage message,
			boolean isEmap,
			boolean maintenancemode
			) {
		this.mapLayer=mapLayer;
		this.ds=ds;
		this.dbWrk = dbWrk;
		this.rR = rR;
		this.prR = prR;
		this.message = message;
		this.isEmap = isEmap;
		this.maintenancemode = maintenancemode;
		this.mapWriterProcName = mapLayer.getAppSettings().get("db").get("mapMsgWrProc");
		this.resptimeout = Integer.parseInt(mapLayer.getAppSettings().get("http").get("resptimeout"));
	}

	@Override
	public void run() {	
		long start_time = System.currentTimeMillis();
		UssMessage ussMsg =null;
		if (message instanceof ProcessUnstructuredSSRequest){			
			ProcessUnstructuredSSRequest pussr = (ProcessUnstructuredSSRequest)message;
			logger.info("Processing: "+ pussr.toString());
			ussMsg = new UssMessage();
			ussMsg.setSource("app");
			ussMsg.setDialogId(pussr.getMAPDialog().getLocalDialogId());
			ussMsg.setDpc(pussr.getMAPDialog().getRemoteAddress().getSignalingPointCode());
			ussMsg.setOpc(pussr.getMAPDialog().getLocalAddress().getSignalingPointCode());
			ussMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setMessageType(pussr.getMessageType().name());
			ussMsg.setInvokeId(pussr.getInvokeId());
			//if msisdn missing we get it from destRef
			ussMsg.setMsisdn((pussr.getMSISDNAddressString()!= null)?
					pussr.getMSISDNAddressString().getAddress() : 
						pussr.getMAPDialog().getReceivedDestReference().getAddress());
			//Extracting ussdtext
			byte[] ussdbytes = pussr.getUSSDString().getEncodedString();
			if ("UCS2".equalsIgnoreCase(pussr.getDataCodingScheme().getCharacterSet().name())){
				ussMsg.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
				ussMsg.setCharset("72");
			}else{
				ussMsg.setCharset("15");
				try {
					ussMsg.setUssdText(pussr.getUSSDString().getString(Charset.forName("UTF-8")));
				} catch (MAPException e) {
					logger.error("Failed to decode ussd message: "+e.getMessage());
					e.printStackTrace();
				}
			}
			String serviceCode = null;
			if (ussMsg.getUssdText().startsWith("*")){
				serviceCode = ussMsg.getUssdText().substring(ussMsg.getUssdText().indexOf("*") + 1, ussMsg.getUssdText().indexOf("#"));
			}else{
				serviceCode = ussMsg.getUssdText();
			}
			ussMsg.setServiceCode(serviceCode);
			//Detect routing rule			
			ussMsg.setRouteRule(rR.get(ussMsg.getUssdText()));			
			ussMsg.setMaintenanceRouteRule(prR.get(ussMsg.getUssdText()));			
		}
		else if (message instanceof UnstructuredSSRequest){
			UnstructuredSSRequest ussr = (UnstructuredSSRequest)message;
			logger.info("Processing: "+ ussr.toString());
			ussMsg = this.mapLayer.getUssMessages().get(ussr.getMAPDialog().getLocalDialogId());
			if (ussMsg == null){
				logger.info(String.format("Failed to processig UnstructuredSSRequest: %s no incoming PUSSR for it",ussr.toString()));
				return;
			}			
			ussMsg.setOutTimeStamp(null);
			ussMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setMessageType(ussr.getMessageType().name());
			ussMsg.setIntermediateInvokeId(ussr.getInvokeId());
			//Extracting ussdtext
			byte[] ussdbytes = ussr.getUSSDString().getEncodedString();
			if ("UCS2".equalsIgnoreCase(ussr.getDataCodingScheme().getCharacterSet().name())){
				ussMsg.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
				ussMsg.setCharset("72");
			}
			else{
				ussMsg.setCharset("15");
				try {
					ussMsg.setUssdText(ussr.getUSSDString().getString(Charset.forName("UTF-8")));
				} catch (MAPException e) {
					logger.error("Failed to decode ussd message: "+e.getMessage());
					e.printStackTrace();
				}
			}
		}
		else if (message instanceof UnstructuredSSResponse){
			UnstructuredSSResponse ussr = (UnstructuredSSResponse)message;
			logger.info("Processing: "+ ussr.toString());
			ussMsg = this.mapLayer.getUssMessages().get(ussr.getMAPDialog().getLocalDialogId());
			if (ussMsg == null){
				logger.info(String.format("Failed to processig UnstructuredSSResponse: %s no incoming PUSSR or USSR for it",ussr.toString()));
				return;
			}
			ussMsg.setOutTimeStamp(null);
			ussMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setMessageType(ussr.getMessageType().name());
			ussMsg.setIntermediateInvokeId(ussr.getInvokeId());
			//Extracting ussdtext
			byte[] ussdbytes = ussr.getUSSDString().getEncodedString();
			if ("UCS2".equalsIgnoreCase(ussr.getDataCodingScheme().getCharacterSet().name())){
				ussMsg.setUssdText(new String(ussdbytes,Charset.forName("UTF-16")));
				ussMsg.setCharset("72");
			}else{
				ussMsg.setCharset("15");
				try {
					ussMsg.setUssdText(ussr.getUSSDString().getString(Charset.forName("UTF-8")));
				} catch (MAPException e) {
					logger.error("Failed to decode ussd message: "+e.getMessage());
					e.printStackTrace();
				}
			}		
		}
		else{
			logger.error("Unsuported map type");			
			return;
		}
		//Store to db incoming request
		try{
			MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
			dbWrk.execute(dbWriter);
		}catch(Exception e){
			logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
			e.printStackTrace();
		}
		//Store UssMessage to internal HashMap
		this.mapLayer.keepUssMessage(ussMsg.getDialogId(), ussMsg);
		
		//Prepare Response:
		SsRouteRules routeRule = null;
		if (maintenancemode)
			routeRule = ussMsg.getMaintenanceRouteRule();
		else
			routeRule = ussMsg.getRouteRule();
		if(routeRule==null){
			ussMsg.setCharset("15");
			ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
			ussMsg.setInTimeStamp(null);
			ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setUssdText("No Content Provider for dialed SC");
			//Store to db request
			try{
				MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
				dbWrk.execute(dbWriter);
			}catch(Exception e){
				logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
				e.printStackTrace();
			}
			sendussmessage(ussMsg);
			return;
		}
		String destination = routeRule.getDestAddress();
		String dstprotocol = routeRule.getProtocolType();	
		logger.debug(String.format("Routing dialog id: %d to %s destination %s", ussMsg.getDialogId(), dstprotocol, destination));
		//Check request per seconds
		mapLayer.incrementRequest(routeRule.getId());
		Long conps = (routeRule.getConnps() == null || "".equalsIgnoreCase(routeRule.getConnps()) ) ? conps=new Long(99999) :  Long.parseLong(routeRule.getConnps());
		if ((conps <= mapLayer.countRequests(routeRule.getId())) && 
				(MAPMessageType.processUnstructuredSSRequest_Request.name().equalsIgnoreCase(ussMsg.getMessageType()))){
			logger.error(String.format("Number of connection per seconds for rule_id: %d exceeded (%d)/%s",routeRule.getId(), 
					mapLayer.countRequests(routeRule.getId()), routeRule.getConnps()));
			//TODO Send Retry letter ussd to client
			ussMsg.setCharset("15");
			ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
			ussMsg.setInTimeStamp(null);
			ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setUssdText("Content Provider is bussy, please try again later");			
			sendussmessage(ussMsg);
			try{
				MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
				dbWrk.execute(dbWriter);
			}catch(Exception e){
				logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
				e.printStackTrace();
			}
			return;
		}
		switch(dstprotocol){
		case "HTTP":
			processHttp(ussMsg, destination);
			break;
		case "MAP":
			logger.debug(String.format("Init Map request to %s: from dialog_id=%d, ussd_text=%s, msisdn=%s, service_code=%s",
					destination,ussMsg.getDialogId(),ussMsg.getUssdText(),ussMsg.getMsisdn(),ussMsg.getServiceCode()));
			switch (message.getMessageType()){
			case processUnstructuredSSRequest_Request :
				//TODO replace short number with one from the routing rule
				String[] shortcodes = destination.split(",");
				if (shortcodes.length!=2){
					logger.error(
							String.format("wrong destination field in route rule %s", destination));
					ussMsg.setCharset("15");
					ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
					ussMsg.setInTimeStamp(null);
					ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
					ussMsg.setUssdText("Error while processing, no valid destination");					
					sendussmessage(ussMsg);				
					//write error to db
					try{
						MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
						dbWrk.execute(dbWriter);
					}catch(Exception e){
						logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
						e.printStackTrace();
					}
					return;	
				}
				String ussdtext = ussMsg.getUssdText();
				ussdtext = ussdtext.replaceFirst(shortcodes[0], shortcodes[1]);
				logger.info("Ivoke PUSSR with text: "+ussdtext+", msisdn: "+ussMsg.getMsisdn());
				long routedDialogId = this.mapLayer.initiatePUSSR(ussMsg.getMsisdn(), ussdtext, 1, ussMsg.getDialogId(), (isEmap)?"emap":null);
				if (routedDialogId == -1){
					logger.error(
							String.format("Error sending PROCESS_UNSTRUCTURED_SS_REQUEST = %s", ussMsg.toString()));
					ussMsg.setCharset("15");
					ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
					ussMsg.setInTimeStamp(null);
					ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
					ussMsg.setUssdText("Error while processing, please try again later");					
					sendussmessage(ussMsg);										
					//write error to db
					try{
						MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
						dbWrk.execute(dbWriter);
					}catch(Exception e){
						logger.error("Failed to store Supplementary Message to db: "+e.getMessage());
						e.printStackTrace();
					}
					return;
				}
				break;
			case unstructuredSSRequest_Request:
			case unstructuredSSRequest_Response:			
				logger.info("Ivoke USSR with text: "+ussMsg.getUssdText()+", dialogID: "+ussMsg.getDialogId()+", msisdn: "+ussMsg.getDialogId());
				try{
					UssMessage subUssMsg = mapLayer.findSubsession(ussMsg.getDialogId());
					if (subUssMsg == null){
						throw new Exception("No available subsession");
					}
					this.mapLayer.initiateUSSR(ussMsg.getUssdText(), Long.toString(subUssMsg.getDialogId()),					
							subUssMsg.getMsisdn(),ussMsg.getCharset() , subUssMsg.getIntermediateInvokeId(), 1, ussMsg.getDialogId(), (isEmap)?"emap":null);
				}catch(Exception e){					
					logger.error(
							String.format("Error sendint usssr = %s ", ussMsg.toString()));
					ussMsg.setCharset("15");
					ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
					ussMsg.setInTimeStamp(null);
					ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
					ussMsg.setUssdText("Error while processing unstructuredSS, please try again later");								
					//write error to db
					try{
						MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
						dbWrk.execute(dbWriter);
					}catch(Exception ex){
						logger.error("Failed to store Supplementary Message to db: "+ex.getMessage());
						e.printStackTrace();
					}
					sendussmessage(ussMsg);
					return;
				}
				break;
			default:
				logger.error("Unsuported Message type "+message.getMessageType().name());
				break;
			}
			break;
		case "SMPP":
			logger.debug(String.format("Init SMPP request"));
			logger.debug(String.format("Not yet implemented"));
			break;
		default:
			logger.debug(String.format("Unsuported protocol %s to route dialog id: %d to destination %s",dstprotocol,ussMsg.getDialogId(), destination));
			break;
		
		}
		long end_time = System.currentTimeMillis();
		logger.debug("Processed in :"+ (end_time-start_time));
	}

	/**
	 * @param ussMsg
	 */
	private void processHttp(UssMessage ussMsg, String destination) {
		UssMessage response = null;
		try {
			response = this.doHttpExchange(ussMsg, destination);
			response.setInTimeStamp(null);
			response.setOutTimeStamp(new Timestamp(new Date().getTime()));
			if ("Error".equalsIgnoreCase(response.getMessageType()))
				throw new IOException("Some error ocure in http transfer");			
			try{				MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
				
				dbWrk.execute(dbWriter);
			}catch(Exception ex){
				logger.error("Failed to store Supplementary Message to db: "+ex.getMessage());
				ex.printStackTrace();
			}
			sendussmessage(response);
		} catch (IOException | URISyntaxException e) {
			logger.warn("Some error ocuring during http transfer: " + e.getMessage());
			if("true".equalsIgnoreCase(this.mapLayer.getAppSettings().get("app").get("forwardFailure"))){
				logger.warn("Unable to get response from remote communication node.");
				logger.info("Redirect the message via reserved rule.");
				SsRouteRules rrr = ussMsg.getMaintenanceRouteRule();
				if (rrr != null){
					String resevdestination = rrr.getDestAddress();
					String reservdstprotocol = rrr.getProtocolType();
					switch(reservdstprotocol){
					case "HTTP":
						try{
							response = this.doHttpExchange(ussMsg,resevdestination);
						}catch(Exception e1){
							ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
							ussMsg.setUssdText("Some error ocure during information exchange, please try again later");
							ussMsg.setInTimeStamp(null);
							ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
							ussMsg.setCharset("15");	
							sendussmessage(ussMsg);
							try{
								MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
								dbWrk.execute(dbWriter);
							}catch(Exception ex){
								logger.error("Failed to store Supplementary Message to db: "+ex.getMessage());
								ex.printStackTrace();
							}
							return;
						}
						if("Error".equalsIgnoreCase(response.getMessageType())){
							response.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
						}
						response.setInTimeStamp(null);
						response.setOutTimeStamp(new Timestamp(new Date().getTime()));
						sendussmessage(response);
						try{
							MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
							dbWrk.execute(dbWriter);
						}catch(Exception ex){
							logger.error("Failed to store Supplementary Message to db: "+ex.getMessage());
							ex.printStackTrace();
						}
						break;
					case "MAP":									
						String[] reservedshortcodes = resevdestination.split(",");								
						if (reservedshortcodes.length!=2){
							logger.error(
									String.format("Wrong destination field in route rule: ", resevdestination));
							break;
						}
						String reservedShortCode = rrr.getUssdText().replaceFirst(reservedshortcodes[0], reservedshortcodes[1]);
						logger.info("Ivoke PUSSR with text: "+reservedShortCode+", msisdn: "+ussMsg.getMsisdn());
						long routedDialogId = this.mapLayer.initiatePUSSR(ussMsg.getMsisdn(), reservedShortCode, 1, ussMsg.getDialogId(), (isEmap)?"emap":null);
						if (routedDialogId == -1){
							logger.error(
									String.format("Error sending PROCESS_UNSTRUCTURED_SS_REQUEST_INDICATION = %s", ussMsg.toString()));
							break;
						}						
						break;
					}

					return;
				}
			}
			ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
			ussMsg.setUssdText("Some error ocure during information exchange, please try again later");
			ussMsg.setInTimeStamp(null);
			ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setCharset("15");	
			sendussmessage(ussMsg);
			try{
				MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ussMsg, mapWriterProcName);
				dbWrk.execute(dbWriter);
			}catch(Exception ex){
				logger.error("Failed to store Supplementary Message to db: "+ex.getMessage());
				e.printStackTrace();
			}
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * @param destination
	 * @param dialogId
	 * @param ussdtext
	 * @param msisdn
	 * @param serviceCode 
	 * @throws IOException 
	 * @throws URISyntaxException  
	 */
	private UssMessage doHttpExchange(UssMessage ussMsg, String destination) throws IOException, URISyntaxException {
		String dialogId,ussdtext,msisdn,serviceCode;
		try {
			dialogId = URLEncoder.encode(Long.toString(ussMsg.getDialogId()), "UTF-8");		
			ussdtext = URLEncoder.encode(ussMsg.getUssdText(), "UTF-8");
			msisdn = URLEncoder.encode(ussMsg.getMsisdn(), "UTF-8");
			serviceCode = URLEncoder.encode(ussMsg.getServiceCode(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn("Some error ocure while URL encode the parameters: "+e.getMessage());
			ussMsg.setUssdText("Some error ocure during information exchange, please try again later");
			ussMsg.setInTimeStamp(null);
			ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setCharset("15");		
			ussMsg.setMessageType("Error");
			e.printStackTrace();
			return ussMsg;
		}
		String url = String.format("%s?dialog_id=%s&ussd_text=%s&msisdn=%s&service_code=%s",destination,dialogId,ussdtext,msisdn,serviceCode);
		URL urlObj = new URL(url);
		logger.info("Call URL :" + urlObj.toURI());
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("GET");
		con.setConnectTimeout(resptimeout);
		//con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0");
		int responseCode = con.getResponseCode();		
		String respCharset = con.getHeaderField("charset");
		String respDialogId = con.getHeaderField("dialog_id");
		String respMsgTye =	con.getHeaderField("message_type");
		String respMsisdn =	con.getHeaderField("msisdn");
		String respUssdText= con.getHeaderField("ussd_text");
		String respServiceCode = con.getHeaderField("service_code");
		con.disconnect();		
		try{
			ussMsg.setCharset(URLDecoder.decode(respCharset, "UTF-8"));			
			switch(URLDecoder.decode(respMsgTye, "UTF-8")){
			case "TCAP Continue":
				ussMsg.setMessageType(MAPMessageType.unstructuredSSRequest_Request.name());
				break;
			case "TCAP End":
				ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
				break;
			case "processUnstructuredSSRequest_Response":
				ussMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
				break;
			case "unstructuredSSRequest_Request":
				ussMsg.setMessageType(MAPMessageType.unstructuredSSRequest_Request.name());
				break;
			default:
				throw new UnsupportedEncodingException("Unsuported mesage type please use (\"TCAP Continue\" or  \"TCAP End\")");
			}
			
			ussMsg.setUssdText(URLDecoder.decode(respUssdText, "UTF-8"));
			//to make sure that we receive this parameters 
			URLDecoder.decode(respDialogId, "UTF-8");
			URLDecoder.decode(respMsisdn, "UTF-8"); 
			URLDecoder.decode(respServiceCode, "UTF-8"); 
		}catch (Exception e) {
			logger.warn("Some error ocure while decoding http response parameters from headers: "+e.getMessage());
			ussMsg.setUssdText("Some error ocure during information exchange, please try again later");
			ussMsg.setInTimeStamp(null);
			ussMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
			ussMsg.setCharset("15");		
			ussMsg.setMessageType("Error");
			e.printStackTrace();
			return ussMsg;
		}
		//TODO write response to vas logs ?????
		return ussMsg;
	}

	/**
	 * @param ussMsg
	 */
	private void sendussmessage(UssMessage ussMsg) {
		logger.debug(String.format("Send Uss Message: %s ", ussMsg.toString()));
		mapLayer.handleUssProcReq(ussMsg);
		
	}
}
