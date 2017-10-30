package com.unifun.sigtran.checksubscriber;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.AuthenticationFailureReportRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.AuthenticationFailureReportResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ForwardCheckSSIndicationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ResetRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeRequest_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeResponse_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeSubscriptionInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeSubscriptionInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.DomainType;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.DeleteSubscriberDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.DeleteSubscriberDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.NoteSubscriberPresentRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReadyForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReadyForSMResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_MTI;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.checksubscriber.servlets.MapMessagesCache;

import net.jodah.expiringmap.ExpiringMap;



/**
 * @author <a href="mailto:romanbabin@gmail.com">Roman Babin </a>
 *
 */
public class MapLayer implements MAPDialogListener, MAPServiceSmsListener, MAPServiceMobilityListener {

    public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[MapLayer"));
    private final ProtocolIdentifierImpl pi = new ProtocolIdentifierImpl(0);
    // private MAPStackImpl mapStack;
    private MAPProvider mapProvider;
    private MAPApplicationContext mtFoSMSMAPApplicationContext;
    private SccpAddress serviceCenterSCCPAddress = null;
    private SccpAddress hlrSCCPAddress = null;
    private AddressString serviceCenterAddress;
    private MapMessagesCache mapMessageCache;
    //private MapLayerPreference cfg;
    private Map<String, Map<String,String>> appSettings = null;
    private ExecutorService pool;
    private SccpStack sccpStack;
    //private Set<Long> smsdialogs = new HashSet<>();
    private Map<Long,Long> smsdialogs = ExpiringMap.builder().expiration(60, TimeUnit.SECONDS).build();
   //private ConcurrentHashMap<Long, Long> srismsdialogs = new ConcurrentHashMap<>();
    private Map<Long, Long> srismsdialogs = ExpiringMap.builder().expiration(60, TimeUnit.SECONDS).build();
   // private ConcurrentHashMap<Long, Long> smsResponse = new ConcurrentHashMap<>();

    public MapLayer(MAPStackImpl mapStack,SccpStack sccpStack, Map<String, Map<String,String>> appSettings, ExecutorService pool) {
        this.mapProvider = mapStack.getMAPProvider();
        this.sccpStack = sccpStack;
        this.appSettings = appSettings;
        this.pool = pool;
    }

    public boolean init() {
        try {
            logger.debug("Initializing LbsMapLayer ....");
            if(this.mapProvider==null)
            	return false;
            //logger.debug("this.mapProvider = " + this.mapProvider);
            this.mapProvider.addMAPDialogListener(this);
            this.mapProvider.getMAPServiceSms().addMAPServiceListener(this);
            this.mapProvider.getMAPServiceSms().acivate();
            this.mapProvider.getMAPServiceMobility().addMAPServiceListener(this);
            this.mapProvider.getMAPServiceMobility().acivate();
            
           // mapMessageCache = MapMessagesCache.getInstance();
            mapMessageCache = new MapMessagesCache();

//            this.mapStack.start();
            logger.debug("Initialized LbsMapLayer ....");
            return true;
        } catch (Exception ex) {
            logger.error("[initMap]: " + ex.getMessage());
        }
        return false;
    }

    public void stop() {
        logger.debug("Stopping MAP Stack ....");
        this.mapProvider.getMAPServiceSms().removeMAPServiceListener(this);        
        this.mapProvider.getMAPServiceMobility().removeMAPServiceListener(this);
        this.mapProvider.removeMAPDialogListener(this);
        this.mapProvider.getMAPServiceMobility().deactivate();
        //this.mapProvider.getMAPServiceSupplementary().deactivate();
        this.mapProvider.getMAPServiceSms().deactivate();
        //this.mapStack.stop();
        logger.debug("Stopped MAP Stack ....");
    }
    
	
   
    private MAPDialogSms prepareSRI(String msisdnSubs) throws Exception{
		MAPDialogSms mapDialog;        
		SccpAddress origAddr = getServiceCenterSccpAddress();
		SccpAddress destAddr = getHlrSccpAddress();
		//SccpAddress destAddr = getServiceCenterSccpAddress(msisdnSubs);
		mapDialog = this.mapProvider.getMAPServiceSms().createNewDialog(
				MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version3),
				origAddr, null, destAddr, null);


		ISDNAddressString msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
				AddressNature.international_number, NumberingPlan.ISDN, msisdnSubs);

