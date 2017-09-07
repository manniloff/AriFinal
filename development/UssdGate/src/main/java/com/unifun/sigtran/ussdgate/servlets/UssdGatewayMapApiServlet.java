/**
 *
 */
package com.unifun.sigtran.ussdgate.servlets;

import com.unifun.map.JsonMessage;
import com.unifun.ussd.context.HttpExecutionContext;
import com.unifun.sigtran.ussdgate.AsyncMapProcessor;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.unifun.sigtran.ussdgate.UssMessage;
import com.unifun.sigtran.ussdgate.UssdMapLayer;
import java.io.InputStreamReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.AsyncContext;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
// PUSSR:
// http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=962798361129&ussd_text=*642%23
//emap format:
// http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=962798361129&ussd_text=*642%23&emap=1
// with custom sccpCallingParty: 
// http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=962798361129&ussd_text=*642%23&emap=1&dest=<customGT>
//with custom sccpCallingParty and sccpCalledParty: 
//http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=962798361129&ussd_text=*642%23&emap=1&dest=<customGT>&orig=<customGT>
//with custom sccpCallingParty, sccpCalledParty and ssn: 
//http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=962798361129&ussd_text=*642%23&emap=1&dest=<customGT>&orig=<customGT>&dssn=<customssn>&ossn=<customssn>
//
// USSR:
// http://127.0.0.1:7080/UssdGate/mapapi?action=ussr&ussd_text=<dialed text>&dialogid=<dialogid from pussr response>&charset=<15 for GSM7 72 for UCS2>
// emap format:
// http://127.0.0.1:7080/UssdGate/mapapi?action=ussr&ussd_text=<dialed text>&dialogid=<dialogid from pussr response>&charset=<15 for GSM7 72 for UCS2>&emap=1
//
//
@WebServlet(name = "UssdGatewayMapApiServlet", urlPatterns = {"/mapapi"}, displayName = "UssdGatewayMapApiServlet", asyncSupported = true)
public class UssdGatewayMapApiServlet extends HttpServlet {
    private static final long serialVersionUID = 4183065359591890797L;

    private UssdMapLayer mapLayer;
    
    private static final Logger LOGGER = Logger.getLogger(UssdGatewayMapApiServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            processReq(req, resp);
        } catch (ServletException | IOException e) {
            LOGGER.warn(e.getMessage(), e);
            resp.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}", e.getMessage()));
            resp.getWriter().flush();
            resp.getWriter().close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //Obtain reference for the map processor from the servlet context
        AsyncMapProcessor mapProcessor = (AsyncMapProcessor) req.getServletContext().getAttribute("mapProcessor");
        try {
            //read message as context in json format
            JsonReader reader = Json.createReader(new InputStreamReader(req.getInputStream()));
            JsonObject obj = reader.readObject();
            
            //Start the asynchronous execution!
            //Asynchronous execution allows to leave methos Servlet.service() immediately
            //without holding this thread and container will be able to recycle this thread
            //for receiving incoming messages.
            //Final HTTP response will be handled later (when it will actually arrive)
            //by callback object HttpExecutionContext
            AsyncContext context = req.startAsync();
            
            //send received message over map with async callback handler
            mapProcessor.send(new UssMessage(new JsonMessage(obj)), new HttpExecutionContext(context));
        } catch (Throwable e) {
            //Worst case. Could not do anything more
            resp.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void processReq(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        mapLayer = (UssdMapLayer) req.getServletContext().getAttribute("ussMapLayer");
        if (mapLayer == null) {
            throw new ServletException("Unable to obtain UssdMapLayer");
        }
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        String action = req.getParameter("action");
        switch (action) {
            case "pussr":
                genPussr(req, resp, action);
                break;
            case "ussr":
                genUssr(req, resp, action);
                break;
            case "pussrd":
                break;
            case "atempttoclosesubsession":
                atemptToClose(req, resp, action);
                break;
            case "stat":
                try {
                    long curentDialogCount = this.mapLayer.getCurentDialogCount();
                    resp.setHeader("curentDialogCount", Long.toString(curentDialogCount));
                    resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"curentDialogCount\":\"%d\"}",
                            action, curentDialogCount));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                resp.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}", action));
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
        if (mainDialogID == null || "".equalsIgnoreCase(mainDialogID)) {
            throw new ServletException("invalid dialogid");
        }
        UssMessage message = this.mapLayer.getUssMessages().get(Long.parseLong(mainDialogID));
        if (message == null) {
            throw new ServletException("Dialog id no more present in application stack");
        }
        // get msisdn
        String msisdn = message.getMsisdn();
        if (msisdn == null || "".equalsIgnoreCase(msisdn)) {
            throw new ServletException("Unable to determine msisdn");
        }
        // get dialog id from map
        long dialogid = this.mapLayer.findSubsession(message.getDialogId()).getDialogId();
        resp.setHeader("dialogid", Long.toString(dialogid));
        // gen respone

    }

