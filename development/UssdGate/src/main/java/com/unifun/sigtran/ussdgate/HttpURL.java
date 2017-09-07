/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

/**
 *
 * @author okulikov
 */
public class HttpURL {
    private final String url;
    private final String ussdText;
    
    public HttpURL(String url) {
        String params[] = url.split(",");        
        this.url = params[0];
        this.ussdText = params.length > 1 ? params[1] : null;
    }
    
    public String url() {
        return url;
    }
    
    public String ussdText() {
        return ussdText;
    }
    
    public boolean hasUssdText() {
        return ussdText != null && ussdText.length() > 0;
    }
}
