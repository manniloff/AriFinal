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
public class JsonMapOperationTest {

    public JsonMapOperationTest() {
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
     * Test of getMsisdn method, of class JsonMapOperation.
     */
    @Test
    public void testToString() {
        JsonDataCodingScheme dcs1 = new JsonDataCodingScheme();
        dcs1.setCodingGroup("gsm");
        dcs1.setLanguage("unknown");

        JsonAddressString as1 = new JsonAddressString();
        as1.setNatureOfAddress("INTERNATIONAL");
        as1.setNumberingPlan("ISDN_TELEPHONY");
        as1.setAddress("9023629581");

        JsonMapOperation op1 = new JsonMapOperation();
        op1.setCodingScheme(dcs1);
        op1.setMsisdn(as1);
        op1.setUssdString("Hello");

        ByteArrayInputStream bin = new ByteArrayInputStream(op1.toString().getBytes());
        JsonReader reader = Json.createReader(bin);

        System.out.println(op1.toString());

        JsonMapOperation op2 = new JsonMapOperation(reader.readObject());
        assertEquals(op1.getUssdString(), op2.getUssdString());
    }

}
