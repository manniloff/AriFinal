/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.sigtran.ussdgate;

import com.unifun.ussd.context.ExecutionContext;
import com.unifun.ussd.context.MapExecutionContext;
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
 *
 * @author okulikov
 */
public class AsyncMapProcessor implements MAPDialogListener, MAPServiceSupplementaryListener {

    private final static Logger LOGGER = Logger.getLogger(AsyncMapProcessor.class);

    private final MAPProvider mapProvider;
    private final SccpProvider sccpProvider;

    private final ConcurrentHashMap<Long, ExecutionContext> asyncContextQueue = new ConcurrentHashMap();
    private final AsyncHttpProcessor httpProcessor = new AsyncHttpProcessor(this);

    public AsyncMapProcessor(MAPStack mapStack, SccpProvider sccpProvider) {
        this.sccpProvider = sccpProvider;
        this.mapProvider = mapStack.getMAPProvider();
    }

    public void init() throws IOReactorException {
        this.mapProvider.addMAPDialogListener(this);
        this.mapProvider.getMAPServiceSupplementary().addMAPServiceListener(this);
        this.mapProvider.getMAPServiceSupplementary().acivate();
        this.httpProcessor.init();
    }

    /**
     * Sends given message over MAP within specified context.
     * 
     * This method does not throw any exception and instead it delivers events via
     * execution context.
     *
     * @param msg ussd message in json format.
     * @param context asynchronous execution context related to the underlying
     * transport.
     */
    public void send(UssMessage msg, ExecutionContext context) {
        try {
            assert msg != null : "Message can not be null";
            assert context != null : "Execution context not specified";
            
            JsonTcap tcap = msg.getTcap();
            JsonSccp sccp = msg.getSccp();

            MAPDialogSupplementary mapDialog = null;

            JsonComponent component = tcap.getComponents().get(0);
            JsonMap map = mapMessage(component);

            switch (map.operationName()) {
                case "proccess-unstructured-ss-request":
                    mapDialog = processUnstructuredSSRequest(sccp, tcap, (JsonMapOperation) map.operation(), component.getType());
                    break;
                case "unstructured-ss-request":
                    mapDialog = unstructuredSSRequest(sccp, tcap, component, component.getType());
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented yet " + map.operationName());
            }

            assert mapDialog != null : "Could not create or find dialog";

            context.setId(mapDialog.getLocalDialogId());
            this.asyncContextQueue.put(mapDialog.getLocalDialogId(), context);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("(TC-%s): {%s} : %s ---> %s", tcap.getType(), component.getType(), map.operationName(), context.toString()));
            }

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
            JsonTcap tcap = new JsonTcap();
            tcap.setType("Abort");
            tcap.setAbortMessage(t.getMessage());
            
            JsonMessage abort = new JsonMessage();
            abort.setTcap(tcap);
            
