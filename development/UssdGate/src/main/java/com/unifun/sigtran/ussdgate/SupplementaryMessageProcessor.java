/**
 *
 */
package com.unifun.sigtran.ussdgate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.mobicents.protocols.ss7.map.api.MAPMessageType;
import org.mobicents.protocols.ss7.map.api.service.supplementary.SupplementaryMessage;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import com.unifun.sigtran.ussdgate.db.MapMsgDbWriter;
import org.apache.log4j.Logger;

/**
 * @author rbabin
 *
 */
public class SupplementaryMessageProcessor implements Runnable {

    public static final Logger LOGGER = Logger.getLogger(SupplementaryMessageProcessor.class);

    private final SupplementaryMessage message;
    private final Map<String, SsRouteRules> primaryRoute;
    private final Map<String, SsRouteRules> secondaryRoute;
    private boolean isEmap = false;
    private final DataSource ds;
    private final ExecutorService dbWrk;
    private boolean isMaintenanceMode = false;
    private final UssdMapLayer mapLayer;
    private final String mapWriterProcName;
    private final int respTimeout;
    private final CongestionControl congestionControl;
    private final WhiteList whiteList;
    
    public SupplementaryMessageProcessor(UssdMapLayer mapLayer, WhiteList whiteList, DataSource ds, ExecutorService dbWrk,
            Map<String, SsRouteRules> rR, Map<String, SsRouteRules> prR, SupplementaryMessage message, boolean isEmap,
            boolean maintenancemode) {
        this.mapLayer = mapLayer;
        this.whiteList = whiteList;
        this.ds = ds;
        this.dbWrk = dbWrk;
        this.primaryRoute = rR;
        this.secondaryRoute = prR;
        this.message = message;
        this.isEmap = isEmap;
        this.isMaintenanceMode = maintenancemode;
        this.mapWriterProcName = mapLayer.getAppSettings().get("db").get("mapMsgWrProc");
        this.respTimeout = Integer.parseInt(mapLayer.getAppSettings().get("http").get("resptimeout"));
        this.congestionControl = new CongestionControl(mapLayer);
    }

    @Override
    public void run() {
        final long startTime = System.currentTimeMillis();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(dialogId() + "Starting message processing");
        }
        
        
        //parse arrived message
        UssMessage msg = null;//UssMessage.parse(message, mapLayer, primaryRoute, secondaryRoute);

        // we are expecting a valid message here
        if (!this.isValid(msg)) {
            LOGGER.error(dialogId() + "MAP message is not valid");
            return;
        }

        // we are expecting a valid message here
        if (!whiteList.isAllowed(msg.getMsisdn())) {
            reply(msg, "Prohibited");
            return;
        }
        
        // We have a valid message, store it in our DB
        storeMessage(msg);

        // Store this message in memmory as well
        mapLayer.keepUssMessage(msg.getDialogId(), msg);

