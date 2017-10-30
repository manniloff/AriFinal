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
public class JsonDataCodingSchemeTest {

    public JsonDataCodingSchemeTest() {
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
     * Test of getLanguage method, of class JsonDataCodingScheme.
     */
    @Test
    public void testToString() {
        JsonDataCodingScheme dcs1 = new JsonDataCodingScheme();
        dcs1.setCodingGroup("gsm");
        dcs1.setLanguage("unknown");

        ByteArrayInputStream bin = new ByteArrayInputStream(dcs1.toString().getBytes());
        JsonReader reader = Json.createReader(bin);

        System.out.println(dcs1.toString());
        
        JsonDataCodingScheme dcs2 = new JsonDataCodingScheme(reader.readObject());
        assertEquals(dcs1.getCodingGroup(), dcs2.getCodingGroup());
        assertEquals(dcs1.getLanguage(), dcs2.getLanguage());

    }

}