		AddressString scAddress = this.mapProvider.getMAPParameterFactory().createAddressString(AddressNature.international_number, NumberingPlan.ISDN, 
				this.appSettings.get("map").get("serviceCenter"));

		//mapDialog.addSendRoutingInfoForSMRequest(msisdn, true, scAddress, null, true, SM_RP_MTI.SMS_Deliver, null, null);
		mapDialog.addSendRoutingInfoForSMRequest(msisdn, true, scAddress, null, true, SM_RP_MTI.SMS_Deliver, null, null, false, null, false, false, null);
		return mapDialog;
	}
    
    public long sendSriSm(String msisdnSubs) {
		try{
			MAPDialogSms mapDialog = prepareSRI(msisdnSubs);

            mapDialog.send();
            return mapDialog.getLocalDialogId();
        } catch (MAPException ex) {
            logger.error("[sendSriSm]: " + ex.getMessage());
            return -1;
        } catch (Exception ex) {
            logger.error("[sendSriSm]: " + ex.getMessage());
            return -1;
        }
    }
    
	public long sendServiceSMS(String msisdn) {
		try{
			MAPDialogSms mapDialog = prepareSRI(msisdn);
			long dialogId = mapDialog.getLocalDialogId();
			smsdialogs.put(dialogId,null);
			mapDialog.send();
			return dialogId;
		}catch (Exception ex){
			logger.error("[sendServiceSMS]: " + ex.getMessage());
            return -1;
		}		
	}
    
    public long sendRds(String msisdn){  
    	try {
    		MAPDialogSms mapDialogSms = null;
    		SccpAddress origAddr = getServiceCenterSccpAddress();
    		SccpAddress destAddr = getServiceCenterSccpAddress(msisdn);    		
    		mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(
    				MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version2), 
    				origAddr, null, destAddr, null);
    		ISDNAddressString calledPartyAddress = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
                    AddressNature.international_number, NumberingPlan.ISDN, msisdn);    		
    		mapDialogSms.addReportSMDeliveryStatusRequest(calledPartyAddress, getServiceCenterAddressString(), 
    				SMDeliveryOutcome.getInstance(1),
    				null, null, false, false, null, null);    		
    		mapDialogSms.send();
    		return mapDialogSms.getLocalDialogId();
		} catch (MAPException ex) {
			logger.error("[sendSriSm]: " + ex.getMessage());
            return -1; 
		} catch (Exception ex) {
			logger.error("[sendSriSm]: " + ex.getMessage());
            return -1;
		}
    	
