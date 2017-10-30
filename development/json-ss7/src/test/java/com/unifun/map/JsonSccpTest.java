/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author okulikov
 */
public class JsonSccpTest {
    
    public JsonSccpTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getCallingPartyAddress method, of class JsonSccp.
     */
    @Test
    public void testToString() {
        JsonGlobalTitle gt1 = new JsonGlobalTitle();
        gt1.setNumberingPlan("ISDN_TELEPHONY");
        gt1.setNatureOfAddressIndicator("INTERNATIONAL");
        gt1.setEncodingSchema("BCD");
        gt1.setDigits("9023629581");

        JsonSccpAddress a1 = new JsonSccpAddress();
        a1.setGlobalTitle(gt1);
        a1.setGtIndicator("GT_INCLUDES_EVERYTHING");
        a1.setRoutingIndicator("ROUTE_BASED_ON_GLOBAL_TITLE");
        a1.setPc(1);
        a1.setSsn(6);

        JsonGlobalTitle gt2 = new JsonGlobalTitle();
        gt2.setNumberingPlan("ISDN_TELEPHONY");
        gt2.setNatureOfAddressIndicator("INTERNATIONAL");
        gt2.setEncodingSchema("BCD");
        gt2.setDigits("9023629580");

        JsonSccpAddress a2 = new JsonSccpAddress();
        a2.setGlobalTitle(gt2);
        a2.setGtIndicator("GT_INCLUDES_EVERYTHING");
        a2.setRoutingIndicator("ROUTE_BASED_ON_GLOBAL_TITLE");
        a2.setPc(1);
        a2.setSsn(6);
        
        JsonSccp sccp1 = new JsonSccp();
        sccp1.setCalledPartyAddress(a1);
        sccp1.setCallingPartyAddress(a2);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(sccp1.toString().getBytes());
        JsonReader reader = Json.createReader(bin);

        System.out.println(sccp1.toString());
        
        JsonSccp sccp2 = new JsonSccp(reader.readObject());
        
        assertEquals(sccp1.getCalledPartyAddress().getGlobalTitle().getDigits(), sccp2.getCalledPartyAddress().getGlobalTitle().getDigits());
        assertEquals(sccp1.getCallingPartyAddress().getGlobalTitle().getDigits(), sccp2.getCallingPartyAddress().getGlobalTitle().getDigits());
    }

}
