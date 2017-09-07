package com.unifun.sigtran.checksubscriber.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.checksubscriber.MapLayer;


/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */

//SRI Request:
//curl -s "http://127.0.0.1:7080/CheckSubscriber/LbsService?action=SRI&msisdn=22222000176"
//PSI Request
//curl -s "http://127.0.0.1:7080/CheckSubscriber/LbsService?action=PSI&imsi=434041004084730&vlr=998909139999"
//ATI Request:
//curl -s "http://127.0.0.1:7080/CheckSubscriber/LbsService?action=ATI&msisdn=998901093263"
//RDS Request:
//curl -s "http://127.0.0.1:7080/CheckSubscriber/LbsService?action=RDS&msisdn=998901093263"
//Service SMS (Hidden)
//curl -s "http://127.0.0.1:7080/CheckSubscriber/LbsService?action=ServiceSMS&msisdn=998901093263"

public class CheckSubscriberWrk implements Runnable {

    static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", CheckSubscriberWrk.class.getSimpleName(), ""));
    private AsyncContext asyncContext;
    private static MapLayer lbsMap;
    private  Map<String, Map<String,String>> cfg;
    private String action, msisdn, info, imsi, vlr;
    private MapMessagesCache mapMessageCache;
    private PrintWriter out;
    private int sriResponseTimeOut;
    private int rdsResponseTimeOut;
    private int smsResponseTimeOut;
    private int psiResponseTimeOut;

    @Override
    public void run() {    	
        cfg = (Map<String, Map<String,String>>)asyncContext.getRequest().getServletContext().getAttribute("mapPreference");
    	lbsMap = (MapLayer) asyncContext.getRequest().getServletContext().getAttribute("mapLayer");
    	HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
    	try {
			out = asyncContext.getResponse().getWriter();
		} catch (IOException e1) {
			logger.warn(e1.getMessage());
			e1.printStackTrace();
		}
        if (cfg == null || lbsMap == null) {            
            response.setHeader("RESULT", "NOK");
            response.setHeader("ERROR", "1");
            out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Wrong configuration \"}"));
            out.flush();
            out.close();
            try {
				asyncContext.complete();
			} catch (IllegalStateException ex) {
				// Alresady completed.
				logger.trace("Already resumed!", ex);
			}
            return;
        } else {
        	mapMessageCache = lbsMap.getMapMessageCache();
            Enumeration<String> parameterNames = asyncContext.getRequest().getParameterNames();
            //Initiate response timeout
            try{
            	sriResponseTimeOut = Integer.parseInt(cfg.get("app").get("sritimeout"));
            }catch(Exception e){
            	sriResponseTimeOut = 3000;
            }
            try{
            	rdsResponseTimeOut = Integer.parseInt(cfg.get("app").get("rdstimeout"));
            }catch(Exception e){
            	rdsResponseTimeOut=30000;
            }
            try{
            	psiResponseTimeOut = Integer.parseInt(cfg.get("app").get("rdstimeout"));
            }catch(Exception e){
            	psiResponseTimeOut=30000;
            }
            try{
            	smsResponseTimeOut = Integer.parseInt(cfg.get("app").get("sritimeout"));
            }catch(Exception e){
            	smsResponseTimeOut=60000;
            }
            // Get input parameters from the request
            // and validate them
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                // Get parameters
                switch (paramName) {
                    case "action":
                        action = asyncContext.getRequest().getParameterValues(paramName)[0];
                        break;
                    case "msisdn":
                        msisdn = asyncContext.getRequest().getParameterValues(paramName)[0];
                        break;
                    case "info":
                        info = asyncContext.getRequest().getParameterValues(paramName)[0];
                        break;
                    case "imsi":
                        imsi = asyncContext.getRequest().getParameterValues(paramName)[0];
                        break;
                    case "vlr":
                        vlr = asyncContext.getRequest().getParameterValues(paramName)[0];
                        break;
                    default:
                        logger.warn("Unknown parameter: " + paramName);
                        break;
                }
            }
            // Execute actions
            if (action == null) {                
                response.setHeader("RESULT", "NOK");
                response.setHeader("ERROR", "EMPTY_ACTION");
                out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"EMPTY_ACTION\"}"));
            } else {
                switch (action) {
                    case "SRI":                        
					try {
						actionSRI();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage());
						e.printStackTrace();
					}
                        break;
                    case "PSI":
                    	try{
                    		actionPSI();
                    	} catch (InterruptedException e) {
                    		logger.warn(e.getMessage());
                    		e.printStackTrace();
                    	}
                        break;
                    case "SRIPSI":
                        actionSRIPSI();
                        break;
                    case "ATI":
                    	try{
                    		actionATI();
                    	} catch (InterruptedException e) {
                    		logger.warn(e.getMessage());
                    		e.printStackTrace();
                    	}
                        break;
                    case "RDS":
                    	try{
                    		actionRDS();
                    	} catch (InterruptedException e) {
                    		logger.warn(e.getMessage());
                    		e.printStackTrace();
                    	}
                    	break;
                    case "ServiceSMS":
    					try{
    						sendServiceSMS();
    					} catch (Exception e) {
    						response.setHeader("RESULT", "NOK");
    						response.setHeader("ERROR", e.getMessage());
    					}
    					break;
                    default:                        
                        response.setHeader("RESULT", "NOK");
                        response.setHeader("ERROR", "WRONG_ACTION");
                        out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"WRONG_ACTION\"}"));
                        break;
                }
            }
            // Exception here then calling complete() method java.lang.IllegalStateException: Calling [asyncComplete()] is not valid for a request with Async state [MUST_COMPLETE]
            // https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/coyote/AsyncStateMachine.html
            // MUST_COMPLETE - complete() has been called before the request in which
            //                 ServletRequest.startAsync() has finished. As soon as that
            //                 request finishes, the complete() will be processed.
