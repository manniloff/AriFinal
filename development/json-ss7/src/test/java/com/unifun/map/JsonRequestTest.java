/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

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
public class JsonRequestTest {
    
    public JsonRequestTest() {
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

    @Test
    public void testParsing() {
        InputStream in = getClass().getResourceAsStream("/message.json");
        JsonReader reader = Json.createReader(new InputStreamReader(in));
        JsonObject obj = reader.readObject();
        
        JsonMessage req = new JsonMessage(obj);
        JsonSccp sccp = req.getSccp();
        
        assertEquals("Route-on-GT", sccp.getCallingPartyAddress().getRoutingIndicator());
        assertEquals("Route-on-SSN", sccp.getCalledPartyAddress().getRoutingIndicator());

        assertEquals("0100", sccp.getCallingPartyAddress().getGtIndicator());
        assertEquals("0100", sccp.getCalledPartyAddress().getGtIndicator());

        assertEquals(6, sccp.getCallingPartyAddress().getSsn().intValue());
        assertEquals(6, sccp.getCalledPartyAddress().getSsn().intValue());

        assertEquals(null, sccp.getCallingPartyAddress().getPc());
        assertEquals(null, sccp.getCalledPartyAddress().getPc());

        assertEquals("1234567", sccp.getCallingPartyAddress().getGlobalTitle().getDigits());
        assertEquals("1234567", sccp.getCalledPartyAddress().getGlobalTitle().getDigits());

        JsonTcap tcap = req.getTcap();
        assertEquals("123", tcap.getDialog().getOriginationReference().getAddress());
        assertEquals("234", tcap.getDialog().getMsisdn().getAddress());
        assertEquals("567", tcap.getDialog().getVlrAddress().getAddress());
        
    }

    @Test
    public void testWithoutTcapDialog() {
        InputStream in = getClass().getResourceAsStream("/message_1.json");
        JsonReader reader = Json.createReader(new InputStreamReader(in));
        JsonObject obj = reader.readObject();
        
        JsonMessage req = new JsonMessage(obj);
        JsonSccp sccp = req.getSccp();
        
        assertEquals("Route-on-GT", sccp.getCallingPartyAddress().getRoutingIndicator());
        assertEquals("Route-on-SSN", sccp.getCalledPartyAddress().getRoutingIndicator());

        assertEquals("0100", sccp.getCallingPartyAddress().getGtIndicator());
        assertEquals("0100", sccp.getCalledPartyAddress().getGtIndicator());

        assertEquals(6, sccp.getCallingPartyAddress().getSsn().intValue());
        assertEquals(6, sccp.getCalledPartyAddress().getSsn().intValue());

        assertEquals(null, sccp.getCallingPartyAddress().getPc());
        assertEquals(null, sccp.getCalledPartyAddress().getPc());

        assertEquals("1234567", sccp.getCallingPartyAddress().getGlobalTitle().getDigits());
        assertEquals("1234567", sccp.getCalledPartyAddress().getGlobalTitle().getDigits());

        JsonTcap tcap = req.getTcap();
        assertTrue(tcap.getDialog() == null);
    }
    
}
