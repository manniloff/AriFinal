package com.unifun.sigtran.smsgate.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Server;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.m3ua.RouteAs;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.smsgate.MapLayer;
import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.enums.Direction;
import com.unifun.sigtran.smsgate.smpp.ClientController;
import com.unifun.sigtran.smsgate.smpp.ServerController;
import com.unifun.sigtran.smsgate.smpp.client.SmppClient;
import com.unifun.sigtran.stack.SccpUnifunStackWrapper;

@WebServlet(name = "SmscGateWayServlet", urlPatterns = {"/smscgw"}, displayName = "SmscGateWayServlet", asyncSupported = true)
public class SmscGateWayServlet extends HttpServlet {

	private static final long serialVersionUID = -7890015957201168169L;
	private static final Logger logger = Logger.getLogger(SmscGateWayServlet.class);
	
	private MapLayer mapLayer;
	private SigtranStackBean sigStack;
	private SmsGateWay smsGateWay;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		this.sigStack = (SigtranStackBean) req.getServletContext().getAttribute("sigtranStack");
		if (sigStack==null){
			throw new ServletException("Unable to obtain SigtranStack");
		}
		this.mapLayer = (MapLayer)req.getServletContext().getAttribute("smsMapLayer");
		if (mapLayer==null){
			throw new ServletException("Unable to obtain smsMapLayer");
		}
		this.smsGateWay = (SmsGateWay)req.getServletContext().getAttribute("smsGateWay");
		if (smsGateWay==null){
			throw new ServletException("Unable to obtain smsGateWay");
		}
		
