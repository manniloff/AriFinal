/**
 * 
 */
package com.unifun.sigtran.ussdgate.servlets;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
@WebServlet(name = "UssdGatewayTestServlet", urlPatterns = {"/test"}, displayName = "UssdGatewayTestServlet", asyncSupported = true)
public class UssdGatewayTestServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4348239565938673418L;
	private static final Logger logger = LogManager.getLogger(UssdGatewayTestServlet.class);


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.setStatus(HttpServletResponse.SC_OK);
		//Get incoming parameters
		String dialogId =  req.getParameter("dialog_id"); 
		String ussd_text = req.getParameter("ussd_text"); 
		String msisdn = req.getParameter("msisdn");
		String service_code = req.getParameter("service_code");
		logger.info(String.format("Rx: ussd_text=%s, sc=%s, msisdn=%s", ussd_text, service_code, msisdn));
		//Setting headers
		//dialog_id, ussd_text, msisdn, charset, message_type (TCAP Continue or TCAP End),service_code
		resp.setHeader("dialog_id", dialogId);
		resp.setHeader("msisdn", msisdn);
		resp.setHeader("charset", "72");
		resp.setHeader("service_code", service_code);
		if (ussd_text.contains("*") || ussd_text.startsWith("#")){
			logger.info("Retrun menu to req");
			String txt= "Test Menu :\n0. Balance\n1. Option 1\n2. Option 2";
			//String txt = "Il y a un probleme avec votre compte.  Veuillez contacter le service clientele. Merci.Il y a un probleme avec votre compte.  Veuillez contacter le service clientele. Merci.";
			//String txt= "Кайдасың? 10 күнгө АКЫСЫЗ, андан  ары 3 сом/күнүнө (КНС)\nТы где? 10 дней БЕСПЛАТНО, далее 3 сома/день(с НДС)\n1>Жазылуу / Подписаться";
			resp.setHeader("ussd_text", URLEncoder.encode(txt,"UTF-8"));
			resp.setHeader("message_type", "TCAP Continue");	
			//resp.setHeader("message_type", "TCAP End");
		}else if (ussd_text.equalsIgnoreCase("2")){
			logger.info("Return content for option 2");
			resp.setHeader("ussd_text", URLEncoder.encode("Submeniu \n1. Option 1\n2. Option 2","UTF-8"));
			resp.setHeader("message_type", "TCAP Continue");
		}
		else{
			logger.info("Return content for default");
			resp.setHeader("ussd_text", "USSRequest "+ ussd_text + " has recived.");
			resp.setHeader("message_type", "TCAP End");
		}


	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

}
