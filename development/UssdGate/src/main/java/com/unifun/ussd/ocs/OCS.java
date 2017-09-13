/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.ocs;

import com.unifun.map.JsonSccpAddress;

/**
 *
 * @author okulikov
 */
public class OCS {
    private String name;
    private JsonSccpAddress callingPartyAddress;
    private JsonSccpAddress calledPartyAddress;
    
    public OCS() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonSccpAddress getCallingPartyAddress() {
        return callingPartyAddress;
    }

    public void setCallingPartyAddress(JsonSccpAddress callingPartyAddress) {
        this.callingPartyAddress = callingPartyAddress;
    }

    public JsonSccpAddress getCalledPartyAddress() {
        return calledPartyAddress;
    }

    public void setCalledPartyAddress(JsonSccpAddress calledPartyAddress) {
        this.calledPartyAddress = calledPartyAddress;
    }
    
    
}