//            out.flush();
//            out.close();
			try {
				asyncContext.complete();
			} catch (IllegalStateException ex) {
				// Alresady completed.
				logger.trace("Already resumed!", ex);
			}
        }
    }


	public void setAsyncContext(AsyncContext aContext) {
        asyncContext = aContext;
    }
	
	private void sendServiceSMS() throws InterruptedException {
		HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
		if (msisdn==null ||"".equalsIgnoreCase(msisdn)){
			response.setHeader("RESULT", "NOK");
			response.setHeader("ERROR", "Provide msisdn paramaeter");
		}        	
		msisdn = msisdn.replaceAll("\\D+","");		
		long dialogId = lbsMap.sendServiceSMS(msisdn);          
		if (dialogId == -1){
			response.setHeader("RESULT", "NOK");
			response.setHeader("ERROR", "Unable to create dialog");
		}else{
			int counter=0;
			TimeUnit.MILLISECONDS.sleep(10);
			Long smsDialogId = null;
			try{
				smsDialogId = lbsMap.getSrismsdialogs().get(dialogId);
			}catch (NullPointerException ex){
				smsDialogId = null;
			}           	
			while(smsDialogId == null){
				try{
					smsDialogId = lbsMap.getSrismsdialogs().get(dialogId);
				}catch (NullPointerException ex){
					smsDialogId = null;
				}        		
				TimeUnit.MILLISECONDS.sleep(10);
				counter++;
				if (counter > (sriResponseTimeOut/10))
					break;
			}
			logger.debug("Counter value: "+counter );
			if (smsDialogId == null){        		
				response.setHeader("RESULT", "NOK");
				response.setHeader("ERROR", "Unable to obtain sri response");				
			}else{
				counter = 0;
				Long deliveredStatus = null;
				MtForwardShortMessageResponse mtForwSmResp = null;
	        	MAPErrorMessage mapErrorMessage = null;
//				try{
//					deliveredStatus = lbsMap.getSmsResponse().get(smsDialogId);
//				}catch (NullPointerException ex){
//					deliveredStatus = null;
//				}
				while(mtForwSmResp == null && mapErrorMessage == null){
					mtForwSmResp = (MtForwardShortMessageResponse) this.mapMessageCache.getMapMessage(smsDialogId);  
	        		mapErrorMessage = this.mapMessageCache.getMapErrorMessage(smsDialogId);
	        		TimeUnit.MILLISECONDS.sleep(10);
	        		counter++;
	        		if (counter > (smsResponseTimeOut/10))
	        			break;
//					try{
//						deliveredStatus = lbsMap.getSmsResponse().get(smsDialogId);
//					}catch (NullPointerException ex){
//						deliveredStatus = null;
//					}        		
//					TimeUnit.MILLISECONDS.sleep(10);
//					counter++;
//					if (counter > 5000)
//						break;
				}
				if (mtForwSmResp != null){
					deliveredStatus = 0L;
				}else if (mapErrorMessage != null) {
					deliveredStatus = mapErrorMessage.getErrorCode();
				}else {
					deliveredStatus = null;
				}
				if (deliveredStatus == null){        		
					response.setHeader("RESULT", "NOK");
					response.setHeader("ERROR", "Unable to obtain sms response");
				}else{
					response.setHeader("RESULT", "OK");
					response.setHeader("msisdn", msisdn);
					response.setHeader("deliverystatus", Long.toString(deliveredStatus));
				}
			}
		}

	}
	
	
    private void actionRDS() throws InterruptedException {
    	long dialogId = lbsMap.sendRds(msisdn);
    	logger.debug("Waiting response for dialogid: "+dialogId);
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
        if (dialogId == -1){
        	response.setHeader("RESULT", "NOK");
            response.setHeader("ERROR", "Unable to create dialog");
            out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to create dialog\"}"));
        }else{
        	int counter=0;
        	ReportSMDeliveryStatusResponse rdsResp = null;
        	MAPErrorMessage mapErrorMessage = null;
        	TimeUnit.MILLISECONDS.sleep(5);
        	while(rdsResp == null && mapErrorMessage == null ){        	
        		rdsResp = (ReportSMDeliveryStatusResponse) this.mapMessageCache.getMapMessage(dialogId);  
        		mapErrorMessage = this.mapMessageCache.getMapErrorMessage(dialogId);
        		TimeUnit.MILLISECONDS.sleep(10);
        		counter++;
        		if (counter > (rdsResponseTimeOut/10))
        			break;
        	}
        	logger.debug("Counter value: "+counter );
        	logger.debug("dialogID: "+dialogId+" ReportSMDeliveryStatus:"+rdsResp+" maperror:"+mapErrorMessage );
        	if (rdsResp == null && mapErrorMessage == null){        		
        		response.setHeader("RESULT", "NOK");
                response.setHeader("ERROR", "Unable to obtain ReportSMDeliveryStatus response");
                out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to obtain ReportSMDeliveryStatus response\"}"));
        	}else{
        		if (rdsResp != null){
	        		response.setHeader("RESULT", "OK");
	        		response.setHeader("OperationCode", Integer.toString(rdsResp.getOperationCode()));
	        		response.setHeader("InvokeId", Long.toString(rdsResp.getInvokeId()));
	        		if (rdsResp.getStoredMSISDN()!=null)
	        			response.setHeader("StoredMSISDN", rdsResp.getStoredMSISDN().getAddress());
	        		out.println(String.format("{\"RESULT\":\"OK\", "
	        				+ "\"OperationCode\":%d, "
	        				+ "\"InvokeId\":%d, "
	        				+ "\"StoredMSISDN\":\"%s\"}",
	        				rdsResp.getOperationCode(),
	        				rdsResp.getInvokeId(),
	        				(rdsResp.getStoredMSISDN()!=null)?rdsResp.getStoredMSISDN().getAddress():"n/a"));
        		}
        		if (mapErrorMessage != null){
        			response.setHeader("RESULT", "NOK");
	        		response.setHeader("MAPERRORCODE", Long.toString(mapErrorMessage.getErrorCode()));
	        		response.setHeader("MAPERRORMESSAGE", mapErrorMessage.toString());
//	        		if(mapErrorMessage.isEmUnknownSubscriber())
//	        			response.setHeader("UnknownSubscriber", mapErrorMessage.getEmUnknownSubscriber().toString());
	        		out.println(String.format("{\"RESULT\":\"NOK\", "
	        				+ "\"MAPERRORCODE\":%d, "
	        				+ "\"MAPERRORMESSAGE\":\"%s\"}",
	        				mapErrorMessage.getErrorCode(),
	        				mapErrorMessage.toString()));
        		}
        	}
        }
		
	}

    private void actionSRI() throws InterruptedException {
        long dialogId = lbsMap.sendSriSm(msisdn);
        logger.debug("Waiting response for dialogid: "+dialogId);
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
        if (dialogId == -1){
        	response.setHeader("RESULT", "NOK");
            response.setHeader("ERROR", "Unable to create dialog");
            out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to create dialog\"}"));
        }else{
        	int counter=0;
        	SendRoutingInfoForSMResponse sriResp = null;
        	MAPErrorMessage mapErrorMessage = null;
        	TimeUnit.MILLISECONDS.sleep(5);
        	while(sriResp == null && mapErrorMessage == null ){        	
        		sriResp = (SendRoutingInfoForSMResponse) this.mapMessageCache.getMapMessage(dialogId);  
        		mapErrorMessage = this.mapMessageCache.getMapErrorMessage(dialogId);
        		TimeUnit.MILLISECONDS.sleep(10);
        		counter++;
        		if (counter > (sriResponseTimeOut/10))
        			break;
        	}
        	logger.debug("Counter value: "+counter );
        	logger.debug("dialogID: "+dialogId+" sri:"+sriResp+" maperror:"+mapErrorMessage );
        	if (sriResp == null && mapErrorMessage == null){        		
        		response.setHeader("RESULT", "NOK");
                response.setHeader("ERROR", "Unable to obtain sri response");
                out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to obtain sri response\"}"));
        	}else{
        		if (sriResp != null){
	        		response.setHeader("RESULT", "OK");
	        		response.setHeader("IMSI", sriResp.getIMSI().getData());
	        		response.setHeader("VLR", sriResp.getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress());
	        		out.println(String.format("{\"RESULT\":\"OK\", "
	        				+ "\"IMSI\":\"%s\", "
	        				+ "\"VLR\":\"%s\"}",
	        				sriResp.getIMSI().getData(),
	        				sriResp.getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress()));
        		}
        		if (mapErrorMessage != null){
        			response.setHeader("RESULT", "NOK");
	        		response.setHeader("MAPERRORCODE", Long.toString(mapErrorMessage.getErrorCode()));
	        		response.setHeader("MAPERRORMESSAGE", mapErrorMessage.toString());
//	        		if(mapErrorMessage.isEmUnknownSubscriber())
//	        			response.setHeader("UnknownSubscriber", mapErrorMessage.getEmUnknownSubscriber().toString());
	        		out.println(String.format("{\"RESULT\":\"NOK\", "
	        				+ "\"MAPERRORCODE\":%d, "
	        				+ "\"MAPERRORMESSAGE\":\"%s\"}",
	        				mapErrorMessage.getErrorCode(),
	        				mapErrorMessage.toString()));
        		}
        	}
        }
    }

    private void actionPSI() throws InterruptedException{    	
    	logger.debug("Generate PSI");
    	long dialogId = lbsMap.sendPsi(imsi, vlr);
    	logger.debug("Waiting response for dialogid: "+dialogId);
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
        //response.setHeader("RESULT", "OK");
        if (dialogId == -1){
        	response.setHeader("RESULT", "NOK");
            response.setHeader("ERROR", "Unable to create dialog");
            out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to create dialog\"}"));
        }else{
        	int counter=0;
        	ProvideSubscriberInfoResponse psiResp = null;
        	MAPErrorMessage mapErrorMessage = null;
        	while(psiResp == null && mapErrorMessage == null){
        		psiResp = (ProvideSubscriberInfoResponse)mapMessageCache.getMapMessage(dialogId);        		
        		mapErrorMessage = mapMessageCache.getMapErrorMessage(dialogId);
        		TimeUnit.MILLISECONDS.sleep(10);
        		counter++;
        		if (counter > (psiResponseTimeOut/10))
        			break;
        	}
        	logger.debug("Counter value: "+counter );
        	if (psiResp == null && mapErrorMessage == null){        		
        		response.setHeader("RESULT", "NOK");
                response.setHeader("ERROR", "Unable to obtain psi response");
                out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to obtain psi response\"}"));
        	}else{
        		if(psiResp != null){
        			String age,cellid,lac = null,vlr,substate = null;
        			response.setHeader("RESULT", "OK");        			
        			age=Integer.toString(psiResp.getSubscriberInfo().getLocationInformation().getAgeOfLocationInformation());
        			response.setHeader("AGE", age);
        			try {
        				cellid=Integer.toString(psiResp.getSubscriberInfo().getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI().getCellGlobalIdOrServiceAreaIdFixedLength().getCellIdOrServiceAreaCode());
        				response.setHeader("CELLID",  cellid);
        				lac=Integer.toString(psiResp.getSubscriberInfo().getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI().getCellGlobalIdOrServiceAreaIdFixedLength().getLac());
        				response.setHeader("LAC", lac);					
        			} catch (MAPException e) {
        				cellid="Error";
        				response.setHeader("CELLID","Error");
        				logger.error("Map Exception when retriving CellID or LAC", e);
        			}
        			vlr=psiResp.getSubscriberInfo().getLocationInformation().getVlrNumber().getAddress();
        			response.setHeader("VLR", vlr);
        			
        			try{
        				logger.debug("Subscriber State: "+psiResp.getSubscriberInfo().getSubscriberState().toString());
        				substate = psiResp.getSubscriberInfo().getSubscriberState().toString();
        			} catch (Exception e){
        				logger.error("Unable to retrive Subscriber State: ");
        				e.printStackTrace();        			
        			}
        			if (substate != null){
        				response.setHeader("SubscriberState", substate);
        			}
        			out.println(String.format("{\"RESULT\":\"OK\", "	        				
	        				+ "\"AGE\":\"%s\", "
	        				+ "\"CELLID\":\"%s\", "
	        				+ "\"LAC\":\"%s\", "
	        				+ "\"VLR\":\"%s\", "
	        				+ "\"SubscriberState\":\"%s\""
	        				+ "}",
	        				age,
	        				cellid,
	        				lac,
	        				vlr,
	        				substate));
        			//response.setHeader("GEOINF",  psiResp.getSubscriberInfo().getLocationInformation().getGeographicalInformation().toString());
        		}
        		if (mapErrorMessage != null){
        			response.setHeader("RESULT", "NOK");
	        		response.setHeader("MAPERRORCODE", Long.toString(mapErrorMessage.getErrorCode()));
	        		response.setHeader("MAPERRORMESSAGE", mapErrorMessage.toString());
//	        		if(mapErrorMessage.isEmUnknownSubscriber())
//	        			response.setHeader("UnknownSubscriber", mapErrorMessage.getEmUnknownSubscriber().toString());
	        		out.println(String.format("{\"RESULT\":\"NOK\", "
	        				+ "\"MAPERRORCODE\":%d, "
	        				+ "\"MAPERRORMESSAGE\":\"%s\"}",
	        				mapErrorMessage.getErrorCode(),
	        				mapErrorMessage.toString()));
        		}
        	}
        }
        
    }

    private void actionSRIPSI() {
    }

    private void actionATI() throws InterruptedException {
    	logger.debug("Generate AnyTimeInterrogation");
    	long dialogId = lbsMap.sendATI(msisdn);
    	logger.debug("Waiting response for dialogid: "+dialogId);
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
        //response.setHeader("RESULT", "OK");
        if (dialogId == -1){
        	response.setHeader("RESULT", "NOK");
            response.setHeader("ERROR", "Unable to create dialog");
            out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to create dialog\"}"));
        }else{
        	int counter=0;
        	AnyTimeInterrogationResponse atiResp = null;
        	MAPErrorMessage mapErrorMessage = null;
        	while(atiResp == null && mapErrorMessage == null){
        		atiResp = (AnyTimeInterrogationResponse)mapMessageCache.getMapMessage(dialogId);        		
        		mapErrorMessage = mapMessageCache.getMapErrorMessage(dialogId);
        		TimeUnit.MILLISECONDS.sleep(10);
        		counter++;
        		if (counter > (psiResponseTimeOut/10))
        			break;
        	}
        	logger.debug("Counter value: "+counter );
        	if (atiResp == null && mapErrorMessage == null){        		
        		response.setHeader("RESULT", "NOK");
                response.setHeader("ERROR", "Unable to obtain ati response");
                out.println(String.format("{\"RESULT\":\"NOK\", \"ERROR\":\"Unable to obtain ati response\"}"));
        	}else{
        		if(atiResp != null){
        			String age,cellid,lac = null,vlr,substate = null;
        			response.setHeader("RESULT", "OK");        			
        			age=Integer.toString(atiResp.getSubscriberInfo().getLocationInformation().getAgeOfLocationInformation());
        			response.setHeader("AGE", age);
        			try {
        				cellid=Integer.toString(atiResp.getSubscriberInfo().getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI().getCellGlobalIdOrServiceAreaIdFixedLength().getCellIdOrServiceAreaCode());
        				response.setHeader("CELLID",  cellid);
        				lac=Integer.toString(atiResp.getSubscriberInfo().getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI().getCellGlobalIdOrServiceAreaIdFixedLength().getLac());
        				response.setHeader("LAC", lac);					
        			} catch (MAPException e) {
        				cellid="Error";
        				response.setHeader("CELLID","Error");
        				logger.error("Map Exception when retriving CellID or LAC", e);
        			}
        			vlr=atiResp.getSubscriberInfo().getLocationInformation().getVlrNumber().getAddress();
        			response.setHeader("VLR", vlr);
        			
        			try{
        				logger.debug("Subscriber State: "+atiResp.getSubscriberInfo().getSubscriberState().toString());
        				substate = atiResp.getSubscriberInfo().getSubscriberState().toString();
        			} catch (Exception e){
        				logger.error("Unable to retrive Subscriber State: ");
        				e.printStackTrace();        			
        			}
        			if (substate != null){
        				response.setHeader("SubscriberState", substate);
        			}
        			out.println(String.format("{\"RESULT\":\"OK\", "	        				
	        				+ "\"AGE\":\"%s\", "
	        				+ "\"CELLID\":\"%s\", "
	        				+ "\"LAC\":\"%s\", "
	        				+ "\"VLR\":\"%s\", "
	        				+ "\"SubscriberState\":\"%s\""
	        				+ "}",
	        				age,
	        				cellid,
	        				lac,
	        				vlr,
	        				substate));
        			//response.setHeader("GEOINF",  psiResp.getSubscriberInfo().getLocationInformation().getGeographicalInformation().toString());
        		}
        		if (mapErrorMessage != null){
        			response.setHeader("RESULT", "NOK");
	        		response.setHeader("MAPERRORCODE", Long.toString(mapErrorMessage.getErrorCode()));
	        		response.setHeader("MAPERRORMESSAGE", mapErrorMessage.toString());
//	        		if(mapErrorMessage.isEmUnknownSubscriber())
//	        			response.setHeader("UnknownSubscriber", mapErrorMessage.getEmUnknownSubscriber().toString());
	        		out.println(String.format("{\"RESULT\":\"NOK\", "
	        				+ "\"MAPERRORCODE\":%d, "
	        				+ "\"MAPERRORMESSAGE\":\"%s\"}",
	        				mapErrorMessage.getErrorCode(),
	        				mapErrorMessage.toString()));
        		}
        	}
        }
    }
}
