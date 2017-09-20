/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonMessage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.jboss.logging.Logger;

/**
 *
 * @author okulikov
 */
public class TestMenu implements Deployment {

    private final File file;
    private Date lastReload = new Date(0);

    private final Logger LOGGER = Logger.getLogger(TestMenu.class);
    private final AtomicReference<Map<String, JsonMessage>> menu = new AtomicReference();
    
    public TestMenu(String path) {
        this.file = new File(path);
    }
    @Override
    public boolean isModified() {
        return new Date(file.lastModified()).after(lastReload);
    }

    @Override
    public void reload() throws Exception {
        lastReload = new Date();
        LOGGER.info("Deploying  " + file.getAbsolutePath());
        
        HashMap<String, JsonMessage> messages = new HashMap();
        FileInputStream fin = new FileInputStream(file);
        JsonReader reader = Json.createReader(fin);
        
        JsonObject obj = reader.readObject();
        JsonArray list = obj.getJsonArray("menu");
        for (int i = 0; i < list.size(); i++) {
            JsonObject item = list.getJsonObject(i);
            String key = item.getString("ussd-text");
            JsonMessage msg = new JsonMessage(item.getJsonObject("message"));
            messages.put(key, msg);
        }
        menu.set(Collections.unmodifiableMap(messages));
    }
    
}
