package com.unifun.sigtran.adaptor;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
public class SigtranObjectFactory implements ObjectFactory {    
    private static SigtranStackBean bean = null;
    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SigtranObjectFactory"));

    
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {        
    	if(bean != null) return bean;        
        logger.info("Loading SigtranObjectFactory.");
        logger.info("Initiating SigtranConnectorBean");
        String workerThreads = "16";
    	String configPath = "../temp";
    	String log4jcfg = "../conf/log4j.properties";
    	String logbackcfg = "../conf/logback.xml";
    	Reference ref = (Reference) obj;
        Enumeration attrs = ref.getAll();
        while (attrs.hasMoreElements()) {
            RefAddr attr = (RefAddr) attrs.nextElement();
            String attrName = attr.getType();
            String attrValue = (String) attr.getContent();
            if (attrName.equals("workerThreads")) {
            	workerThreads = attrValue;
            }
            if (attrName.equals("log4jcfg")) {
            	log4jcfg = attrValue;
            }
            if (attrName.equals("configPath")) {
            	configPath = attrValue;
            }
            if (attrName.equals("logbackcfg")) {
            	logbackcfg = attrValue;
            }
        }
        bean = new SigtranStackBean(workerThreads,configPath);
        bean.intiLoggers(log4jcfg, logbackcfg);
        return (bean);
    }
  

}