//    	try {
//    		MAPDialogSms mapDialogSms = null;
//    		SccpAddress origAddr = getServiceCenterSccpAddress();
//    		SccpAddress destAddr = getServiceCenterSccpAddress(msisdn);    		
//            MAPApplicationContext appCnt = null;
//            appCnt = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext,
//                    MAPApplicationContextVersion.version1);
//
////    		mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(
////    				MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version1), 
////    				origAddr, null, destAddr, null);
//            mapDialogSms =this.mapProvider.getMAPServiceSms().createNewDialog(appCnt, origAddr, null,
//                    destAddr, null);
//    		ISDNAddressString calledPartyAddress = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
//                    AddressNature.international_number, NumberingPlan.ISDN, msisdn);    		
//    	    /**
//    	     * Sending MAP-SEND-ROUTING-INFO-FOR-SM request
//    	     *
//    	     * @param msisdn mandatory
//    	     * @param serviceCentreAddress mandatory
//    	     * @param sMDeliveryOutcome mandatory
//    	     * @param sbsentSubscriberDiagnosticSM mandatory
//    	     * @param extensionContainer optional
//    	     * @param gprsSupportIndicator optional
//    	     * @param deliveryOutcomeIndicator optional
//    	     * @param additionalSMDeliveryOutcome optional
//    	     * @param additionalAbsentSubscriberDiagnosticSM optional
//    	     * @return
//    	     * @throws MAPException
//    	     */
//    		mapDialogSms.addReportSMDeliveryStatusRequest(calledPartyAddress, getServiceCenterAddressString(), null, null, null, false, false, null,
//                    null);
////    		mapDialogSms.addReportSMDeliveryStatusRequest(
////    				calledPartyAddress, 
////    				getServiceCenterAddressString(), 
////    				SMDeliveryOutcome.getInstance(-1),
////    				null, 
////    				null, 
////    				false, 
////    				false, 
////    				null, 
////    				null);    		
//    		mapDialogSms.send();
//    		return mapDialogSms.getLocalDialogId();
//		} catch (MAPException ex) {
//			logger.error("[sendRDS]: " + ex.getMessage());
//			ex.printStackTrace();
//            return -1; 
//		} catch (Exception ex) {
//			logger.error("[sendRDS]: " + ex.getMessage());
//			ex.printStackTrace();
//            return -1;
//		}
    }

    public long sendPsi(String imsi, String vlrAddress) {
        MAPDialogMobility mapDialog;
        try {
            SccpAddress origAddr = getServiceCenterSccpAddress();
            SccpAddress destAddr = getHlrSccpAddress(vlrAddress);
            mapDialog = this.mapProvider.getMAPServiceMobility().createNewDialog(
                    MAPApplicationContext.getInstance(MAPApplicationContextName.subscriberInfoEnquiryContext, MAPApplicationContextVersion.version3),
                    origAddr, null, destAddr, null);

            IMSI imsiSubs = new IMSIImpl(imsi);
            RequestedInfo ri = new RequestedInfoImpl(true, true, null, true, DomainType.csDomain, false, false, false);

            mapDialog.addProvideSubscriberInfoRequest(imsiSubs, null, ri, null, null);
            mapDialog.send();
            return mapDialog.getLocalDialogId();
        } catch (MAPException ex) {
            logger.error("[sendPsi]: " + ex.getMessage());
            return -1;
        } catch (Exception ex) {
            logger.error("[sendPsi]: " + ex.getMessage());
            return -1;
        }
    }
    
    public long sendATI(String msisdn){
    	MAPDialogMobility mapDialog;
    	try {
    		SccpAddress origAddr = getServiceCenterSccpAddress();
    		SccpAddress destAddr = getHlrSccpAddress();
    		mapDialog = this.mapProvider.getMAPServiceMobility().createNewDialog(
    				MAPApplicationContext.getInstance(MAPApplicationContextName.anyTimeEnquiryContext, MAPApplicationContextVersion.version3),
    				origAddr, null, destAddr, null);

    		//IMSI imsi = this.mapParameterFactory.createIMSI("33334444");
    		ISDNAddressString msisdnObj = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
    				AddressNature.international_number, NumberingPlan.ISDN, msisdn);
    		SubscriberIdentity subscriberIdentity = this.mapProvider.getMAPParameterFactory().createSubscriberIdentity(msisdnObj);//this.mapParameterFactory.createSubscriberIdentity(imsi);
    		RequestedInfo requestedInfo = this.mapProvider.getMAPParameterFactory().createRequestedInfo(true, true, null, false, null, false, false,
    				false);
    		ISDNAddressString gsmSCFAddress =  this.mapProvider.getMAPParameterFactory().createISDNAddressString(AddressNature.international_number,
    				NumberingPlan.ISDN, this.appSettings.get("map").get("serviceCenter") );

    		mapDialog.addAnyTimeInterrogationRequest(subscriberIdentity, requestedInfo, gsmSCFAddress, null);
    		mapDialog.send();
    		return mapDialog.getLocalDialogId();
    	} catch (MAPException ex) {
    		logger.error("[sendPsi]: " + ex.getMessage());
    		return -1;
    	} catch (Exception ex) {
    		logger.error("[sendPsi]: " + ex.getMessage());
    		return -1;
    	}
    }

    private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) throws Exception {
        GlobalTitle gtG = null;        
        switch (this.appSettings.get("map").get("gtmscType")) {
            case "GT0001":                
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(networkNodeNumber.getAddress(), 
                		NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
                break;
            case "GT0010":                
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(networkNodeNumber.getAddress(), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")));
                break;
            case "GT0011":
                //gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), networkNodeNumber.getAddress());
                gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(networkNodeNumber.getAddress(), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
    					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
    					null);
                break;
            case "GT0100":
                //gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), networkNodeNumber.getAddress());
            	gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(networkNodeNumber.getAddress(), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
    					org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
    					null, NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
            	break;
        }
        if (gtG == null) {
            throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(this.appSettings.get("map").get("ri")), gtG, Integer.parseInt(this.appSettings.get("map").get("dpc")), Integer.parseInt(this.appSettings.get("map").get("dpcssn")));
        return sccpAddress;
    }

    private MAPDialogSms setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponse evt, String smsText) throws MAPException, Exception {
        MAPDialogSms mapDialogSmsLoc = this.mapProvider.getMAPServiceSms().createNewDialog(this.getMtFoSMSMAPApplicationContext(),
                this.getServiceCenterSccpAddress(), null, this.getMSCSccpAddress(evt.getLocationInfoWithLMSI().getNetworkNodeNumber()), null);
        SM_RP_DA sm_RP_DA = this.mapProvider.getMAPParameterFactory().createSM_RP_DA(evt.getIMSI());
        SM_RP_OA sm_RP_OA = this.mapProvider.getMAPParameterFactory().createSM_RP_OA_ServiceCentreAddressOA(this.getServiceCenterAddressString());
        UserDataImpl ud = new UserDataImpl(null/*smsText*/, new DataCodingSchemeImpl(0), null, null);
        Calendar cal = Calendar.getInstance();
        //cal.add(Calendar.HOUR_OF_DAY, 3);
        AbsoluteTimeStampImpl serviceCentreTimeStamp = new AbsoluteTimeStampImpl((cal.get(Calendar.YEAR) % 100 ), 
        		cal.get(Calendar.MONTH), 
        		cal.get(Calendar.DAY_OF_MONTH), 
        		cal.get(Calendar.HOUR_OF_DAY), 
        		cal.get(Calendar.MINUTE), 
        		cal.get(Calendar.SECOND), 
        		cal.get(Calendar.ZONE_OFFSET));
        SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true, this.getSmsTpduOriginatingAddress(), new ProtocolIdentifierImpl(64),
                serviceCentreTimeStamp, ud);
        SmsSignalInfoImpl smsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);
        mapDialogSmsLoc.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, smsSignalInfoImpl, false, null);
        return mapDialogSmsLoc;
    }

    /**
     * This is SCCP Address for the subscriber
     *
     * @return
     * @throws Exception
     */
    private SccpAddress getServiceCenterSccpAddress(String msisdn) throws Exception {
    	GlobalTitle gtG = null;
    	switch (this.appSettings.get("map").get("gtmscType")) {
    	case "GT0001":                
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, 
    				NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
    		break;
    	case "GT0010":                
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(this.appSettings.get("map").get("gtmscTT")));
    		break;
    	case "GT0011":            
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
    				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
    				null);
    		break;
    	case "GT0100":            
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(msisdn, Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
    				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
    				null, NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
    		break;
    	}
    	if (gtG == null) {
    		throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
    	}
    	SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(this.appSettings.get("map").get("ri")), gtG, Integer.parseInt(this.appSettings.get("map").get("dpc")), Integer.parseInt(this.appSettings.get("map").get("dpcssn")));
    	return sccpAddress;
    }

    /**
     * This is our (Service Center) SCCP Address for GT
     *
     * @return
     */
    private SccpAddress getServiceCenterSccpAddress() throws Exception {
        if (this.serviceCenterSCCPAddress == null) {
            GlobalTitle gtG = null;            
            switch (this.appSettings.get("map").get("gtmscType")) {
        	case "GT0001":                
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("serviceCenter"), 
        				NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
        		break;
        	case "GT0010":                
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("serviceCenter"), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")));
        		break;
        	case "GT0011":            
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("serviceCenter"), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
        				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
        				null);
        		break;
        	case "GT0100":            
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("serviceCenter"), Integer.parseInt(this.appSettings.get("map").get("gtmscTT")),
        				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gtmscNP")),
        				null, NatureOfAddress.valueOf(this.appSettings.get("map").get("gtmscNOA")));
        		break;
        	}
            if (gtG == null) {
                throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
            }
            //SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), gtG, cfg.getSccpPc(), cfg.getSsnO());
            SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(this.appSettings.get("map").get("ri")), gtG, Integer.parseInt(this.appSettings.get("map").get("spc")), Integer.parseInt(this.appSettings.get("map").get("spcssn")));
            
            this.serviceCenterSCCPAddress = sccpAddress;
        }
        return this.serviceCenterSCCPAddress;
    }

    /**
     * This is our (Service Center) SCCP Address for GT
     *
     * @return
     */
    private SccpAddress getHlrSccpAddress() throws Exception {
        if (this.hlrSCCPAddress == null) {
            GlobalTitle gtG = null;
            switch (this.appSettings.get("map").get("gthlrType")) {
        	case "GT0001":                
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("gthlrDigits"), 
        				NatureOfAddress.valueOf(this.appSettings.get("map").get("gthlrNOA")));
        		break;
        	case "GT0010":                
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("gthlrDigits"), 
        				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")));
        		break;
        	case "GT0011":            
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("gthlrDigits"), 
        				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")),
        				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gthlrNP")),
        				null);
        		break;
        	case "GT0100":            
        		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(this.appSettings.get("map").get("gthlrDigits"), 
        				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")),
        				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gthlrNP")),
        				null, NatureOfAddress.valueOf(this.appSettings.get("map").get("gthlrNOA")));
        		break;
        	}
            if (gtG == null) {
                throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
            }           
            SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(this.appSettings.get("map").get("ri")), gtG, 
            		Integer.parseInt(this.appSettings.get("map").get("dpc")), Integer.parseInt(this.appSettings.get("map").get("dpcssn")));
            this.hlrSCCPAddress = sccpAddress;
        }
        return this.hlrSCCPAddress;
    }

    /*
     * SCCP Address for GT VLR
     */
    private SccpAddress getHlrSccpAddress(String digits) throws Exception {
        GlobalTitle gtG = null;
        switch (this.appSettings.get("map").get("gthlrType")) {
    	case "GT0001":                
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(digits, 
    				NatureOfAddress.valueOf(this.appSettings.get("map").get("gthlrNOA")));
    		break;
    	case "GT0010":                
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(digits, 
    				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")));
    		break;
    	case "GT0011":            
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(digits, 
    				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")),
    				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gthlrNP")),
    				null);
    		break;
    	case "GT0100":            
    		gtG = this.sccpStack.getSccpProvider().getParameterFactory().createGlobalTitle(digits, 
    				Integer.parseInt(this.appSettings.get("map").get("gthlrTT")),
    				org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(this.appSettings.get("map").get("gthlrNP")),
    				null, NatureOfAddress.valueOf(this.appSettings.get("map").get("gthlrNOA")));
    		break;
    	}
        if (gtG == null) {
            throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        //SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), gtG, cfg.getMscPc(), 7);
        SccpAddress sccpAddress = new SccpAddressImpl(RoutingIndicator.valueOf(this.appSettings.get("map").get("ri")), gtG, 
        		Integer.parseInt(this.appSettings.get("map").get("dpc")), 7);
        return sccpAddress;//(new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getMscPc(), gtG, 7));
    }

    private MAPApplicationContext getMtFoSMSMAPApplicationContext() {
        if (this.mtFoSMSMAPApplicationContext == null) {
            this.mtFoSMSMAPApplicationContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMTRelayContext,
                    MAPApplicationContextVersion.version3);
        }
        return this.mtFoSMSMAPApplicationContext;
    }

    /**
     * This is our own number. We are Service Center.
     *
     * @return
     */
    private AddressString getServiceCenterAddressString() {

        if (this.serviceCenterAddress == null) {
            this.serviceCenterAddress = this.mapProvider.getMAPParameterFactory().createAddressString(AddressNature.valueOf(this.appSettings.get("map").get("an")),
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.valueOf(this.appSettings.get("map").get("np")), 
                    this.appSettings.get("map").get("serviceCenter"));
        }
        return this.serviceCenterAddress;
    }

    private AddressField getSmsTpduOriginatingAddress() {

        return new AddressFieldImpl(TypeOfNumber.valueOf(this.appSettings.get("map").get("ton")), 
        		NumberingPlanIdentification.valueOf(this.appSettings.get("map").get("npi")), this.appSettings.get("map").get("serviceCenter"));
    }

    /**
     * SendRoutingInfoForSMResponse
     *
     * @param sendRoutingInfoForSMRespInd
     */
    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMRespInd) {
        logger.debug("[onSendRoutingInfoForSMResponse]: " + sendRoutingInfoForSMRespInd);
        logger.debug(String.format("Adding SRI response for dialog=%d to queue.",sendRoutingInfoForSMRespInd.getMAPDialog().getLocalDialogId()));
        long dialogId = sendRoutingInfoForSMRespInd.getMAPDialog().getLocalDialogId();
    	if(smsdialogs.containsKey(dialogId)){
    		smsdialogs.remove(dialogId);
    		this.mtFoSMSMAPApplicationContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMTRelayContext,
                    MAPApplicationContextVersion.version3);
            MAPDialogSms mapDialogSms = null; 
            try{
            	mapDialogSms = this.setupMtForwardShortMessageRequestIndication(sendRoutingInfoForSMRespInd, "serviceSMS");
            }catch(Exception e){
            	logger.error("[onSendRoutingInfoForSMResponse] Dialog is null...  "+e.getMessage());
            }

            if (mapDialogSms != null) {
                // 3. Finaly send SMS
                logger.debug("[onSendRoutingInfoForSMResponse] Sending SMS...");
                long smsdialogid = mapDialogSms.getLocalDialogId();
                srismsdialogs.put(dialogId, smsdialogid);
                try{
                	mapDialogSms.send();
                }catch (Exception e){
                	logger.error("[onSendRoutingInfoForSMResponse] Unable to send SMS... "+e.getMessage());
                }
                
            } else {
                logger.error("[onSendRoutingInfoForSMResponse] Dialog is null...");
            }
    		
    	}else{
    		//lbsdCache.addSriResponse(dialogId, sendRoutingInfoForSMRespInd);
    		this.mapMessageCache.addMapMessage(sendRoutingInfoForSMRespInd.getMAPDialog().getLocalDialogId(), sendRoutingInfoForSMRespInd);
    	}       
    }

    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwSmRespInd) {
        logger.debug("[onMtForwardShortMessageResponse]: " + mtForwSmRespInd);
        long dialogId = mtForwSmRespInd.getMAPDialog().getLocalDialogId();
        //smsResponse.put(dialogId, 0L);
        this.mapMessageCache.addMapMessage(dialogId, mtForwSmRespInd);
    }

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
        logger.debug("[onDialogDelimiter]: " + mapDialog);
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog, AddressString destReference, AddressString origReference, MAPExtensionContainer extensionContainer) {
        logger.debug("[onDialogRequest]: " + mapDialog + " [AddressString]: " + destReference + " [AddressString]: "
                + origReference + " [MAPExtensionContainer]: " + extensionContainer);
    }

