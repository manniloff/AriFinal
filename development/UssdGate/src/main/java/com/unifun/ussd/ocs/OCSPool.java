/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.ocs;

import com.unifun.sigtran.ussdgate.Deployment;
import com.unifun.ussd.router.Route;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
public class OCSPool implements Deployment {

    AtomicReference<ArrayList<OCS>> pool = new AtomicReference();

    private final File file;
    private Date lastReload = new Date(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future future;

    private final Logger LOGGER = Logger.getLogger(OCSPool.class);

    public OCSPool(String path) {
        file = new File(path);
    }

    @Override
    public void reload() throws Exception {
        lastReload = new Date();
        LOGGER.info("Deploying  " + file.getAbsolutePath());
        
        ArrayList<Route> routeList = new ArrayList();
        
        try (FileInputStream fin = new FileInputStream(file)) {
            JsonReader reader = Json.createReader(fin);
            JsonObject obj = reader.readObject();
            
            JsonArray list = obj.getJsonArray("routes");
            for (int i = 0; i < list.size(); i++) {
                obj = list.getJsonObject(i);
                
                String code = obj.getString("pattern");
                String primaryURL = obj.getString("primary-url");
                
                String secondaryURL = null;
                JsonString v = obj.getJsonString("secondary-url");
                if (v != null) {
                    secondaryURL = v.getString();
                }
                
                Route route = new Route(code, primaryURL, secondaryURL);
                routeList.add(route);
                
                LOGGER.info("Configured " + route);
            }
            
            Collections.sort(routeList);
//            this.pool.set(Collections.unmodifiableList(routeList));
        }
    }
    
    @Override
    public boolean isModified() {
        return new Date(file.lastModified()).after(lastReload);
    }
    
}
