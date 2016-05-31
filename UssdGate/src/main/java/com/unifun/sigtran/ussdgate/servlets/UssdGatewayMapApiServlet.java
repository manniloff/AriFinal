/**
 * 
 */
package com.unifun.sigtran.ussdgate.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.ussdgate.UssMessage;
import com.unifun.sigtran.ussdgate.UssdMapLayer;


/**
 * @author rbabin
 *
 */
@WebServlet(name = "UssdGatewayMapApiServlet", urlPatterns = {"/mapapi"}, displayName = "UssdGatewayMapApiServlet", asyncSupported = true)
public class UssdGatewayMapApiServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4183065359591890797L;
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[UssdGatewayMapApiServlet"));
	private UssdMapLayer mapLayer;


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		processReq(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		processReq(req, resp);
	}

	private void processReq(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		mapLayer = (UssdMapLayer) req.getServletContext().getAttribute("ussMapLayer");
		if (mapLayer==null){
			throw new ServletException("Unable to obtain UssdMapLayer");
		}
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_OK);
		String action = req.getParameter("action");
		switch(action){
		case "pussr":        	
			genPussr(req, resp, action);
			break;
		case "ussr":        	
			genUssr(req, resp, action);
			break;	
		case "pussrd":        	
			genDetachedPussr(req, resp, action);
			break;	
		case "atempttoclosesubsession":
			atemptToClose(req, resp, action);
			break;
		case "stat":
			try{
				long curentDialogCount = this.mapLayer.getCurentDialogCount();
				resp.setHeader("curentDialogCount", Long.toString(curentDialogCount));
				resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"curentDialogCount\":\"%d\"}",
						action,curentDialogCount));
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		default:
			resp.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}",action));
			break; 
		}
		resp.getWriter().flush();
		resp.getWriter().close();
	}

	/**
	 * @param req
	 * @param resp
	 * @param action
	 * @throws ServletException 
	 */
	private void atemptToClose(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException {		
		String mainDialogID = req.getParameter("dialogid");
		if (mainDialogID==null || "".equalsIgnoreCase(mainDialogID)){
			throw new ServletException("invalid dialogid");
		}
		UssMessage message = this.mapLayer.getUssMessages().get(Long.parseLong(mainDialogID));
		if (message == null){
			throw new ServletException("Dialog id no more present in application stack");
		}
		// get msisdn
		String msisdn = message.getMsisdn();
		if (msisdn == null || "".equalsIgnoreCase(msisdn)){
			throw new ServletException("Unable to determine msisdn");
		}
		// get dialog id from map
		long dialogid = this.mapLayer.findSubsession(message.getDialogId()).getDialogId();
		resp.setHeader("dialogid", Long.toString(dialogid));
		// gen respone
		
	}

	private void genPussr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException{
		String ussdText= req.getParameter("ussd_text");
		String msisdn= req.getParameter("msisdn");
		String emap= req.getParameter("emap");
		int counter = 0;		
		logger.debug(String.format("Incomming http req: %s", req.getRequestURL()));
		logger.info(String.format("Incoming http req: action: %s ussd_text: %s msisdn: %s emap: %s", action, ussdText, msisdn, emap));    		
		long dialogId = mapLayer.initiatePUSSR(msisdn, ussdText, 0, 0, emap);		
		if(dialogId == -1){        		
			throw new ServletException("Unable to send ussd to short number: "+ ussdText);
		}
		//Wait for response:
		logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn);    	
		try {
			TimeUnit.MILLISECONDS.sleep(10);
			UssMessage ussMsgLog = null;
			while (ussMsgLog == null){				
				ussMsgLog = this.mapLayer.getUssMsgRespnose().remove(dialogId);
				counter++;
				TimeUnit.MILLISECONDS.sleep(5);				
				//FIXME make this configurable
				if (counter > 12000){
					throw new ServletException("Unable to get ussd response from short number: "+ ussdText);
				}
			}
			//create response:
			resp.setHeader("msisdn", msisdn);
			resp.setHeader("shortnumber", ussdText);
			resp.setHeader("ussd_text", URLEncoder.encode(ussMsgLog.getUssdText(),"UTF-8"));
			resp.setHeader("message_type", ussMsgLog.getMessageType());
			resp.setHeader("dialogid", Long.toString(dialogId));

			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"dialogid\":\"%d\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
					action,ussdText,msisdn,dialogId,URLEncoder.encode(ussMsgLog.getUssdText(),"UTF-8"), ussMsgLog.getMessageType()));
		} catch (InterruptedException e) {				
			e.printStackTrace();
		}
	}

	private void genUssr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException{
		String ussdText= req.getParameter("ussd_text");				
		String emap= req.getParameter("emap");
		String dialogid= req.getParameter("dialogid");
		String charset = req.getParameter("charset");
		String type = req.getParameter("message_type");
		int counter = 0;		
		logger.debug(String.format("Incomming http req: %s", req.getRequestURL()));
		logger.info(String.format("Incoming http req: action: %s ussd_text: %s emap: %s dialogid %s", action, ussdText, emap, dialogid));
		long dialogId = Long.parseLong(dialogid);
		long invokeId = 0;					
		
		UssMessage message = this.mapLayer.getUssMessages().get(dialogid);
		if (message == null){
			throw new ServletException("Unable to find uss message for dialogid: "+dialogId);
		}
		invokeId = message.getIntermediateInvokeId();		
		String msisdn = message.getMsisdn();
		try {			
			this.mapLayer.initiateUSSR(ussdText, dialogid, msisdn, charset,invokeId, 0,0, emap);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException("Unable to send ussr to short number: "+ ussdText);
		}
		//Wait for response:
		logger.debug("Waiting respnose for dialogid: "+dialogid);
		try {
			TimeUnit.MILLISECONDS.sleep(10);
			UssMessage ussMsgResponse = null;
			while (ussMsgResponse == null){
				ussMsgResponse = this.mapLayer.getUssMsgRespnose().remove(dialogId);				
				counter++;
				TimeUnit.MILLISECONDS.sleep(5);
				//FIXME make this configurable
				if (counter > 12000){
					throw new ServletException("Unable to get ussd response from short number: "+ ussdText);
				}
			}
			//create response:
			resp.setHeader("msisdn", msisdn);			
			resp.setHeader("ussd_text", URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"));
			resp.setHeader("message_type", ussMsgResponse.getMessageType());
			resp.setHeader("dialogId", Long.toString(dialogId));

			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"msisdn\":\"%s\",\"dialogid\":\"%d\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
					action,msisdn,dialogId,URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"), ussMsgResponse.getMessageType()));
		} catch (InterruptedException e) {				
			e.printStackTrace();
		} 
	}
	private void genDetachedPussr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException{
		throw new ServletException("Dprecated method");
//		String ussdText= req.getParameter("ussd_text");
//		String msisdn= req.getParameter("msisdn");
//		String emap= req.getParameter("emap");
//		int counter = 0;
//		logger.debug(String.format("Incoming http req: http://ip:port/mapapi?action=%s&ussd_text=%s&msisdn=%s", action, URLEncoder.encode(ussdText,"UTF-8"), msisdn));
//		logger.info(String.format("Incoming http req: action: %s ussd_text: %s msisdn: %s emap: %s", action, ussdText, msisdn, emap));
//
//		long dialogId = this.ussdGateway.getSctpServer().getMap().initiateDetachedUSSD(msisdn, ussdText, emap);
//		if(dialogId == -1){        		
//			throw new ServletException("Unable to send ussd to short number: "+ ussdText);
//		}
//		//Wait for response:
//		logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn);
//		try {
//			TimeUnit.MILLISECONDS.sleep(10);
//			SsMapMessageLog ussMsgResponse = null;
//			while (ussMsgResponse == null){
//				ussMsgResponse = this.ussdGateway.getSctpServer().getMap().getProcUssrResponse().remove(dialogId);
//				counter++;
//				TimeUnit.MILLISECONDS.sleep(10);
//				//logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn+" retry: "+counter);
//				if (counter > 3000){
//					throw new ServletException("Unable to get ussd response from short number: "+ ussdText);
//				}
//			}
//			//create response:
//			resp.setHeader("msisdn", msisdn);			
//			resp.setHeader("ussd_text", URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"));
//			resp.setHeader("message_type", ussMsgResponse.getMessageType());
//			resp.setHeader("dialogId", Long.toString(dialogId));
//
//			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
//					action,ussdText,msisdn,URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"), ussMsgResponse.getMessageType()));
//		} catch (InterruptedException e) {				
//			e.printStackTrace();
//		}
	}
	private void billPussr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException{
		throw new ServletException("Dprecated method");
//		String ussdText= req.getParameter("ussd_text");
//		String msisdn= req.getParameter("msisdn");
//		String emap= req.getParameter("emap");
//		int counter = 0;
//		logger.debug(String.format("Incoming http req: http://ip:port/mapapi?action=%s&ussd_text=%s&msisdn=%s", action, URLEncoder.encode(ussdText,"UTF-8"), msisdn));
//		logger.info(String.format("Incoming http req: action: %s ussd_text: %s msisdn: %s", action, ussdText, msisdn));
//		//retrive billing ussd text
//		SsRouteRules routeRule = new SsRouteRules();
//		if (ussdText.startsWith("*")){
//			routeRule = db.listRouteRule(ussdText, false,false).iterator().next();
//		} else {
//			routeRule = db.listRouteRule(ussdText, true,false).iterator().next();
//		}
//		String ussdbillsc=routeRule.getUssdsc();
//		if (ussdbillsc==null || !(ussdbillsc.startsWith("*") || ussdbillsc.startsWith("#"))){
//			throw new ServletException("Pleas specify ussd short code where to send ussr");
//		}
//		logger.debug("Retriver RR "+ routeRule.toString());
//		long dialogId = this.ussdGateway.getSctpServer().getMap().initiatePUSSR(msisdn, routeRule.getUssdsc(), 0, 0, null);
//		if(dialogId == -1){        		
//			throw new ServletException("Unable to send ussd to short number: "+ routeRule.getUssdsc());
//		}
//		//Wait for response:
//		logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn);
//		try {
//			TimeUnit.MILLISECONDS.sleep(100);
//			SsMapMessageLog ussMsgResponse = null;
//			while (ussMsgResponse == null){
//				ussMsgResponse = this.ussdGateway.getSctpServer().getMap().getProcUssrResponse().remove(dialogId);
//				counter++;
//				TimeUnit.MILLISECONDS.sleep(100);
//				//logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn+" retry: "+counter);
//				if (counter > 300){
//					throw new ServletException("Unable to get ussd response from short number: "+ routeRule.getUssdsc());
//				}
//			}
//			//create response:
//			resp.setHeader("msisdn", msisdn);			
//			resp.setHeader("ussd_text", URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"));
//			resp.setHeader("message_type", ussMsgResponse.getMessageType());
//			resp.setHeader("dialogId", Long.toString(dialogId));
//
//			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
//					action,routeRule.getUssdsc(),msisdn,URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"), ussMsgResponse.getMessageType()));
//		} catch (InterruptedException e) {				
//			e.printStackTrace();
//		}
	}
	private void beepMe(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException{
		throw new ServletException("Dprecated method");
//		String ussdText= req.getParameter("ussd_text");
//		String msisdn= req.getParameter("msisdn");
//		String emap= req.getParameter("emap");
//		int counter = 0;
//		logger.debug(String.format("Incoming http req: http://ip:port/mapapi?action=%s&ussd_text=%s&msisdn=%s", action, URLEncoder.encode(ussdText,"UTF-8"), msisdn));
//		logger.info(String.format("Incoming http req: action: %s ussd_text: %s msisdn: %s", action, ussdText, msisdn));        	
//		long dialogId = this.ussdGateway.getSctpServer().getMap().initiateBeepMe(msisdn, ussdText);
//		//Wait for response:
//		logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn);
//		try {
//			TimeUnit.MILLISECONDS.sleep(100);
//			SsMapMessageLog ussMsgResponse = null;
//			while (ussMsgResponse == null){
//				ussMsgResponse = this.ussdGateway.getSctpServer().getMap().getProcUssrResponse().remove(dialogId);
//				counter++;
//				TimeUnit.MILLISECONDS.sleep(100);
//				if (ussMsgResponse != null){
//					logger.info("Succesfull get response for: "+ String.format(" http req: http://ip:port/mapapi?action=%s&ussd_text=%s&msisdn=%s", action, URLEncoder.encode(ussdText,"UTF-8"), msisdn) );
//				}
//				//logger.debug("Waiting respnose for dialogid: "+dialogId+" msisdn: "+msisdn+" retry: "+counter);
//				if (counter > 300){
//					logger.info("Failed to get response for: "+ String.format(" http req: http://ip:port/mapapi?action=%s&ussd_text=%s&msisdn=%s", action, URLEncoder.encode(ussdText,"UTF-8"), msisdn) );
//					throw new ServletException("Unable to get ussd response for: "+ ussdText);
//				}
//			}
//			//create response:
//			resp.setHeader("msisdn", msisdn);
//			resp.setHeader("ussd_text", URLEncoder.encode(ussMsgResponse.getUssdText(),"UTF-8"));
//
//			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"ussd_text\":\"%s\"}",
//					action,URLEncoder.encode(ussdText,"UTF-8"),msisdn,ussMsgResponse.getUssdText()));
//		} catch (InterruptedException e) {				
//			e.printStackTrace();
//		}
	}
}
