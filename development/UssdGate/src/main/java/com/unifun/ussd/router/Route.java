/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.router;

/**
 *
 * @author okulikov
 */
public class Route implements Comparable {
    private final String pattern;
    private final String[] primaryDestination;
    private final String failureDestination;
    
    private int k = 0;
    private final int maxRoutes;
    
    public Route(String pattern, String primaryDestination[], String failureDestination) {
        this.pattern = pattern;
        this.primaryDestination = primaryDestination;
        this.failureDestination = failureDestination;
        this.maxRoutes = primaryDestination.length;
    }
        
    public String pattern() {
        return pattern;
    }
    
    public String[] primaryDestionation() {
        return primaryDestination;
    }
    
    public String failureDestination() {
        return failureDestination;
    }
    
    public synchronized String nextDestination() {
        if (k == maxRoutes) {
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
