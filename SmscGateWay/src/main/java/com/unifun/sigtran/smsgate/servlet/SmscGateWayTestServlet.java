package com.unifun.sigtran.smsgate.servlet;

import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.smsgate.MapLayer;
import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.models.SMSQueue;

@WebServlet(name = "SmscGateWayTestServlet", urlPatterns = {"/test"}, displayName = "SmscGateWayTestServlet", asyncSupported = true)
public class SmscGateWayTestServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3505052657113086796L;
	private static final Logger logger = Logger.getLogger(SmscGateWayServlet.class);
	private SigtranStackBean sigStack;
	private MapLayer mapLayer;
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		this.sigStack = (SigtranStackBean) req.getServletContext().getAttribute("sigtranStack");
		if (sigStack==null){
			throw new ServletException("Unable to obtain SigtranStack");
		}
		this.mapLayer = (MapLayer)req.getServletContext().getAttribute("smsMapLayer");
		if (mapLayer==null){
			throw new ServletException("Unable to obtain smsMapLayer");
		}
		
		resp.setContentType("text/plain");
		resp.setStatus(HttpServletResponse.SC_OK);
		String action = req.getParameter("action");	
		String message = req.getParameter("message");
		switch(action){
		case "hiddenSms":
			if(message == null) {
				message = "Test Hidden sms";
			}
			sendSms(req, resp, true, message, "GSM7", 1, action);
			break;
		case "SimlpeSms":
			if(message == null) {
				message = "Test GSM7 encoding!";
			}
			sendSms(req, resp, false, message, "GSM7", 1, action);
			break;
		case "TestGSM8Sms":
			if(message == null) {
				message = "Test GSM8 encoding! ΩΩΩ 	¤¤¤";
			}
			sendSms(req, resp, false, message, "GSM8", 1, action);
			break;
		case "TestUCS2Sms":
			if(message == null) {
				message = "Тест кодировки УКС2! ЁЪХЮБЬЯЧС";
			}
			sendSms(req, resp, false, message, "UCS2", 1, action);
			break;
		case "ConcatinateSms":
			message = "Test Concatinate sms! first part. Test Concatinate sms! Test Concatinate sms! Test Concatinate sms! Test Concatinate sms! Test Concatinate sms!first_end Second Part Start End Test Concatinate.";
			sendSms(req, resp, false, message, "GSM7", 2, action);
			break;
		default:
			resp.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}",action));
			break;        		 
		}
	}
	/**
	 * @param req
	 * @param resp
	 * @param isHidden
	 * @param encoding
	 * @param action
	 * @throws IOException 
	 */
	private void sendSms(HttpServletRequest req, HttpServletResponse resp
			, boolean isHidden, String message, String Encoding, int Qty, String action) throws IOException {
		try {
			long msisdn = Long.valueOf(req.getParameter("msisdn"));
			long messageId = SmsGateWay.getNextMessageId();
			int dcs = (Encoding.equals("GSM7")) ? 0 
					: (Encoding.equals("GSM8")) ? 4 : 8; 
			Timestamp now = new Timestamp(System.currentTimeMillis());
			Timestamp sendUntil = new Timestamp(System.currentTimeMillis() + SmsGateWay.getDefaultSMSLiveTime());
			String segmentLen = "160";
			if(Qty == 1) {
				segmentLen = (Encoding.equals("GSM7")) ? "160" 
						: (Encoding.equals("GSM8")) ? "140" : "70"; 
			} else {
				segmentLen = (Encoding.equals("GSM7")) ? "153" 
						: (Encoding.equals("GSM8")) ? "134" : "67";
			}
			SMSQueue smsQueue = new SMSQueue(messageId, 1, "Unifun", "5", "1", msisdn, "1", "1"
					, message, Qty, dcs, isHidden ? 64 : 0, now, now, sendUntil, "0", "0", segmentLen);
			SmsGateWay.addSmsToQueue(smsQueue);
			resp.getWriter().println("Sms processed. MessageId - " + messageId + "; Message - " + message);
		} catch (Exception e) {
			logger.error(e);
			resp.getWriter().println(e.getMessage());	
		}
	}	
}
