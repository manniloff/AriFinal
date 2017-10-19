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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    
    private State state;
    
    public TestMenu(String path) {
        this.file = new File(path);
    }
    
    @Override
    public boolean isModified() {
        return new Date(file.lastModified()).after(lastReload);
    }

    public State state() {
        return state;
    }
    
    public void reset() {
        menu.get().values().stream().filter((s) -> (s.isInitial())).forEach((s) -> {
            state = s;
        });
    }
    
    public State transit(String s) {
        state = state.transit(s);
        return state;
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
            
            boolean initial = state.getBoolean("initial");
            boolean end = state.getBoolean("final");
            
            ArrayList<Transition> transitions = new ArrayList();
            JsonArray list = state.getJsonArray("transitions");
            
            for (int j = 0; j < list.size(); j++) {
                JsonObject o = list.getJsonObject(j);
                
                String pattern = o.getString("pattern");
                String stateName = o.getString("state");
                
                transitions.add(new Transition(pattern, stateName));
            }
            map.put(name, new State(name, msg, url, transitions, initial, end));
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
        private final JsonMessage msg;
        private final String url;
        private final boolean initial;
        private final boolean end;
        
        private List<Transition> transitions;
        
        public State(String name, JsonMessage msg, String url, 
                List<Transition> transitions, boolean initial, boolean end) {
            this.name = name;
            this.msg = msg;
            this.url = url;
            this.transitions = transitions;
            this.initial = initial;
            this.end = end;
        }

        public State transit(String key) {
            for (Transition t : transitions) {
                if (key.matches(t.pattern)) {
                    return menu.get().get(t.stateName);
                }
            }
            return null;
        }
        
        public String getName() {
            return name;
        }

        public JsonMessage getMsg() {
            return msg;
        }

        public String getUrl() {
            return url;
        }
        
        public boolean isInitial() {
            return initial;
        }

        public boolean isEnd() {
            return end;
        }
    }
    
    public class Transition {
        private final String pattern;
        private final String stateName;
        
        public Transition(String pattern, String stateName) {
            this.pattern = pattern;
            this.stateName = stateName;
        }
    }
}
