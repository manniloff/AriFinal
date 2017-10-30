/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.checksubscriber.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
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
import com.unifun.sigtran.checksubscriber.MapLayer;
import com.unifun.sigtran.checksubscriber.utils.FetchSettings;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class CheckSubscriberServletListener implements ServletContextListener {

    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-20s] ", "[CheckSubscriberServletListener"));
	private static SigtranStackBean bean = null;
    private static MapLayer mapLayer;
    private ExecutorService exec = Executors.newFixedThreadPool(1);
    private ExecutorService pool ;
    private DataSource ds;	
	private ExecutorService dbWorker = null;
	private Map<String, Map<String,String>> appSettings = null;

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
        this.dbWorker.shutdown();
    }

	/**
	 * 
	 */
	private void loadMapListiner(ServletContextEvent sce) {
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
			if(Thread.currentThread().isInterrupted()){
				break;
			}
		}
		logger.info("Initiate dbWorker pool");
		dbWorker = Executors.newFixedThreadPool(getDbmaxActive());
		String tableName="settings";
		try{
			tableName=settingsTableName();
		}catch(Exception e){
			e.printStackTrace();
			tableName="ussdgate_settings";
		}
		logger.info("Fetch setting from database");
		FetchSettings ftSt = new FetchSettings(ds, tableName);
		try {
			appSettings = ftSt.fetchSettings();
		} catch (SQLException e) {
			logger.error("No Settings was faound in db. "+ e.getMessage());
			e.printStackTrace();
		}
		
		logger.info("Lookup for SigtranObjectFactory in JNDI");
		boolean contextloop = true;
		while (contextloop){
			if (bean != null){
				logger.info("Trying to append Map Listiner");
				//Initiate map listiner for usssd messages types
				addMapListener();
				contextloop = false;
				sce.getServletContext().setAttribute("mapLayer", mapLayer);
				sce.getServletContext().setAttribute("mapPreference", appSettings);
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
					logger.error("Unable to initiate SigtranStackBean context. \n"+e);
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
			if(Thread.currentThread().isInterrupted()){
				break;
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
			if(Thread.currentThread().isInterrupted()){
				break;
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
			if(Thread.currentThread().isInterrupted()){
				break;
			}
		}
		while (bean.getStack().getSccp().getSccpStack()== null){
			logger.info("Sccp Stack is not initiated");
			logger.info("Waiting 10 second for recheck");
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
			if(Thread.currentThread().isInterrupted()){
				break;
			}
		}				
		pool = Executors.newFixedThreadPool(Integer.parseInt(appSettings.get("app").get("threads")));		
		logger.info("Starting CheckSubscriber Map Listiner");
		mapLayer = new MapLayer(bean.getStack().getMap().getMapStack(),bean.getStack().getSccp().getSccpStack(),this.appSettings, this.pool);
		while(!mapLayer.init()){
			logger.error("Unable to init MAP");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(Thread.currentThread().isInterrupted()){
				break;
			}
		}
		
	}
		 
		private DataSource lookupForDs(){
			try {
				Context initContext = new InitialContext();				
				//return (DataSource) initContext.lookup("java:comp/env/jdbc/UssdGateDb");
				return (DataSource) initContext.lookup("java:comp/env/jdbc/CheckSub");
				
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
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
				e.printStackTrace();
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
