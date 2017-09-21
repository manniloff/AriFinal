/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonMessage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.jboss.logging.Logger;

/**
 *
 * @author okulikov
 */
public class TestMenu implements Deployment {
    private final File file;
    private Date lastReload = new Date(0);

    private final Logger LOGGER = Logger.getLogger(TestMenu.class);
    private final AtomicReference<HashMap<String, State>> menu = new AtomicReference();
    
    public TestMenu(String path) {
        this.file = new File(path);
    }
    @Override
    public boolean isModified() {
        return new Date(file.lastModified()).after(lastReload);
    }

    public State initial() {
        for (State s : menu.get().values()) {
            if (s.isInitial()) {
                return s;
            }
        }
        return null;
    }
    
    public State find(String name) {
        return menu.get().get(name);
    }
    
    @Override
    public void reload() throws Exception {
        lastReload = new Date();
        LOGGER.info("Deploying  " + file.getAbsolutePath());
        
        ArrayList<MenuItem> items = new ArrayList();
        FileInputStream fin = new FileInputStream(file);
        JsonReader reader = Json.createReader(fin);
        
        JsonObject obj = reader.readObject();
        JsonArray states = obj.getJsonArray("states");
        HashMap map = new HashMap(); 
        for (int i = 0; i < states.size(); i++) {
            JsonObject state = states.getJsonObject(i);
            
            String name = state.getString("name");
            String pattern = state.getString("ussd-text");
            
            JsonMessage msg = null;
            JsonObject m = state.getJsonObject("message");
            if (m != null) {
                msg = new JsonMessage(m);
            }
            
            JsonString s = state.getJsonString("external-url");
            String url = null;
            if (s != null) {
                url = s.getString();
            }
            
            String transition = state.getString("transition");
            boolean initial = state.getBoolean("initial");
            boolean end = state.getBoolean("final");
            map.put(name, new State(name, pattern, msg, url, transition, initial, end));
        }
        menu.set(map);
    }
    
    private class MenuItem {
        private final String pattern;
        private final JsonMessage msg;
        
        public MenuItem(String pattern, JsonMessage msg) {
            this.pattern = pattern;
            this.msg = msg;
        }
    }
    
    public class State {
        private final String name;
        private final String pattern;
        private final JsonMessage msg;
        private final String url;
        private final String transition;
        private final boolean initial;
        private final boolean end;
        
        public State(String name, String pattern, JsonMessage msg, String url, 
                String transition, boolean initial, boolean end) {
            this.name = name;
            this.pattern = pattern;
            this.msg = msg;
            this.url = url;
            this.transition = transition;
            this.initial = initial;
            this.end = end;
        }

        
        public String getName() {
            return name;
        }

        public String getPattern() {
            return pattern;
        }

        public JsonMessage getMsg() {
            return msg;
        }

        public String getUrl() {
            return url;
        }
        
        public String getTransition() {
            return transition;
        }

        public boolean isInitial() {
            return initial;
        }

        public boolean isEnd() {
            return end;
        }
        
        
    }
}
