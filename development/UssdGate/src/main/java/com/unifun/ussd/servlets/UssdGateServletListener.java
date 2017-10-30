/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.servlets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.ussd.Gateway;
import org.apache.log4j.Logger;

public class UssdGateServletListener implements ServletContextListener {

    public static final Logger LOGGER = Logger.getLogger(UssdGateServletListener.class);

    private static SigtranStackBean bean = null;
//    private Map<String, Map<String, String>> appSettings = null;
    private final ExecutorService exec = Executors.newFixedThreadPool(1);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initiating UssdGateServletListener");
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
                
                LOGGER.info("Creating gateway instance");
                final Gateway gateway = new Gateway(
                        bean.getStack().getMap().getMapStack(),
                        bean.getStack().getSccp().getSccpStack().getSccpProvider()
                );
                
                LOGGER.info("Starting gateway instance");
                try {
                    gateway.start();
                } catch (Exception e) {
                    LOGGER.error("MAP-Processor could not be initialized", e);
                }

                sce.getServletContext().setAttribute("ussd.gateway", gateway);

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

}
