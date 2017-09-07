/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.Serializable;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 *
 * @author okulikov
 */
public class JsonMapOperation  implements Serializable {
    private JsonAddressString msisdn;
    private String ussdString;
    private JsonDataCodingScheme codingScheme;
    
    public JsonMapOperation() {
    }
    
    public JsonMapOperation(JsonObject obj) {
        JsonString str = obj.getJsonString("ussdString");
        if (str != null) {
            ussdString = str.getString();
        }

        JsonObject a = obj.getJsonObject("msisdn");
        if (a != null) {
            msisdn = new JsonAddressString(a);
        }
        
        a = obj.getJsonObject("coding-scheme");
        if (a != null) {
            codingScheme = new JsonDataCodingScheme(a);
        }
    }

    public JsonAddressString getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(JsonAddressString msisdn) {
        this.msisdn = msisdn;
    }

    public String getUssdString() {
        return ussdString;
    }

    public void setUssdString(String ussdString) {
        this.ussdString = ussdString;
    }

    public JsonDataCodingScheme getCodingScheme() {
        return codingScheme;
    }

    public void setCodingScheme(JsonDataCodingScheme codingScheme) {
        this.codingScheme = codingScheme;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        builder.append("\"ussdString\"");
        builder.append(":");
        builder.append('"');
        builder.append(ussdString);
        builder.append('"');

        if (codingScheme != null) {
            builder.append(",");
            builder.append("\"coding-scheme\"");
            builder.append(":");
            builder.append(codingScheme);
        }

        if (msisdn != null) {
            builder.append(",");
            builder.append("\"msisdn\"");
            builder.append(":");
            builder.append(msisdn);
        }

        builder.append("}");
        
        return builder.toString();
    }
}
