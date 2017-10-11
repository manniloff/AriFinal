/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.map;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author okulikov
 */
public class MapURLConnection extends URLConnection {

    public MapURLConnection(URL u) {
        super(u);
    }
    
    @Override
    public void connect() throws IOException {
    }
    
}
