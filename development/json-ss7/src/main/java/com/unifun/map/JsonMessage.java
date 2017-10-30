/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.Serializable;
import javax.json.JsonObject;

/**
 *
 * @author okulikov
 */
public class JsonMessage  implements Serializable {
    private JsonSccp sccp;
    private JsonTcap tcap;
    
    public JsonMessage() {
    }
    
    public JsonMessage(JsonObject obj) {
        if (obj.getJsonObject("sccp") != null) {
            sccp = new JsonSccp(obj.getJsonObject("sccp"));
        }
        tcap = new JsonTcap(obj.getJsonObject("tcap"));
    }

    public JsonSccp getSccp() {
        return sccp;
    }

    public void setSccp(JsonSccp sccp) {
        this.sccp = sccp;
    }

    public JsonTcap getTcap() {
        return tcap;
    }

    public void setTcap(JsonTcap tcap) {
        this.tcap = tcap;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        
        if (sccp != null) {
            builder.append("\"sccp\":");
            builder.append(sccp);
            builder.append(",");
        }

        
        builder.append("\"tcap\":");
        builder.append(tcap);
        
        builder.append("}");
        return builder.toString();
    }
}
