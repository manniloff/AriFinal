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
import com.unifun.ussd.Channel;
import com.unifun.ussd.Gateway;
import com.unifun.ussd.TestMenu;
import com.unifun.ussd.TestMenu.State;
import com.unifun.ussd.UssMessage;
import com.unifun.ussd.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.AsyncContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
@WebServlet(name = "UssdGatewayTestServlet", urlPatterns = {"/test"}, 
        displayName = "UssdGatewayTestServlet", asyncSupported = true)
public class TestServiceServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 4348239565938673418L;
    private static final Logger LOGGER = LogManager.getLogger(TestServiceServlet.class);

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
        HttpSession session = req.getSession(true);
        try {
            Gateway gateway = (Gateway) req.getServletContext().getAttribute("ussd.gateway");
            TestMenu menu = gateway.testMenu();

            JsonReader reader = Json.createReader(new InputStreamReader(req.getInputStream()));
            JsonObject obj = reader.readObject();

            JsonMessage request = new JsonMessage(obj);
            response(gateway, request, session, menu, req.startAsync());
        } catch (Throwable e) {
            e.printStackTrace();
            resp.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void response(Gateway gateway, JsonMessage request, HttpSession session, TestMenu menu, AsyncContext context) throws IOException {
        JsonTcap tcap = request.getTcap();
        long dialogId = tcap.getDialog().getDialogId();

        JsonComponent component = tcap.getComponents().get(0);
        JsonMap map = null;
        switch (component.getType()) {
            case "invoke":
                map = (JsonMap) ((JsonInvoke) component.getValue()).component();
                break;
            default:
                map = (JsonMap) ((JsonReturnResultLast) component.getValue()).component();
                break;
        }

        JsonMapOperation op = (JsonMapOperation) map.operation();
        String text = op.getUssdString();

        LOGGER.info("Request " + text);

        State state = (State) session.getAttribute("state");
        if (state == null) {
            state = menu.initial();
        }

        LOGGER.info("State=" + state.getName() + ", URL=" + state.getUrl());
        TestContext menuContext = new TestContext(dialogId, invokeId(request), context);
        if (state.getUrl() == null) {
            menuContext.completed(new UssMessage(state.getMsg()));
        } else {
            LOGGER.info("Requesting URI: " + state.getUrl());
            try {
                Channel channel = gateway.channel(state.getUrl());
                channel.send(text, new UssMessage(request), menuContext);
            LOGGER.info("Sent: " + state.getUrl());
            } catch (Exception e) {
                menuContext.failed(e);
            }
        }
        State next = menu.find(state.getTransition());
        session.setAttribute("state", next);
    }

    private long invokeId(JsonMessage msg) {
        return ((JsonInvoke) msg.getTcap().getComponents().get(0).getValue()).getInvokeId();
    }

    private JsonAddressString msisdn(JsonMessage msg) {
        JsonMap map = (JsonMap) (((JsonInvoke) msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation) map.operation()).getMsisdn();
    }

    private JsonDataCodingScheme codingScheme(JsonMessage msg) {
        JsonMap map = (JsonMap) (((JsonInvoke) msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation) map.operation()).getCodingScheme();
    }

    private JsonMessage processRedirect(String url, JsonMessage request) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        try {
            post.setEntity(new StringEntity(request.toString()));
        } catch (UnsupportedEncodingException e) {
        }

        HttpResponse response = httpclient.execute(post);
        JsonReader reader = Json.createReader(new InputStreamReader(response.getEntity().getContent()));
        return new JsonMessage(reader.readObject());
    }

    private class TestContext implements ExecutionContext {

        private final long dialogID;
        private final long invokeID;
        private final AsyncContext context;

        public TestContext(long dialogID, long invokeID, AsyncContext context) {
            this.dialogID = dialogID;
            this.invokeID = invokeID;
            this.context = context;
        }

        @Override
        public void completed(UssMessage msg) {
            LOGGER.info("Completed: ");
            JsonComponent component = (JsonComponent) msg.getTcap().getComponents().get(0);

            switch (component.getType()) {
                case "invoke":
                    ((JsonInvoke) component.getValue()).setInvokeId(invokeID);
                    break;
                default:
                    ((JsonReturnResultLast) component.getValue()).setInvokeId(invokeID);
            }

            msg.getTcap().getDialog().setDialogId(dialogID);
            
            try {
                HttpServletResponse resp = (HttpServletResponse) context.getResponse();

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");

                String content = msg.toString();
                resp.setContentLength(content.length());

                resp.getWriter().println(content);
                resp.getWriter().flush();
            } catch (IOException e) {
                LOGGER.error("Could not send response", e);
                failed(e);
            }
        }

        @Override
        public void failed(Exception excptn) {
        }

        @Override
        public void cancelled() {
        }

    }
}