        // Select rule
        SsRouteRules routeRule = this.isMaintenanceMode
                ? msg.getSecondaryRoute()
                : msg.getRouteRule();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(dialogId() + "Selected route "  + routeRule);
        }
        
        //check rule for routing message to
        if (!this.isValid(routeRule)) {
            LOGGER.error(dialogId() + "Could not detect routing rule for dialed digits " + msg.getUssdText());
            reply(msg, "No Content Provider for dialed SC");
            return;
        }

        //check for congestion
        if (congestionControl.isCongested(routeRule, msg)) {
            LOGGER.error(dialogId() + "The system now congested ");
            reply(msg, "Content Provider is bussy, please try again later");
            return;
        }

        //forward this message over other transport
        forwardMessage(routeRule, msg);

        //measure execution time
        final long endTime = System.currentTimeMillis();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(dialogId() + "Message processing complete, duration = " + (endTime - startTime));
        }
    }

    /**
     * Forwards message over other transport.
     *
     * The transport type is carried by routing rule.
     *
     * @param route
     * @param msg
     */
    private void forwardMessage(SsRouteRules route, UssMessage msg) {
        if (route == null) {
            LOGGER.warn(dialogId() + " no route for this message");
            return;
        }

        switch (route.getProtocolType()) {
            case "HTTP":
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(dialogId() + "Forwarding over HTTP");
                }
                forwardOverHttp(msg, new HttpURL(route.getDestAddress()));
                break;
            case "MAP":
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(dialogId() + "Forwarding over MAP");
                }
                forwardOverMAP(msg, route.getDestAddress());
                break;
            case "SMPP":
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(dialogId() + "Not yet implemented");
                }
                break;
            default:
                break;

        }
    }

    /**
     * Sends reply with given text.
     *
     * @param msg
     * @param text
     */
    private void reply(UssMessage msg, String text) {
        msg.setCharset("15");
        msg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
        msg.setInTimeStamp(null);
        msg.setOutTimeStamp(new Timestamp(new Date().getTime()));
        msg.setUssdText(text);
        // write error to db

        storeMessage(msg);
        sendUssMessage(msg);
    }

    /**
     * Send specified message using HTTP protocol.
     *
     * @param request
     * @param uri The comma separated URL and possibly another ussd text.
     */
    private void forwardOverHttp(UssMessage request, HttpURL uri) {
        UssMessage response;

        if (uri.hasUssdText()
                && MAPMessageType.processUnstructuredSSRequest_Request.name().equalsIgnoreCase(request.getMessageType())) {
            request.setUssdText(uri.ussdText());
            request.setServiceCode(Utils.serviceCode(uri.ussdText()));
        }

        try {
            response = this.doHttpExchange(request, uri.url());
            response.setInTimeStamp(null);
            response.setOutTimeStamp(new Timestamp(new Date().getTime()));

            if ("Error".equalsIgnoreCase(response.getMessageType())) {
                throw new Exception("Some error ocure in http transfer");
            }

            storeMessage(response);
            sendUssMessage(response);
        } catch (Exception e) {
            LOGGER.warn(dialogId() + "Some error ocuring during http transfer: ", e);
            if ("true".equalsIgnoreCase(this.mapLayer.getAppSettings().get("app").get("forwardFailure"))) {
                LOGGER.warn(dialogId() + "Unable to get response from remote communication node. Redirect message using spare route");
                this.forwardMessage(request.getSecondaryRoute(), request);
            }
        }
    }

    private void postOverHttp() {
        
    }
    
    /**
     * @param destination
     * @param dialogId
     * @param ussdtext
     * @param msisdn
     * @param serviceCode
     * @throws Exception
     */
    private UssMessage doHttpExchange(UssMessage ussMsg, String destination) throws Exception {
        String url = String.format("%s?dialog_id=%s&ussd_text=%s&msisdn=%s&service_code=%s",
                destination,
                URLEncoder.encode(Long.toString(ussMsg.getDialogId()), "UTF-8"),
                URLEncoder.encode(ussMsg.getUssdText(), "UTF-8"),
                URLEncoder.encode(ussMsg.getMsisdn(), "UTF-8"),
                URLEncoder.encode(ussMsg.getServiceCode(), "UTF-8")
        );

        //include GT if called party address is translated
        SccpAddress calledParty = message.getMAPDialog().getLocalAddress();
        if (calledParty.isTranslated()) {
            url += ("&tgt" + calledParty.getGlobalTitle().getDigits());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(dialogId() + "Request to " + url);
        }

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setConnectTimeout(respTimeout);
            con.setReadTimeout(respTimeout);

            // con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu;
            // Linux x86_64; rv:17.0) Gecko/20100101 Firefox/17.0");
            int responseCode = con.getResponseCode();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(dialogId() + "Response code " + responseCode);
            }
            
            String respCharset = con.getHeaderField("charset");
            String respMsgTye = con.getHeaderField("message_type");
            String respUssdText = con.getHeaderField("ussd_text");

            ussMsg.setCharset(URLDecoder.decode(respCharset, "UTF-8"));
            ussMsg.setMessageType(Utils.typeOf(URLDecoder.decode(respMsgTye, "UTF-8")));
            ussMsg.setUssdText(URLDecoder.decode(respUssdText, "UTF-8"));

        } finally {
            con.disconnect();
        }
        // TODO write response to vas logs ?????
        return ussMsg;
    }

    /**
     * Forwards message over MAP.
     *
     * @param ussMsg
     * @param destination
     */
    private void forwardOverMAP(UssMessage ussMsg, String destination) {
        switch (message.getMessageType()) {
            case processUnstructuredSSRequest_Request:
                //replace short number with one from the routing rule
                //looks like destination contains multiple parameters:
                //<service_code>,<original_osc_code>[,<calledParty_GT>]
                String[] params = destination.split(",");

                if (params.length < 2) {
                    LOGGER.error(String.format("wrong destination field in route rule %s", destination));
                    reply(ussMsg, "Error while processing, no valid destination");
                    return;
                }

                //replace code specified by user with code predefined original for
                //this service
                String ussdText = ussMsg.getUssdText();
                ussdText = ussdText.replaceFirst(params[0], params[1]);

                //take GT from params
                String globalTitle = null;
                if (params.length >= 3) {
                    globalTitle = params[2];
                }

                //and overwrite from original map dialog if possible
//                SccpAddress calledParty = message.getMAPDialog().getLocalAddress();
//                if (calledParty.isTranslated()) {
//                    globalTitle = calledParty.getGlobalTitle().getDigits();
//                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(dialogId() + "Sending Process_Unstructured_SS_Request:  " + ussdText);
                }
                
                long dialogId = this.mapLayer.initiatePUSSR(ussMsg.getMsisdn(), // isdn
                        ussdText, // ussdText
                        1, // transferType (0 - http, 1 - map, default -
                        // unkonown )
                        ussMsg.getDialogId(), // initialDialogId
                        (isEmap) ? "emap" : null, // isemap
                        globalTitle, // sccpCalledParty
                        null, // sccpCallingParty
                        null, // dssn
                        null // ossn
                );

                if (dialogId == -1) {
                    LOGGER.error(
                            String.format("Error sending PROCESS_UNSTRUCTURED_SS_REQUEST = %s", ussMsg.toString()));
                    reply(ussMsg, "Error while processing unstructuredSS, please try again later");
                }
                break;
            case unstructuredSSRequest_Request:
            case unstructuredSSRequest_Response:
                UssMessage subUssMsg = mapLayer.findSubsession(ussMsg.getDialogId());

                if (subUssMsg == null) {
                    reply(ussMsg, "Error while processing unstructuredSS, please try again later");
                    return;
                }

                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(dialogId() + "Sending Unstructured_SS_Request:  " + ussMsg.getUssdText());
                    }
                    this.mapLayer.initiateUSSR(ussMsg.getUssdText(), Long.toString(subUssMsg.getDialogId()),
                            subUssMsg.getMsisdn(), ussMsg.getCharset(), subUssMsg.getIntermediateInvokeId(), 1,
                            ussMsg.getDialogId(), (isEmap) ? "emap" : null);
                } catch (Exception e) {
                    LOGGER.error(String.format("Error sendint usssr = %s ", ussMsg.toString()));
                    reply(ussMsg, "Error while processing unstructuredSS, please try again later");
                }
                break;
            default:
                LOGGER.error("Unsuported Message type " + message.getMessageType().name());
                break;
        }
    }

    /**
     * Writes specified message into database.
     *
     * @param msg
     */
    private void storeMessage(UssMessage msg) {
        try {
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, msg, mapWriterProcName);
            dbWrk.execute(dbWriter);
        } catch (Exception ex) {
            LOGGER.error("Failed to store Supplementary Message to db: " + ex.getMessage());
        }
    }

    /**
     * @param ussMsg
     */
    private void sendUssMessage(UssMessage ussMsg) {
        LOGGER.debug(String.format("Send Uss Message: %s ", ussMsg.toString()));
        mapLayer.handleUssProcReq(ussMsg);
    }

    /**
     * Validates given object.
     *
     * @param msg
     * @return
     */
    private boolean isValid(Object msg) {
        return msg != null;
    }

    private String dialogId() {
        return "(Dialog-ID: " + message.getMAPDialog().getLocalDialogId() + ") ";
    }
}
