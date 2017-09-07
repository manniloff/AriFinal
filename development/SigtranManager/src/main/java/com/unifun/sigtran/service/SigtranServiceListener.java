package com.unifun.sigtran.service;



import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.adaptor.SigtranStackBean;

public class SigtranServiceListener implements ServletContextListener {	
	static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[SigtranServiceListener"));

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.info("Stop Sigtran Stack");
		SigtranStackBean bean = (SigtranStackBean) event.getServletContext().getAttribute("sigtranStackBean");
		bean.getStack().stop();

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger.info("Initiating SigtranServiceListener");
		InputStream fstream = null;
		try {
			ServletContext sc = event.getServletContext();
			Context initContext = new InitialContext();
			Context envCtx = (Context) initContext.lookup("java:comp/env");
			SigtranStackBean bean = (SigtranStackBean) envCtx.lookup("bean/SigtranObjectFactory");
			sc.setAttribute("sigtranStackBean", bean);
			fstream = this.getClass().getClassLoader().getResourceAsStream("config.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			bean.setConfig(br);			
	        bean.addListener();	       
			bean.initializeStack();			

		} catch (NamingException  e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}finally {
			if (fstream!=null){
				try {
					fstream.close();					
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}

	}

}
