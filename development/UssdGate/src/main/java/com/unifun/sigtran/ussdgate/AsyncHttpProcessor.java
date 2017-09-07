/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

import com.unifun.map.JsonMessage;
import com.unifun.ussd.context.MapExecutionContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.tcap.api.MessageType;

/**
 *
 * @author okulikov
 */
public class AsyncHttpProcessor {

    private final AsyncMapProcessor mapProcessor;
    private HttpAsyncRequester requester;
    private BasicNIOConnPool pool;
    private ConnectingIOReactor ioReactor;

    private final static Logger LOGGER = Logger.getLogger(AsyncHttpProcessor.class);

    public AsyncHttpProcessor(AsyncMapProcessor mapProcessor) {
        this.mapProcessor = mapProcessor;
    }

    public void init() throws IOReactorException {
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                // Use standard client-side protocol interceptors
                .add(new RequestContent())
                .add(new RequestTargetHost())
                .add(new RequestConnControl())
                .add(new RequestUserAgent("Ussd-Gateway/3.0"))
                .add(new RequestExpectContinue(true)).build();

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

    public UssMessage processMessage(UssMessage msg, String url) {

        long dialogId = msg.getTcap().getDialog().getDialogId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Dialog-Id: " + dialogId + " ---> ");
        }

        HttpHost target = new HttpHost("127.0.0.1", 7081, "http");
        HttpPost post = new HttpPost(url);

        try {
            post.setEntity(new StringEntity(msg.toString()));
        } catch (UnsupportedEncodingException e) {
        }

        HttpCoreContext coreContext = HttpCoreContext.create();
        requester.execute(
                new BasicAsyncRequestProducer(target, post),
                new BasicAsyncResponseConsumer(),
                pool,
                coreContext,
                // Handle HTTP response from a callback
                new FutureCallback<HttpResponse>() {

            @Override
            public void completed(final HttpResponse response) {
                //we have response from external HHTP server
                //and trying to forward it over map
                MapExecutionContext context = new MapExecutionContext(mapProcessor);
                
                //read and parse messge first
                JsonMessage resp;
                
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode != 200) {
                    //land on spare band
                    return;
                }
                
                try {
                    JsonReader reader = Json.createReader(new InputStreamReader(response.getEntity().getContent()));
                    resp = new JsonMessage(reader.readObject());
                } catch (IOException e) {
                    //broken pipe and we could not get response
                    //initiate landing on spare band
                    return;
                }
                
                //we got response finally. is this response positive?
                try {
                    context.setId(resp.getTcap().getDialog().getDialogId());
                    mapProcessor.send(new UssMessage(resp), context);
                } catch (Throwable t) {
                    //map issue
                }
            }

            @Override
            public void failed(final Exception ex) {
                System.out.println(target + "->" + ex);
            }

            @Override
            public void cancelled() {
                System.out.println(target + " cancelled");
            }

        });


        return null;
    }

    private UssMessage abort(UssMessage req, String message) {
        JsonMessage resp = new JsonMessage();
        resp.setSccp(req.getSccp());
        resp.setTcap(req.getTcap());
        resp.getTcap().setType(MessageType.Abort.name());
        resp.getTcap().setAbortMessage(message);
        return new UssMessage(resp);
    }
}
