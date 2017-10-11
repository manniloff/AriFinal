/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.ussd.context.ExecutionContext;
import com.unifun.map.JsonAddressString;
import com.unifun.map.JsonComponent;
import com.unifun.map.JsonDataCodingScheme;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonSccp;
import com.unifun.map.JsonSccpAddress;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import com.unifun.ussd.context.MapLocalContext;
import com.unifun.ussd.router.Route;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;
import org.jgroups.JChannel;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.map.api.datacoding.CBSDataCodingGroup;
import org.mobicents.protocols.ss7.map.api.datacoding.CBSNationalLanguage;
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
import org.mobicents.protocols.ss7.map.api.service.supplementary.SupplementaryMessage;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSNotifyRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSNotifyResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingSchemaMessageClass;
import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

/**
 * This class implements MAP related RX/TX processes.
 *
 * @author okulikov
 */
public class MapChannel implements Channel, MAPDialogListener, MAPServiceSupplementaryListener {

    private final static String INFO = "(DID:%s) %s";

    private final Gateway gateway;
    //Reference for the MAP Provider
    private final MAPProvider mapProvider;

    //Reference for the SCCP Provider
    private final SccpProvider sccpProvider;

    private JChannel clusterChannel;

    //Logger instance
    private final static Logger LOGGER = Logger.getLogger(MapChannel.class);

    /**
     * Constructs new instance of this class.
     *
     * @param gateway
     * @param mapStack
     * @param sccpProvider
     */
    public MapChannel(Gateway gateway, MAPStack mapStack, SccpProvider sccpProvider) {
        this.gateway = gateway;
        this.sccpProvider = sccpProvider;
        this.mapProvider = mapStack.getMAPProvider();
    }

    @Override
    public void start() throws Exception {
        this.mapProvider.addMAPDialogListener(this);
        this.mapProvider.getMAPServiceSupplementary().addMAPServiceListener(this);
        this.mapProvider.getMAPServiceSupplementary().acivate();

//        clusterChannel = new JChannel();
//        clusterChannel.connect("USSD");
    }

