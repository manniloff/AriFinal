/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonComponent;
import com.unifun.map.JsonComponents;
import com.unifun.map.JsonDataCodingScheme;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import com.unifun.ussd.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.json.Json;
import javax.json.JsonReader;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.tcap.api.MessageType;

/**
 *
 * @author okulikov
 */
public class HttpChannel implements Channel {
    
    private final Gateway gateway;
    
    private HttpAsyncRequester requester;
    private BasicNIOConnPool pool;
    private ConnectingIOReactor ioReactor;

    private final static Logger LOGGER = Logger.getLogger(HttpChannel.class);
    
    public HttpChannel(Gateway gateway) {
        this.gateway = gateway;
    }
    

    @Override
    public void start() throws Exception {
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                // Use standard client-side protocol interceptors
                .add(new RequestContent())
                .add(new RequestTargetHost())
                .add(new RequestConnControl())
                .add(new RequestUserAgent("Ussd-Gateway/3.0"))
                /*.add(new RequestExpectContinue(true))*/.build();

        // Create client-side HTTP protocol handler
        HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();

        // Create client-side I/O event dispatch
        final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
                protocolHandler,
                ConnectionConfig.DEFAULT);

        // Create client-side I/O reactor
        ioReactor = new DefaultConnectingIOReactor();

        // Create HTTP connection pool
        pool = new BasicNIOConnPool(ioReactor, ConnectionConfig.DEFAULT);

        // Limit total number of connections
        pool.setDefaultMaxPerRoute(200);
        pool.setMaxTotal(200);

        // Run the I/O reactor in a separate thread
        new Thread(() -> {
            try {
                // Ready to go!
                ioReactor.execute(ioEventDispatch);
            } catch (InterruptedIOException ex) {
                LOGGER.warn("Interrupted I/O reactor");
            } catch (IOException e) {
                LOGGER.warn("I/O error: " + e.getMessage());
            }
        }).start();

        requester = new HttpAsyncRequester(httpproc);
    }

    @Override
    public void send(String uri, UssMessage msg, ExecutionContext context) {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL: " + uri);
            context.failed(e);
            return;
        }
        
        HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        HttpPost post = new HttpPost(url.toExternalForm());
        post.setHeader("Content-Type", "application/json");

        long dialogId = msg.getTcap().getDialog().getDialogId();

        
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("(DID:%d) ---> %s", dialogId, uri));
        }

        String content = msg.toString();
        try {
            post.setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
        }

//        post.setHeader("Content-Length", Integer.toString(content.length()));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("TX: " + content);
        }
        
        HttpCoreContext coreContext = HttpCoreContext.create();
        
        
        requester.execute(
                new BasicAsyncRequestProducer(target, post),
                new BasicAsyncResponseConsumer(),
                pool,
                coreContext,
                // Handle HTTP response from a callback
                new PrimaryPathResponseHandler(dialogId, context));
    }

    @Override
    public void stop() {
    }

    private class PrimaryPathResponseHandler implements FutureCallback<HttpResponse> {

        private final long dialogId;
        private final ExecutionContext context;
        
        public PrimaryPathResponseHandler(long dialogId, ExecutionContext context) {
            this.dialogId = dialogId;
            this.context = context;
        }

        @Override
        public void completed(HttpResponse response) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("(DID: " + dialogId + ") <--- " + response.getStatusLine());
            }

            JsonMessage resp;
            
            Channel channel = null;            
            try {
                channel = gateway.channel("map://");
            } catch (Exception e) {
            }
            
            switch (statusCode) {
                case 200:
                    try {
                        JsonReader reader = Json.createReader(new InputStreamReader(response.getEntity().getContent()));
                        resp = new JsonMessage(reader.readObject());
                        
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("RX: " + resp);
                        }
                        
                        context.completed(new UssMessage(resp));
                    } catch (Exception e) { 
                        LOGGER.error("Could not read message: ", e);
                        context.failed(e);
                    }
                    break;
                default:
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("(Dialog-Id): " + dialogId + " trying spare destination");
                    }
                    context.failed(new IOException("Response: " + statusCode));
            }

        }

        @Override
        public void failed(Exception e) {
            LOGGER.error("(Dialog-Id): " + dialogId + "Tranmission failure: ", e);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("(Dialog-Id): " + dialogId + " trying spare destination");
            }
            context.failed(e);
        }

        @Override
        public void cancelled() {
            context.cancelled();
        }

    }

    private JsonMessage errorResponse(UssMessage msg, String message) {
        JsonMessage resp = new JsonMessage();
        resp.setTcap(msg.getTcap());
        resp.getTcap().setType(MessageType.End.name());

        JsonDataCodingScheme codingScheme = new JsonDataCodingScheme();
        codingScheme.setCodingGroup("GeneralGsm7");
        codingScheme.setLanguage("UCS2");

        JsonMapOperation op = new JsonMapOperation();
        op.setUssdString(message);
        op.setCodingScheme(codingScheme);

        JsonMap map = new JsonMap("unstructured-ss-request", op);
        JsonReturnResultLast returnResultLast = new JsonReturnResultLast(msg.invokeId(), map);

        JsonComponent component = new JsonComponent();
        component.setType("returnResultLast");
        component.setValue(returnResultLast);

        JsonComponents components = new JsonComponents();
        components.add(component);

        JsonTcapDialog dialog = new JsonTcapDialog();
        dialog.setDialogId(msg.getTcap().getDialog().getDialogId());

        JsonTcap tcap = new JsonTcap();
        tcap.setType(MessageType.End.name());
        tcap.setDialog(dialog);
        tcap.setComponents(components);

        resp.setTcap(tcap);
        return resp;
    }

}
