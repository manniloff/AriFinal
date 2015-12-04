/**
 * 
 */
package com.unifun.sigtran.beepcall.controler;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author rbabin
 *
 */
@WebServlet(name = "IsupBeep", urlPatterns = {"/beep"}, displayName = "Unifun Isup beepme", asyncSupported = true)
public class HttpControler extends HttpServlet {	
	private static final long serialVersionUID = 3030677466151475919L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}
	
	private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
		AsyncContext aContext = req.startAsync(req, resp);
		AppWorker appWorker = new AppWorker();
		appWorker.setAsyncContext(aContext);
		aContext.setTimeout(60000);
		aContext.addListener(new AsyncContextListiner());
		aContext.start(appWorker);
	}

}
