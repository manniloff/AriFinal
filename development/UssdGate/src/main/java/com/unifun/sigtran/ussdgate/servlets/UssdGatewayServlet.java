/**
 *
 */
package com.unifun.sigtran.ussdgate.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Server;
import org.mobicents.protocols.sctp.AssociationImpl;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.m3ua.RouteAs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.adaptor.SigtranStackBean;
import com.unifun.sigtran.stack.SccpUnifunStackWrapper;
import com.unifun.sigtran.ussdgate.UssdMapLayer;

//import com.unifun.sigtran.scpmsctest.ProgramMain;
/**
 * @author rbabin
 *
 */
//http://127.0.0.1:7080/UssdGate/gwsrv?action=maintanacemode&mode=start
//http://127.0.0.1:7080/UssdGate/gwsrv?action=maintanacemode&mode=stop
//http://127.0.0.1:7080/UssdGate/gwsrv?action=maintanacemode&mode=status
//http://127.0.0.1:7080/UssdGate/gwsrv?action=editsccp&line=some%comand
@WebServlet(name = "UssdGatewayServlet", urlPatterns = {"/gwsrv"}, displayName = "UssdGatewayServlet", asyncSupported = true)
public class UssdGatewayServlet extends HttpServlet {

    private static final long serialVersionUID = -299856690242156646L;
    private static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[UssdGatewayMapApiServlet"));
    private SigtranStackBean sigStack;
    private UssdMapLayer mapLayer;

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException,
            IOException {
        this.sigStack = (SigtranStackBean) request.getServletContext().getAttribute("sigtranStack");
        if (sigStack == null) {
            throw new ServletException("Unable to obtain SigtranStack");
        }
        this.mapLayer = (UssdMapLayer) request.getServletContext().getAttribute("ussMapLayer");
        if (mapLayer == null) {
            throw new ServletException("Unable to obtain UssdMapLayer");
        }
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        String action = request.getParameter("action");
        switch (action) {
            case "removerule":
                //removeRule(session, request, response);
                break;
            case "createrule":
                //createRule(session, request, response);
                break;
            case "getsmpprules":
                //getSmppRules(session, request, response);
                break;
            case "getroute":
                //getRoute(session, request, response);
                break;
            case "reloadrules":
                reloadRules(request, response, action);
                break;
            case "status":
                //response.getWriter().println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",action,ussdGateway.isScpRunning()));
                break;
            case "start":
                //startStack(session, request, response, action);
                break;
            case "stop":
                //stopStack(request);
                break;
            case "notifications":
                //notification(request, response, action);
                break;
            case "m3uastatus":
                m3uaStatus(request, response, action);
                break;
            case "m3uainfo":
                m3uaInfo(request, response, action);
                break;
            case "mtpstatus":
                mtpStatus(request, response, action);
                break;
            case "sctpstatus":
                sctpStatus(request, response, action);
                break;
            case "maintanacemode":
                maintanaceMode(request, response, action);
                break;
            case "editsccp":
                try {
                    editsccp(request, response, action);
                } catch (Exception e) {
                    response.getWriter().println(String.format(
                            "{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"%s.\"}", action,
                            e.getMessage()));

                }
                break;
            default:
                response.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"Unsuported action.\"}", action));
                break;
        }
        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * @param request
     * @param response
     * @param action
     */
    private void editsccp(HttpServletRequest request, HttpServletResponse response, String action)
            throws Exception {
        String cmd = request.getParameter("line");
        if (cmd == null) {
            throw new Exception("Nothing to do, please fill the command");
        }
        try {
            cmd = URLDecoder.decode(cmd, "UTF-8");
        } catch (Exception e) {
            logger.warn("Some error while decoding comand for sccp rules", e);
        }
        String[] args = cmd.split(" ");
        String respString = sigStack.getStack().getSccp().getSccpShellExecuter().execute(args);//this.sccpShellExecuter.execute(args);
        //return encode(respString);
        PrintWriter out = response.getWriter();
        out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
                action, respString));
    }

    /**
     * @param request
     * @param response
     * @throws IOException
     */
    private void reloadRules(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
        this.mapLayer.fetchRoutingRule(true);
        this.mapLayer.fetchRoutingRule(false);
        PrintWriter out = response.getWriter();
        out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
                action, "Routing rules reloaded"));
    }

    /**
     * @param request
     * @param response
     * @param action
     * @throws IOException
     */
    private void maintanaceMode(HttpServletRequest request, HttpServletResponse response,
            String action) throws IOException {
        String mode = request.getParameter("mode");
        PrintWriter out = response.getWriter();
        logger.info(String.format("Reciving http Maintenance Mode request from %s:%d, mode requested: %s", request.getRemoteAddr(), request.getRemotePort(), mode));
        //com.unifun.sigtran.stack.Map map = ussdGateway.getSctpServer().getMap();

        switch (mode) {
            case "start":
                this.mapLayer.setMaintenancemode(true);
                out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
                        action, "Enable maintenance mode"));
                break;
            case "stop":
                this.mapLayer.setMaintenancemode(false);
                out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
                        action, "Disable maintenance mode"));
                break;
            case "status":
                out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"mode\":\"%s\"}",
                        action, (this.mapLayer.isMaintenancemode()) ? "Started" : "Stoped"));
                break;
            default:
                out.println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",
                        action, "Unsuported Mode"));
                break;
        }
    }
