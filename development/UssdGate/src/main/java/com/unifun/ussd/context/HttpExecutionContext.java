/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.sigtran.ussdgate.UssMessage;
import java.io.IOException;
import javax.servlet.AsyncContext;

/**
 *
 * @author okulikov
 */
public class HttpExecutionContext implements ExecutionContext {

    private final AsyncContext asyncContext;
    private long id;
    
    public HttpExecutionContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }
    
    
    @Override
    public void completed(UssMessage msg) {
        EXECUTOR.execute(() -> {
            try {
                asyncContext.getResponse().getWriter().println(msg);
                asyncContext.getResponse().flushBuffer();
            } catch (IOException e) {
            }
        });
    }

    @Override
    public void failed(Exception e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancelled() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        return "Execution-Context {HTTP, " + id + "}";
    }
}
