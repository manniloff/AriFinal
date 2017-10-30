/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.ussd.router.Router;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.sccp.SccpProvider;

/**
 *
 * @author okulikov
 */
public class Gateway {
    //HTTP channel for HTTP endpoints
    private final HttpChannel httpChannel;
    private final MapChannel mapChannel;
    //Local channel between HTTP or MAP Endpoints
    private final LocalChannel localChannel;

    private final Router router;
    private final OCSQueryCluster ocsCluster;
    private final DeploymentScaner deploymentScaner = new DeploymentScaner();
    private final TestMenu testMenu;
    
    private final static Logger LOGGER = Logger.getLogger(Gateway.class);
    
    
    /**
     * Creates new instance of this gateway.
     * 
     * @param mapStack
     * @param sccpProvider 
     */
    public Gateway(MAPStack mapStack, SccpProvider sccpProvider) {
        mapChannel = new MapChannel(this, mapStack, sccpProvider);
        localChannel = new LocalChannel(this);
        httpChannel = new HttpChannel(this);
        testMenu = new TestMenu(System.getProperty("catalina.base") + "/conf/test-menu.json");

        router = new Router(System.getProperty("catalina.base") + "/conf/router.json");
        ocsCluster = new OCSQueryCluster(System.getProperty("catalina.base") + "/conf");
        deploymentScaner.add(ocsCluster);
    }
    
    
    /**
     * Starts gateway instance.
     * 
     * @throws Exception 
     */
    public void start() throws Exception {
        LOGGER.info("Starting");
        deploymentScaner.start();
        deploymentScaner.add(router);
        deploymentScaner.add(ocsCluster);
        deploymentScaner.add(testMenu);
        
        mapChannel.start();
        httpChannel.start();
        localChannel.start();
    }
    
    /**
     * Shuts down gateway instance
     */
    public void stop() {
        
    }
    
    public Router router() {
        return router;
    }
    
    public OCSQueryCluster ocs() {
        return this.ocsCluster;
    }
    
    public TestMenu testMenu() {
        return testMenu;
    }
    
    /**
     * Gets channel related to the URL protocol.
     * 
     * @param url 
     * @return 
     * @throws com.unifun.ussd.UnknownProtocolException 
     */
    public Channel channel(String url) throws UnknownProtocolException {
        switch (protocol(url)) {
            case "http" :
                return httpChannel;
            case "map" :
                return mapChannel;
            case "proxy" :
                return localChannel;
            default : 
                throw new UnknownProtocolException("Protocol not supported: " + url);
        }
    }
    
    private String protocol(String url) {
        return url.substring(0, url.indexOf("://"));
    }
}
