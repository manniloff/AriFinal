/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template ocsDir, choose Tools | Templates
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonReader;
import org.apache.log4j.Logger;

/**
 * This class manages collection of OCS queries.
 * 
 * Typical OCS system includes several nodes. Each query of in this collection
 * belongs to a specific node. Query contains <code>ProcessUnstructuredSSRequest</code>
 * message in json format wrapped with TCAP and SCCP portions.
 * 
 * Collection is build from files that are located in the deployment directory.
 * The name of the file should start with <code>ocs-</code> prefix.
 * 
 * Query can be accessed by name which matches the name of the <code>ocs-</code> file 
 * without extension or via another strategy.
 * 
 * 
 * @author okulikov
 */
public class OCSQueryCluster implements Deployment {
    
    //pool of objets
    private final AtomicReference<Map<String, JsonMessage>> pool = new AtomicReference();
    private Iterator<JsonMessage> it;
    
    private final File ocsDir;
    private List<File> files = new ArrayList();
    
    //date&time when messages were loaded
    private Date lastReload = new Date(0);

    //logger instance
    private final Logger LOGGER = Logger.getLogger(OCSQueryCluster.class);

    
    /**
     * Creates new instance of this collection.
     * 
     * @param path 
     */
    public OCSQueryCluster(String path) {
        ocsDir = new File(path);
    }

    /**
     * Provides access to query with given name.
     * 
     * @param name
     * @return 
     */
    public JsonMessage query(String name) {
        return pool.get().get(name);
    }
    
    /**
     * Gets query based on round robin strategy.
     * 
     * @return 
     */
    public JsonMessage nextQuery() {
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
    
    /**
     * Gets file name without extension.
     * 
     * @param f
     * @return 
     */
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

    /**
     * List ocs related files located in working directory.
     * 
     * @return 
     */
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