            context.completed(new UssMessage(abort));
        }
    }

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

    private MAPDialogSupplementary processUnstructuredSSRequest(JsonSccp sccp, JsonTcap tcap, JsonMapOperation mapMessage, String type) throws MAPException {

        MAPParameterFactory mapParameterFactory = mapProvider.getMAPParameterFactory();
        ISDNAddressString origReference = valueOf(tcap.getDialog().getOriginationReference());
        ISDNAddressString destReference = valueOf(tcap.getDialog().getDestinationReference());

        SccpAddress callingPartyAddress = valueOf(sccp.getCallingPartyAddress());
        SccpAddress calledPartyAddress = valueOf(sccp.getCalledPartyAddress());

        ISDNAddressString msisdn = valueOf(mapMessage.getMsisdn());

        CBSDataCodingSchemeImpl codingScheme = codingScheme(mapMessage.getCodingScheme());
        USSDString ussdString = mapParameterFactory.createUSSDString(mapMessage.getUssdString(), codingScheme, Charset.forName("UTF-8"));

        MAPDialogSupplementary mapDialog = this.mapProvider.getMAPServiceSupplementary()
                .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.networkUnstructuredSsContext, MAPApplicationContextVersion.version2),
                        callingPartyAddress, origReference, calledPartyAddress, destReference);

        mapDialog.addProcessUnstructuredSSRequest(codingScheme, ussdString, null, msisdn);
        mapDialog.getLocalDialogId();

        if (this.isEricsson(tcap.getDialog())) {
            ISDNAddressString msisdn1 = valueOf(tcap.getDialog().getMsisdn());
            ISDNAddressString vlrAddress = valueOf(tcap.getDialog().getVlrAddress());
            mapDialog.addEricssonData(msisdn1, vlrAddress);
        }

        return mapDialog;
    }

    private MAPDialogSupplementary unstructuredSSRequest(JsonSccp sccp, JsonTcap tcap, JsonComponent component, String type) throws MAPException {
        JsonMapOperation mapMessage = (JsonMapOperation) mapMessage(component).operation();
        MAPParameterFactory mapParameterFactory = mapProvider.getMAPParameterFactory();

        CBSDataCodingSchemeImpl codingScheme = codingScheme(mapMessage.getCodingScheme());
        USSDString ussdString = mapParameterFactory.createUSSDString(mapMessage.getUssdString(), codingScheme, Charset.forName("UTF-8"));

        MAPDialogSupplementary dialog = (MAPDialogSupplementary) mapProvider.getMAPDialog(tcap.getDialog().getDialogId());

        switch (type) {
            case "invoke":
                dialog.addUnstructuredSSRequest(codingScheme, ussdString, null, null);
                break;
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component.getValue();
                dialog.addUnstructuredSSResponse(returnResultLast.invokeId(), codingScheme, ussdString);
                break;
        }

        return dialog;
    }

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

    private CBSDataCodingSchemeImpl codingScheme(JsonDataCodingScheme scheme) {
        return new CBSDataCodingSchemeImpl(
                CBSDataCodingGroup.valueOf(scheme.getCodingGroup()),
                CharacterSet.valueOf(scheme.getLanguage()),
                CBSNationalLanguage.LanguageUnspecified,
                DataCodingSchemaMessageClass.Class0,
                false
        );

    }

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
        ExecutionContext context = asyncContextQueue.remove(mapDialog.getLocalDialogId());
        LOGGER.info("(TC-Close) <---> " + ctxName(context, mapDialog.getLocalDialogId()));
    }

    @Override
    public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
    }

    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        ExecutionContext context = asyncContextQueue.remove(mapDialog.getLocalDialogId());
        LOGGER.info("(TC-Release) <---> " + ctxName(context, mapDialog.getLocalDialogId()));
    }

    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        ExecutionContext context = asyncContextQueue.remove(mapDialog.getLocalDialogId());
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
        httpProcessor.processMessage(msg, "http://127.0.0.1:7081/UssdGate/test");
    }

    @Override
    public void onProcessUnstructuredSSResponse(ProcessUnstructuredSSResponse msg) {
        LOGGER.info("<--- On process unstructured ss response");
        UssMessage m = new UssMessage(msg, "process-unstructured-ss-request");

        ExecutionContext context = new MapExecutionContext(this);
    }

    @Override
    public void onUnstructuredSSRequest(UnstructuredSSRequest msg) {
        UssMessage m = new UssMessage(msg, "unstructured-ss-request");
        ExecutionContext context = asyncContextQueue.get(msg.getMAPDialog().getLocalDialogId());

        String ctxName = context != null
                ? context.toString()
                : "ExecutionContext(NULL, " + msg.getMAPDialog().getLocalDialogId();
        JsonComponent component = m.getTcap().getComponents().get(0);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("(TC-%s):{%s}: unstructured-ss-request <--- %s",
                    m.getTcap().getType(), component.getType(), ctxName));
        }

        if (context != null) {
            context.completed(m);
        }
    }

    @Override
    public void onUnstructuredSSResponse(UnstructuredSSResponse msg) {
        LOGGER.info("<--- " + msg.getMessageType().name());
        UssMessage m = new UssMessage(msg, "unstructured-ss-request");

        ExecutionContext context = asyncContextQueue.get(msg.getMAPDialog().getLocalDialogId());
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
