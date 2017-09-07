/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

import org.mobicents.protocols.ss7.map.api.MAPMessageType;

/**
 * Implements congestion control function.
 * 
 * @author okulikov
 */
public class CongestionControl {
    private final UssdMapLayer mapLayer;
    
    public CongestionControl(UssdMapLayer mapLayer) {
        this.mapLayer = mapLayer;
    }
    
    public boolean isCongested(SsRouteRules route,UssMessage ussMsg) {
            mapLayer.incrementRequest(route.getId());
        Long conps = (route.getConnps() == null || "".equalsIgnoreCase(route.getConnps()))
                ? new Long(99999) : Long.parseLong(route.getConnps());
        return ((conps <= mapLayer.countRequests(route.getId())) && (MAPMessageType.processUnstructuredSSRequest_Request
                .name().equalsIgnoreCase(ussMsg.getMessageType())));
    
    }
}
