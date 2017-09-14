/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template ocsDir, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonMessage;
import com.unifun.ussd.Deployment;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonReader;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
public class OCSCluster implements Deployment {

    private final AtomicReference<Map<String, JsonMessage>> pool = new AtomicReference();
    private Iterator<JsonMessage> it;
    
    private final File ocsDir;
    private List<File> files = new ArrayList();
    
    private Date lastReload = new Date(0);

    private final Logger LOGGER = Logger.getLogger(OCSCluster.class);

    public OCSCluster(String path) {
        ocsDir = new File(path);
    }

    public JsonMessage ocs(String name) {
        return pool.get().get(name);
    }
    
    public JsonMessage nextRoundRobin() {
        if (it == null) {
            it = pool.get().values().iterator();
        }
        
        if (!it.hasNext()) {
            it = pool.get().values().iterator();
        }
        
        return it.next();
    }
    
    @Override
    public void reload() throws Exception {
        lastReload = new Date();
        LOGGER.info("Deploying  " + ocsDir.getAbsolutePath());
        
        files = Collections.unmodifiableList(listFiles());
        HashMap<String, JsonMessage> items = new HashMap();
        
        for (File f : files) {
            LOGGER.info("Deploying  " + f.getName());
            try (FileInputStream fin = new FileInputStream(f)) {
                JsonReader reader = Json.createReader(fin);
                items.put(name(f), new JsonMessage(reader.readObject()));
            }
        }
        this.pool.set(Collections.unmodifiableMap(items));
    }
    
    private String name(File f) {
        return f.getName().substring(0, f.getName().indexOf('.'));
    }
    
    @Override
    public boolean isModified() {
        ArrayList<File> list = listFiles();
        if (list.size() != files.size()) {
            return true;
        }
        return list.stream().anyMatch((f) -> (new Date(f.lastModified()).after(lastReload)));
    }
    
    private ArrayList<File> listFiles() {
        ArrayList<File> items = new ArrayList();
        File[] newfiles = ocsDir.listFiles();
        for (File newfile : newfiles) {
            if (newfile.getName().startsWith("ocs-")) {
                items.add(newfile);
            }
        }
        return items;
    } 
}
