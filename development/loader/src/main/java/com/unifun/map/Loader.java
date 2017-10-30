/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
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

/**
 *
 * @author okulikov
 */
public class Loader {

    private HttpAsyncRequester requester;
    private BasicNIOConnPool pool;
    private ConnectingIOReactor ioReactor;
    
    private String msg;
    private final String uri;
    private final String path;
    private HttpClient httpClient;
    
    private volatile long started, completed, failed;
    
    public Loader(String uri, String path) {
        this.uri = uri;
        this.path = path;
    }
    
    private void loadContent() throws IOException {
        FileInputStream fin = new FileInputStream(path);
        int b;
        
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        while ((b = fin.read()) != -1) {
            buff.write(b);
        }
        
        msg = new String(buff.toByteArray());
    }
    
    public void start() throws Exception {
        httpClient = HttpClients.createDefault();
        loadContent();
        
        new Thread(new Monitor()).start();

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
                System.err.println("Interrupted I/O reactor");
            } catch (IOException e) {
                System.err.println("I/O error: " + e.getMessage());
            }
        }).start();

        requester = new HttpAsyncRequester(httpproc);
    }


    public void send2() {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            return;
        }
        
        HttpPost post = new HttpPost(url.toExternalForm());
        try {
            post.setEntity(new StringEntity(msg));
        } catch (UnsupportedEncodingException e) {
        }
        
        try {
            started++;
            HttpResponse resp = httpClient.execute(post);
            completed++;
        } catch (IOException e) {
            failed++;
        }
    }
    
    public void send() {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            return;
        }
        
        HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        HttpPost post = new HttpPost(url.toExternalForm());
        post.setHeader("Content-Type", "application/json");

        
        try {
            post.setEntity(new StringEntity(msg));
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
            public void completed(HttpResponse t) {
                completed++;
            }

            @Override
            public void failed(Exception e) {
                System.err.println("Failed: " + e);
                failed++;
            }

            @Override
            public void cancelled() {
                
            }
        });
        
        started++;
    }

    
    public void stop() {
    }    
    
    public void runTest(long pause) throws Exception {
        while (true) {
            send();
            synchronized(this) {
                wait(pause);
            }
        }
    }
    
    private class Monitor implements Runnable {

        @Override
        public void run() {
            while (true) {
                System.out.println(String.format("started %d, completed %d, failed %d", started, completed, failed));
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
        
    }
    
    public static void main(String args[]) throws Exception {
        Loader loader = new Loader(args[0], args[1]);
        
        loader.start();
        
        loader.runTest(Integer.parseInt(args[2]));
    }
}
