/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.checksubscriber.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.checksubscriber.MapLayer;
import com.unifun.sigtran.checksubscriber.utils.MapLayerPreference;

public class CheckSubscriberServletListener implements ServletContextListener {

    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-20s] ", "[CheckSubscriberServletListener"));
	private static SigtranStackBean bean = null;
    private static MapLayer mapLayer;
    private ExecutorService exec = Executors.newFixedThreadPool(1);
    private ForkJoinPool pool ;
    private Map<String, Map<String, String>> modulePreference = new HashMap<>();
    //FIXME  instead of read the configuration from database 
    private MapLayerPreference cfg;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	logger.info("Initiating CheckSubscriberServletListener");	
		final Runnable resetCounterTask = new Runnable() {
	       public void run() { 
	    	   loadMapListiner(sce);
	    	   }
	     };
		try {
			exec.submit(resetCounterTask);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		logger.debug("Lookup executor started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (mapLayer!=null){
        	mapLayer.stop();
        }
        if (pool!=null){
        	pool.shutdown();
        }
        this.exec.shutdown();
    }

	/**
	 * 
	 */
	private void loadMapListiner(ServletContextEvent sce) {
		logger.info("Lookup for SigtranObjectFactory in JNDI");
		boolean contextloop = true;
		while (contextloop){
			if (bean != null){
				logger.info("Trying to append Map Listiner");
				//Initiate map listiner for usssd messages types
				addMapListener();
				contextloop = false;
				sce.getServletContext().setAttribute("mapLayer", mapLayer);
				sce.getServletContext().setAttribute("mapPreference", cfg);
				sce.getServletContext().setAttribute("fjpool", pool);
				if(!exec.isTerminated())
					exec.shutdown();
				break;
			}else{
				try {
					Context initContext = new InitialContext();			
					bean = (SigtranStackBean) initContext.lookup("java:comp/env/bean/SigtranObjectFactory");
					sce.getServletContext().setAttribute("sigtranStack", bean);
//					Context initContext = new InitialContext();
//					Context envCtx = (Context) initContext.lookup("java:comp/env");
//					bean = (SigtranStackBean) envCtx.lookup("bean/SigtranObjectFactory");
				} catch (NamingException e) {
					logger.error("Unable to initiate Beepcall context. \n"+e);
					logger.error("Retraing to lookup after 10 seconds");	
					e.printStackTrace();
					contextloop = true;
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e1) {
						logger.error(e1.getMessage());
						e1.printStackTrace();
					}
				}
			}
		}
		
	}
	
	/**
	 * 
	 */
	private void addMapListener() {		
		logger.info("Check Sigtran Stack Status");
		int stage = bean.getStage();
		while (stage!=2){
			logger.info("Sigtran Stack is not initiated");
			logger.info("Waiting 10 second for recheck");
			stage = bean.getStage();
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
			
		}
		while (bean.getStack().getMap().getMapStack()== null){
			logger.info("Map Stack is not initiated");
			logger.info("Waiting 10 second for recheck");
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
		}
		this.cfg = loadConfiguration();		
		pool = new ForkJoinPool(cfg.getThreads());		
		logger.info("Starting CheckSubscriber Map Listiner");
		mapLayer = new MapLayer(bean.getStack().getMap().getMapStack(),this.cfg, this.pool);
		mapLayer.init();
		
	}
	
	 private MapLayerPreference loadConfiguration() {
	        InputStream inStream = this.getClass().getClassLoader().getResourceAsStream("settings.yml");
	        try {
	            if (inStream != null) {
	                Yaml yaml = new Yaml();
	                return (MapLayerPreference) yaml.loadAs(inStream, MapLayerPreference.class);
	            }
	        } catch (Exception ex) {
	            logger.error(ex.toString());
	        }
	        return null;
	    }
}
