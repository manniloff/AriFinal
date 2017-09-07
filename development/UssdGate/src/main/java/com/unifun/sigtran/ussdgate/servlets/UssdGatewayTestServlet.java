/**
 *
 */
package com.unifun.sigtran.ussdgate.servlets;

import com.unifun.map.JsonAddressString;
import com.unifun.map.JsonComponent;
import com.unifun.map.JsonComponents;
import com.unifun.map.JsonDataCodingScheme;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import com.unifun.sigtran.ussdgate.UssMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
@WebServlet(name = "UssdGatewayTestServlet", urlPatterns = {"/test"}, displayName = "UssdGatewayTestServlet", asyncSupported = true)
public class UssdGatewayTestServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 4348239565938673418L;
    private static final Logger logger = LogManager.getLogger(UssdGatewayTestServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println("Hellow");
        resp.flushBuffer();
/*        
        //Get incoming parameters
        String dialogId = req.getParameter("dialog_id");
        String ussd_text = req.getParameter("ussd_text");
        String msisdn = req.getParameter("msisdn");
        String service_code = req.getParameter("service_code");
        logger.info(String.format("Rx: ussd_text=%s, sc=%s, msisdn=%s", ussd_text, service_code, msisdn));
        //Setting headers
        //dialog_id, ussd_text, msisdn, charset, message_type (TCAP Continue or TCAP End),service_code
        resp.setHeader("dialog_id", dialogId);
        resp.setHeader("msisdn", msisdn);
        resp.setHeader("charset", "72");
        resp.setHeader("service_code", service_code);
        if (ussd_text.contains("*") || ussd_text.startsWith("#")) {
            logger.info("Retrun menu to req");
            String txt = "Test Menu :\n0. Balance\n1. Option 1\n2. Option 2";
            //String txt = "Il y a un probleme avec votre compte.  Veuillez contacter le service clientele. Merci.Il y a un probleme avec votre compte.  Veuillez contacter le service clientele. Merci.";
            //String txt= "Кайдасың? 10 күнгө АКЫСЫЗ, андан  ары 3 сом/күнүнө (КНС)\nТы где? 10 дней БЕСПЛАТНО, далее 3 сома/день(с НДС)\n1>Жазылуу / Подписаться";
            resp.setHeader("ussd_text", URLEncoder.encode(txt, "UTF-8"));
            resp.setHeader("message_type", "TCAP Continue");
            //resp.setHeader("message_type", "TCAP End");
        } else if (ussd_text.equalsIgnoreCase("2")) {
            logger.info("Return content for option 2");
            resp.setHeader("ussd_text", URLEncoder.encode("Submeniu \n1. Option 1\n2. Option 2", "UTF-8"));
            resp.setHeader("message_type", "TCAP Continue");
        } else {
            logger.info("Return content for default");
            //resp.setHeader("ussd_text", "USSRequest "+ ussd_text + " has recived.");
            resp.setHeader("ussd_text", doextpussr(msisdn));
            resp.setHeader("message_type", "TCAP End");
        }
*/
    }

    /**
     * @return
     */
    private String doextpussr(String msisdn) {
        String url = String.format("http://127.0.0.1:7080/UssdGate/mapapi?action=pussr&msisdn=%s&ussd_text=*643#", msisdn);
//		HttpClient httpClient = new HttpClient();
//		//if(jettythreadPool!=null)
//			httpClient.setExecutor(this.jettythreadPool);
        HttpURLConnection con = null;
        try {
//			httpClient.start();			
//			Request request = httpClient.newRequest(url);
//			logger.info(request.getURI().toString());
//			request.method(HttpMethod.GET);
//			request.agent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0");
//			ContentResponse response = null;			
//			response = request
//					.timeout(60000, TimeUnit.MILLISECONDS)
//					.send();
//			String respUssdText= response.getHeaders().get("ussd_text");

            URL urlObj = new URL(url);
            logger.info("Call URL :" + urlObj.toURI());
            con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            //con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0");
            int responseCode = con.getResponseCode();
            //String respCharset = con.getHeaderField("charset");
            //String respDialogId = con.getHeaderField("dialog_id");
            //String respMsgTye =	con.getHeaderField("message_type");
            //String respMsisdn =	con.getHeaderField("msisdn");
            String respUssdText = con.getHeaderField("ussd_text");
            //String respServiceCode = con.getHeaderField("service_code");

            return respUssdText;
        } catch (Exception e) {
            return "Error in extpussrreq: " + e.getMessage();
        } finally {
            con.disconnect();
//			try {
//				httpClient.stop();
//			} catch (Exception e) {				
//				e.printStackTrace();
//			}
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonReader reader = Json.createReader(new InputStreamReader(req.getInputStream()));
            JsonObject obj = reader.readObject();

            JsonMessage request = new JsonMessage(obj);
            logger.info("Request <---> " + request.toString());
            
            JsonMessage response = response(request);
            logger.info("Response <---> " + response.toString());
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            
            resp.getWriter().println(response.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            resp.getWriter().println("{\"Error\": \"" + e.getMessage() + "\"}");
        }
    }

    private JsonMessage response(JsonMessage request) {
        long dialogId = request.getTcap().getDialog().getDialogId();
        
        final JsonTcapDialog dialog = new JsonTcapDialog();
        dialog.setDialogId(dialogId);
        
        final JsonTcap tcap = new JsonTcap();
        tcap.setType("Continue");
        tcap.setDialog(dialog);
        
        
        final JsonMapOperation ussr = new JsonMapOperation();
        ussr.setUssdString("Hellow");
        ussr.setCodingScheme(codingScheme(request));
        ussr.setMsisdn(msisdn(request));
        
        final JsonMap map = new JsonMap("unstructured-ss-request", ussr);
        final JsonInvoke invoke = new JsonInvoke(invokeId(request), map);
        
        final JsonComponent component = new JsonComponent();
        component.setType("invoke");
        component.setValue(invoke);
        
        final JsonComponents components = new JsonComponents();
        components.add(component);
        
        tcap.setComponents(components);
        
        final JsonMessage response = new JsonMessage();
        response.setTcap(tcap);
        
        return response;
    }

    private long invokeId(JsonMessage msg) {
        return ((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).invokeId();
    }
    
    private JsonAddressString msisdn(JsonMessage msg) {
        JsonMap map = (JsonMap)(((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation)map.operation()).getMsisdn();
    }
    
    private JsonDataCodingScheme codingScheme(JsonMessage msg) {
        JsonMap map = (JsonMap)(((JsonInvoke)msg.getTcap().getComponents().get(0).getValue()).component());
        return ((JsonMapOperation)map.operation()).getCodingScheme();
    }
    
}
