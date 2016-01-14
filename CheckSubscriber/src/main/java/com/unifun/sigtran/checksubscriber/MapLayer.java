/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.checksubscriber;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ForkJoinPool;

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
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoResponse;
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
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.DomainType;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
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
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.checksubscriber.servlets.MapMessagesCache;
import com.unifun.sigtran.checksubscriber.utils.GTPreference;
import com.unifun.sigtran.checksubscriber.utils.MapLayerPreference;




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
    private MapLayerPreference cfg;
    private ForkJoinPool pool;

    public MapLayer(MAPStackImpl mapStack, MapLayerPreference cfg, ForkJoinPool pool) {
        this.mapProvider = mapStack.getMAPProvider();
        this.cfg = cfg;
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
        this.mapProvider.getMAPServiceSupplementary().deactivate();
        this.mapProvider.getMAPServiceSms().deactivate();
        //this.mapStack.stop();
        logger.debug("Stopped MAP Stack ....");
    }
    
   

    public long sendSriSm(String msisdnSubs) {
        MAPDialogSms mapDialog;
        try {
            SccpAddress origAddr = getServiceCenterSccpAddress();
            //SccpAddress destAddr = getHlrSccpAddress();
            SccpAddress destAddr = getServiceCenterSccpAddress(msisdnSubs);
            mapDialog = this.mapProvider.getMAPServiceSms().createNewDialog(
                    MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version3),
                    origAddr, null, destAddr, null);

            ISDNAddressString msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
                    AddressNature.international_number, NumberingPlan.ISDN, msisdnSubs);

            AddressString scAddress = this.mapProvider.getMAPParameterFactory().createAddressString(AddressNature.international_number, NumberingPlan.ISDN, cfg.getSc());

            mapDialog.addSendRoutingInfoForSMRequest(msisdn, true, scAddress, null, true, SM_RP_MTI.SMS_Deliver, null, null);
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
    
    public long sendRds(String msisdn){
    	try {
    		MAPDialogSms mapDialogSms = null;
    		SccpAddress origAddr = getServiceCenterSccpAddress();
    		SccpAddress destAddr = getServiceCenterSccpAddress(msisdn);    		
    		mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(
    				MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, MAPApplicationContextVersion.version3), 
    				origAddr, null, destAddr, null);
    		ISDNAddressString calledPartyAddress = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
                    AddressNature.international_number, NumberingPlan.ISDN, msisdn);    		
    		mapDialogSms.addReportSMDeliveryStatusRequest(calledPartyAddress, getServiceCenterAddressString(), 
    				SMDeliveryOutcome.getInstance(1),
    				null, null, false, true, null, null);    		
    		mapDialogSms.send();
    		return mapDialogSms.getLocalDialogId();
		} catch (MAPException ex) {
			logger.error("[sendSriSm]: " + ex.getMessage());
            return -1; 
		} catch (Exception ex) {
			logger.error("[sendSriSm]: " + ex.getMessage());
            return -1;
		}
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

    private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) throws Exception {
        GlobalTitle gtG = null;
        GTPreference gt = cfg.getGlobalTitleMsc();
        switch (gt.getType()) {
            case "GT0001":
                gtG = GlobalTitle.getInstance(NatureOfAddress.valueOf(gt.getNatureOfAddress()), networkNodeNumber.getAddress());
                break;
            case "GT0010":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), networkNodeNumber.getAddress());
                break;
            case "GT0011":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), networkNodeNumber.getAddress());
                break;
            case "GT0100":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), networkNodeNumber.getAddress());
                break;
        }
        if (gtG == null) {
            throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        return new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getMscPc(), gtG, cfg.getSsnD());
    }

    private MAPDialogSms setupMtForwardShortMessageRequestIndication(SendRoutingInfoForSMResponse evt, String smsText) throws MAPException, Exception {
        // this.mapParameterFactory.creat

        //= smsMap.get(evt.getMAPDialog().getLocalDialogId());
        MAPDialogSms mapDialogSmsLoc = this.mapProvider.getMAPServiceSms().createNewDialog(this.getMtFoSMSMAPApplicationContext(),
                this.getServiceCenterSccpAddress(), null, this.getMSCSccpAddress(evt.getLocationInfoWithLMSI().getNetworkNodeNumber()), null);

        SM_RP_DA sm_RP_DA = this.mapProvider.getMAPParameterFactory().createSM_RP_DA(evt.getIMSI());

        SM_RP_OA sm_RP_OA = this.mapProvider.getMAPParameterFactory().createSM_RP_OA_ServiceCentreAddressOA(this.getServiceCenterAddressString());

        UserDataImpl ud = new UserDataImpl(smsText, new DataCodingSchemeImpl(0), null, null);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 3);

        AbsoluteTimeStampImpl serviceCentreTimeStamp = new AbsoluteTimeStampImpl(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), 20);

        SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true, this.getSmsTpduOriginatingAddress(), pi,
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
        GTPreference gt = cfg.getGlobalTitleMsc();
        switch (gt.getType()) {
            case "GT0001":
                gtG = GlobalTitle.getInstance(NatureOfAddress.valueOf(gt.getNatureOfAddress()), msisdn);
                break;
            case "GT0010":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), msisdn);
                break;
            case "GT0011":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), msisdn);
                break;
            case "GT0100":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), msisdn);
                break;
        }
        if (gtG == null) {
            throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        return new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getMscPc(), gtG, cfg.getSsnD());
    }

    /**
     * This is our (Service Center) SCCP Address for GT
     *
     * @return
     */
    private SccpAddress getServiceCenterSccpAddress() throws Exception {
        if (this.serviceCenterSCCPAddress == null) {
            GlobalTitle gtG = null;
            GTPreference gt = cfg.getGlobalTitleMsc();
            switch (gt.getType()) {
                case "GT0001":
                    gtG = GlobalTitle.getInstance(NatureOfAddress.valueOf(gt.getNatureOfAddress()), cfg.getSc());
                    break;
                case "GT0010":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), cfg.getSc());
                    break;
                case "GT0011":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), cfg.getSc());
                    break;
                case "GT0100":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), cfg.getSc());
                    break;
            }
            if (gtG == null) {
                throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
            }
            this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getSccpPc(), gtG, cfg.getSsnO());
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
            GTPreference gt = cfg.getGlobalTitleHlr();
            switch (gt.getType()) {
                case "GT0001":
                    gtG = GlobalTitle.getInstance(NatureOfAddress.valueOf(gt.getNatureOfAddress()), gt.getDigits());
                    break;
                case "GT0010":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), gt.getDigits());
                    break;
                case "GT0011":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), gt.getDigits());
                    break;
                case "GT0100":
                    gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), gt.getDigits());
                    break;
            }
            if (gtG == null) {
                throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
            }
            this.hlrSCCPAddress = new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getMscPc(), gtG, cfg.getSsnD());
        }
        return this.hlrSCCPAddress;
    }

    /*
     * SCCP Address for GT VLR
     */
    private SccpAddress getHlrSccpAddress(String digits) throws Exception {
        GlobalTitle gtG = null;
        GTPreference gt = cfg.getGlobalTitleHlr();
        switch (gt.getType()) {
            case "GT0001":
                gtG = GlobalTitle.getInstance(NatureOfAddress.valueOf(gt.getNatureOfAddress()), digits);
                break;
            case "GT0010":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), digits);
                break;
            case "GT0011":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), digits);
                break;
            case "GT0100":
                gtG = GlobalTitle.getInstance(gt.getTranslationType(), org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(gt.getNumberingPlan()), NatureOfAddress.valueOf(gt.getNatureOfAddress()), digits);
                break;
        }
        if (gtG == null) {
            throw new Exception("[MAP] Sms GT is not defined correctly. Type must be GT0001 or GT0010 or GT0011 or GT0100");
        }
        return (new SccpAddress(RoutingIndicator.valueOf(cfg.getRoutingIndicator()), cfg.getMscPc(), gtG, 7));
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
            this.serviceCenterAddress = this.mapProvider.getMAPParameterFactory().createAddressString(AddressNature.valueOf(cfg.getAddressNature()),
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.valueOf(cfg.getNumberingPlan()), cfg.getSc());
        }
        return this.serviceCenterAddress;
    }

    private AddressField getSmsTpduOriginatingAddress() {

        return new AddressFieldImpl(TypeOfNumber.valueOf(cfg.getTypeOfNumber()), NumberingPlanIdentification.valueOf(cfg.getNumberingPlanIdentification()), cfg.getSc());
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
        this.mapMessageCache.addMapMessage(sendRoutingInfoForSMRespInd.getMAPDialog().getLocalDialogId(), sendRoutingInfoForSMRespInd);
    }

    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwSmRespInd) {
        logger.debug("[onMtForwardShortMessageResponse]: " + mtForwSmRespInd);
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

    @Override
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
    			AlertHttpWorker alertHttpWorker = new AlertHttpWorker(this.cfg.getAlerturl(), alertServiceCentreInd.getMsisdn().getAddress());
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
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
        logger.debug("[onRejectComponent]: " + mapDialog + " [invokeId]: " + invokeId + " [Problem]: " + problem + " [isLocalOriginated]: " + isLocalOriginated);
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
        logger.debug("[onInvokeTimeout]: " + mapDialog + " [invokeId]: " + invokeId);
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
        logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();	
		con.disconnect();		
		return responseCode;
	}
	
}