//	private void removeRule(Session session, HttpServletRequest request, HttpServletResponse response){
//		logger.info(String.format("Reciving http Remove Rule request from %s:%d", request.getRemoteAddr(),request.getRemotePort()));
//		String rr_id =  request.getParameter("id");
//		session = db.getSession();             
//		try{
//			session.beginTransaction();
//			SsRouteRules ss_route_rule = 
//					(SsRouteRules)session.get(SsRouteRules.class, Long.parseLong(rr_id)); 
//			if ("smpp".equalsIgnoreCase(ss_route_rule.getProtocolType())){
//				List<SmppSettings> smppSettingsList = db.listSmppSettings(Long.parseLong(rr_id));
//				SmppSettings smppSettings = smppSettingsList.iterator().next();
//				session.delete(smppSettings);
//			}
//			session.delete(ss_route_rule); 
//
//			session.getTransaction().commit();
//			logger.info("Removed Rule with id:"+ rr_id);
//		}catch (HibernateException e) {
//			if (session.getTransaction() !=null) session.getTransaction().rollback();
//			logger.error(e);	   	      
//		}finally {	   	    	
//			session.close(); 
//		}
//	}
//
//	private void createRule(Session session, HttpServletRequest request, HttpServletResponse response){
//		logger.info(String.format("Reciving http Create Rule request from %s:%d", request.getRemoteAddr(),request.getRemotePort()));
//		String ussdtext =  request.getParameter("ussdtext");
//		String destination =  request.getParameter("destination");
//		String protocol =  request.getParameter("protocol");
//		String connps = request.getParameter("connps");
//		String serviceCode ;
//		if (ussdtext.startsWith("*")){
//			serviceCode = ussdtext.substring(ussdtext.indexOf("*") + 1, ussdtext.indexOf("#"));
//		}else{
//			serviceCode = ussdtext;
//		}
//		String ussdSc = request.getParameter("ussdsc");
//		//smpp params
//		String smscuser = request.getParameter("smscuser");
//		String smscpass = request.getParameter("smscpass");
//		//--smpp params
//		SsRouteRules rr = new SsRouteRules();
//		rr.setUssdText(ussdtext);
//		rr.setServiceCode(serviceCode);
//		rr.setDestAddress(destination);
//		rr.setProtocolType(protocol);
//		rr.setConnps(connps);
//		rr.setUssdsc(ussdSc);
//		rr.setMaintenancemode(0);
//		boolean isSmpp = false;
//		SmppSettings smppSetting = new SmppSettings();
//		if ("smpp".equalsIgnoreCase(protocol)){
//			smppSetting.setUsername(smscuser);
//			smppSetting.setPassword(smscpass);
//			isSmpp = true;
//
//		}
//		session = db.getSession();
//		try{
//			session.beginTransaction();
//			session.save(rr);
//			if(isSmpp){
//				smppSetting.setRule_id(rr.getId());
//				session.save(smppSetting);
//			}
//			session.getTransaction().commit();
//			logger.info(String.format("Inserting parameters: %s %s %s",ussdtext, destination, protocol));
//		}catch (HibernateException e) {
//			if (session.getTransaction() !=null) session.getTransaction().rollback();
//			logger.error(e);	   	      
//		}finally {	   	    	
//			session.close(); 
//		}
//	}
//
//	private void getSmppRules(Session session, HttpServletRequest request, HttpServletResponse response) throws IOException{
//		logger.info(String.format("Reciving http getSmppRules request from %s:%d", request.getRemoteAddr(),request.getRemotePort()));
//		String route_rule_id = request.getParameter("ruleid");
//		session = db.getSession();
//		try{
//			session.beginTransaction();
//			List<SmppSettings> smppSettingsList = db.listSmppSettings(Long.parseLong(route_rule_id));
//			SmppSettings smpp = smppSettingsList.iterator().next();	   	        
//			StringBuffer buff = new StringBuffer();
//			buff.append(String.format("{\"smscip\":\"%s\",\"smscport\":\"%d\",\"smscuser\":\"%s\",\"smscpass\":\"%s\",\"bindmode\":\"%s\",\"addrton\":\"%s\",\"addrnpi\":\"%s\",\"srcton\":\"%s\",\"srcnpi\":\"%s\",\"srcaddr\":\"%s\",\"dstton\":\"%s\",\"dstnpi\":\"%s\",\"dstaddr\":\"%s\",\"srvtype\":\"%s\",\"timeout\":\"%d\"}",
//					smpp.getSmscIpAddress(), smpp.getSmscPort(), smpp.getUsername(), smpp.getPassword(), smpp.getBindmode(), smpp.getAddrTon(), smpp.getAddrNpi(),smpp.getSourceTon(), smpp.getSourceNpi(), smpp.getSourceAddress(), smpp.getDestinationTon(), smpp.getDestinationNpi(), smpp.getDestintaionAddress(), smpp.getSystemType(), smpp.getTimeOut())); 	  
//			response.getWriter().println(String.format("%s",buff));
//		}catch (HibernateException e) {
//			if (session.getTransaction() !=null) session.getTransaction().rollback();
//			logger.error(e);	   	      
//		}finally {
//			session.close(); 
//		}
//	}
//	private void getRoute(Session session, HttpServletRequest request, HttpServletResponse response) throws IOException{
//		session = db.getSession();
//		try{
//			session.beginTransaction();
//			List<SsRouteRules> ss_rr = session.createQuery("FROM SsRouteRules").list(); 
//			StringBuffer buff = new StringBuffer();
//			for (Iterator iterator = ss_rr.iterator(); iterator.hasNext();){
//				SsRouteRules ss_rr_item = (SsRouteRules) iterator.next();
//				if(iterator.hasNext()){
//					buff.append(String.format("{\"ussdtext\":\"%s\",\"destaddress\":\"%s\",\"servicecode\":\"%s\",\"protocoltype\":\"%s\",\"connps\":\"%s\",\"ussdsc\":\"%s\",\"id\":%d},", ss_rr_item.getUssdText(), 
//							ss_rr_item.getDestAddress(),ss_rr_item.getServiceCode(),ss_rr_item.getProtocolType(),ss_rr_item.getConnps(),ss_rr_item.getUssdsc(),ss_rr_item.getId()));
//				}else{
//					buff.append(String.format("{\"ussdtext\":\"%s\",\"destaddress\":\"%s\",\"servicecode\":\"%s\",\"protocoltype\":\"%s\",\"connps\":\"%s\",\"ussdsc\":\"%s\",\"id\":%d}", ss_rr_item.getUssdText(), 
//							ss_rr_item.getDestAddress(),ss_rr_item.getServiceCode(),ss_rr_item.getProtocolType(),ss_rr_item.getConnps(),ss_rr_item.getUssdsc(),ss_rr_item.getId()));
//				}	   	             
//			}
//			response.getWriter().println(String.format("[%s]",buff));
//		}catch (HibernateException e) {
//			if (session.getTransaction() !=null) session.getTransaction().rollback();
//			logger.error(e);	   	      
//		}finally {
//			session.close(); 
//		}
//	}
//	private void startStack (Session session, HttpServletRequest request, HttpServletResponse response, String action) throws IOException{
//		logger.info(String.format("Reciving http Start Sigtran Stack request from %s:%d", request.getRemoteAddr(),request.getRemotePort()));
//		logs.removeAll(logs);
//		if (!ussdGateway.isScpRunning()) {
//			logs.add("Starting SCP...\n\r");                 
//			if (ussdGateway.startScp() == 0) {
//				logs.add("Started SCP...\n\r");
//				response.getWriter().println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",action,"Stack initiated"));
//			} else {
//				logs.add("Failed to start SCP...\n\r");
//				response.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"%s\"}",action,"Stack failed to start"));
//			}
//
//		} else {
//			logs.add("ERROR: SCP already running...\n\r");
//			response.getWriter().println(String.format("{\"Status\":\"Error\",\"action\":\"%s\",\"msg\":\"%s\"}",action,"Stack already running"));
//		}
//	}
//	private void stopStack(HttpServletRequest request){
//		logger.info(String.format("Reciving http Sigtran Stack request from %s:%d", request.getRemoteAddr(),request.getRemotePort()));
//		logs.removeAll(logs);
//		if (ussdGateway.isScpRunning()) {
//			logs.add("Stoping SCP...\n\r");                 
//			if (ussdGateway.stopScp() == 0) {
//				logs.add("Stopped SCP...\n\r");
//			} else {
//				logs.add("Failed to stop SCP...\n\r");
//			}                 
//		} else {
//			logs.add("ERROR: SCP not running...\n\r");                 
//		}
//	}
//	private void notification(Session session, HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
//		StringBuffer strbuff = new StringBuffer();
//		for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
//			String string = (String) iterator.next();
//			strbuff.append(string.trim());
//		}
//		response.getWriter().println(String.format("{\"Status\":\"Ok\",\"action\":\"%s\",\"msg\":\"%s\"}",action,strbuff));
//	}

    private void m3uaStatus(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
        try {
            synchronized (this.sigStack.getStack().getM3ua()) {
                boolean m3uaIsStarted = this.sigStack.getStack().getM3ua().isM3UAManagementStarted();
                // curentDialogCount = this.ussdGateway.getSctpServer().getMap().getCurentDialogCount();
                response.setHeader("m3uastatus", (m3uaIsStarted) ? "started" : "stoped");
                response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"m3uastatus\":\"%s\"}",
                        action, (m3uaIsStarted) ? "started" : "stoped"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void m3uaInfo(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
        try {
            List<String> tmp = new ArrayList<>();
            response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",",
                    action));
            M3UAManagement m3uaserver = this.sigStack.getStack().getM3ua().getServerM3UAMgmt();
            String m3uaservername = m3uaserver.getName();
            response.getWriter().println("\"m3ua\":\"" + m3uaservername + "\",\"as\":");
            List<As> as = m3uaserver.getAppServers();
            logger.debug("as");
            for (As a : as) {
                StringBuffer asStringBuff = new StringBuffer();
                long na = 0;
                String ri = null;
                try {
                    na = a.getNetworkAppearance().getNetApp();
                } catch (Exception e) {
                }
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
                        + "\"ApplicationServerPreference\":", a.getName(),
                        a.getFunctionality().name(),
                        a.getExchangeType().name(),
                        a.getIpspType().name(),
                        ri,
                        a.getTrafficModeType().getMode(),
                        na));
                List<Asp> aspList = a.getAspList();
                List<String> jsonAsp = new ArrayList<>();
                logger.debug("asp");
                for (Asp asp : aspList) {
                    jsonAsp.add(String.format("{"
                            + "\"name\":\"%s\","
                            + "\"associationName\":\"%s\","
                            + "\"state\":\"%s\""
                            + "}",
                            asp.getName(),
                            asp.getAspFactory().getAssociation().getName(),
                            asp.getState().getName()));
                }
                asStringBuff.append(String.format("%s}", jsonAsp.toString()));
                tmp.add(asStringBuff.toString());
            }
            response.getWriter().println(String.format("%s,\"routes\":", tmp.toString()));
            Map<String, RouteAs> routes = m3uaserver.getRoute();
            tmp.clear();
            for (String s : routes.keySet()) {
                tmp.add(String.format("{"
                        + "\"route\":\"%s\""
                        + "}", s));
            }
            response.getWriter().println(String.format("%s}", tmp.toString()));
            tmp.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mtpStatus(HttpServletRequest request, HttpServletResponse response, String action) {
        List<String> tmp = new ArrayList<>();
        try {
            response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"mtpstatus\":",
                    action));
            for (Entry<Long, String> e : ((SccpUnifunStackWrapper) this.sigStack.getStack().getSccp().getSccpStack()).getMtpstatus().entrySet()) {
                tmp.add(String.format("{\"PC\":\"%d\",\"STATUS\":\"%s\"}", e.getKey(), e.getValue()));
            }

            response.getWriter().println(String.format("%s}", tmp.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sctpStatus(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
        List<String> tmp = new ArrayList<>();
        try {
            List<Server> servers = this.sigStack.getStack().getSctp().getSctpManagement().getServers();
            Map<String, Association> associations = this.sigStack.getStack().getSctp().getSctpManagement().getAssociations();
            response.getWriter().println(String.format("{\"Status\":\"OK\",\"action\":\"%s\",\"sctpassociationsstatus\":",
                    action));
            for (String associationName : associations.keySet()) {
                //AssociationImpl asso = (AssociationImpl)associations.get(associationName);
                Association asso = associations.get(associationName);
                if ("SERVER".equalsIgnoreCase(asso.getAssociationType().name())) {
                    String serveripAddress = null;
                    int serverport = 0;
                    for (Server s : servers) {
                        if (s.getAssociations().contains(associationName)) {
                            serveripAddress = s.getHostAddress();
                            serverport = s.getHostport();
                            break;
                        }
                    }
                    tmp.add(String.format("{\"Name\":\"%s\",\"hostAddress\":\"%s\",\"hostPort\":\"%d\",\"peerAddress\":\"%s\",\"peerPort\":\"%d\",\"status\":\"%s\",\"extraHost:\":\"%s\",\"type:\":\"%s\"}", associationName, serveripAddress,
                            serverport, asso.getPeerAddress(), asso.getPeerPort(), (asso.isUp()) ? "UP" : "DOWN", (asso.getExtraHostAddresses() != null) ? asso.getExtraHostAddresses().toString() : "", asso.getAssociationType()));
                } else {
                    tmp.add(String.format("{\"Name\":\"%s\",\"hostAddress\":\"%s\",\"hostPort\":\"%d\",\"peerAddress\":\"%s\",\"peerPort\":\"%d\",\"status\":\"%s\",\"extraHost:\":\"%s\",\"type:\":\"%s\"}", associationName, asso.getHostAddress(),
                            asso.getHostPort(), asso.getPeerAddress(), asso.getPeerPort(), (asso.isUp()) ? "UP" : "DOWN", (asso.getExtraHostAddresses() != null) ? asso.getExtraHostAddresses().toString() : "", asso.getAssociationType()));
                }
            }
            response.getWriter().println(String.format("%s}", tmp.toString()));
            tmp.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
