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
import com.unifun.ussd.router.Route;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
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

    public void processMessage(UssMessage msg, Route route, URL url) {
        execute(msg, url, new PrimaryPathResponseHandler(msg, route));
    }

    private void execute(UssMessage msg, URL url, FutureCallback<HttpResponse> callback) {
        HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        HttpPost post = new HttpPost(url.toExternalForm());
        post.setHeader("Content-Type", "application/json");

        long dialogId = msg.getTcap().getDialog().getDialogId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("(Dialog-Id): " + dialogId + " ---> " + url.toExternalForm());
        }

        String content = msg.toString();
        LOGGER.info(content);
        try {
            post.setEntity(new StringEntity(content));
        } catch (UnsupportedEncodingException e) {
        }

//        post.setHeader("Content-Length", Integer.toString(content.length()));
        HttpCoreContext coreContext = HttpCoreContext.create();
        requester.execute(
                new BasicAsyncRequestProducer(target, post),
                new BasicAsyncResponseConsumer(),
                pool,
                coreContext,
                // Handle HTTP response from a callback
                callback);
    }

    private class PrimaryPathResponseHandler implements FutureCallback<HttpResponse> {

        private final UssMessage msg;
        private final Route route;
        private long dialogId;

        public PrimaryPathResponseHandler(UssMessage msg, Route route) {
            this.msg = msg;
            this.route = route;
        }

        @Override
        public void completed(HttpResponse response) {
            int statusCode = response.getStatusLine().getStatusCode();
            dialogId = msg.getTcap().getDialog().getDialogId();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("(Dialog-Id): " + dialogId + " <--- " + response.getStatusLine());
            }

            JsonMessage resp;

            switch (statusCode) {
                case 200:
                    try {
                        JsonReader reader = Json.createReader(new InputStreamReader(response.getEntity().getContent()));
                        resp = new JsonMessage(reader.readObject());
                        mapProcessor.send(new UssMessage(resp), null);
                    } catch (IOException e) {
                        //broken pipe and we could not get response
                        //initiate landing on spare band
                        LOGGER.error("(Dialog-Id): " + dialogId + "Broken pipe: ", e);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("(Dialog-Id): " + dialogId + " trying spare destination");
                        }

                        execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
                    } catch (JsonParsingException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Bad response format: ", e);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("(Dialog-Id): " + dialogId + " trying spare destination");
                        }

                        execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
                    } catch (UnsupportedOperationException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Could not send response back ", e);
                    } catch (RuntimeException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Unexpected error: ", e);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("(Dialog-Id): " + dialogId + " trying spare destination");
                        }

                        execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
                    }
                    break;
                default:
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("(Dialog-Id): " + dialogId + " trying spare destination");
                    }
                    execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
            }

        }

        @Override
        public void failed(Exception e) {
            LOGGER.error("(Dialog-Id): " + dialogId + "Tranmission failure: ", e);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("(Dialog-Id): " + dialogId + " trying spare destination");
            }
            execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
        }

        @Override
        public void cancelled() {
            LOGGER.info("(Dialog-Id): " + dialogId + "Tranmission has been canceled ");
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("(Dialog-Id): " + dialogId + " trying spare destination");
            }
            execute(msg, route.failureDestination(), new FailurePathResponseHandler(msg));
        }

    }

    private class FailurePathResponseHandler implements FutureCallback<HttpResponse> {

        private final UssMessage msg;
        private long dialogId;

        public FailurePathResponseHandler(UssMessage msg) {
            this.msg = msg;
        }

        @Override
        public void completed(HttpResponse response) {
            int statusCode = response.getStatusLine().getStatusCode();
            dialogId = msg.getTcap().getDialog().getDialogId();

            JsonMessage resp;
            switch (statusCode) {
                case 200:
                    try {
                        JsonReader reader = Json.createReader(new InputStreamReader(response.getEntity().getContent()));
                        resp = new JsonMessage(reader.readObject());
                    } catch (IOException e) {
                        //broken pipe and we could not get response
                        //initiate landing on spare band
                        LOGGER.error("(Dialog-Id): " + dialogId + "Broken pipe: ", e);
                        resp = errorResponse(msg, "Service temporary unavailable");
                    } catch (JsonParsingException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Bad response format: ", e);
                        resp = errorResponse(msg, "Service temporary unavailable");
                    } catch (UnsupportedOperationException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Could not send response back ", e);
                        resp = errorResponse(msg, "Service temporary unavailable");
                    } catch (RuntimeException e) {
                        LOGGER.error("(Dialog-Id): " + dialogId + "Unexpected error: ", e);
                        resp = errorResponse(msg, "Service temporary unavailable");
                    }
                    break;
                default:
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("(Dialog-Id): " + dialogId + " send error message");
                    }
                    resp = errorResponse(msg, "Service temporary unavailable");
            }

            mapProcessor.send(new UssMessage(resp), null);
        }

        @Override
        public void failed(Exception e) {
            LOGGER.error("(Dialog-Id): " + dialogId + "Tranmission failure: ", e);
        }

        @Override
        public void cancelled() {
            LOGGER.error("(Dialog-Id): " + dialogId + "Tranmission canceled");
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
