/**
 *
 */
package com.unifun.ussd.servlets;

import com.unifun.map.JsonMessage;
import com.unifun.ussd.Channel;
import com.unifun.ussd.Gateway;
import com.unifun.ussd.UnknownProtocolException;
import com.unifun.ussd.context.HttpLocalContext;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.unifun.ussd.UssMessage;
import com.unifun.ussd.context.ExecutionContext;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.AsyncContext;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
@WebServlet(name = "UssdServlet", urlPatterns = {"/mapapi"}, displayName = "UssdServlet", asyncSupported = true)
public class UssdServlet extends HttpServlet {

    private static final long serialVersionUID = 4183065359591890797L;
    private static final Logger LOGGER = Logger.getLogger(UssdServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        try (PrintWriter writer = resp.getWriter()) {
            writer.println("<body>");
            writer.println("<p>");
            writer.println("curl -X POST -H \"Content-Type: application/json\" -d @/home/okulikov/work/unifun/unifun-sigtran-modules/development/UssdGate/pussr2.json http://127.0.0.1:7080/UssdGate/mapapi");
            writer.println("</p>");
            writer.println("</body>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //Obtain reference for the map processor from the servlet context
        Gateway gateway = (Gateway) req.getServletContext().getAttribute("ussd.gateway");
        //Start the asynchronous execution!
        //Asynchronous execution allows to leave methos Servlet.service() immediately
        //without holding this thread and container will be able to recycle this thread
        //for receiving incoming messages.
        //Final HTTP response will be handled later (when it will actually arrive)
        //by callback object HttpLocalContext
        AsyncContext context = req.startAsync();

        ExecutionContext.EXECUTOR.submit(() -> {
            try {
                //read message as context in json format
                JsonReader reader = Json.createReader(new InputStreamReader(req.getInputStream()));
                JsonObject obj = reader.readObject();
                UssMessage msg = new UssMessage(new JsonMessage(obj));
                
                Channel channel = gateway.channel("map://localhost");
                
                LOGGER.info("Channel " + channel);
                channel.send(null, msg, new HttpLocalContext(context));
            } catch (IOException | UnknownProtocolException e) {
                LOGGER.error("Could not process message", e);
                //Worst case. Could not do anything more
                try {
                    resp.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
                } catch (IOException ex) {
                }
            }
        });
    }

}
