package com.unifun.sigtran.smsgate.service;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.smsgate.MapLayer;
import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.DataBaseLayer;
import com.unifun.sigtran.smsgate.hibernate.models.GateWaySettings;
import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.ServerController;


public class SmsGateServletListener implements ServletContextListener {

	private static SmsGateWay smsGateWay;
	private static MapLayer mapLayer;
	private static DataBaseLayer dbl;
	private ExecutorService dbWorker = null;
	private static SigtranStackBean bean = null;
	
	private Map<String, Map<String,String>> appSettings = null;
	private ExecutorService exec = Executors.newFixedThreadPool(1);
	
	private static final Logger logger = LogManager.getLogger(SmsGateServletListener.class);
//	private static SigtranStackBean bean = null;
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("Initiating SmsGateServletListener");
		
		Map<String, String> mysqlConfig = loadDBConfiguration();
		logger.info("Lookup for data Source");
		dbl = new DataBaseLayer(mysqlConfig);
		dbl.initHibernate();
		logger.info("Fetch setting from database");		
		List<GateWaySettings> settings = dbl.getGateWaySetting();
		if(!settings.isEmpty()) {
			appSettings = smsGateWay.fetchSettings(settings);
			loadGateWayConfig(sce);
			smsGateWay.init();
			exec.submit(() -> {
					loadMapListiner(sce);
					smsGateWay.MapWorkers(true);
			});
			loadSmppClient(sce);
			loadSmppServer(sce);
			if(smsGateWay.getSmppClientController() != null) {
				smsGateWay.getSmppClientController().initSmppClient();	
			}
			if(smsGateWay.getSmppServerController() != null) {
				smsGateWay.getSmppServerController().initSmppServers();	
			}
			sce.getServletContext().setAttribute("smsGateWay", smsGateWay);
		} else
			logger.error("configuration list is empty");
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		//stop map workers
		smsGateWay.MapWorkers(false);
		smsGateWay.stop();
		//stop smpp servers
		if(smsGateWay.getSmppServerController() != null) {
			smsGateWay.getSmppServerController().stopSmppServers();
		}
		//stop smpp clients	
		if(smsGateWay.getSmppClientController() != null) {
			smsGateWay.getSmppClientController().stopSmppClient();
		}
		//stop mapLayer
		if(smsGateWay.getMapLayer() != null) {
			smsGateWay.getMapLayer().stop();
		}
		//stop hibernate
		smsGateWay.getDbl().stop();
		exec.shutdown();
	}

	private void loadGateWayConfig(ServletContextEvent sce) {
		smsGateWay = new SmsGateWay(appSettings, dbl);
	}

	private void loadSmppServer(ServletContextEvent sce) {
		try {
			Map<String, String> serverControllerConfig = appSettings.get("serverConrollerConfig");
			int threadPoolSize = Integer.valueOf(serverControllerConfig.get("threadPoolSize"));
			int sendUntil = Integer.valueOf(serverControllerConfig.get("sendDLRUntilInMin"));
			int checkSMPPAccessList = Integer.valueOf(serverControllerConfig.get("checkSMPPAccessList"));
			int dlrReSendMaxAttempts = Integer.valueOf(serverControllerConfig.get("dlrReSendMaxAttempts"));
			int dlrReSendIntervalAttemptSec = Integer.valueOf(serverControllerConfig.get("dlrReSendIntervalAttemptSec"));
			int dlrSendSpeedPerSec = Integer.valueOf(serverControllerConfig.get("dlrSendSpeedPerSec"));
			ServerController sc = new ServerController(threadPoolSize, sendUntil
					, checkSMPPAccessList, dlrReSendMaxAttempts, dlrReSendIntervalAttemptSec, dlrSendSpeedPerSec);
			smsGateWay.setSmppServerController(sc);	
		} catch (Exception e) {
			logger.warn("Could not load smpp server Info");
			logger.error(e.getStackTrace());
		}
	}

	private void loadSmppClient(ServletContextEvent sce) {
		try {
			Map<String, String> clientControllerConfig = appSettings.get("clientConrollerConfig");
			int threadPoolSize = Integer.valueOf(clientControllerConfig.get("threadPoolSize"));
			int checkDLRWaitingList = Integer.valueOf(clientControllerConfig.get("checkDLRWaitingList"));
			int defaultSmsLiveTime = Integer.valueOf(clientControllerConfig.get("defaultSmsLiveTime"));
			int reSendSubmitPerSec = Integer.valueOf(clientControllerConfig.get("reSendSubmitPerSec"));
			int increaseWaitDLRFromSMSC = Integer.valueOf(clientControllerConfig.get("increaseWaitDLRFromSMSC"));
			ClientController cController = new ClientController(threadPoolSize, checkDLRWaitingList, defaultSmsLiveTime, reSendSubmitPerSec
					, increaseWaitDLRFromSMSC);
			smsGateWay.setSmppClientController(cController);	
		} catch (Exception e) {
			logger.warn("Could not load smpp client Info");
			logger.error(e.getStackTrace());
		}
	}
	
	private void loadMapListiner(ServletContextEvent sce) {
		logger.info("Lookup for SigtranObjectFactory in JNDI");
		boolean contextloop = true;
		while (contextloop){
			if (bean != null){
				logger.info("Trying to append Map Listiner");
				//Initiate map listiner
				addMapListener(sce);
				contextloop = false;
				sce.getServletContext().setAttribute("smsMapLayer", mapLayer);
				smsGateWay.setMapLayer(mapLayer);
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
					logger.info("sigtranStack was setted");
				} catch (NamingException e) {
					logger.error("Unable to initiate SmsGateWay context. \n"+e);
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
		logger.info("Starting SMS Map Service Listiner");
		mapLayer = new MapLayer(bean.getStack().getMap().getMapStack(),
				bean.getStack().getTcap().getTcapStack(),
				bean.getStack().getSccp().getSccpStack());
//		logger.info("Set datasource to Map Service Listiner");
		logger.info("Set dbworker to  Map Service Listiner");
		mapLayer.setDbWorker(dbWorker);
		logger.info("Set sms preference to Map Service Listiner");
		mapLayer.setAppSettings(appSettings);
//		ussMapLayer.setAppSettings(appSettings);
		logger.info("Initiating Map Service Listiner");
		mapLayer.init();
	}
	
//	private Map<String, Map<String, String>> fetchSettings(List<GateWaySettings> settings) {
//		Map<String, Map<String, String>> preference = new HashMap<>();
//		Set<String> types = null;
//		types = settings.stream().collect(Collectors.groupingBy(GateWaySettings::getType)).keySet();
//		if(types != null) {
//			types.forEach(type -> {
//				Map<String, String> params = new HashMap<>();
//				settings.forEach(row -> {
//					if(row.getType().equals(type)) {
//						params.put(row.getName(), row.getValue());
//					}
//				});
//				preference.put(type, params);
//			});	
//		}
//		return preference;
//	}

	private Map<String, String> loadDBConfiguration() {
		InputStream inStream = null;
		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			inStream = classloader.getResourceAsStream("settings.txt");
			StringWriter writer = new StringWriter();
			IOUtils.copy(inStream, writer);
			String theString = writer.toString();
			String[] rows = theString.split("\n");
			Map<String, String> result = new HashMap<String, String>();
			for (int i = 0; i < rows.length; i++) {
				String[] temp = rows[i].split(": ");
				if(temp.length != 2)
					logger.error("WRONG config info for row - " + i);
				else {
					result.put(temp[0].trim(), temp[1].trim());
				}
			}
			return result;
		} catch (Exception ex) {
			logger.error(ex.toString());
		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
				logger.error("Unable to close FileInputStream: " + e);
			}
		}
		return null;
	}
}