    @Override
    public void send(String url, UssMessage msg, ExecutionContext context) {
        try {
            //verify parameters
            assert msg != null : "Message can not be null";

            //break original message into parts
            JsonTcap tcap = msg.getTcap();
            JsonSccp sccp = msg.getSccp();

            //extract component from TCAP part
            JsonComponent component = tcap.getComponents().get(0);
            JsonMap map = mapMessage(component);

            //create or find dialog
            //if dialog exists the existing dialog will be returned
            //if not exits then new dialog will be created 
            MAPDialogSupplementary mapDialog = mapDialog(sccp, tcap);
            //append MAP operation
            switch (map.operationName()) {
                case "process-unstructured-ss-request":
                    processUnstructuredSSRequest(mapDialog, (JsonMapOperation) map.operation());
                    break;
                case "unstructured-ss-request":
                    unstructuredSSRequest(mapDialog, component, component.getType());
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented yet " + map.operationName());
            }

            //assign unique identifier to context and store context.
            if (mapDialog.getUserObject() == null) {
                JsonTcapDialog dialog = new JsonTcapDialog();
                dialog.setDialogId(mapDialog.getLocalDialogId());
                mapDialog.setUserObject(new MapDialog(dialog));
            }
            
            ((MapDialog) mapDialog.getUserObject()).setContext(context);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("TX: " + msg.toString());
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "---> " + tcap.getType() + ":" + map.operationName()));
            }

            //wrap with suitable component and sendOverMap the message
            switch (tcap.getType()) {
                case "Begin":
                case "Continue":
                    mapDialog.send();
                    break;
                case "End":
                    mapDialog.close(false);
                    break;
            }
        } catch (Throwable t) {
            LOGGER.error("Could not send message", t);
            JsonTcap tcap = new JsonTcap();
            tcap.setType("Abort");
            tcap.setAbortMessage(t.getMessage());

            JsonMessage abort = new JsonMessage();
            abort.setTcap(tcap);

            if (context != null) {
                context.completed(new UssMessage(abort));
            }
        }
    }

    @Override
    public void stop() {
    }

    /**
     * Extracts MAP message from TCAP component.
     *
     * @param component
     * @return
     */
    private JsonMap mapMessage(JsonComponent component) {
        switch (component.getType()) {
            case "invoke":
                JsonInvoke invoke = (JsonInvoke) component.getValue();
                return (JsonMap) invoke.component();
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component.getValue();
                return (JsonMap) returnResultLast.component();
            default:
                return null;
        }
    }

    /**
     * Builds MAP dialog.
     *
     * If other side works in load sharing mode then we can receive intermediate
     * messages related to some existing transaction. In case of TCAP BEGIN we
     * will always create new dialog. For other message types we will try to
     * find existing dialog first and create new one only when it does not
     * exist. Later this dialog might be closed explicit or expired.
     *
     * @param sccp
     * @param tcap
     * @return
     */
    private MAPDialogSupplementary mapDialog(JsonSccp sccp, JsonTcap tcap) throws MAPException {
        MAPDialogSupplementary dialog = (MAPDialogSupplementary) mapProvider.getMAPDialog(tcap.getDialog().getDialogId());
        if (dialog == null) {
            assert sccp != null : "SCCP part should be defined for each new dialog";
            
            ISDNAddressString origReference = valueOf(tcap.getDialog().getOriginationReference());
            ISDNAddressString destReference = valueOf(tcap.getDialog().getDestinationReference());

            SccpAddress callingPartyAddress = valueOf(sccp.getCallingPartyAddress());
            SccpAddress calledPartyAddress = valueOf(sccp.getCalledPartyAddress());

            dialog = mapProvider.getMAPServiceSupplementary()
                    .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2),
                            callingPartyAddress, origReference, calledPartyAddress, destReference);

            LOGGER.info(String.format(INFO, dialog.getLocalDialogId(), "---> Started"));

            if (this.isEricsson(tcap.getDialog())) {
                ISDNAddressString msisdn = valueOf(tcap.getDialog().getMsisdn());
                ISDNAddressString vlrAddress = valueOf(tcap.getDialog().getVlrAddress());
                dialog.addEricssonData(msisdn, vlrAddress);
            }
        }
        return dialog;
    }

    /**
     * Appends ProcessUnstructuredSSRequest message to the given dialog.
     *
     * @param dialog
     * @param mapMessage
     * @throws MAPException
     */
    private void processUnstructuredSSRequest(MAPDialogSupplementary dialog, JsonMapOperation mapMessage) throws MAPException {
        MAPParameterFactory mapParameterFactory = mapProvider.getMAPParameterFactory();

        ISDNAddressString msisdn = valueOf(mapMessage.getMsisdn());
        CBSDataCodingSchemeImpl codingScheme = codingScheme(mapMessage.getCodingScheme());
        USSDString ussdString = mapParameterFactory.createUSSDString(mapMessage.getUssdString(), codingScheme, Charset.forName("UTF-8"));

        dialog.addProcessUnstructuredSSRequest(codingScheme, ussdString, null, msisdn);
    }

    /**
     * Appends UnstructuredSSRequest to the given Dialog.
     *
     * @param dialog
     * @param component
     * @param type
     * @throws MAPException
     */
    private void unstructuredSSRequest(MAPDialogSupplementary dialog, JsonComponent component, String type) throws MAPException {
        JsonMapOperation mapMessage = (JsonMapOperation) mapMessage(component).operation();
        MAPParameterFactory mapParameterFactory = mapProvider.getMAPParameterFactory();

        CBSDataCodingSchemeImpl codingScheme = codingScheme(mapMessage.getCodingScheme());
        USSDString ussdString = mapParameterFactory.createUSSDString(mapMessage.getUssdString(), codingScheme, Charset.forName("UTF-8"));

        switch (type) {
            case "invoke":
                dialog.addUnstructuredSSRequest(codingScheme, ussdString, null, null);
                break;
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component.getValue();
                dialog.addUnstructuredSSResponse(returnResultLast.getInvokeId(), codingScheme, ussdString);
                break;
        }
    }

    /**
     * Constructs ISDNAddressString from related Json object.
     *
     * @param address
     * @return
     */
    private ISDNAddressString valueOf(JsonAddressString address) {
        if (address == null) {
            return null;
        }
        MAPParameterFactory factory = mapProvider.getMAPParameterFactory();
        return factory.createISDNAddressString(
                AddressNature.valueOf(address.getNatureOfAddress()),
                NumberingPlan.valueOf(address.getNumberingPlan()),
                address.getAddress());
    }

    private JsonAddressString valueOf(AddressString address) {
        if (address == null) {
            return null;
        }

        JsonAddressString value = new JsonAddressString();
        value.setNumberingPlan(address.getNumberingPlan().name());
        value.setNatureOfAddress(address.getAddressNature().name());
        value.setAddress(address.getAddress());

        return value;
    }

    /**
     * Constructs coding scheme from corresponding Json object.
     *
     * @param scheme
     * @return
     */
    private CBSDataCodingSchemeImpl codingScheme(JsonDataCodingScheme scheme) {
        return new CBSDataCodingSchemeImpl(
                CBSDataCodingGroup.valueOf(scheme.getCodingGroup()),
                CharacterSet.valueOf(scheme.getLanguage()),
                CBSNationalLanguage.LanguageUnspecified,
                DataCodingSchemaMessageClass.Class0,
                false
        );
    }

    /**
     * Constructs SCCP address from corresponding Json object.
     *
     * @param address
     * @return
     */
    public SccpAddress valueOf(JsonSccpAddress address) {
        int ssn = address.getSsn() != null ? address.getSsn() : -1;
        int pc = address.getPc() != null ? address.getPc() : -1;

        ParameterFactory factory = sccpProvider.getParameterFactory();
        String gti = address.getGtIndicator();

        GlobalTitle gt = null;
        switch (gti) {
            case "0001":
            case "GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY" :
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0010":
            case "GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY" :
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0011":
            case "GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME" :
                gt = factory.createGlobalTitle(digits(address), na(address));
                break;
            case "0100":
            case "GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS" :
                gt = factory.createGlobalTitle(digits(address), 0, np(address), null, na(address));
                break;
        }

        return new SccpAddressImpl(
                RoutingIndicator.valueOf(address.getRoutingIndicator()),
                gt, pc, ssn);
    }

    /**
     * Extracts digits from SCCP address in JSON format.
     *
     * @param address
     * @return
     */
    private String digits(JsonSccpAddress address) {
        return address.getGlobalTitle().getDigits();
    }

    /**
     * Constructs numbering plan from corresponding Json object.
     *
     * @param address
     * @return
     */
    private org.mobicents.protocols.ss7.indicator.NumberingPlan np(JsonSccpAddress address) {
        return org.mobicents.protocols.ss7.indicator.NumberingPlan.valueOf(address.getGlobalTitle().getNumberingPlan());
    }

    /**
     * Constructs nature of address indicator from corresponding Json object.
     *
     * @param address
     * @return
     */
    private NatureOfAddress na(JsonSccpAddress address) {
        return NatureOfAddress.valueOf(address.getGlobalTitle().getNatureOfAddressIndicator());
    }

    /**
     * Constructs translation type from corresponding Json object.
     *
     * @param address
     * @return
     */
    private int tt(JsonSccpAddress address) {
        return 0;
    }

    /**
     * Test given TCAP dialog for Ericsson style.
     *
     * @param dialog
     * @return
     */
    private boolean isEricsson(JsonTcapDialog dialog) {
        return dialog.getVlrAddress() != null && dialog.getMsisdn() != null;
    }

    private String ussdString(UssMessage msg) {
        JsonComponent component = msg.getTcap().getComponents().get(0);
        JsonMap map = mapMessage(component);
        JsonMapOperation op = (JsonMapOperation) map.operation();
        return op.getUssdString();
    }

    private void cleanup(long id) {
    }

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog, AddressString destReference, AddressString origReference, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Started"));

        JsonTcapDialog dialog = new JsonTcapDialog();
        dialog.setDialogId(mapDialog.getLocalDialogId());
        dialog.setDestinationReference(valueOf(destReference));
        dialog.setOriginationReference(valueOf(origReference));

        mapDialog.setUserObject(new MapDialog(dialog));
