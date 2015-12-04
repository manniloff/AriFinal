/**
 * 
 */
package com.unifun.sigtran.beepcall.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.beepcall.ISUPEventHandler;


/**
 * @author rbabin
 *
 */
public class AppWorker implements Runnable{
	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", AppWorker.class.getSimpleName(), ""));
	private AsyncContext asyncContext;	
	private HashMap<String, String> params = new HashMap<>();
	private PrintWriter out;
	private SigtranStackBean sigtranStack;
	private ISUPEventHandler isupEventHandler;

	@Override
	public void run() {
		asyncContext.getResponse().setContentType("text/plain;charset=UTF-8");
		//Load parameters into hashmap
		Enumeration<String> parameterNames = asyncContext.getRequest().getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String paramName = parameterNames.nextElement();
			for (String value : asyncContext.getRequest().getParameterValues(paramName)) {                
				params.putIfAbsent(paramName, value);
			}
		}
		//setup printer
		try {
			setOut(asyncContext.getResponse().getWriter());
		} catch (IOException e1) {			
			logger.error(e1.getMessage());
			e1.printStackTrace();
			closeResource();
			return;
		}
		//lookup for sigtranstack and isup handler
		sigtranStack = (SigtranStackBean) asyncContext.getRequest().getServletContext().getAttribute("sigtranStack");
		isupEventHandler = (ISUPEventHandler) asyncContext.getRequest().getServletContext().getAttribute("isupEventHandler");
		if (sigtranStack==null || isupEventHandler == null){
			out.print("{\"Status\":\"-1\", \"Error\":\"Unable to obtain sigtran stack or isup event handler\"}");
			closeResource();
			return;
		}
		//Validate Parameters
		if(!params.containsKey("msisdnA")){
			out.print("{\"Status\":\"-1\", \"Error\":\"Missing msisdnA parameter\"}");
			closeResource();
			return;
		}
		if(!params.containsKey("msisdnB")){
			out.print("{\"Status\":\"-1\", \"Error\":\"Missing msisdnB parameter\"}");
			closeResource();
			return;
		}
		//Validate Parameters Value
		params.keySet().forEach((value) -> {
			if (!params.get(value).matches("[0-9]+")){
				out.print(String.format("{\"Status\":\"-1\", \"Error\":\"Invalid number: %s in parameter %s \"}",params.get(value), value ));
				closeResource();
				return;
			}
		});
		try {
			isupEventHandler.sendIAM(params.get("msisdnA"), params.get("msisdnB"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.print(String.format("{\"Status\":\"0\", \"Message\":\"Operation result will be writing in database \"}"));
		closeResource();
	}
	
	private void closeResource(){
		asyncContext.complete();
	}
	public void setAsyncContext(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	public PrintWriter getOut() {
		return out;
	}

	public void setOut(PrintWriter out) {
		this.out = out;
	}

}
