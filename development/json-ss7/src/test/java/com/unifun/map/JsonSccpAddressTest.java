/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.ByteArrayInputStream;
import javax.json.Json;
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
public class JsonSccpAddressTest {

    public JsonSccpAddressTest() {
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
     * Test of getRoutingIndicator method, of class JsonSccpAddress.
     */
    @Test
    public void testToStringWithAllFields() {
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

        ByteArrayInputStream bin = new ByteArrayInputStream(a1.toString().getBytes());
        JsonReader reader = Json.createReader(bin);

        System.out.println(a1.toString());
        
        JsonSccpAddress a2 = new JsonSccpAddress(reader.readObject());
        
        assertEquals(a1.getPc(), a2.getPc());
        assertEquals(a1.getSsn(), a2.getSsn());
        assertEquals(a1.getGtIndicator(), a2.getGtIndicator());
        assertEquals(a1.getRoutingIndicator(), a2.getRoutingIndicator());

    }

}