//        dialogs.put(mapDialog.getLocalDialogId(), dialog);
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference, AddressString msisdn, AddressString vlrNo) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Started"));
        JsonTcapDialog dialog = new JsonTcapDialog();

        dialog.setDialogId(mapDialog.getLocalDialogId());
        dialog.setDestinationReference(valueOf(destReference));
        dialog.setOriginationReference(valueOf(origReference));
        dialog.setMsisdn(valueOf(msisdn));
        dialog.setVlrAddress(valueOf(vlrNo));

        mapDialog.setUserObject(new MapDialog(dialog));
//        dialogs.put(mapDialog.getLocalDialogId(), dialog);
    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Accepted"));
    }

    @Override
    public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason refuseReason, ApplicationContextName alternativeApplicationContext, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Rejected"));
        cleanup(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice userReason, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Aborted by user"));
        cleanup(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason abortProviderReason, MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Aborted by provider"));
        cleanup(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Closed"));
        cleanup(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Noticed"));
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "<--- Released"));
        cleanup(mapDialog.getLocalDialogId());
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        cleanup(mapDialog.getLocalDialogId());
        LOGGER.info(String.format(INFO, mapDialog.getLocalDialogId(), "Timeout"));
    }

    @Override
    public void onRegisterSSRequest(RegisterSSRequest request) {
    }

    @Override
    public void onRegisterSSResponse(RegisterSSResponse response) {
    }

    @Override
    public void onEraseSSRequest(EraseSSRequest request) {
    }

    @Override
    public void onEraseSSResponse(EraseSSResponse response) {
    }

    @Override
    public void onActivateSSRequest(ActivateSSRequest request) {
    }

    @Override
    public void onActivateSSResponse(ActivateSSResponse response) {
    }

    @Override
    public void onDeactivateSSRequest(DeactivateSSRequest request) {
    }

    @Override
    public void onDeactivateSSResponse(DeactivateSSResponse response) {
    }

    @Override
    public void onInterrogateSSRequest(InterrogateSSRequest request) {
    }

    @Override
    public void onInterrogateSSResponse(InterrogateSSResponse response) {
    }

    @Override
    public void onGetPasswordRequest(GetPasswordRequest request) {
    }

    @Override
    public void onGetPasswordResponse(GetPasswordResponse response) {
    }

    @Override
    public void onRegisterPasswordRequest(RegisterPasswordRequest request) {
    }

    @Override
    public void onRegisterPasswordResponse(RegisterPasswordResponse response) {
    }

    @Override
    public void onProcessUnstructuredSSRequest(ProcessUnstructuredSSRequest mapMessage) {
        long dialogID = mapMessage.getMAPDialog().getLocalDialogId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(INFO, dialogID, "<--- " + mapMessage.getMAPDialog().getTCAPMessageType() + ": process-unstrcutured-ss-request"));
        }

        SccpAddress src = mapMessage.getMAPDialog().getRemoteAddress();
        int pc = src.getSignalingPointCode();

        try {
            UssMessage msg = jsonMessage(mapMessage, "process-unstructured-ss-request");

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("RX : %d : %s", pc, msg.toString()));
            }
            
            //This message is initiated by provider and must be forwarded to the
            //user using another transport
            Route route = gateway.router().find(ussdString(msg));
            if (route == null) {
                throw new IllegalArgumentException("Unknown or undefined key: " + ussdString(msg));
            }
            
            //store destination into memory
            ((MapDialog) mapMessage.getMAPDialog().getUserObject()).setRoute(route);

            //select primary destination
            String url = route.nextDestination();            
            String url2 = route.failureDestination();
            
            Channel channel = gateway.channel(url);
            Channel channel2 = gateway.channel(url2);
            
            channel.send(url, msg, new MapLocalContext(this, url2, msg, channel2));
        } catch (Throwable e) {
            LOGGER.error(String.format(INFO, dialogID, "Could not start dialog"), e);
        }
    }

    @Override
    public void onProcessUnstructuredSSResponse(ProcessUnstructuredSSResponse msg) {
        long dialogID = msg.getMAPDialog().getLocalDialogId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(INFO, dialogID, "<--- " + msg.getMAPDialog().getTCAPMessageType() + ": unstrcutured-ss-request"));
        }

        SccpAddress src = msg.getMAPDialog().getRemoteAddress();
        int pc = src.getSignalingPointCode();

        try {
            UssMessage m = jsonMessage(msg, "unstructured-ss-request");

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("RX : %d : %s", pc, m.toString()));
            }

            //This message has arrived from map domain as response for message
            //initiated by user so we need to reply using context
            ExecutionContext context = ((MapDialog) msg.getMAPDialog().getUserObject()).getContext();
            if (context == null) {
                throw new IllegalStateException("Out of context");
            }

            context.completed(m);
        } catch (Throwable t) {
            LOGGER.error(String.format(INFO, dialogID, "Unexpected error"), t);
        }
    }

    @Override
    public void onUnstructuredSSRequest(UnstructuredSSRequest msg) {
        long dialogID = msg.getMAPDialog().getLocalDialogId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(INFO, dialogID, "<--- " + msg.getMAPDialog().getTCAPMessageType() + ": unstrcutured-ss-request"));
        }

        SccpAddress src = msg.getMAPDialog().getRemoteAddress();
        int pc = src.getSignalingPointCode();

        try {
            UssMessage m = jsonMessage(msg, "unstructured-ss-request");

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("RX : %d : %s", pc, m.toString()));
            }

           MapDialog dialog = ((MapDialog) msg.getMAPDialog().getUserObject());
           if (dialog != null && dialog.getContext() != null) {
               dialog.getContext().completed(m);
               return;
           }
            //Route object might exists 
            Route route = ((MapDialog) msg.getMAPDialog().getUserObject()).getRoute();            
            if (route == null) {
                //if Route object is not defined yet, then we can define it now
                route = gateway.router().find(ussdString(m));
                if (route == null) {
                    throw new IllegalArgumentException("Unknown or undefined key: " + ussdString(m));
                }
            
                //store destination into memory
                ((MapDialog) msg.getMAPDialog().getUserObject()).setRoute(route);
            }

            //select primary destination
            String url = route.nextDestination();
            String url2 = route.failureDestination();
            
            Channel channel = gateway.channel(url);
            Channel channel2 = gateway.channel(url2);
            
            channel.send(url, m, new MapLocalContext(this, url2, m, channel2));
        } catch (Throwable t) {
            LOGGER.error(String.format(INFO, dialogID, "Unexpected error"), t);
        }
    }

    @Override
    public void onUnstructuredSSResponse(UnstructuredSSResponse msg) {
        long dialogID = msg.getMAPDialog().getLocalDialogId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(INFO, dialogID, "<---" + msg.getMAPDialog().getTCAPMessageType() + ": unstrcutured-ss-request"));
        }

        SccpAddress src = msg.getMAPDialog().getRemoteAddress();
        int pc = src.getSignalingPointCode();

        try {
            UssMessage m = jsonMessage(msg, "unstructured-ss-request");

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("RX : %d : %s", pc, m.toString()));
            }

            //This message has arrived from map domain as response for message
            //initiated by user so we need to reply using context
            ExecutionContext context = ((MapDialog) msg.getMAPDialog().getUserObject()).getContext();
            if (context == null) {
                throw new IllegalStateException("Out of context");
            }

            context.completed(m);
        } catch (Throwable t) {
            LOGGER.error(String.format(INFO, dialogID, "Unexpected error"), t);
        }
    }

    @Override
    public void onUnstructuredSSNotifyRequest(UnstructuredSSNotifyRequest unstrNotifyInd) {
    }

    @Override
    public void onUnstructuredSSNotifyResponse(UnstructuredSSNotifyResponse unstrNotifyInd) {
    }

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
        LOGGER.warn(String.format(INFO, mapDialog.getLocalDialogId(), "Error component invokeID=" + invokeId + ", " + mapErrorMessage));
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {
    }

    /**
     * Converts
     *
     * @param msg
     * @param evt
     * @return
     */
    private UssMessage jsonMessage(SupplementaryMessage msg, String evt) {
        return new UssMessage(msg, ((MapDialog) msg.getMAPDialog().getUserObject()).getDialog(), evt);
    }


}
