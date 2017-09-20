/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.router;

import java.net.URL;

/**
 *
 * @author okulikov
 */
public class Route implements Comparable {
    private final String pattern;
    private final URL[] primaryDestination;
    private final URL failureDestination;
    
    private int k = 0;
    
    public Route(String pattern, URL primaryDestination[], URL failureDestination) {
        this.pattern = pattern;
        this.primaryDestination = primaryDestination;
        this.failureDestination = failureDestination;
    }
        
    public String pattern() {
        return pattern;
    }
    
    public URL[] primaryDestionation() {
        return primaryDestination;
    }
    
    public URL failureDestination() {
        return failureDestination;
    }
    
    public URL nextDestination() {
        if (k == primaryDestination.length) {
            k = 0;
        }
        return primaryDestination[k++];
    }
    
    @Override
    public String toString() {
        return String.format("{pattern:%s}", pattern);
    }

    @Override
    public int compareTo(Object o) {
        Route other = (Route) o;
        return Integer.compare(this.pattern.length(), other.pattern.length());
    }
}