//    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference, IMSI eriImsi, AddressString eriVlrNo) {
        logger.debug("[onDialogRequestEricsson]: " + mapDialog + " [AddressString]: " + destReference + " [AddressString]: "
                + origReference + " [IMSI]: " + eriImsi + " [AddressString]: " + eriVlrNo);
    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
        logger.debug("[onDialogAccept]: " + mapDialog + " [MAPExtensionContainer]: " + extensionContainer);
    }

    @Override
    public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason refuseReason, ApplicationContextName alternativeApplicationContext, MAPExtensionContainer extensionContainer) {
        logger.debug("[onDialogReject]: " + mapDialog + " [MAPRefuseReason]: " + refuseReason + " [ApplicationContextName]: " + alternativeApplicationContext + " [MAPExtensionContainer]: " + extensionContainer);
    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice userReason, MAPExtensionContainer extensionContainer) {
        logger.debug("[onDialogUserAbort]: " + mapDialog + " [MAPUserAbortChoice]: " + userReason + " [MAPExtensionContainer]: " + extensionContainer);
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason abortProviderReason, MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
        logger.debug("[onDialogProviderAbort]: " + mapDialog + " [MAPAbortProviderReason]: " + abortProviderReason
                + " [MAPAbortSource]: " + abortSource + " [MAPExtensionContainer]: " + extensionContainer);
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        logger.debug("[onDialogClose]: " + mapDialog);
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
        logger.debug("[onDialogNotice]: " + mapDialog + "[ MAPNoticeProblemDiagnostic]: " + noticeProblemDiagnostic);
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        logger.debug("[onDialogRelease]: " + mapDialog);
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        logger.debug("[onDialogTimeout]: " + mapDialog);
        long dialogId= mapDialog.getLocalDialogId();
        mapMessageCache.clean(dialogId);
    }

    @Override
    public void onForwardShortMessageRequest(ForwardShortMessageRequest forwSmInd) {
        logger.debug("[onForwardShortMessageRequest]: " + forwSmInd);
    }

    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse forwSmRespInd) {
        logger.debug("[onForwardShortMessageResponse]: " + forwSmRespInd);
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwSmInd) {
        logger.debug("[onMoForwardShortMessageRequest]: " + moForwSmInd);
    }

    @Override
    public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
        logger.debug("[onMoForwardShortMessageResponse]: " + moForwSmRespInd);
        
    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
        logger.debug("[onMtForwardShortMessageRequest]: " + mtForwSmInd);
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
        logger.debug("[onSendRoutingInfoForSMRequest]: " + sendRoutingInfoForSMInd);
    }

    @Override
    public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
        logger.debug("[onReportSMDeliveryStatusRequest]: " + reportSMDeliveryStatusInd);
    }

    @Override
    public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse reportSMDeliveryStatusRespInd) {
        logger.debug("[onReportSMDeliveryStatusResponse]: " + reportSMDeliveryStatusRespInd);
        this.mapMessageCache.addMapMessage(reportSMDeliveryStatusRespInd.getMAPDialog().getLocalDialogId(), reportSMDeliveryStatusRespInd);
    }

    @Override
    public void onInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreInd) {
        logger.debug("[onInformServiceCentreRequest]: " + informServiceCentreInd);
    }

    @Override
    public void onAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreInd) {
        logger.debug("[onAlertServiceCentreRequest]: " + alertServiceCentreInd);
        try {
    		if(alertServiceCentreInd.getMsisdn() != null) {
    			logger.info("Processing alert message for - " + alertServiceCentreInd.getMsisdn().getAddress());    			
    			MAPDialogSms dialogSms = alertServiceCentreInd.getMAPDialog();
    			dialogSms.addAlertServiceCentreResponse(alertServiceCentreInd.getInvokeId());
    			dialogSms.close(false);	
    			//Call http Alert Link
    			AlertHttpWorker alertHttpWorker = new AlertHttpWorker(this.appSettings.get("app").get("alerturl"), alertServiceCentreInd.getMsisdn().getAddress());
    			try{
    				pool.execute(alertHttpWorker);
    			}catch(Exception e){
    				logger.error(e.getMessage());
    			}
    		} else 
    			logger.warn("MSISDN info Is NULL");
		} catch (MAPException e) {
			logger.warn("[onAlertServiceCentreRequest] Sending response" + e.getMessage());
		} catch (Exception e) {
			logger.warn("[onAlertServiceCentreRequest] Processing request. Error Message - " + e.getMessage());
		}
    }

    @Override
    public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
        logger.debug("[onAlertServiceCentreResponse]: " + alertServiceCentreInd);
    }

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
        logger.debug("[onErrorComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [MAPErrorMessage]: " + mapErrorMessage);
        logger.debug("Error code: " + mapErrorMessage.getErrorCode());        
        //this.mapMessageCache.addMapMessage(mapDialog.getLocalDialogId(), null);
        this.mapMessageCache.addErrorMapMessage(mapDialog.getLocalDialogId(), mapErrorMessage);      
