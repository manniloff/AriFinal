/**
 * 
 */
package com.unifun.sigtran.beepcall.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.beepcall.ISUPEventHandler;


/**
 * @author rbabin
 *
 */
public class BeepCallServiceListener implements ServletContextListener {
	
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-20s] ", "[BeepCallServiceListener"));
	private static SigtranStackBean bean = null;
	private ISUPEventHandler isupEventHandler;
	//private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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
		//Start Scheduler that will reset amount of request
//		logger.debug("Start Request amount scheduler");
//		final Runnable resetCounterTask = new Runnable() {
//		       public void run() { 
//		    	   loadIsupListiner();
//		    	   }
//		     };
//		try{
//		     scheduler.scheduleAtFixedRate(resetCounterTask, 1, 1, TimeUnit.SECONDS);
//		}catch (Exception e){
//			e.printStackTrace();
//			logger.error(e.getMessage());
//		}
		
		
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (isupEventHandler!=null){
			isupEventHandler.destroy();
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
				addIsupEventHandler();
				contextloop = false;
				sce.getServletContext().setAttribute("isupEventHandler", isupEventHandler);
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
	private void addIsupEventHandler() {		
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
		while (bean.getStack().getIsup().getStack() == null){
			logger.info("ISUP Stack is not initiated");
			logger.info("Waiting 10 second for recheck");
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			}
		}
		logger.info("Starting BeepCall Service Listiner");
		isupEventHandler = new ISUPEventHandler(bean.getStack().getIsup().getStack());
		isupEventHandler.init();
		
	}


}
