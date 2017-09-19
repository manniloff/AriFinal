/**
 *
 */
package com.unifun.ussd.servlets;

import com.unifun.map.JsonAddressString;
import com.unifun.map.JsonComponent;
import com.unifun.map.JsonDataCodingScheme;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonTcap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
    private static final Logger LOGGER = LogManager.getLogger(UssdGatewayTestServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println("Hellow");
        resp.flushBuffer();
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, JsonMessage> menu = (Map<String, JsonMessage>) req.getServletContext().getAttribute("test.menu");
        try {
            JsonReader reader = Json.createReader(new InputStreamReader(req.getInputStream()));
            JsonObject obj = reader.readObject();

            JsonMessage request = new JsonMessage(obj);
            
            LOGGER.info("Request <---> " + request.toString());
            
            JsonMessage response = response(request, menu);
            LOGGER.info("Response <---> " + response.toString());
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            
            resp.getWriter().println(response.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            resp.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
        }
    }

    private JsonMessage response(JsonMessage request, Map<String, JsonMessage> menu) {
        JsonTcap tcap = request.getTcap();
        long dialogId = tcap.getDialog().getDialogId();
        
        JsonComponent component = tcap.getComponents().get(0);
        JsonMap map = null;
        switch (component.getType()) {
            case "invoke" :
                map = (JsonMap) ((JsonInvoke)component.getValue()).component();
                break;
            default :
                map = (JsonMap) ((JsonReturnResultLast)component.getValue()).component();
                break;
        }
        
        JsonMapOperation op = (JsonMapOperation) map.operation();
        String text = op.getUssdString();

        LOGGER.info("Request " + text);
        
        JsonMessage resp = menu.get(text);
        JsonComponent component2  = (JsonComponent) resp.getTcap().getComponents().get(0);
        
        switch (component2.getType()) {
            case "invoke" :
                ((JsonInvoke)component.getValue()).setInvokeId(invokeId(request));
                break;
            default :
                ((JsonReturnResultLast)component.getValue()).setInvokeId(invokeId(request));
        }
        
        resp.getTcap().getDialog().setDialogId(dialogId);
        return resp;
    }

    private long invokeId(JsonMessage msg) {
        return ((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).getInvokeId();
    }
    
    private JsonAddressString msisdn(JsonMessage msg) {
        JsonMap map = (JsonMap)(((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation)map.operation()).getMsisdn();
    }
    
    private JsonDataCodingScheme codingScheme(JsonMessage msg) {
        JsonMap map = (JsonMap)(((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation)map.operation()).getCodingScheme();
    }
    
}