//        long dialogId = mapDialog.getLocalDialogId();        
//        srismsdialogs.values().forEach((val) -> {if (val == dialogId){
//        	smsResponse.put(dialogId, mapErrorMessage.getErrorCode());
//        }});
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
        logger.debug("[onRejectComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [Problem]: " + problem + " [isLocalOriginated]: " + isLocalOriginated);
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
        logger.debug("[onInvokeTimeout]: " + mapDialog + " [invokeId]: " + invokeId);
        long dialogId= mapDialog.getLocalDialogId();
        if(smsdialogs.containsKey(dialogId)){
    		smsdialogs.remove(dialogId);
    		}
        if(srismsdialogs.containsKey(dialogId))
        	srismsdialogs.remove(dialogId);
        mapMessageCache.clean(dialogId);
        mapDialog.release();  
    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {
        logger.debug("[onMAPMessage]: " + mapMessage);
    }

    @Override
    public void onUpdateLocationRequest(UpdateLocationRequest ind) {
        logger.debug("[onUpdateLocationRequest]: " + ind);
    }

    @Override
    public void onUpdateLocationResponse(UpdateLocationResponse ind) {
        logger.debug("[onUpdateLocationResponse]: " + ind);
    }

    @Override
    public void onCancelLocationRequest(CancelLocationRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCancelLocationResponse(CancelLocationResponse response) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSendIdentificationRequest(SendIdentificationRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSendIdentificationResponse(SendIdentificationResponse response) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onUpdateGprsLocationRequest(UpdateGprsLocationRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onUpdateGprsLocationResponse(UpdateGprsLocationResponse response) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onPurgeMSRequest(PurgeMSRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onPurgeMSResponse(PurgeMSResponse response) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSendAuthenticationInfoRequest(SendAuthenticationInfoRequest ind) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSendAuthenticationInfoResponse(SendAuthenticationInfoResponse ind) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse response) {
    	logger.debug("[AnyTimeInterrogationResponse]" + response); //To change body of generated methods, choose Tools | Templates.
        logger.debug(String.format("Add ATI response for dialog=%d to queue", response.getMAPDialog().getLocalDialogId()));
        this.mapMessageCache.addMapMessage(response.getMAPDialog().getLocalDialogId(), response);
    }

    @Override
    public void onProvideSubscriberInfoRequest(ProvideSubscriberInfoRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onProvideSubscriberInfoResponse(ProvideSubscriberInfoResponse response) {
        logger.debug("[ProvideSubscriberInfoResponse]" + response); //To change body of generated methods, choose Tools | Templates.
        logger.debug(String.format("Add PSI response for dialog=%d to queue", response.getMAPDialog().getLocalDialogId()));
        this.mapMessageCache.addMapMessage(response.getMAPDialog().getLocalDialogId(), response);
    }

    @Override
    public void onInsertSubscriberDataRequest(InsertSubscriberDataRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onInsertSubscriberDataResponse(InsertSubscriberDataResponse request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCheckImeiRequest(CheckImeiRequest request) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCheckImeiResponse(CheckImeiResponse response) {
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

	public MapMessagesCache getMapMessageCache() {
		return mapMessageCache;
	}

	
	@Override
	public void onAuthenticationFailureReportRequest(AuthenticationFailureReportRequest ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onAuthenticationFailureReportResponse(AuthenticationFailureReportResponse ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onResetRequest(ResetRequest ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}


	@Override
	public void onForwardCheckSSIndicationRequest(ForwardCheckSSIndicationRequest ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}


	@Override
	public void onRestoreDataRequest(RestoreDataRequest ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onRestoreDataResponse(RestoreDataResponse ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onDeleteSubscriberDataRequest(DeleteSubscriberDataRequest request) {
		logger.debug(String.format("Not supported yet. %s",request.toString()));
		
	}

	@Override
	public void onDeleteSubscriberDataResponse(DeleteSubscriberDataResponse request) {
		logger.debug(String.format("Not supported yet. %s",request.toString()));
		
	}

	@Override
	public void onActivateTraceModeRequest_Mobility(ActivateTraceModeRequest_Mobility ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onActivateTraceModeResponse_Mobility(ActivateTraceModeResponse_Mobility ind) {
		logger.debug(String.format("Not supported yet. %s",ind.toString()));
		
	}

	@Override
	public void onReadyForSMRequest(ReadyForSMRequest request) {
		logger.debug(String.format("Not supported yet. %s",request.toString()));
		
	}

	@Override
	public void onReadyForSMResponse(ReadyForSMResponse response) {
		logger.debug(String.format("Not supported yet. %s",response.toString()));
		
	}

	@Override
	public void onNoteSubscriberPresentRequest(NoteSubscriberPresentRequest request) {
		logger.debug(String.format("Not supported yet. %s",request.toString()));
		
	}

	public Map<Long, Long> getSrismsdialogs() {
		return srismsdialogs;
	}

	@Override
	public void onAnyTimeSubscriptionInterrogationRequest(AnyTimeSubscriptionInterrogationRequest arg0) {
		logger.debug(String.format("Not supported yet. %s",arg0.toString()));
		
	}

	@Override
	public void onAnyTimeSubscriptionInterrogationResponse(AnyTimeSubscriptionInterrogationResponse arg0) {
		logger.debug(String.format("Not supported yet. %s",arg0.toString()));
		
	}

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference, AddressString eriMsisdn, AddressString eriVlrNo) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

class AlertHttpWorker implements Runnable{
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[AlertHttpWorker"));

	private String url= null;
	private String msisdn = null;

	public AlertHttpWorker(String url, String msisdn) {
		this.url = url;
		this.msisdn = msisdn;
	}
	@Override
	public void run() {
		logger.info("Call http alert link.");
		try {
			sendGet();
		} catch (Exception e) {
			logger.warn(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private int sendGet() throws Exception {				
		URL urlObj = new URL(url+"?msisdn="+msisdn);
		logger.info("URL :" + urlObj.toURI());
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("GET");
		con.setConnectTimeout(10000);
		con.setReadTimeout(10000);
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();	
		con.disconnect();		
		return responseCode;
	}
	
}
