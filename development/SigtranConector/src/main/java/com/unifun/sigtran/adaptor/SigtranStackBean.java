package com.unifun.sigtran.adaptor;

import java.io.BufferedReader;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.util.StackPreference;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 * 
 * <GlobalNamingResources>
 * <Resource auth="Container" 
 * 	factory="com.unifun.sigtran.adaptor.SigtranObjectFactory" 
 * 	name="shared/bean/SigtranObjectFactory" 
 * 	type="com.unifun.sigtran.adaptor.SigtranStackBean"
 * 	workerThreads="16"
 * 	configPath="../logs/cfg"
 *  log4jcfg="../conf/log4j.properties"
 *  logbackcfg="../conf/logback.xml"
 *  />
 */
public class SigtranStackBean implements LifecycleListener {
	private static final String CLASS_NAME = "com.unifun.sigtran.adaptor.SigtranStackBean";
	private static SigtranStackBean bean = null;
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SigtranStackBean"));	
	private StackPreference stack;	
	private BufferedReader config;
	/*
     Indicates current status: 0 - created, 1 - AFTER_START_EVENT received, 2 - SCtpServer loaded
	 */
	private int stage;


	public SigtranStackBean(String workerThreads, String configPath) {
		logger.info("Loading SigtranStack");
		logger.info(String.format("workerThreads: %s, configPath %s ", workerThreads, configPath));
		stage = 0;
		this.stack = new StackPreference(Integer.parseInt(workerThreads), configPath);
	}  
	
	public void intiLoggers(String log4jcfg, String logbackcfg){
		//logger.debug(this.cfgPath + " loaded.");
        PropertyConfigurator.configure(log4jcfg);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(logbackcfg);
        } catch (JoranException je) {
            je.printStackTrace();
        }        
	}


	public void initializeStack() throws Exception{		
		if(config!=null){
			logger.info("Set stack configuration file");
			this.stack.setStackConfiguration(config);
		}
		logger.info("initializeing Stack");
		this.stack.initializeStack();			
		stage =2;
	}
	
	public void shutDownSctp() {
		if (stack != null) {
			stack.stop();			
		}
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (null != event.getType()) {
			switch (event.getType()) {
			case Lifecycle.AFTER_START_EVENT:
				/*                    startReceived = true;
                    Logger.getLogger(CLASS_NAME).log(Level.INFO, "Lifecycle.AFTER_START_EVENT");
                    stage = 1;
                    startSctp();
                    stage = 2;*/
				break;
			case Lifecycle.AFTER_STOP_EVENT:
				logger.info("Lifecycle.AFTER_STOP_EVENT");
				shutDownSctp();
				break;
			default:
				logger.info(event.getType()+ " Lifecycle EVENT Ocured");
				break;
			}
		}
	}

	/**
	 * @return the stack
	 */
	public StackPreference getStack() {
		return stack;
	}

	public void addListener() {
		MBeanServer mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
		ObjectName name;
		try {
			name = new ObjectName("Catalina", "type", "Server");
			Server server = (Server) mBeanServer.getAttribute(name, "managedResource");
			server.addLifecycleListener(this);
		} catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException ex) {
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * @return the stage
	 */
	public int getStage() {
		return stage;
	}

	public void setConfig(BufferedReader conf) {
		this.config = conf;
	}


}
