/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonSccpAddress;
import com.unifun.map.JsonTcap;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.m3ua.RouteAs;
import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPMessageType;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.primitives.USSDString;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ActivateSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ActivateSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.DeactivateSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.DeactivateSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.EraseSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.EraseSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.GetPasswordRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.GetPasswordResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.InterrogateSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.InterrogateSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPDialogSupplementary;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterPasswordRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterPasswordResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSNotifyRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSNotifyResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

import com.unifun.sigtran.ussdgate.db.MapMsgDbWriter;

import javolution.util.FastMap;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;

public class UssdMapLayer implements MAPDialogListener, MAPServiceSupplementaryListener {

    public static final Logger LOGGER = Logger.getLogger(UssdMapLayer.class);

    private final MAPStackImpl mapStack;
    protected MAPProvider mapProvider;
    private final TCAPStack tcapStack;
    private final SccpStack sccpStack;

    //private ConcurrentHashMap<Long, SsMapMessageLog> ussMsgRespnose = new ConcurrentHashMap<>();    
    private final Set<Long> httpDialogs = ConcurrentHashMap.newKeySet();

    private DataSource ds;
    private ExecutorService dbWorker;
    private final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
    private ExecutorService appWorker;
    //private ForkJoinPool appWorker;
    private Map<String, Map<String, String>> appSettings = null;
    //private transient Map<Long, UssMessage> ussMessages = new HashMap<>();
    //private transient FastMap<Long, UssMessage> ussMessages = new FastMap<Long, UssMessage>();
    private transient Map<Long, UssMessage> ussMessages = ExpiringMap.builder().expiration(60, TimeUnit.SECONDS).build();
    //
    private ConcurrentHashMap<Long, Long> request = new ConcurrentHashMap<>();
    //private FastMap<Long, Long> request = new FastMap<Long, Long>();
    //<generated dialogid, initial dialgid>
    //private ConcurrentHashMap<Long, Long> mapRoutingdialogs = new ConcurrentHashMap<>();
    private final FastMap<Long, Long> mapRoutingdialogs = new FastMap<Long, Long>();
    private boolean maintenancemode = false;

    private final Map<String, SsRouteRules> rR = new HashMap<>();
    private final Map<String, SsRouteRules> prR = new HashMap<>();

    private final ConcurrentHashMap<Long, Boolean> isEriStyle = new ConcurrentHashMap<>();
    private transient Map<Long, UssMessage> ussMsgRespnose = ExpiringMap.builder().expiration(60, TimeUnit.SECONDS).build();
    //	private ConcurrentHashMap<Long, UssMessage> ussMsgRespnose = new ConcurrentHashMap<>();
//	private transient FastMap<String, SsRouteRules> rR = new FastMap<>();
//	private transient FastMap<String, SsRouteRules> prR =  new FastMap<>();
//	private transient FastMap<Long, Boolean> isEriStyle = new FastMap<>();
    //private transient FastMap<Long, UssMessage> ussMsgRespnose = new FastMap<>();

    private WhiteList whiteList;

    /**
     *
     */
    public UssdMapLayer(MAPStack mapStack, TCAPStack tcapStack, SccpStack sccpStack) {
        this.sccpStack = sccpStack;
        this.tcapStack = tcapStack;
        this.mapStack = (MAPStackImpl) mapStack;
        this.mapProvider = this.mapStack.getMAPProvider();
    }

    public void init() {
        LOGGER.debug("Appending Ussd Map Listiner to MapStack ....");
        this.mapProvider.addMAPDialogListener(this);
        this.mapProvider.getMAPServiceSupplementary().addMAPServiceListener(this);
        this.mapProvider.getMAPServiceSupplementary().acivate();
        appWorker = Executors.newFixedThreadPool(Integer.parseInt(this.appSettings.get("app").get("threads")), new UssdgateThreadFactory("MapLayer"));
        //appWorker = new ForkJoinPool(Integer.parseInt(this.appSettings.get("app").get("threads")));
        //Load routing rules from db
        fetchRoutingRule(false);
        fetchRoutingRule(true);
        //init scheduler that well check for modification in db, bad idea look for other way.
        _startSchde();
        //init Expiringhashmap
        LOGGER.debug("Time out: " + this.tcapStack.getDialogIdleTimeout());
        ussMessages = ExpiringMap.builder().expiration(this.tcapStack.getDialogIdleTimeout(), TimeUnit.MILLISECONDS).build();
        ussMsgRespnose = ExpiringMap.builder().expiration(this.tcapStack.getDialogIdleTimeout(), TimeUnit.MILLISECONDS).build();

        //load white list
        whiteList = new WhiteList();
    }

