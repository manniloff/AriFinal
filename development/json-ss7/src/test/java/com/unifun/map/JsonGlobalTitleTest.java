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
public class JsonGlobalTitleTest {

    public JsonGlobalTitleTest() {
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
     * Test of getNumberingPlan method, of class JsonGlobalTitle.
     */
    @Test
    public void testAllFields() {
        JsonGlobalTitle gt1 = new JsonGlobalTitle();
        gt1.setNumberingPlan("ISDN_TELEPHONY");
        gt1.setNatureOfAddressIndicator("INTERNATIONAL");
        gt1.setEncodingSchema("BCD");
        gt1.setDigits("9023629581");

        ByteArrayInputStream bin = new ByteArrayInputStream(gt1.toString().getBytes());
        JsonReader reader = Json.createReader(bin);

        System.out.println(gt1.toString());
        
        JsonObject obj = reader.readObject();
        JsonGlobalTitle gt2 = new JsonGlobalTitle(obj);
        
        assertEquals(gt1.getDigits(), gt2.getDigits());
        assertEquals(gt1.getEncodingSchema(), gt2.getEncodingSchema());
        assertEquals(gt1.getNatureOfAddressIndicator(), gt2.getNatureOfAddressIndicator());
        assertEquals(gt1.getNumberingPlan(), gt2.getNumberingPlan());

    }

}
