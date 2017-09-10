/**
 *
 */
package com.unifun.sigtran.ussdgate.servlets;

import com.unifun.map.JsonMessage;
import com.unifun.ussd.context.HttpExecutionContext;
import com.unifun.sigtran.ussdgate.AsyncMapProcessor;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.unifun.sigtran.ussdgate.UssMessage;
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

//    private UssdMapLayer mapLayer;
    
    private static final Logger LOGGER = Logger.getLogger(UssdGatewayMapApiServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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


}
