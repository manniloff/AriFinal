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
    private final String primaryURL;
    private final String secondaryURL;
    
    public Route(String pattern, String primaryURL, String secondaryURL) {
        this.pattern = pattern;
        this.primaryURL = primaryURL;
        this.secondaryURL = secondaryURL;
    }
    
    public String pattern() {
        return pattern;
    }
    
    public String primaryURL() {
        return primaryURL;
    }
    
    public String secondaryURL() {
        return secondaryURL;
    }
    
    @Override
    public String toString() {
        return String.format("route:%s, primary-url:%s, secondary-url:%s", pattern, primaryURL,secondaryURL);
    }

    @Override
    public int compareTo(Object o) {
        Route other = (Route) o;
        return Integer.compare(this.pattern.length(), other.pattern.length());
    }
}
