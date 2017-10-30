/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.ByteArrayInputStream;
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
public class JsonAddressStringTest {
    
    public JsonAddressStringTest() {
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
     * Test of getNumberingPlan method, of class JsonAddressString.
     */
    @Test
    public void testAddressString() {
        JsonAddressString as1 = new JsonAddressString();
        as1.setNatureOfAddress("INTERNATIONAL");
        as1.setNumberingPlan("ISDN_TELEPHONY");
        as1.setAddress("9023629581");
        
        String str = as1.toString();
        
        ByteArrayInputStream bin = new ByteArrayInputStream(str.getBytes());
        JsonReader reader = Json.createReader(bin);
        
        JsonObject obj = reader.readObject();
        JsonAddressString as2 = new JsonAddressString(obj);
        
        assertEquals(as1.getAddress(), as2.getAddress());
        assertEquals(as1.getNatureOfAddress(), as2.getNatureOfAddress());
        assertEquals(as1.getNumberingPlan(), as2.getNumberingPlan());
    }

}
