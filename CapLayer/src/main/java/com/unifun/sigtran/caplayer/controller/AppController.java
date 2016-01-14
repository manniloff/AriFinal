//package com.unifun.sigtran.caplayer.controller;
//
//import java.io.IOException;
//
//import javax.servlet.AsyncContext;
//import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
////http://127.0.0.1:7080/BeepCall/controller?action=channelsstatus
////http://127.0.0.1:7080/BeepCall/controller?action=resetallchannels
////http://127.0.0.1:7080/BeepCall/controller?action=resetchannel&cic=1&dpc=6659
////http://127.0.0.1:7080/BeepCall/controller?action=unblock&cic=1&dpc=6659
//
//@WebServlet(name = "AppController", urlPatterns = {"/controller"}, displayName = "Unifun Isup App Controller", asyncSupported = true)
//public class AppController extends HttpServlet{
//	private static final long serialVersionUID = 2153121655669831003L;
//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		processRequest(req, resp);
//	}
//
//	@Override
//	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		processRequest(req, resp);
//	}
//	
//	private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
//		AsyncContext aContext = req.startAsync(req, resp);
//		AppControllerWorker appWorker = new AppControllerWorker();
//		appWorker.setAsyncContext(aContext);
//		aContext.setTimeout(60000);
//		aContext.addListener(new AsyncContextListiner());
//		aContext.start(appWorker);
//	}
//}
