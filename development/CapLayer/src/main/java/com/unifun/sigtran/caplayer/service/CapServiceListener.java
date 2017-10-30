/**
 * 
 */
package com.unifun.sigtran.caplayer.service;

import java.sql.SQLException;
import java.util.Map;
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
import com.unifun.sigtran.caplayer.persistence.FetchSettings;


/**
 * @author rbabin
 *
 */
public class CapServiceListener implements ServletContextListener {
	
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-20s] ", "[BeepCallServiceListener"));
	private static SigtranStackBean bean = null;
	//private ISUPEventHandler isupEventHandler;
	private DataSource ds;	
	private ExecutorService dbWorker = null;
	private Map<String, Map<String,String>> appSettings = null;
	private ExecutorService exec = Executors.newFixedThreadPool(1);
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("Initiating BeepCallServiceListener");	
		final Runnable resetCounterTask = new Runnable() {
	       public void run() { 
	    	   loadIsupListiner(sce);
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
//		if (isupEventHandler!=null){
//			isupEventHandler.destroy();
//		}
		this.exec.shutdown();
		if (dbWorker!=null){
			dbWorker.shutdown();
		}
		
	}
	
	/**
	 * 
	 */
	private void loadIsupListiner(ServletContextEvent sce) {
		logger.info("Lookup for SigtranObjectFactory in JNDI");
		boolean contextloop = true;
		while (contextloop){
			if (bean != null){
				logger.info("Trying to append ISUP event handler");
				//Initiate map listiner for usssd messages types
				addIsupEventHandler(sce);
				contextloop = false;
//				sce.getServletContext().setAttribute("isupEventHandler", isupEventHandler);				
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
	private void addIsupEventHandler(ServletContextEvent sce) {		
//		logger.info("Lookup for data Source");
//		ds = lookupForDs();
//		while (ds == null){
//			logger.error("Unable to obtain DB Data Source");
//			try {
//				TimeUnit.MILLISECONDS.sleep(500);
//			} catch (InterruptedException e) {
//				logger.error(e.getMessage());
//				e.printStackTrace();
//			}
//			logger.error("Lookup for data Source");
//			ds = lookupForDs();
//		}
//		logger.info("Initiate dbWorker pool");
//		dbWorker = Executors.newFixedThreadPool(getDbmaxActive());
//		logger.info("Fetch setting from database");
//		FetchSettings ftSt = new FetchSettings(ds);
//		try {
//			appSettings = ftSt.fetchSettings();
//		} catch (SQLException e) {
//			logger.error("No Settings was faound in db. "+ e.getMessage());
//			e.printStackTrace();
//		}
//		
//		logger.info("Check Sigtran Stack Status");
//		int stage = bean.getStage();
//		while (stage!=2){
//			logger.info("Sigtran Stack is not initiated");
//			logger.info("Waiting 10 second for recheck");
//			stage = bean.getStage();
//			try {
//				TimeUnit.SECONDS.sleep(10);
//			} catch (InterruptedException e1) {
//				logger.error(e1.getMessage());
//				e1.printStackTrace();
//			}
//			
//		}
//		while (bean.getStack().getIsup().getStack() == null){
//			logger.info("ISUP Stack is not initiated");
//			logger.info("Waiting 10 second for recheck");
//			try {
//				TimeUnit.SECONDS.sleep(10);
//			} catch (InterruptedException e1) {
//				logger.error(e1.getMessage());
//				e1.printStackTrace();
//			}
//		}
//		logger.info("Starting BeepCall Service Listiner");
//		isupEventHandler = new ISUPEventHandler(bean.getStack().getIsup().getStack());
//		logger.info("Set datasource to IsupEventHandler");
//		isupEventHandler.setDs(ds);
//		logger.info("Set dbworker to IsupEventHandler");
//		isupEventHandler.setDbWorker(dbWorker);
//		logger.info("Set isup preference to IsupEventHandler");
//		isupEventHandler.setIsupPreference(appSettings.get("isup"));
//		logger.info("Initiating IsupEventHandler");
//		isupEventHandler.init();
		
	}
	
	private DataSource lookupForDs(){
		try {
			Context initContext = new InitialContext();				
			return (DataSource) initContext.lookup("java:comp/env/jdbc/BeepCallSig");
			
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


}
