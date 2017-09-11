/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.router;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.jboss.logging.Logger;

/**
 *
 * @author okulikov
 */
public class Router  implements Runnable {
    private final ArrayList<Route> routes = new ArrayList();
    private final File file;
    private Date lastReload = new Date(0);
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future future;
    
    private final Logger LOGGER = Logger.getLogger(Router.class);
    
    /**
     * Creates new instance of this router.
     * 
     * @param path absolute path to the configuration file
     */
    public Router(String path) {
        file = new File(path);
    }

    /**
     * Finds route matching given selector.
     * 
     * @param selector
     * @return 
     */
    public Route find(String selector) {
        for (Route route : routes) {
            if (route.pattern().matches(selector)) {
                return route;
            }
        }
        return null;
    }
    
    /**
     * Starts router.
     */
    public void start() {
        future = scheduler.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
        LOGGER.info("Router has been started from " + file.getAbsolutePath());
    }
    
    /**
     * Terminates Router.
     */
    public void stop() {
        if (future != null) future.cancel(true);
        LOGGER.info("Router has been stopped");
    }
    
    /**
     * Loads router data from configuration file.
     */
    private void reload() throws IOException {
        LOGGER.info("Deploying  " + file.getAbsolutePath());
        try (FileInputStream fin = new FileInputStream(file)) {
            JsonReader reader = Json.createReader(fin);
            JsonObject obj = reader.readObject();
            
        }
    }
    
    @Override
    public void run() {
        Date lm = new Date(file.lastModified());
        if (lm.after(lastReload)) {
            lastReload = lm;
            try {
                reload();
            } catch (IOException e) {
                LOGGER.warn("Could deploy " + e.getMessage());
            }
        }
    }
}