    private void genPussr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException {
        String ussdText = req.getParameter("ussd_text");
        String msisdn = req.getParameter("msisdn");
        String emap = req.getParameter("emap");
        String sccpCallingParty = req.getParameter("orig");
        String sccpOssn = req.getParameter("ossn");
        String sccpCalledParty = req.getParameter("dest");
        String sccpDssn = req.getParameter("dssn");
        if (sccpCallingParty != null) {
            if (!sccpCallingParty.matches("[0-9]+")) {
                LOGGER.info("Invalid calling party GT ignore it.");
                sccpCallingParty = null;
            }
        }
        if (sccpCalledParty != null) {
            if (!sccpCalledParty.matches("[0-9]+")) {
                LOGGER.info("Invalid called party GT ignore it.");
                sccpCalledParty = null;
            }
        }
        if (sccpOssn != null) {
            if (!sccpOssn.matches("[0-9]+")) {
                LOGGER.info("Invalid ossn ignore it.");
                sccpOssn = null;
            }
        }
        if (sccpDssn != null) {
            if (!sccpDssn.matches("[0-9]+")) {
                LOGGER.info("Invalid dssn ignore it.");
                sccpDssn = null;
            }
        }
        int counter = 0;
        LOGGER.debug(String.format("Incomming http req: %s", req.getRequestURL()));
        LOGGER.info(String.format("Incoming http req: action: %s ussd_text: %s msisdn: %s emap: %s", action, ussdText, msisdn, emap));
        long dialogId = mapLayer.initiatePUSSR(msisdn, ussdText, 0, 0, emap,
                (sccpCalledParty != null) ? sccpCalledParty : null,
                (sccpCallingParty != null) ? sccpCallingParty : null,
                sccpDssn,
                sccpOssn);
        if (dialogId == -1) {
            throw new ServletException("Unable to send ussd: " + ussdText);
        }
        //Wait for response:
        LOGGER.debug("Waiting respnose for dialogid: " + dialogId + " msisdn: " + msisdn);
        try {
            TimeUnit.MILLISECONDS.sleep(10);
            UssMessage ussMsgLog = null;
            while (ussMsgLog == null) {
                ussMsgLog = this.mapLayer.getUssMsgRespnose().remove(dialogId);
                counter++;
                TimeUnit.MILLISECONDS.sleep(5);
                //FIXME make this configurable
                if (counter > 12000) {
                    throw new ServletException("Unable to get ussd response from short number: " + ussdText);
                }
            }
            //create response:
            resp.setHeader("msisdn", msisdn);
            resp.setHeader("shortnumber", ussdText);
            resp.setHeader("ussd_text", URLEncoder.encode(ussMsgLog.getUssdText(), "UTF-8"));
            resp.setHeader("message_type", ussMsgLog.getMessageType());
            resp.setHeader("dialogid", Long.toString(dialogId));
//			resp.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"dialogid\":\"%d\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
//					action,ussdText,msisdn,dialogId,URLEncoder.encode(ussMsgLog.getUssdText(),"UTF-8"), ussMsgLog.getMessagemsg

            String response = String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"shortnumber\":\"%s\",\"msisdn\":\"%s\",\"dialogid\":\"%d\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
                    action, ussdText, msisdn, dialogId, URLEncoder.encode(ussMsgLog.getUssdText(), "UTF-8"), ussMsgLog.getMessageType());
            resp.getWriter().println(response);
            LOGGER.debug(String.format("Incoming http req: %s?%s", req.getRequestURI(), req.getQueryString()) + "\n\tResp: " + response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void genUssr(HttpServletRequest req, HttpServletResponse resp, String action) throws ServletException, IOException {
        String ussdText = req.getParameter("ussd_text");
        String emap = req.getParameter("emap");
        String dialogid = req.getParameter("dialogid");
        String charset = req.getParameter("charset");
        String type = req.getParameter("message_type");
        int counter = 0;
        LOGGER.debug(String.format("Incomming http req: %s", req.getRequestURL()));
        LOGGER.info(String.format("Incoming http req: action: %s ussd_text: %s emap: %s dialogid %s", action, ussdText, emap, dialogid));
        long dialogId = Long.parseLong(dialogid);
        long invokeId = 0;

        UssMessage message = this.mapLayer.getUssMessages().get(dialogId);
        if (message == null) {
            throw new ServletException("Unable to find uss message for dialogid: " + dialogId);
        }
        invokeId = message.getIntermediateInvokeId();
        String msisdn = message.getMsisdn();
        try {
            this.mapLayer.initiateUSSR(ussdText, dialogid, msisdn, charset, invokeId, 0, 0, emap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Unable to send ussr to short number: " + ussdText);
        }
        //Wait for response:
        LOGGER.debug("Waiting respnose for dialogid: " + dialogid);
        try {
            TimeUnit.MILLISECONDS.sleep(10);
            UssMessage ussMsgResponse = null;
            while (ussMsgResponse == null) {
                ussMsgResponse = this.mapLayer.getUssMsgRespnose().remove(dialogId);
                counter++;
                TimeUnit.MILLISECONDS.sleep(5);
                //FIXME make this configurable
                if (counter > 12000) {
                    throw new ServletException("Unable to get ussd response from short number: " + ussdText);
                }
            }
            //create response:
            resp.setHeader("msisdn", msisdn);
            resp.setHeader("ussd_text", URLEncoder.encode(ussMsgResponse.getUssdText(), "UTF-8"));
            resp.setHeader("message_type", ussMsgResponse.getMessageType());
            resp.setHeader("dialogId", Long.toString(dialogId));
            String response = String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"msisdn\":\"%s\",\"dialogid\":\"%d\",\"ussd_text\":\"%s\",\"message_type\":\"%s\"}",
                    action, msisdn, dialogId, URLEncoder.encode(ussMsgResponse.getUssdText(), "UTF-8"), ussMsgResponse.getMessageType());
            resp.getWriter().println(response);
            LOGGER.debug(String.format("Incoming http req: %s?%s", req.getRequestURI(), req.getQueryString()) + "\n\tResp: " + response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
