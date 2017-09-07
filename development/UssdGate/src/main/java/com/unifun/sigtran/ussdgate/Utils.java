/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

import org.mobicents.protocols.ss7.map.api.MAPMessageType;

/**
 *
 * @author okulikov
 */
public class Utils {
    public static String serviceCode(String ussdText) {
        return ussdText.startsWith("*") ? ussdText.substring(ussdText.indexOf("*") + 1,
                            ussdText.indexOf("#")) : ussdText;
    }
    
    public static String typeOf(String typeName) {
        switch (typeName) {
            case "TCAP Continue":
                return MAPMessageType.unstructuredSSRequest_Request.name();
            case "TCAP End":
                return MAPMessageType.processUnstructuredSSRequest_Response.name();
            case "processUnstructuredSSRequest_Response":
                return MAPMessageType.processUnstructuredSSRequest_Response.name();
            case "unstructuredSSRequest_Request":
                return MAPMessageType.unstructuredSSRequest_Request.name();
            default:
                return "unknown";
        }
    }
}
