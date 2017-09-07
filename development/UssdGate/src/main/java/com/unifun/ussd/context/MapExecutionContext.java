/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.sigtran.ussdgate.AsyncMapProcessor;
import com.unifun.sigtran.ussdgate.UssMessage;

/**
 *
 * @author okulikov
 */
public class MapExecutionContext implements ExecutionContext {

    private final AsyncMapProcessor mapProcessor;
    private long id;
    
    public MapExecutionContext(AsyncMapProcessor mapProcessor) {
        this.mapProcessor = mapProcessor;
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
        mapProcessor.send(msg, this);
    }

    @Override
    public void failed(Exception excptn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancelled() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
