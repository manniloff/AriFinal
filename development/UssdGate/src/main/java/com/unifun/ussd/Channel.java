/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.ussd.context.ExecutionContext;

/**
 *
 * @author okulikov
 */
public interface Channel {
    /**
     * Starts channel.
     * 
     * @throws Exception 
     */
    public void start() throws Exception;
    
    /**
     * Send asynchronously given message.
     * 
     * @param url
     * @param msg
     * @param context 
     */
    public void send(String url, UssMessage msg, ExecutionContext context);
    
    /**
     * Stops this channel.
     */
    public void stop();
}
