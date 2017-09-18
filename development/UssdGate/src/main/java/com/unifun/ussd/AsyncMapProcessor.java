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
import com.unifun.ussd.router.Route;
import com.unifun.ussd.router.Router;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.log4j.Logger;
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
public class AsyncMapProcessor implements MAPDialogListener, MAPServiceSupplementaryListener {

    //Reference for the MAP Provider
    private final MAPProvider mapProvider;

    //Reference for the SCCP Provider
    private final SccpProvider sccpProvider;

    //Holds execution callback context
    private final ConcurrentHashMap<Long, ExecutionContext> contextQueue = new ConcurrentHashMap();

    //HTTP related RX/TX 
    private final AsyncHttpProcessor httpProcessor = new AsyncHttpProcessor(this);

    private final Router router;
    
    //Logger instance
    private final static Logger LOGGER = Logger.getLogger(AsyncMapProcessor.class);

    /**
     * Constructs new instance of this class.
     *
     * @param mapStack
     * @param sccpProvider
     */
    public AsyncMapProcessor(MAPStack mapStack, SccpProvider sccpProvider) {
        this.router = new Router(System.getProperty("catalina.base") + "/conf/router.json");
        this.sccpProvider = sccpProvider;
        this.mapProvider = mapStack.getMAPProvider();
    }

    /**
     * Gets access to the message router.
     * 
     * @return 
     */
    public Router router() {
        return router;
    }
    
    /**
     * Initializes MAP/HTTP resources.
     *
     * @throws IOReactorException
     */
    public void init() throws IOReactorException {
        this.mapProvider.addMAPDialogListener(this);
        this.mapProvider.getMAPServiceSupplementary().addMAPServiceListener(this);
        this.mapProvider.getMAPServiceSupplementary().acivate();
        this.httpProcessor.init();
    }

    /**
     * Sends given message over MAP within specified context.
     *
     * This method does not throw any exception and instead it delivers events
     * via execution context.
     *
     * @param msg ussd message in json format.
     * @param context asynchronous execution context related to the underlying
     * transport.
     */
    public void send(UssMessage msg, ExecutionContext context) {
        try {
            //verify parameters
            assert msg != null : "Message can not be null";
            assert context != null : "Execution context not specified";

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
            context.setId(mapDialog.getLocalDialogId());
            contextQueue.put(mapDialog.getLocalDialogId(), context);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("(TC-%s): {%s} : %s ---> %s", tcap.getType(), component.getType(), map.operationName(), context.toString()));
            }

            //wrap with suitable component and send the message
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

            context.completed(new UssMessage(abort));
        }
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
            ISDNAddressString origReference = valueOf(tcap.getDialog().getOriginationReference());
            ISDNAddressString destReference = valueOf(tcap.getDialog().getDestinationReference());

            SccpAddress callingPartyAddress = valueOf(sccp.getCallingPartyAddress());
            SccpAddress calledPartyAddress = valueOf(sccp.getCalledPartyAddress());

            dialog = mapProvider.getMAPServiceSupplementary()
                    .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2),
                            callingPartyAddress, origReference, calledPartyAddress, destReference);

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
                dialog.addUnstructuredSSResponse(returnResultLast.invokeId(), codingScheme, ussdString);
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

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {
    }

    @Override
    public void onDialogRequest(MAPDialog mapDialog, AddressString destReference, AddressString origReference, MAPExtensionContainer extensionContainer) {
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference, AddressString eriMsisdn, AddressString eriVlrNo) {
    }

    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
    }

    @Override
    public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason refuseReason, ApplicationContextName alternativeApplicationContext, MAPExtensionContainer extensionContainer) {
    }

    @Override
    public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice userReason, MAPExtensionContainer extensionContainer) {
    }

    @Override
    public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason abortProviderReason, MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        ExecutionContext context = contextQueue.remove(mapDialog.getLocalDialogId());
        LOGGER.info("(TC-Close) <---> " + ctxName(context, mapDialog.getLocalDialogId()));
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        ExecutionContext context = contextQueue.remove(mapDialog.getLocalDialogId());
        LOGGER.info("(TC-Release) <---> " + ctxName(context, mapDialog.getLocalDialogId()));
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        ExecutionContext context = contextQueue.remove(mapDialog.getLocalDialogId());
        LOGGER.info("(TC-Timeout) <---> " + ctxName(context, mapDialog.getLocalDialogId()));
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
        UssMessage msg = new UssMessage(mapMessage, "process-unstructured-ss-request");
        Route route = router.find("");        
        httpProcessor.processMessage(msg, route, route.nextDestination());
    }

    @Override
    public void onProcessUnstructuredSSResponse(ProcessUnstructuredSSResponse msg) {
        LOGGER.info("<--- On process unstructured ss response");
        UssMessage m = new UssMessage(msg, "process-unstructured-ss-request");

        JsonComponent component = m.getTcap().getComponents().get(0);
        ExecutionContext context = contextQueue.get(msg.getMAPDialog().getLocalDialogId());
        
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("(TC-%s):{%s}: unstructured-ss-request <--- %s",
                    m.getTcap().getType(), component.getType(), ctxName(context, msg.getMAPDialog().getLocalDialogId())));
        }

        if (context != null) {
            context.completed(m);
        } 
    }

    @Override
    public void onUnstructuredSSRequest(UnstructuredSSRequest msg) {
        UssMessage m = new UssMessage(msg, "unstructured-ss-request");
        ExecutionContext context = contextQueue.get(msg.getMAPDialog().getLocalDialogId());

        JsonComponent component = m.getTcap().getComponents().get(0);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("(TC-%s):{%s}: unstructured-ss-request <--- %s",
                    m.getTcap().getType(), component.getType(), ctxName(context, msg.getMAPDialog().getLocalDialogId())));
        }

        if (context != null) {
            context.completed(m);
        } else {
            httpProcessor.processMessage(m, "http://127.0.0.1:7081/UssdGate/test");
        }
    }

    @Override
    public void onUnstructuredSSResponse(UnstructuredSSResponse msg) {
        LOGGER.info("<--- " + msg.getMessageType().name());
        UssMessage m = new UssMessage(msg, "unstructured-ss-request");

        ExecutionContext context = contextQueue.get(msg.getMAPDialog().getLocalDialogId());
        if (context == null) {
            LOGGER.warn("Process-unstructured-ss-request: Out of context: " + msg.getMAPDialog().getLocalDialogId());
            return;
        }

        context.completed(m);
    }

    @Override
    public void onUnstructuredSSNotifyRequest(UnstructuredSSNotifyRequest unstrNotifyInd) {
    }

    @Override
    public void onUnstructuredSSNotifyResponse(UnstructuredSSNotifyResponse unstrNotifyInd) {
    }

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
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

    private String ctxName(ExecutionContext context, long dialogId) {
        return context != null
                ? context.toString()
                : "ExecutionContext(NULL, " + dialogId + ")";
    }
}
