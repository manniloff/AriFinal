/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.servlets;

import com.unifun.map.JsonMessage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.ussd.AsyncMapProcessor;
import com.unifun.ussd.DeploymentScaner;
import com.unifun.ussd.OCSQueryCluster;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.log4j.Logger;

public class UssdGateServletListener implements ServletContextListener {

    public static final Logger LOGGER = Logger.getLogger(UssdGateServletListener.class);

    private static SigtranStackBean bean = null;
//    private Map<String, Map<String, String>> appSettings = null;
    private ExecutorService exec = Executors.newFixedThreadPool(1);
    private final DeploymentScaner deploymentScaner = new DeploymentScaner();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initiating UssdGateServletListener");
        deploymentScaner.start();

        try {
            final OCSQueryCluster ocsCluster = new OCSQueryCluster(System.getProperty("catalina.base") + "/conf");
            deploymentScaner.add(ocsCluster);
            sce.getServletContext().setAttribute("ocs.cluster", ocsCluster);
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        try {
            sce.getServletContext().setAttribute("test.menu", this.loadTestMenu());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        final Runnable resetCounterTask = () -> {
            loadMapListiner(sce);
        };

        try {
            exec.submit(resetCounterTask);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.debug("Lookup executor started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        deploymentScaner.stop();
        this.exec.shutdown();
    }

    /**
     *
     */
    private void loadMapListiner(ServletContextEvent sce) {
        LOGGER.info("Lookup for SigtranObjectFactory in JNDI");
        boolean contextloop = true;
        while (contextloop) {
            if (bean != null) {
                LOGGER.info("Trying to append Map Listiner");
                //Initiate map listiner for usssd messages types
                addMapListener(sce);
                contextloop = false;

                final AsyncMapProcessor mapProcessor = new AsyncMapProcessor(
                        bean.getStack().getMap().getMapStack(),
                        bean.getStack().getSccp().getSccpStack().getSccpProvider()
                );

                deploymentScaner.add(mapProcessor.router());

                try {
                    mapProcessor.init();
                } catch (Exception e) {
                    LOGGER.error("MAP-Processor could not be initialized", e);
                }

                sce.getServletContext().setAttribute("mapProcessor", mapProcessor);

                LOGGER.info("Initialized OCS cluster");
                if (!exec.isTerminated()) {
                    exec.shutdown();
                }
                break;
            } else {
                try {
                    Context initContext = new InitialContext();
                    bean = (SigtranStackBean) initContext.lookup("java:comp/env/bean/SigtranObjectFactory");
                    sce.getServletContext().setAttribute("sigtranStack", bean);
                } catch (NamingException e) {
                    LOGGER.error("Unable to initiate UssdGate context. \n" + e);
                    LOGGER.error("Retraing to lookup after 10 seconds");
                    e.printStackTrace();
                    contextloop = true;
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e1) {
                        LOGGER.error(e1.getMessage());
                        e1.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     *
     */
    private void addMapListener(ServletContextEvent sce) {
        LOGGER.info("Check Sigtran Stack Status");
        int stage = bean.getStage();
        while (stage != 2) {
            LOGGER.info("Sigtran Stack is not initiated");
            LOGGER.info("Waiting 10 second for recheck");
            stage = bean.getStage();
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e1) {
                LOGGER.error(e1.getMessage());
                e1.printStackTrace();
            }

        }
        while (bean.getStack().getMap().getMapStack() == null) {
            LOGGER.info("Map Stack is not initiated");
            LOGGER.info("Waiting 10 second for recheck");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e1) {
                LOGGER.error(e1.getMessage());
                e1.printStackTrace();
            }
        }
        LOGGER.info("Starting Ussd Map Service Listiner");

    }

    private Map<String, JsonMessage> loadTestMenu() throws IOException {
        HashMap<String, JsonMessage> messages = new HashMap();
        FileInputStream fin = new FileInputStream(System.getProperty("catalina.base") + "/conf/test-menu.json");
        JsonReader reader = Json.createReader(fin);
        JsonArray list = reader.readArray();
        for (int i = 0; i < list.size(); i++) {
            JsonObject obj = list.getJsonObject(i);
            String key = obj.getString("ussd-text");
            JsonMessage msg = new JsonMessage(obj.getJsonObject("message"));
            messages.put(key, msg);
        }
        return Collections.unmodifiableMap(messages);
    }
}
