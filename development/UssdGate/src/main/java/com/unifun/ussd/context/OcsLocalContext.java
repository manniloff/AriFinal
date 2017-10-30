/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.ussd.UssMessage;
import javax.servlet.AsyncContext;

/**
 *
 * @author okulikov
 */
public class OcsLocalContext extends HttpLocalContext {

    private final long origDialogId;
    
    public OcsLocalContext(AsyncContext asyncContext, long origDialogId) {
        super(asyncContext);
        this.origDialogId = origDialogId;
    }

    @Override
    public void completed(UssMessage msg) {
        //restore original dialog ID and send back
        msg.getTcap().getDialog().setDialogId(origDialogId);
        super.completed(msg);
    }
}
