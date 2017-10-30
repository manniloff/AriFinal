/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.Serializable;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 *
 * @author okulikov
 */
public class JsonTcapDialog implements Serializable {

    private long dialogId = -1;
    private String applicationContextName;
    private JsonAddressString originationReference;
    private JsonAddressString destinationReference;
    private JsonAddressString msisdn;
    private JsonAddressString vlrAddress;

    public JsonTcapDialog() {
    }

    public JsonTcapDialog(JsonObject obj) {
        JsonString str = obj.getJsonString("application-context-name");
        if (str != null) {
            applicationContextName = str.getString();
        }

        if (obj.getJsonObject("origination-reference") != null) {
            originationReference = new JsonAddressString(obj.getJsonObject("origination-reference"));
        }
        
        if (obj.getJsonObject("destination-reference") != null) {
            destinationReference = new JsonAddressString(obj.getJsonObject("destination-reference"));
        }

        if (obj.getJsonObject("msisdn") != null) {
            msisdn = new JsonAddressString(obj.getJsonObject("msisdn"));
        }
        
        
        if (obj.getJsonObject("vlr-address") != null) {
            vlrAddress = new JsonAddressString(obj.getJsonObject("vlr-address"));
        }

        JsonNumber n = obj.getJsonNumber("dialogId");
        if (n != null) {
            dialogId = n.longValue();
        }

    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public JsonAddressString getOriginationReference() {
        return originationReference;
    }

    public void setOriginationReference(JsonAddressString originatingReference) {
        this.originationReference = originatingReference;
    }

    public JsonAddressString getDestinationReference() {
        return destinationReference;
    }

    public void setDestinationReference(JsonAddressString destinationReference) {
        this.destinationReference = destinationReference;
    }

    public JsonAddressString getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(JsonAddressString msisdn) {
        this.msisdn = msisdn;
    }

    public JsonAddressString getVlrAddress() {
        return vlrAddress;
    }

    public void setVlrAddress(JsonAddressString vlrAddress) {
        this.vlrAddress = vlrAddress;
    }

    public long getDialogId() {
        return dialogId;
    }

    public void setDialogId(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        
        builder.append("\"dialogId\"");
        builder.append(":");
        builder.append(dialogId);
        
        
        if (applicationContextName != null) {
            builder.append(",");
            builder.append('"').append("application-context-name").append('"').append(":");
            builder.append('"').append(applicationContextName).append('"');        
        }
        
        if (this.originationReference != null) {
            builder.append(",");
            builder.append('"').append("origination-reference").append('"').append(":");
            builder.append(this.originationReference);        
        }

        if (this.destinationReference != null) {
            builder.append(",");
            builder.append('"').append("destination-reference").append('"').append(":");
            builder.append(this.destinationReference);        
        }

        if (this.msisdn != null) {
            builder.append(",");
            builder.append('"').append("msisdn").append('"').append(":");
            builder.append(this.msisdn);        
        }

        if (this.vlrAddress != null) {
            builder.append(",");
            builder.append('"').append("vlr-address").append('"').append(":");
            builder.append(this.vlrAddress);        
        }
        
        builder.append("}");
        
        return builder.toString();
    }
    
}
