/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.ussd.UssMessage;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
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
                
                ((HttpServletResponse)asyncContext.getResponse()).setStatus(HttpServletResponse.SC_OK);
                ((HttpServletResponse)asyncContext.getResponse()).addHeader("Connection", "keep-alive");
                asyncContext.getResponse().setContentLength(content.length());

                asyncContext.getResponse().getWriter().println(content);
                asyncContext.getResponse().flushBuffer();
            } catch (IOException e) {
                LOGGER.error("IO error: ", e);
            } finally {
                asyncContext.complete();
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
