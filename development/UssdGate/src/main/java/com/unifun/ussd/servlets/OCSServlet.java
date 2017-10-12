/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.servlets;

import com.unifun.map.JsonComponent;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import com.unifun.ussd.Channel;
import com.unifun.ussd.Gateway;
import com.unifun.ussd.UssMessage;
import com.unifun.ussd.context.OcsLocalContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
@WebServlet(name = "OCSServlet", urlPatterns = {"/ocs"}, asyncSupported = true)
public class OCSServlet extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(OCSServlet.class);

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet OCSServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet OCSServlet at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("Request from " + request.getRemoteAddr() + ":" + request.getRemotePort());
        
        Gateway gateway = (Gateway) request.getServletContext().getAttribute("ussd.gateway");
        
        try {
            //read message as context in json format
            JsonReader reader = Json.createReader(new InputStreamReader(request.getInputStream()));
            JsonObject obj = reader.readObject();

            JsonMessage req = new JsonMessage(obj);
            
            JsonMessage ocsQuery = gateway.ocs().nextQuery();

            copyParams(req, ocsQuery);

            //Start the asynchronous execution!
            //Asynchronous execution allows to leave methos Servlet.service() immediately
            //without holding this thread and container will be able to recycle this thread
            //for receiving incoming messages.
            //Final HTTP response will be handled later (when it will actually arrive)
            //by callback object HttpLocalContext
            Channel channel = gateway.channel("map://localhost");
            long dialogId = req.getTcap().getDialog().getDialogId();
            
            OcsLocalContext context = new OcsLocalContext(request.startAsync(), dialogId);
            //send received message over map with async callback handler
            channel.send(null, new UssMessage(ocsQuery), context);
            //mapProcessor.sendOverMap(new UssMessage(ocsQuery), new HttpLocalContext(context));
        } catch (Throwable e) {
            LOGGER.error("Could not process message", e);
            //Worst case. Could not do anything more
            response.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
        } 
    }

    private void copyParams(JsonMessage m1, JsonMessage m2) {
        JsonTcap tcap1 = m1.getTcap();
        JsonTcap tcap2 = m2.getTcap();

        JsonTcapDialog dialog1 = tcap1.getDialog();
        JsonTcapDialog dialog2 = tcap2.getDialog();

        dialog2.setDestinationReference(dialog1.getDestinationReference());
//        dialog2.setOriginationReference(dialog1.getOriginationReference());

        if (dialog2.getMsisdn() != null) {
            dialog2.setMsisdn(dialog1.getMsisdn());
        }

        if (dialog2.getVlrAddress() != null) {
            dialog2.setVlrAddress(dialog1.getVlrAddress());
        }

        JsonMapOperation op1 = operation(tcap1);
        JsonMapOperation op2 = operation(tcap2);

//        op2.setUssdString(op1.getUssdString());
        if (op2.getMsisdn() != null) {
            op2.setMsisdn(op1.getMsisdn());
        }
    }

    private JsonMapOperation operation(JsonTcap tcap) {
        JsonComponent component1 = tcap.getComponents().get(0);
        switch (component1.getType()) {
            case "invoke":
                JsonInvoke invoke = (JsonInvoke) component1.getValue();
                JsonMap map = (JsonMap) invoke.component();
                return (JsonMapOperation) map.operation();
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component1.getValue();
                return (JsonMapOperation) (((JsonMap) returnResultLast.component()).operation());
        }
        return null;
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
