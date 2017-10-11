/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.ussd.Channel;
import com.unifun.ussd.MapChannel;
import com.unifun.ussd.UssMessage;
import java.net.URL;

/**
 *
 * @author okulikov
 */
public class MapLocalContext implements ExecutionContext {

    private final MapChannel channel;
    private final String url;
    private final Channel txChannel;
    private final UssMessage msg;
    
    private int fcount = 0;
    
    /**
     * Creates context instance.
     * 
     * @param channel originated map channel to which this context belongs.
     * @param url spare URL which should be used in case of failure.
     * @param msg original message
     * @param txChannel spare channel related to the given URL.
     */
    public MapLocalContext(MapChannel channel, String url, UssMessage msg, Channel txChannel) {
        this.channel = channel;
        this.url = url;
        this.msg = msg;
        this.txChannel = txChannel;
    }
    
    @Override
    public void completed(UssMessage msg) {
        channel.send(null, msg, null);
    }

    @Override
    public void failed(Exception e) {
        if (++fcount < 2) {
            txChannel.send(url, msg, this);
        }
    }

    @Override
    public void cancelled() {
    }
    
}
