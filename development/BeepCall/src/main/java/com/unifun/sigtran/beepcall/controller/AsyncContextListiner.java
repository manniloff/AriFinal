/**
 * 
 */
package com.unifun.sigtran.beepcall.controller;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rbabin
 *
 */
public class AsyncContextListiner implements AsyncListener {

	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", AsyncContextListiner.class.getSimpleName(), ""));

    public AsyncContextListiner() {
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        logger.error("[AsyncListener][onTimeout]: " + event.getThrowable().toString());
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        logger.error("[AsyncListener][onError]: " + event.getThrowable().toString());
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }

}
