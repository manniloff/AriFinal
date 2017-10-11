/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.ussd.UssMessage;
import java.io.IOException;
import javax.servlet.AsyncContext;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
public class HttpLocalContext implements ExecutionContext {

    private final AsyncContext asyncContext;

    private final static Logger LOGGER = Logger.getLogger(HttpLocalContext.class);

    public HttpLocalContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }


    @Override
    public void completed(UssMessage msg) {
        EXECUTOR.execute(() -> {
            try {
                String content = msg.toString();
                asyncContext.getResponse().setContentType("application/json");
                asyncContext.getResponse().setContentLength(content.length());

                asyncContext.getResponse().getWriter().println(content);
                asyncContext.getResponse().flushBuffer();
            } catch (IOException e) {
                LOGGER.error("IO error: ", e);
            }
        });
    }

    @Override
    public void failed(Exception e) {
    }

    @Override
    public void cancelled() {
    }

}
