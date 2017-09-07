/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate.servlets;

import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncContextListener implements AsyncListener {
    
    static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", AsyncContextListener.class.getSimpleName(), ""));

    public AsyncContextListener() {
    }
    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        logger.error("[AsyncListener][onTimeout]: " + event);
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        logger.error("[AsyncListener][onError]: " + event);
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }    
}