		resp.setContentType("text/plain");
		resp.setStatus(HttpServletResponse.SC_OK);
		String action = req.getParameter("action");		 
		switch(action){
		case "m3uastatus":
			m3uaStatus(req, resp, action);
			break;
		case "m3uainfo":
			m3uaInfo(req, resp, action);
			break;
		case "mtpstatus":
			mtpStatus(req, resp, action);
			break;
		case "sctpstatus":
			sctpStatus(req, resp, action);
			break;
		case "maintanacemode":
			maintanaceMode(req, resp, action);
			break;
		case "startMapWorker":
			mapWorkers(resp, true, action);
			break;
		case "stopMapWorker":
			mapWorkers(resp, false, action);
			break;
		case "checkSigtranQueues":
			checkQueues(resp, true);
			break;
		case "checkSMPPQueues":
			checkQueues(resp, false);
			break;
		case "report":
			report(resp);
			break;
		case "moveToArchive":
			moveToArchive(resp);
			break;
		case "checkSMPPLayer":
			checkSMPPLayer(resp);
			break;
		case "resetClinet":
			resetClinet(req, resp);
			break;
		case "blackList":
			blackListManipulation(req, resp);
			break;
		case "reloadGateWayConfig":
			reLoadGateWayConfig(req, resp);
			break;
		default:
			resp.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}",action));
			break;        		 
		}
	}
	private void mapWorkers(HttpServletResponse response, boolean start, String action) throws IOException {
		if(start) {
			response.getWriter().println("Starting workers...\n\r");
			smsGateWay.MapWorkers(true);
			response.getWriter().println("workers started...\n\r");
		} else {
			response.getWriter().println("Stopping workers...\n\r");
			smsGateWay.MapWorkers(false);
			response.getWriter().println("workers stopped...\n\r");
		}
	}
	private void moveToArchive(HttpServletResponse response)  throws IOException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.currentThread().setName("_THREAD-MoveToArchive");
					long started = System.currentTimeMillis();
					logger.info("MoveToArchive started...");
					SmsGateWay.getDbl().moteToArchive();
					logger.info("MoveToArchive ended... Duration - " + (System.currentTimeMillis() - started));
					SmsGateWay.getMapLayer().ClearCounter();
					logger.info("Map Counters Cleared...");
				} catch (Exception e) {
					logger.error("Processing MoveToArchive handled error: " + e.getMessage());
				}
			}
		}).start();
		response.getWriter().println("MoveToArchive started...");	
	}
	private void blackListManipulation(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String msisdn = request.getParameter("msisdn");
		String direction = request.getParameter("direction") == null ? "ANY" : request.getParameter("direction").toUpperCase();
		String manipulation = request.getParameter("add");
		if(msisdn == null || manipulation == null) {
			response.getWriter().println("msisdn and add are mandatory");
			return;
		}
		try {
			long number = Long.valueOf(msisdn);
			boolean add = false;
			Direction _direction = Direction.valueOf(direction);
			if("1".equals(manipulation)) {
				add = true;
			} else if("0".equals(manipulation)){
				add = false;
			} else {
				response.getWriter().println("add must be in {1 || 0}, received " + add);
			}
			if(smsGateWay.blackListManipulations(number, _direction, add)) {
				response.getWriter().println("Request successfully processed");
			} else {
				response.getWriter().println("Could not process request");
			}
		} catch (NumberFormatException e) {
			response.getWriter().println("msisdn in wrong format - " + msisdn);
		} catch (IllegalArgumentException e) {
			response.getWriter().println("direction must be in {MT || MO || ANY}, received " + direction);
		}
	}
	private void checkQueues(HttpServletResponse response, boolean isSigtranCheck) throws IOException {
		String result = "";
		if(isSigtranCheck) {
			result = String.format("Current_Dialogs: %d , DBQueue: %d "
					+ ", AlertQueue: %d , NextAttemptQueue: %d , %s"
					, SmsGateWay.getMapLayer().getTCAPStack().getCounterProvider().getCurrentDialogsCount()
					, SmsGateWay.getDbl().getDBQueue()
					, SmsGateWay.getAlertLists().size()
					, SmsGateWay.getNextAttemptList().size()
					, SmsGateWay.getMapLayer().getMapLayerQueue(false, 1));	
		} else {
			if(smsGateWay.getSmppClientController() != null) {
				result = "SMPPReSendQueue size: " + ClientController.getSmppMTQueue().size();
				int queueSize = 0;
				for (SmppClient client : ClientController.getClients().values()) {
					queueSize += client.getSmsToSend().size();
				}
				result += ", SMPPClientQueueSize:" + " " + queueSize;	
			} else {
				result = "Client is not set up!";
			}
		}
		response.getWriter().println(result);	
	}
	private void report(HttpServletResponse response) throws IOException {
		String result = "\nCurrent Dialogs - " + SmsGateWay.getMapLayer().getTCAPStack().getCounterProvider().getCurrentDialogsCount()
				+ ". Max Dialogs - " + SmsGateWay.getMapLayer().getTCAPStack().getMaxDialogs()
				+ "\n oInvokeTimeout - " + SmsGateWay.getMapLayer().getTCAPStack().getInvokeTimeout()
				+ ". DialogTimeout - " + SmsGateWay.getMapLayer().getTCAPStack().getDialogIdleTimeout() + "\n";
		result += SmsGateWay.getMapLayer().getMapLayerQueue(false, 0);	
		
		response.getWriter().println(result);
	}
	private void reLoadGateWayConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if(SmsGateWay.reLoadGatewayConfig())
			response.getWriter().println("config reloaded!");
		else
			response.getWriter().println("Could not reload config!!!");
	}
	
	private void maintanaceMode(HttpServletRequest request, HttpServletResponse response,
			String action) throws IOException {		
		String mode = request.getParameter("mode");
		PrintWriter out = response.getWriter();
		logger.info(String.format("Reciving http Maintenance Mode request from %s:%d, mode requested: %s", request.getRemoteAddr(),request.getRemotePort(),mode));
		//com.unifun.sigtran.stack.Map map = ussdGateway.getSctpServer().getMap();
		
//		switch (mode) {
//		case "start":
//			this.mapLayer.setMaintenancemode(true);
//			out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
//					action,"Enable maintenance mode"));
//			break;
//		case "stop":
//			this.mapLayer.setMaintenancemode(false);
//			out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
//					action,"Disable maintenance mode"));
//			break;
//		case "status":
//			out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"mode\":\"%s\"}",
//					action,(this.mapLayer.isMaintenancemode())?"Started":"Stoped"));
//			break;
//		default:
//			out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
//					action,"Unsuported Mode"));
//			break;
//		}
	}
	
	private void m3uaStatus(HttpServletRequest request, HttpServletResponse response, String action) throws IOException{
		try{        		
			synchronized (this.sigStack.getStack().getM3ua()) {
				boolean m3uaIsStarted = this.sigStack.getStack().getM3ua().isM3UAManagementStarted();
				// curentDialogCount = this.ussdGateway.getSctpServer().getMap().getCurentDialogCount();
				response.setHeader("m3uastatus", (m3uaIsStarted)?"started":"stoped");
				response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"m3uastatus\":\"%s\"}",
						action,(m3uaIsStarted)?"started":"stoped"));
			}        		
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void m3uaInfo(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
		try {
			List<String> tmp = new ArrayList<>();
			response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",",
					action));
			M3UAManagement m3uaserver = this.sigStack.getStack().getM3ua().getServerM3UAMgmt();
			String m3uaservername= m3uaserver.getName();
			response.getWriter().println("\"m3ua\":\""+m3uaservername+"\",\"as\":");
			List<As> as = m3uaserver.getAppServers();
			logger.debug("as");
			for (As a: as){
				StringBuffer asStringBuff = new StringBuffer();
				long na = 0;
				String ri = null;
				try {
					na = a.getNetworkAppearance().getNetApp();
				} catch (Exception e) {}
				try {
					//ri = a.getRoutingContext().getRoutingContexts();
				} catch (Exception e) {
				}

				asStringBuff.append(String.format("{\"name\":\"%s\","
						+ "\"functionality\":\"%s\","
						+ "\"exchangeType\":\"%s\","
						+ "\"ipspType\":\"%s\","
						//+ "\"routingContext\":\"%s\","
						+ "\"trafficModeType\":\"%d\","
						+ "\"networkApparence\":\"%d\","
						+ "\"ApplicationServerPreference\":"        			 		
						, a.getName(),
						a.getFunctionality().name(),
						a.getExchangeType().name(),
						a.getIpspType().name(),        					
						ri,
						a.getTrafficModeType().getMode(),
						na));
				List<Asp> aspList =  a.getAspList();
				List<String> jsonAsp = new ArrayList<>();
				logger.debug("asp");
				for (Asp asp : aspList){
					jsonAsp.add(String.format("{"
							+ "\"name\":\"%s\","
							+ "\"associationName\":\"%s\","
							+ "\"state\":\"%s\""
							+ "}", 
							asp.getName(),
							asp.getAspFactory().getAssociation().getName(),
							asp.getState().getName()));
				}        			 
				asStringBuff.append(String.format("%s}",jsonAsp.toString()));        			 
				tmp.add(asStringBuff.toString());
			}        		 
			response.getWriter().println(String.format("%s,\"routes\":",tmp.toString()));
			Map<String, RouteAs> routes = m3uaserver.getRoute();
			tmp.clear();
			for(String s: routes.keySet()){        			 
				tmp.add(String.format("{"
						+ "\"route\":\"%s\""        			 		
						+ "}",s));
			}
			response.getWriter().println(String.format("%s}",tmp.toString()));
			tmp.clear();
		} catch (Exception e) {				
			e.printStackTrace();
		}
	}
	private void mtpStatus(HttpServletRequest request, HttpServletResponse response, String action){
		List<String> tmp = new ArrayList<>();
		try{
			response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"mtpstatus\":",
					action));
			for(Entry<Long, String> e : ((SccpUnifunStackWrapper)this.sigStack.getStack().getSccp().getSccpStack()).getMtpstatus().entrySet()){
				tmp.add(String.format("{\"PC\":\"%d\",\"STATUS\":\"%s\"}",e.getKey(),e.getValue())); 
			}

			response.getWriter().println(String.format("%s}",tmp.toString()));        		  				        		
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void sctpStatus(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
		List<String> tmp = new ArrayList<>();
		try{
			List<Server> servers =  this.sigStack.getStack().getSctp().getSctpManagement().getServers();
			Map<String, Association> associations = this.sigStack.getStack().getSctp().getSctpManagement().getAssociations();
			response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"sctpassociationsstatus\":",
					action));
			for(String associationName : associations.keySet()){
				//AssociationImpl asso = (AssociationImpl)associations.get(associationName);
				Association asso = associations.get(associationName);
				if ("SERVER".equalsIgnoreCase(asso.getAssociationType().name())){
					String serveripAddress = null; 
					int serverport= 0; 
					for (Server s : servers){
						if (s.getAssociations().contains(associationName)){
							serveripAddress = s.getHostAddress();
							serverport = s.getHostport();
							break;
						}
					}        				  
					tmp.add(String.format("{\"Name\":\"%s\",\"hostAddress\":\"%s\",\"hostPort\":\"%d\",\"peerAddress\":\"%s\",\"peerPort\":\"%d\",\"status\":\"%s\",\"extraHost:\":\"%s\",\"type:\":\"%s\"}",associationName, serveripAddress,
							serverport, asso.getPeerAddress(), asso.getPeerPort(), (asso.isUp())?"UP":"DOWN", (asso.getExtraHostAddresses()!=null)?asso.getExtraHostAddresses().toString():"", asso.getAssociationType()));            		 
				}else
					tmp.add(String.format("{\"Name\":\"%s\",\"hostAddress\":\"%s\",\"hostPort\":\"%d\",\"peerAddress\":\"%s\",\"peerPort\":\"%d\",\"status\":\"%s\",\"extraHost:\":\"%s\",\"type:\":\"%s\"}",associationName, asso.getHostAddress(),
							asso.getHostPort(), asso.getPeerAddress(), asso.getPeerPort(), (asso.isUp())?"UP":"DOWN", (asso.getExtraHostAddresses()!=null)?asso.getExtraHostAddresses().toString():"", asso.getAssociationType())); 
			}
			response.getWriter().println(String.format("%s}",tmp.toString()));
			tmp.clear();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void checkSMPPLayer(HttpServletResponse response) throws IOException {
		response.getWriter().println(String.format("SMPPServers - %s , SMPPServerSession - %d , SMPPClients - %s "
				, smsGateWay.getSmppServerController() != null 
					? (ServerController.isAnyServerAvailable()) ? "UP" : "DOWN"
						: "IS NOT INIT"
				, smsGateWay.getSmppServerController() != null 
					? ServerController.getServersActiveSessions()
						: 0
				, smsGateWay.getSmppClientController() != null
					? (ClientController.isAnyClientAvailable()) ? "UP" : "DOWN"
						: "IS NOT INIT"));
	}
	private void resetClinet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.getWriter().println("Restart smpp client started...");
		int clientId = request.getParameter("ClientId") != null ? Integer.valueOf(request.getParameter("ClientId")) : 0;
		response.getWriter().println("Reset result - " + ClientController.resetClinet(clientId));
		response.getWriter().println("Restart smpp client done...");
	}
}
