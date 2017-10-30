/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.Serializable;
import javax.json.JsonNumber;
import javax.json.JsonObject;

/**
 *
 * @author okulikov
 */
public class JsonSccpAddress  implements Serializable {
    private String routingIndicator;
    private String gtIndicator;
    private Integer ssn;
    private Integer pc;
    private JsonGlobalTitle globalTitle;
    
    public JsonSccpAddress() {
    }
    
    public JsonSccpAddress(JsonObject obj) {
        routingIndicator = obj.getString("routing-indicator");
        gtIndicator = obj.getString("global-title-indicator");
        
        JsonNumber n = obj.getJsonNumber("ssn");
        if (n != null) {
            ssn = n.intValue();
        }

        n = obj.getJsonNumber("pc");
        if (n != null) {
            pc = n.intValue();
        }
        
        JsonObject gt = obj.getJsonObject("global-title");
        if (gt != null) {
            globalTitle = new JsonGlobalTitle(gt);
        }
    }

    public String getRoutingIndicator() {
        return routingIndicator;
    }

    public void setRoutingIndicator(String routingIndicator) {
        this.routingIndicator = routingIndicator;
    }

    public String getGtIndicator() {
        return gtIndicator;
    }

    public void setGtIndicator(String gtIndicator) {
        this.gtIndicator = gtIndicator;
    }

    public Integer getSsn() {
        return ssn;
    }

    public void setSsn(Integer ssn) {
        this.ssn = ssn;
    }

    public Integer getPc() {
        return pc;
    }

    public void setPc(Integer pc) {
        this.pc = pc;
    }

    public JsonGlobalTitle getGlobalTitle() {
        return globalTitle;
    }

    public void setGlobalTitle(JsonGlobalTitle globalTitle) {
        this.globalTitle = globalTitle;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        
        builder.append("\"routing-indicator\"");
        builder.append(":");
        builder.append("\"");
        builder.append(routingIndicator);
        builder.append("\"");

        builder.append(",");
        builder.append("\"global-title-indicator\"");
        builder.append(":");
        builder.append("\"");
        builder.append(gtIndicator);
        builder.append("\"");

        if (pc != null) {
            builder.append(",");
            builder.append("\"pc\"");
            builder.append(":");
            builder.append(pc);
        }

        if (ssn != null) {
            builder.append(",");
            builder.append("\"ssn\"");
            builder.append(":");
            builder.append(ssn);
        }

        if (globalTitle != null) {
            builder.append(",");
            builder.append("\"global-title\"");
            builder.append(":");
            builder.append(globalTitle.toString());
        }
        
        builder.append("}");
        
        return builder.toString();
    }
}