    private void _startSchde() {
        LOGGER.debug("Start Request amount scheduler");
        final Runnable resetCounterTask = new Runnable() {
            public void run() {
                //logger.debug("Reset counters");
                for (Long key : request.keySet()) {
                    request.put(key, 0l);
                }
            }
        };
        try {
            sched.scheduleAtFixedRate(resetCounterTask, 1, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }

    public void stop() {
        sched.shutdown();
        appWorker.shutdown();
        this.mapProvider.removeMAPDialogListener(this);
        this.mapProvider.getMAPServiceSupplementary().removeMAPServiceListener(this);
        this.mapProvider.getMAPServiceSupplementary().deactivate();
        this.ussMessages.clear();
        this.appSettings.clear();
        this.mapRoutingdialogs.clear();
        this.httpDialogs.clear();
        this.rR.clear();
        this.prR.clear();
        this.isEriStyle.clear();

    }

    public void fetchRoutingRule(boolean maintanceRules) {
        /*
		 *   `id` bigint(20) NOT NULL AUTO_INCREMENT,
			  `connectionsps` varchar(255) DEFAULT NULL,
			  `destination_address` varchar(255) DEFAULT NULL,
			  `protocol_type` varchar(255) DEFAULT NULL,
			  `service_code` varchar(255) DEFAULT NULL,
			  `ussd_text` varchar(255) DEFAULT NULL,
			  `ussdsc` varchar(255) DEFAULT NULL,
			  `proxy_mode` int(11) DEFAULT NULL,
         */
        if (maintanceRules) {
            this.prR.clear();
        } else {
            this.rR.clear();
        }

        String query = String.format((maintanceRules)
                ? "select id, connectionsps,destination_address,protocol_type, service_code, ussd_text, ussdsc, proxy_mode   from %s where proxy_mode=1"
                : "select id, connectionsps,destination_address,protocol_type, service_code, ussd_text, ussdsc, proxy_mode   from %s where proxy_mode=0",
                appSettings.get("db").get("routingRuleTable"));
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = this.ds.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                SsRouteRules rr = new SsRouteRules();
                rr.setId(rs.getLong("id"));
                rr.setConnps(rs.getString("connectionsps"));
                rr.setDestAddress(rs.getString("destination_address"));
                rr.setProtocolType(rs.getString("protocol_type"));
                rr.setServiceCode(rs.getString("service_code"));
                rr.setUssdText(rs.getString("ussd_text"));
                rr.setUssdsc(rs.getString("ussdsc"));
                rr.setProxy_mode(rs.getShort("proxy_mode"));
                LOGGER.info("Fetching RR - " + rr.toString());
                if (maintanceRules) {
                    this.prR.put(rr.getUssdText(), rr);
                } else {
                    this.rR.put(rr.getUssdText(), rr);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to fetch routinf rules");
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public void incrementRequest(Long rule_id) {
        Long count = request.get(rule_id);
        if (count == null) {
            count = 0l;
        }
        request.put(rule_id, count + 1);
    }

    public Long countRequests(Long rule_id) {
        Long count = request.get(rule_id);
        if (count == null) {
            count = 0l;
        }
        return count;
    }

    public long getCurentDialogCount() {
        return tcapStack.getCounterProvider().getCurrentDialogsCount();
    }

    public void handleUssProcReq(UssMessage response) {
        LOGGER.debug("Prepare to send response:" + response.toString());
        try {
            if (response.getCharset() == null) {
                response.setCharset("15");
            }
            switch (response.getCharset()) {
                case "15":
                case "72":
                    break;
                default:
                    response.setCharset("15");
                    break;
            }
            int charset = Integer.parseInt(response.getCharset());

//			if ((!"15".equalsIgnoreCase(response.getCharset())) || (!"72".equalsIgnoreCase(response.getCharset()))){
//				LOGGER.error("Unsuported charset code: "+response.getCharset()+", please use 15 for GSM7 and 72 for UCS2");
//			}
            CBSDataCodingSchemeImpl cbs = new CBSDataCodingSchemeImpl(charset);
            USSDString ussdStrObj = this.mapProvider.getMAPParameterFactory().createUSSDString(
                    response.getUssdText(), cbs, Charset.forName("UTF-8"));
            MAPDialogSupplementary dialog = (MAPDialogSupplementary) this.mapProvider.getMAPDialog(response.getDialogId());
            if (dialog == null) {
                LOGGER.warn("Dialogid: " + response.getDialogId() + " expired");
                return;
            }
            long invokeId = response.getInvokeId();
            LOGGER.debug("Setting dialog invoke id to: " + invokeId);
            //dialog.setUserObject(invokeId);    		
            ISDNAddressString msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
                    AddressNature.international_number, NumberingPlan.ISDN, response.getMsisdn());
            LOGGER.debug("Send Dialog - " + dialog);
            LOGGER.debug("Ussd object length: " + ussdStrObj.getEncodedString().length);
            //
            //if("TCAP End".equals(response.getMessageType())){
            if (MAPMessageType.processUnstructuredSSRequest_Response.name().equalsIgnoreCase(response.getMessageType())) {
                LOGGER.debug("addProcessUnstructuredSSResponse invokeId: " + invokeId);
                dialog.addProcessUnstructuredSSResponse(invokeId, cbs, ussdStrObj);
                dialog.close(false);
            } else {
                LOGGER.debug("addUnstructuredSSRequest invokeId: " + invokeId);
                dialog.addUnstructuredSSRequest(cbs, ussdStrObj, null, null/*msisdn*/);
                dialog.send();
            }
        } catch (MAPException e) {
            LOGGER.error("Error while sending UnstructuredSSRequest ", e);
        }
//			catch (UnsupportedEncodingException e) {
//			LOGGER.error("Error while sending UnstructuredSSRequest ", e);
//		}
//			catch (UnsupportedEncodingException e) {
//			logger.error("Error while sending UnstructuredSSRequest ", e);
//		}
    }

    /**
     * initiatePUSSR initiate a ProcessUnstructuredSSRequest
     *
     * @param isdn Msisdn for which ProcessUnstructuredSSRequest is initiated
     * @param ussdtext
     * @param transferType 0 for http, 1 for map routing ussd text
     * @param emap use emap instead of standard map
     * @return Local Dialog Id identificator
     */
    public long initiatePUSSR(String isdn, String ussdtext, int transferType, long initialDialogId, String emap) {
        return initiatePUSSR(isdn, ussdtext, transferType, initialDialogId, emap, null, null, null, null);
    }

    /**
     * Send process process-unstrcutured-ss-request using parameters sepcified
     * by json request.
     *
     * @param request
     * @return
     */
    public long sendProcessUnstructuredSSRequest(JsonMessage request) {
        /*        JsonInvoke invoke = (JsonInvoke)request.getTcap().getComponents().get(0).getValue();
        JsonMap mapMessage = (JsonMap)invoke.component();
        
        JsonMapOperation req = (JsonMapOperation) mapMessage.operation();
        
        MAPParameterFactory mapParameterFactory = this.mapProvider.getMAPParameterFactory();
        JsonTcap tcap = request.getTcap();
        tcap.getDialog().getOriginationReference();
        
        ISDNAddressString origReference = mapParameterFactory.createISDNAddressString(
                AddressNature.international_number,
                NumberingPlan.ISDN, 
                tcap.getDialog().getOriginationReference());
        ISDNAddressString destReference = mapParameterFactory.createISDNAddressString(
                AddressNature.international_number,
                NumberingPlan.ISDN, 
                tcap.getDialog().getDestinationReference());
        
        
        SccpAddress callingPartyAddress = sccpAddress(request.getSccp().getCallingPartyAddress());
        SccpAddress calledPartyAddress = sccpAddress(request.getSccp().getCalledPartyAddress());

        MAPParameterFactory factory = mapProvider.getMAPParameterFactory();
        
        MAPDialogSupplementary mapDialog;
        try {
            CBSDataCodingSchemeImpl cbs = new CBSDataCodingSchemeImpl(0x0f);
            USSDString ussdString = factory.createUSSDString(req.getUssdString(), cbs, null);
            
            ISDNAddressString msisdn = req.getMsisdn() == null ? null :
                    factory.createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, req.getMsisdn());
            
            mapDialog = this.mapProvider.getMAPServiceSupplementary()
                .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2),
                        callingPartyAddress, origReference, calledPartyAddress, destReference);
            
            
            long invokeId = mapDialog.addProcessUnstructuredSSRequest(cbs, ussdString, null, msisdn);                                   
            long dialogId = mapDialog.getLocalDialogId();
            
            if (tcap.getDialog().getMsisdn() != null && tcap.getDialog().getVlrAddress() != null) {
                ISDNAddressString msisdn1 = factory.createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, tcap.getDialog().getMsisdn());
                ISDNAddressString vlrAddress = factory.createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, tcap.getDialog().getVlrAddress());
                mapDialog.addEricssonData(msisdn1, vlrAddress);
            }
            
            httpDialogs.add(dialogId);            
            mapDialog.send();
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sent process unstructured ss request");
            }
            
            storeInDatabase(dialogId, -1, invokeId, "http", req.getMsisdn(), req.getUssdString(), 
                    calledPartyAddress.getSignalingPointCode(), callingPartyAddress.getSignalingPointCode());
            return dialogId;
        } catch (MAPException e) {
            return -1;
          }
         */
        return 0;
    }

    public SccpAddress sccpAddress(JsonSccpAddress address) {
        int ssn = address.getSsn() != null ? address.getSsn() : -1;
        int pc = address.getPc() != null ? address.getPc() : -1;

        ParameterFactory factory = sccpStack.getSccpProvider().getParameterFactory();
        String gti = address.getGtIndicator();

        GlobalTitle gt = null;
        switch (gti) {
            case "0001":
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0010":
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0011":
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0100":
                gt = factory.createGlobalTitle(digits(address), 0, np(address), null, na(address));
                break;
        }

        return new SccpAddressImpl(
                RoutingIndicator.valueOf(address.getRoutingIndicator()),
                gt, pc, ssn);
    }

    private String digits(JsonSccpAddress address) {
        return address.getGlobalTitle().getDigits();
    }

    private org.mobicents.protocols.ss7.indicator.NumberingPlan np(JsonSccpAddress address) {
        return org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(address.getGlobalTitle().getNumberingPlan());
    }

    private NatureOfAddress na(JsonSccpAddress address) {
        return NatureOfAddress.valueOf(address.getGlobalTitle().getNatureOfAddressIndicator());
    }

    private int tt(JsonSccpAddress address) {
        return 0;
    }

    private void storeInDatabase(long dialogId, long initialDialogId, long invokeId,
            String logType, String msisdn, String ussdText, int opc, int dpc) {
        UssMessage ssMapMsg = new UssMessage();
        ssMapMsg.setDialogId(dialogId);
        ssMapMsg.setInvokeId(invokeId);
        ssMapMsg.setMsisdn(msisdn);
        ssMapMsg.setDpc(dpc);
        ssMapMsg.setOpc(opc);
        ssMapMsg.setInTimeStamp(null);
        ssMapMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
        ssMapMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Request.name());
        ssMapMsg.setSource(logType);
        ssMapMsg.setUssdText(ussdText);
        ssMapMsg.setInitialDialogId(initialDialogId);
        ssMapMsg.setCharset("15");
        UssMessage initialMsg = getUssMessages().get(initialDialogId);

        if (initialMsg != null) {
            ssMapMsg.setServiceCode(initialMsg.getServiceCode());
            ssMapMsg.setRouteRule(initialMsg.getRouteRule());
            ssMapMsg.setMaintenanceRouteRule(initialMsg.getSecondaryRoute());
        }
        MapMsgDbWriter worker = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
        try {
            dbWorker.execute(worker);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        keepUssMessage(ssMapMsg.getDialogId(), ssMapMsg);
    }

    public long initiatePUSSR(String isdn, String ussdtext, int transferType, long initialDialogId, String emap,
            String sccpCalledParty, String sccpCallingParty, String dssn, String ossn) {
        String logType = null;
        switch (transferType) {
            case 0:
                logType = "http";
                break;
            case 1:
                logType = "map";
                break;
            default:
                logType = "unkonwn";
        }
        if (this.tcapStack.getMaxDialogs() <= this.getCurentDialogCount()) {
            return -1;
        }
        try {
            MAPParameterFactory mapParameterFactory = this.mapProvider.getMAPParameterFactory();
            ISDNAddressString origReference = mapParameterFactory.createISDNAddressString(AddressNature.international_number,
                    NumberingPlan.ISDN, this.appSettings.get("map").get("serviceCenter"));
            ISDNAddressString destReference = mapParameterFactory.createISDNAddressString(AddressNature.international_number,
                    NumberingPlan.ISDN, isdn);
            //Origination Adddres it is callingParty
            SccpAddress origAddr = this.genSccpAddress((sccpCallingParty == null) ? null : sccpCallingParty, true, ossn);
            //CalledParty
            SccpAddress destAddr = this.genSccpAddress((sccpCalledParty == null) ? isdn : sccpCalledParty, false, dssn);

            MAPDialogSupplementary mapDialog = this.mapProvider.getMAPServiceSupplementary()
                    .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2),
                            origAddr, origReference, destAddr, destReference);
            if (emap != null) {
                mapDialog.addEricssonData(null, null);
            }
            CBSDataCodingSchemeImpl cbs = new CBSDataCodingSchemeImpl(0x0f);
            USSDString ussdString = this.mapProvider.getMAPParameterFactory().createUSSDString(ussdtext, cbs, null);
            ISDNAddressString msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(AddressNature.international_number,
                    NumberingPlan.ISDN, isdn);
            long invokeId = mapDialog.addProcessUnstructuredSSRequest(cbs, ussdString, null, msisdn);
            mapDialog.send();

            long dialogid = mapDialog.getLocalDialogId();
            if (transferType == 1) {
                mapRoutingdialogs.put(dialogid, initialDialogId);
            } else {
                httpDialogs.add(dialogid);
            }

            //invoke DbWriterWorker to log the message to DB
            UssMessage ssMapMsg = new UssMessage();
            ssMapMsg.setDialogId(dialogid);
            ssMapMsg.setMsisdn(isdn);
            ssMapMsg.setInvokeId(invokeId);
            ssMapMsg.setDpc(mapDialog.getRemoteAddress().getSignalingPointCode());
            ssMapMsg.setOpc(mapDialog.getLocalAddress().getSignalingPointCode());
            ssMapMsg.setInTimeStamp(null);
            ssMapMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
            ssMapMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Request.name());
            ssMapMsg.setSource(logType);
            ssMapMsg.setUssdText(ussdtext);
            ssMapMsg.setInitialDialogId(initialDialogId);
            ssMapMsg.setCharset("15");
            UssMessage initialMsg = getUssMessages().get(initialDialogId);
            if (initialMsg != null) {
                ssMapMsg.setServiceCode(initialMsg.getServiceCode());
                ssMapMsg.setRouteRule(initialMsg.getRouteRule());
                ssMapMsg.setMaintenanceRouteRule(initialMsg.getSecondaryRoute());
            }
            MapMsgDbWriter worker = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            try {
                dbWorker.execute(worker);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
            keepUssMessage(ssMapMsg.getDialogId(), ssMapMsg);
            return dialogid;
        } catch (MAPException ex) {
            ex.printStackTrace();
            LOGGER.error("[initiateUSSD]: " + ex.getMessage());
            return -1;
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("[initiateUSSD]: " + ex.getMessage());
            return -1;
        }
    }

    /**
     *
     * @param ussdtext
     * @param dialogid
     * @param transferType 0 for http, 1 for map
     * @param emap
     * @throws Exception
     */
    public void initiateUSSR(String ussdtext, String dialogid, String msisdn, String charset, long invokeId, int transferType, long initialDialogId, String emap)
            throws Exception {
        String logType = null;
        switch (transferType) {
            case 0:
                logType = "http";
                break;
            case 1:
                logType = "map";
                break;
            default:
                logType = "unkonwn";
        }
        Long dialogId = Long.parseLong(dialogid);
        UssMessage ssMapMsg = getUssMessages().get(dialogId);
        if (ssMapMsg == null) {
            throw new Exception("Dialog mising or has expired");
        }
        MAPDialogSupplementary dialog = (MAPDialogSupplementary) this.mapProvider.getMAPDialog(dialogId);
        if (dialog == null) {
            throw new Exception("Map dialog with id: " + dialogid + " didn't exist in MAP stak.");
        }

        int codeSchema = Integer.parseInt(charset);
        if (!(codeSchema == 15 || codeSchema == 72)) {
            throw new Exception("Unsuported charset code, please use 15 for GSM7 and 72 for UCS2");
        }
        CBSDataCodingSchemeImpl cbs = new CBSDataCodingSchemeImpl(codeSchema);
        USSDString ussdStrObj = this.mapProvider.getMAPParameterFactory().createUSSDString(
                ussdtext, cbs, Charset.forName("UTF-8"));
//		LOGGER.debug("Send Dialog - "+ dialog);
//		LOGGER.debug("Ussd object length: "+ ussdStrObj.getEncodedString().length);
//		LOGGER.debug("addUnstructuredSSRequest");
        //dialog.addUnstructuredSSRequest(cbs, ussdStrObj, null, null/*msisdn*/);
        dialog.addUnstructuredSSResponse(invokeId, cbs, ussdStrObj);
        dialog.send();
        if (transferType == 1) {
            mapRoutingdialogs.put(dialogId, initialDialogId);
        } else {
            httpDialogs.add(dialogId);
        }
        //Write to DB		
        ssMapMsg.setIntermediateInvokeId(invokeId);
        ssMapMsg.setInTimeStamp(null);
        ssMapMsg.setOutTimeStamp(new Timestamp(new Date().getTime()));
        ssMapMsg.setMessageType(MAPMessageType.unstructuredSSRequest_Response.name());
        ssMapMsg.setSource(logType);
        ssMapMsg.setUssdText(ussdtext);
        ssMapMsg.setInitialDialogId(initialDialogId);
        MapMsgDbWriter worker = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
        try {
            dbWorker.execute(worker);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
        keepUssMessage(ssMapMsg.getDialogId(), ssMapMsg);
    }

    /**
     * Metod generate SccpAddress to be used in MAP package
     *
     * @param msisdn if null msisdn get value of local GT
     * @param isCallingParty if true dpc = local PC if false dpc = remote
     * available DPC often useed for destination address
     * @return SccpAddress
     * @throws Exception
     */
    protected SccpAddress genSccpAddress(String msisdn, boolean isCallingParty, String sccpSsn) throws Exception {
        GlobalTitle gtG = null;
        Map<String, String> mapSettings = appSettings.get("map");
        int dpc = 0, ssn = 0;
        if (isCallingParty) {
            dpc = Integer.parseInt(mapSettings.get("opc"));
            ssn = Integer.parseInt(mapSettings.get("opcssn"));
        } else {
            ssn = Integer.parseInt(mapSettings.get("dpcssn"));
            //TODO investigate the case with forwarding associations 
            dpc = getAvailableDPC();
            if (dpc == -1) {
                throw new Exception("All routed remote DPC is in pause state.");
            }
            if (mapSettings.containsKey("dstServiceCenter")) {
                msisdn = mapSettings.get("dstServiceCenter");
            }
        }
        if (msisdn == null) {
            msisdn = mapSettings.get("serviceCenter");
        }
        if (sccpSsn != null) {
            try {
                ssn = Integer.parseInt(sccpSsn);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("Wrong ssn is passed to method: " + sccpSsn);
            }
        }
        switch (mapSettings.get("gtType")) {
            case "GT0001":
                //gtG = new GlobalTitle0001Impl(msisdn, NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")));//GlobalTitle.getInstance(NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")), msisdn);
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")));
                break;
            case "GT0010":
                //gtG =  new GlobalTitle0010Impl(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")));//GlobalTitle.getInstance(Integer.parseInt(mapSettings.get("gtTranslationType")), msisdn);
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")));
                break;
            case "GT0011":
//			gtG = GlobalTitle.getInstance(Integer.parseInt(mapSettings.get("gtTranslationType")), 
//					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")), msisdn);
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")),
                        org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")),
                        null);
                break;
            case "GT0100":
//			gtG = GlobalTitle.getInstance(Integer.parseInt(mapSettings.get("gtTranslationType")), 
//					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")), 
//					NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")), msisdn);
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(mapSettings.get("gtTranslationType")),
                        org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(mapSettings.get("gtNumberingPlan")),
                        null, NatureOfAddress.valueOf(mapSettings.get("gtNatureOfAddress")));
                break;
        }
        if (gtG == null) {
            throw new Exception("[MAP] GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        //
        //AddressIndicator aiObj = new AddressIndicator((byte) Integer.parseInt(mapSettings.get("addressIndicator")), SccpProtocolVersion.ITU);
        //SccpAddress sccpAddress = new SccpAddress(RoutingIndicator.valueOf(mapSettings.get("routingIndicator")), dpc, gtG, ssn);
        SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(mapSettings.get("routingIndicator")), gtG, dpc, ssn);

        return sccpAddress;
    }

    private int getAvailableDPC() {
        //ConcurrentHashMap<Long, String> mtpstatus =  ((SccpUnifunStackWrapper)this.sccpStack).getMtpstatus();
        //if ("server".equalsIgnoreCase(cfg.getType())){
        //java.util.Map<String, As[]> routes = new HashMap<>();
        java.util.Map<String, RouteAs> routes = new HashMap<>();
        this.sccpStack.getMtp3UserParts().forEach((id, mtpUserPart) -> {
            if (mtpUserPart instanceof M3UAManagement) {
                routes.putAll(((M3UAManagement) mtpUserPart).getRoute());
            }
        });
        if (routes.size() > 0) {
            //Check dpc from settings
            for (String route : routes.keySet()) {
                String rdpc = route.split(":")[0];
                if (rdpc.equals(appSettings.get("map").get("dpc"))) {
                    //As[] associations = routes.get(route);
                    As[] associations = routes.get(route).getAsArray();
                    for (As assoc : associations) {
                        if (assoc.isUp()) {
                            return Integer.parseInt(appSettings.get("map").get("dpc"));
                        }
                    }
                    break;
                }
            }
            //loop to find another Available PC in case that the main is down.
            for (String route : routes.keySet()) {
                String rdpc = route.split(":")[0];
                As[] associations = routes.get(route).getAsArray();
                for (As assoc : associations) {
                    if (assoc.isUp()) {
                        return Integer.parseInt(rdpc);
                    }
                }
            }
        }
        return -1;

    }

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId,
            MAPErrorMessage mapErrorMessage) {
        LOGGER.debug("[onErrorComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [MAPErrorMessage]: " + mapErrorMessage);
        LOGGER.debug("Error code: " + mapErrorMessage.getErrorCode());
        long dialogId = mapDialog.getLocalDialogId();
        UssMessage ssMapMsg = getUssMessages().remove(dialogId);
        if (ssMapMsg == null) {
            LOGGER.warn("No availabale dialog for: " + mapDialog);
            //TODO if necessary clear all map list 
            return;
        }
        ssMapMsg.setMessageType(MAPMessageType.processUnstructuredSSRequest_Response.name());
        ssMapMsg.setOutTimeStamp(null);
        ssMapMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
        if (httpDialogs.contains(dialogId)) {
            httpDialogs.remove(dialogId);
//			if (ussMsgRespnose.containsKey(dialogId)){
//				ussMsgRespnose.remove(dialogId);
//			}
            ssMapMsg.setSource("http");
            ssMapMsg.setUssdText(" [MAPErrorMessage]: " + mapErrorMessage + "Error code: " + mapErrorMessage.getErrorCode());
            ussMsgRespnose.put(dialogId, ssMapMsg);
            //Store to db incoming request
            try {
                MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
                dbWorker.execute(dbWriter);
            } catch (Exception e) {
                LOGGER.error("Failed to store Supplementary Message to db: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId,
            Problem problem, boolean isLocalOriginated) {
        LOGGER.debug("[onRejectComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [Problem]: " + problem + " [isLocalOriginated]: " + isLocalOriginated);

        mapDialog.release();

    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
        LOGGER.debug("[onInvokeTimeout]: " + mapDialog + " [invokeId]: " + invokeId);
        mapDialog.release();
        _cleareDialogQueue(mapDialog.getLocalDialogId());

    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {
        LOGGER.debug("[onMAPMessage]: " + mapMessage);

    }

    @Override
    public void onProcessUnstructuredSSRequest(ProcessUnstructuredSSRequest procUnstrReqInd) {
        LOGGER.info("Received PROCESS_UNSTRUCTURED_SS_REQUEST_INDICATION for MAP Dialog Id "
                + procUnstrReqInd.getMAPDialog().getLocalDialogId() + " MSISDN: " + procUnstrReqInd.getMSISDNAddressString());
        LOGGER.info("Invoke ID " + procUnstrReqInd.getInvokeId());
        boolean isEmap = false;
        if (isEriStyle.containsKey(procUnstrReqInd.getMAPDialog().getLocalDialogId())) {
            isEmap = isEriStyle.remove(procUnstrReqInd.getMAPDialog().getLocalDialogId());
        }
        SupplementaryMessageProcessor worker = new SupplementaryMessageProcessor(this, whiteList, ds, dbWorker, rR, prR, procUnstrReqInd, isEmap, maintenancemode);
        try {
            appWorker.execute(worker);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onProcessUnstructuredSSResponse(ProcessUnstructuredSSResponse procUnstrResInd) {
        long dialogId = procUnstrResInd.getMAPDialog().getLocalDialogId();
        //long invokeId = procUnstrResInd.getInvokeId();
        LOGGER.info("Received Process UNSTRUCTURED_SS_RESPONSE_INDICATION for MAP Dialog Id "
                + dialogId);
        UssMessage ssMapMsg = getUssMessages().remove(dialogId);
        if (ssMapMsg == null) {
            LOGGER.warn("No availabale dialog for: " + procUnstrResInd);
            //TODO if necessary clear all map list 
            return;
        }
        //Extracting ussdtext
        byte[] ussdbytes = procUnstrResInd.getUSSDString().getEncodedString();
        if ("UCS2".equalsIgnoreCase(procUnstrResInd.getDataCodingScheme().getCharacterSet().name())) {
            ssMapMsg.setUssdText(new String(ussdbytes, Charset.forName("UTF-16")));
            ssMapMsg.setCharset("72");
        } else {
            ssMapMsg.setCharset("15");
            try {
                ssMapMsg.setUssdText(procUnstrResInd.getUSSDString().getString(Charset.forName("UTF-8")));
            } catch (MAPException e) {
                LOGGER.error("Failed to decode ussd message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        ssMapMsg.setMessageType(procUnstrResInd.getMessageType().name());
        ssMapMsg.setOutTimeStamp(null);
        ssMapMsg.setInTimeStamp(new Timestamp(new Date().getTime()));

        if (httpDialogs.contains(dialogId)) {
            httpDialogs.remove(dialogId);
//			if (ussMsgRespnose.containsKey(dialogId)){
//				ussMsgRespnose.remove(dialogId);
//			}
            ssMapMsg.setSource("http");
            ussMsgRespnose.put(dialogId, ssMapMsg);
            //Store to db incoming request
            try {
                MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
                dbWorker.execute(dbWriter);
            } catch (Exception e) {
                LOGGER.error("Failed to store Supplementary Message to db: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }
        if (mapRoutingdialogs.containsKey(dialogId)) {
            long initialDialogId = mapRoutingdialogs.remove(dialogId);
            ssMapMsg.setSource("map");
            ssMapMsg.setInitialDialogId(initialDialogId);
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            MapMsgHandler msgHandlerWorker = new MapMsgHandler(procUnstrResInd, initialDialogId, this);
            try {
                appWorker.execute(msgHandlerWorker);
                dbWorker.execute(dbWriter);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onUnstructuredSSRequest(UnstructuredSSRequest unstrReqInd) {
        LOGGER.info("Received UNSTRUCTURED_SS_REQUEST_INDICATION for MAP Dialog Id "
                + unstrReqInd.getMAPDialog().getLocalDialogId());
        LOGGER.info("Invoke ID " + unstrReqInd.getInvokeId());
        long dialogId = unstrReqInd.getMAPDialog().getLocalDialogId();
        long invokeId = unstrReqInd.getInvokeId();
        UssMessage ssMapMsg = getUssMessages().get(dialogId);
        if (ssMapMsg == null) {
            LOGGER.warn("No availabale dialog for: " + unstrReqInd);
            //TODO if necessary clear all map list 
            return;
        }
        //Extracting ussdtext
        byte[] ussdbytes = unstrReqInd.getUSSDString().getEncodedString();
        if ("UCS2".equalsIgnoreCase(unstrReqInd.getDataCodingScheme().getCharacterSet().name())) {
            ssMapMsg.setUssdText(new String(ussdbytes, Charset.forName("UTF-16")));
            ssMapMsg.setCharset("72");
        } else {
            ssMapMsg.setCharset("15");
            try {
                ssMapMsg.setUssdText(unstrReqInd.getUSSDString().getString(Charset.forName("UTF-8")));
            } catch (MAPException e) {
                LOGGER.error("Failed to decode ussd message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        ssMapMsg.setMessageType(unstrReqInd.getMessageType().name());
        ssMapMsg.setIntermediateInvokeId(invokeId);
        ssMapMsg.setOutTimeStamp(null);
        ssMapMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
        if (httpDialogs.contains(dialogId)) {
            httpDialogs.remove(dialogId);
//			if (ussMsgRespnose.containsKey(dialogId)){
//				ussMsgRespnose.remove(dialogId);
//			}
            ssMapMsg.setSource("http");
            ussMsgRespnose.put(dialogId, ssMapMsg);
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            try {
                dbWorker.execute(dbWriter);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        } else if (mapRoutingdialogs.containsKey(dialogId)) {
            long initialDialogId = mapRoutingdialogs.remove(dialogId);
            ssMapMsg.setSource("map");
            ssMapMsg.setInitialDialogId(initialDialogId);
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            MapMsgHandler msgHandlerWorker = new MapMsgHandler(unstrReqInd, initialDialogId, this);
            try {
                appWorker.execute(msgHandlerWorker);
                dbWorker.execute(dbWriter);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        } else {
            boolean isEmap = false;
            if (isEriStyle.containsKey(dialogId)) {
                isEmap = isEriStyle.remove(dialogId);
            }
            SupplementaryMessageProcessor worker = new SupplementaryMessageProcessor(this, whiteList, ds, dbWorker, rR, prR, unstrReqInd, isEmap, maintenancemode);
            try {
                appWorker.execute(worker);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUnstructuredSSResponse(UnstructuredSSResponse unstrResInd) {
        LOGGER.info("Received UNSTRUCTURED_SS_RESPONSE_INDICATION for MAP Dialog Id "
                + unstrResInd.getMAPDialog().getLocalDialogId());
        LOGGER.info("Invoke ID " + unstrResInd.getInvokeId());
        long dialogId = unstrResInd.getMAPDialog().getLocalDialogId();
        long invokeId = unstrResInd.getInvokeId();
        UssMessage ssMapMsg = getUssMessages().get(dialogId);
        if (ssMapMsg == null) {
            LOGGER.warn("No availabale dialog for: " + unstrResInd);
            //TODO if necessary clear all map list 
            return;
        }
        //Extracting ussdtext
        byte[] ussdbytes = unstrResInd.getUSSDString().getEncodedString();
        if ("UCS2".equalsIgnoreCase(unstrResInd.getDataCodingScheme().getCharacterSet().name())) {
            ssMapMsg.setUssdText(new String(ussdbytes, Charset.forName("UTF-16")));
            ssMapMsg.setCharset("72");
        } else {
            ssMapMsg.setCharset("15");
            try {
                ssMapMsg.setUssdText(unstrResInd.getUSSDString().getString(Charset.forName("UTF-8")));
            } catch (MAPException e) {
                LOGGER.error("Failed to decode ussd message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        ssMapMsg.setMessageType(unstrResInd.getMessageType().name());
        ssMapMsg.setIntermediateInvokeId(invokeId);
        ssMapMsg.setOutTimeStamp(null);
        ssMapMsg.setInTimeStamp(new Timestamp(new Date().getTime()));
        if (httpDialogs.contains(dialogId)) {
            httpDialogs.remove(dialogId);
//			if (ussMsgRespnose.containsKey(dialogId)){
//				ussMsgRespnose.remove(dialogId);
//			}				
            ssMapMsg.setSource("http");
            ussMsgRespnose.put(dialogId, ssMapMsg);
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            try {
                dbWorker.execute(dbWriter);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        } else if (mapRoutingdialogs.containsKey(dialogId)) {
            long initialDialogId = mapRoutingdialogs.remove(dialogId);
            ssMapMsg.setSource("map");
            ssMapMsg.setInitialDialogId(initialDialogId);
            MapMsgDbWriter dbWriter = new MapMsgDbWriter(ds, ssMapMsg, appSettings.get("db").get("mapMsgWrProc"));
            MapMsgHandler msgHandlerWorker = new MapMsgHandler(unstrResInd, initialDialogId, this);
            try {
                appWorker.execute(msgHandlerWorker);
                dbWorker.execute(dbWriter);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        } else {
            boolean isEmap = false;
            if (isEriStyle.containsKey(dialogId)) {
                isEmap = isEriStyle.remove(dialogId);
            }
            SupplementaryMessageProcessor worker = new SupplementaryMessageProcessor(this, whiteList, ds, dbWorker, rR, prR, unstrResInd, isEmap, maintenancemode);
            try {
                appWorker.execute(worker);
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onUnstructuredSSNotifyRequest(
            UnstructuredSSNotifyRequest unstrNotifyInd) {

    }

    @Override
    public void onUnstructuredSSNotifyResponse(
            UnstructuredSSNotifyResponse unstrNotifyInd) {

    }

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
        LOGGER.debug(String.format("onDialogDelimiter for  remote Dialog=%d, local Dialog=%d", mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId()));
        LOGGER.info("Rx :  onDialogDelimiter " + mapDialog);
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog,
            AddressString destReference, AddressString origReference,
            MAPExtensionContainer extensionContainer) {
        LOGGER.debug(String
                .format("onDialogRequest for remote DialogId=%d, local DialogId=%d DestinationReference=%s OriginReference=%s MAPExtensionContainer=%s",
                        mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId(), destReference, origReference, extensionContainer));

    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog,
            MAPExtensionContainer extensionContainer) {
        LOGGER.debug(String.format("onDialogAccept for remote DialogId=%d , local DialogId=%d MAPExtensionContainer=%s",
                mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId(), extensionContainer));

    }

    @Override
    public void onDialogReject(MAPDialog mapDialog,
            MAPRefuseReason refuseReason,
            ApplicationContextName alternativeApplicationContext,
            MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format("onDialogReject for remote DialogId=%d , local DialogId=%d MAPExtensionContainer=%s",
                mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId(), extensionContainer));
        //TODO: terminate connection
        _cleareDialogQueue(mapDialog.getLocalDialogId());

    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog,
            MAPUserAbortChoice userReason,
            MAPExtensionContainer extensionContainer) {
        long dialogId = mapDialog.getLocalDialogId();
        LOGGER.info(String.format("onDialogUserAbort for DialogId=%d MAPUserAbortChoice=%s MAPExtensionContainer=%s",
                dialogId, userReason, extensionContainer));

        //TODO: terminateProtocolConnection
        //Terminate sub sessions
        Map<Long, UssMessage> subDialogs = new HashMap<>();
        try {
            for (Entry<Long, UssMessage> entry : getUssMessages().entrySet()) {
                Long n = entry.getKey();
                UssMessage tmp = entry.getValue();
                if (tmp.getInitialDialogId() == dialogId) {
                    subDialogs.put(n, tmp);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
        subDialogs.forEach((key, value) -> {
            MAPDialog dialog = this.mapProvider.getMAPDialog(value.getDialogId());
            LOGGER.info("Send abort message for dialog: " + dialog.toString());
            if (dialog != null) {
                try {
                    dialog.abort(userReason);
                } catch (Exception e) {
                    LOGGER.warn("Some error ocure while trying to close sub dialog: " + value.getDialogId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            this.ussMessages.remove(value.getDialogId());
        });
        _cleareDialogQueue(dialogId);
        subDialogs.clear();
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog,
            MAPAbortProviderReason abortProviderReason,
            MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format("onDialogProviderAbort for remote DialogId=%d , local DialogId=%d MAPExtensionContainer=%s",
                mapDialog.getLocalDialogId(), mapDialog.getLocalDialogId(), extensionContainer));
        //TODO: terminate connection
        _cleareDialogQueue(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        LOGGER.debug(String.format("onDialogClose for  remote Dialog=%d, local Dialog=%d", mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId()));
        //TODO: terminate connection
        //_cleareDialogQueue(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog,
            MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
        LOGGER.info(String.format("onDialogNotice for remote DialogId=%d, local DialogId=%d, MAPNoticeProblemDiagnostic=%s ",
                mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId(), noticeProblemDiagnostic));

    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        LOGGER.info(String.format("onDialogRelease for  remote Dialog=%d, local Dialog=%d", mapDialog.getLocalDialogId(), mapDialog.getLocalDialogId()));
        //_cleareDialogQueue(mapDialog.getLocalDialogId());

    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        LOGGER.info(String.format("onDialogTimeout for remote DialogId=%d, local DialogId=%d", mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId()));
        /*if (timeoutDialog.contains(mapDialog.getLocalDialogId())){
			timeoutDialog.remove(mapDialog.getLocalDialogId());
		}else{
			mapDialog.keepAlive();
			timeoutDialog.add(mapDialog.getLocalDialogId());
		}
         */
        //clear all queue
        long dialogid = mapDialog.getLocalDialogId();
        _cleareDialogQueue(dialogid);
        if (ussMsgRespnose.containsKey(dialogid)) {
            ussMsgRespnose.remove(dialogid);
        }
    }

    private void _cleareDialogQueue(long dialogid) {
        if (isEriStyle.containsKey(dialogid)) {
            isEriStyle.remove(dialogid);
        }
//		if (ussMsgRespnose.containsKey(dialogid))
//			ussMsgRespnose.remove(dialogid);
        if (httpDialogs.contains(dialogid)) {
            httpDialogs.remove(dialogid);
        }
        if (mapRoutingdialogs.containsKey(dialogid)) {
            mapRoutingdialogs.remove(dialogid);
        }
        if (ussMessages.containsKey(dialogid)) {
            ussMessages.remove(dialogid);
        }
    }

    private void _cleareDialogSubsesion(long dialogid) {
        //Terminate sub sessions
        Map<Long, UssMessage> subDialogs
                = getUssMessages().entrySet().parallelStream()
                .filter(value -> value.getValue().getInitialDialogId() == dialogid)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        subDialogs.forEach((key, value) -> {
            MAPDialog dialog = this.mapProvider.getMAPDialog(value.getDialogId());
            LOGGER.info("Send abort message for dialog: " + dialog.toString());
            if (dialog != null) {
                try {
                    dialog.close(true);
                } catch (Exception e) {
                    LOGGER.warn("Some error ocure while trying to close sub dialog: " + value.getDialogId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public DataSource getDs() {
        return ds;
    }

    public void setDs(DataSource ds) {
        this.ds = ds;
    }

    public ExecutorService getDbWorker() {
        return dbWorker;
    }

    public void setDbWorker(ExecutorService dbWorker) {
        this.dbWorker = dbWorker;
    }

    public Map<String, Map<String, String>> getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(Map<String, Map<String, String>> appSettings) {
        this.appSettings = appSettings;
    }

    public void keepUssMessage(long dialogId, UssMessage message) {
        synchronized (this.ussMessages) {
            this.ussMessages.put(dialogId, message);
        }

    }

    public Map<Long, UssMessage> getUssMessages() {
        return ussMessages;
    }

    public MAPProvider getMapProvider() {
        return mapProvider;
    }

    /**
     * @param ussMsg
     * @return
     */
    public UssMessage findSubsession(long dialogid) {
        Optional<Entry<Long, UssMessage>> subDialogs
                = getUssMessages().entrySet().parallelStream()
                .filter(value -> value.getValue().getInitialDialogId() == dialogid)
                .findFirst();
        return subDialogs.get().getValue();
    }

    //public ConcurrentHashMap<Long, UssMessage> getUssMsgRespnose() {
//	public FastMap<Long, UssMessage> getUssMsgRespnose() {
    public Map<Long, UssMessage> getUssMsgRespnose() {
        return ussMsgRespnose;
    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onRegisterSSRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterSSRequest)
     */
    @Override
    public void onRegisterSSRequest(RegisterSSRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onRegisterSSResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterSSResponse)
     */
    @Override
    public void onRegisterSSResponse(RegisterSSResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onEraseSSRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.EraseSSRequest)
     */
    @Override
    public void onEraseSSRequest(EraseSSRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onEraseSSResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.EraseSSResponse)
     */
    @Override
    public void onEraseSSResponse(EraseSSResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onActivateSSRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.ActivateSSRequest)
     */
    @Override
    public void onActivateSSRequest(ActivateSSRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onActivateSSResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.ActivateSSResponse)
     */
    @Override
    public void onActivateSSResponse(ActivateSSResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onDeactivateSSRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.DeactivateSSRequest)
     */
    @Override
    public void onDeactivateSSRequest(DeactivateSSRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onDeactivateSSResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.DeactivateSSResponse)
     */
    @Override
    public void onDeactivateSSResponse(DeactivateSSResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onInterrogateSSRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.InterrogateSSRequest)
     */
    @Override
    public void onInterrogateSSRequest(InterrogateSSRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onInterrogateSSResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.InterrogateSSResponse)
     */
    @Override
    public void onInterrogateSSResponse(InterrogateSSResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onGetPasswordRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.GetPasswordRequest)
     */
    @Override
    public void onGetPasswordRequest(GetPasswordRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onGetPasswordResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.GetPasswordResponse)
     */
    @Override
    public void onGetPasswordResponse(GetPasswordResponse response) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onRegisterPasswordRequest(org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterPasswordRequest)
     */
    @Override
    public void onRegisterPasswordRequest(RegisterPasswordRequest request) {
        LOGGER.info(request.toString());

    }

    /* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.map.api.service.supplementary.MAPServiceSupplementaryListener#onRegisterPasswordResponse(org.mobicents.protocols.ss7.map.api.service.supplementary.RegisterPasswordResponse)
     */
    @Override
    public void onRegisterPasswordResponse(RegisterPasswordResponse response) {
        LOGGER.info(request.toString());

    }

    public boolean isMaintenancemode() {
        return maintenancemode;
    }

    public void setMaintenancemode(boolean maintenancemode) {
        this.maintenancemode = maintenancemode;
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference, AddressString eriMsisdn, AddressString eriVlrNo) {
        LOGGER.debug(String.format(
                "onDialogRequestEricsson for remote DialogId=%d, local DialogId=%d DestinationReference=%s OriginReference=%s ",
                mapDialog.getRemoteDialogId(), mapDialog.getLocalDialogId(), destReference, origReference));
        //FIXME make sure that it didn't conduct to some memory resources leak
        isEriStyle.put(mapDialog.getLocalDialogId(), true);
    }

}
