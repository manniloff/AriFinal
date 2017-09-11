/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.router;

import java.net.URL;
import org.junit.Test;

/**
 *
 * @author okulikov
 */
public class RouterTest  {
    

    /**
     * Test of find method, of class Router.
     */
    @Test
    public void testRouter() {
        URL url = RouterTest.class.getResource("router.json");
        System.out.println(url.getFile());
    }

}
