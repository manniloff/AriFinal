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
public interface Deployment {
    public boolean isModified();
    public void reload() throws Exception;
}
