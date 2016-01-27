/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.ussdgate.UssdMapLayer;
import com.unifun.sigtran.ussdgate.UssdgateThreadFactory;
import com.unifun.sigtran.ussdgate.db.FetchSettings;


public class UssdGateServletListener implements ServletContextListener {

    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-20s] ", "[UssdGateServletListener"));
	private static SigtranStackBean bean = null;
    private static UssdMapLayer ussMapLayer;
    private ExecutorService dbWorker = null;
	private Map<String, Map<String,String>> appSettings = null;
	private ExecutorService exec = Executors.newFixedThreadPool(1);
	private DataSource ds;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	logger.info("Initiating UssdGateServletListener");	
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
        if (ussMapLayer!=null){
        	ussMapLayer.stop();
        }
        if (dbWorker!=null){
        	dbWorker.shutdown();
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
				addMapListener(sce);
				contextloop = false;
				sce.getServletContext().setAttribute("ussMapLayer", ussMapLayer);
//				sce.getServletContext().setAttribute("mapPreference", cfg);
//				sce.getServletContext().setAttribute("fjpool", pool);
				if(!exec.isTerminated())
					exec.shutdown();
				break;
			}else{
				try {
					Context initContext = new InitialContext();			
					bean = (SigtranStackBean) initContext.lookup("java:comp/env/bean/SigtranObjectFactory");
					sce.getServletContext().setAttribute("sigtranStack", bean);
				} catch (NamingException e) {
					logger.error("Unable to initiate UssdGate context. \n"+e);
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
	private void addMapListener(ServletContextEvent sce) {	
		logger.info("Lookup for data Source");
		ds = lookupForDs();
		while (ds == null){
			logger.error("Unable to obtain DB Data Source");
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			logger.error("Lookup for data Source");
			ds = lookupForDs();
		}
		logger.info("Initiate dbWorker pool");
		dbWorker = Executors.newFixedThreadPool(getDbmaxActive(), new UssdgateThreadFactory("DbWorker"));
		String tableName="ussdgate_settings";
		try{
			tableName=settingsTableName();
		}catch(Exception e){
			e.printStackTrace();
			tableName="ussdgate_settings";
		}
		logger.info("Fetch setting from database, table: "+tableName);		
		FetchSettings ftSt = new FetchSettings(ds, tableName);
		try {
			appSettings = ftSt.fetchSettings();
		} catch (SQLException e) {
			logger.error("No Settings was faound in db. "+ e.getMessage());
			e.printStackTrace();
		}
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
		logger.info("Starting Ussd Map Service Listiner");
		ussMapLayer = new UssdMapLayer(bean.getStack().getMap().getMapStack(),
				bean.getStack().getTcap().getTcapStack(),
				bean.getStack().getSccp().getSccpStack());
		logger.info("Set datasource to Map Service Listiner");
		ussMapLayer.setDs(ds);
		logger.info("Set dbworker to  Map Service Listiner");
		ussMapLayer.setDbWorker(dbWorker);
		logger.info("Set ussd preference to Map Service Listiner");
		ussMapLayer.setAppSettings(appSettings);
		logger.info("Initiating Map Service Listiner");
		ussMapLayer.init();
		
	}
	
	private DataSource lookupForDs(){
		try {
			Context initContext = new InitialContext();				
			return (DataSource) initContext.lookup("java:comp/env/jdbc/UssdGateDb");
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}
	
	private int getDbmaxActive(){
		try {						
			int maxActive = ((org.apache.tomcat.jdbc.pool.DataSource)this.ds).getPoolProperties().getMaxActive();
			logger.info("DS MaxActive: "+maxActive);
			return maxActive;			
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.info("DS MaxActive: 10");
			return 10;
		}
	} 
	
	private String settingsTableName(){
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = this.getClass().getClassLoader().getResourceAsStream("settings.properties");			
			prop.load(input);		
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return (prop.getProperty("settingstable")!=null)?prop.getProperty("settingstable"):"ussdgate_settings";
	}

}
