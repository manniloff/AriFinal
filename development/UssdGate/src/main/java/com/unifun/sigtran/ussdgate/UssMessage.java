/**
 *
 */
package com.unifun.sigtran.ussdgate;

import com.unifun.map.JsonAddressString;
import com.unifun.map.JsonComponent;
import com.unifun.map.JsonComponents;
import com.unifun.map.JsonDataCodingScheme;
import com.unifun.map.JsonGlobalTitle;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonSccp;
import com.unifun.map.JsonSccpAddress;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import java.io.Serializable;
import java.nio.charset.Charset;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.USSDString;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSResponse;
import org.mobicents.protocols.ss7.map.api.service.supplementary.SupplementaryMessage;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;
import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0001;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0011;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * @author rbabin
 *
 */
public class UssMessage implements Serializable {

    private JsonMessage jsonMessage;
    
    public UssMessage() {
    }

    /**
     * Creates instance of this message using the json format.
     * 
     * @param jsonMessage the message in json format.
     */
    public UssMessage(JsonMessage jsonMessage) {
        this.jsonMessage = jsonMessage;
    }
    
    /**
     * Creates instance of this message 
     * @param mapMessage 
     * @param operationName 
     */
    public UssMessage(SupplementaryMessage mapMessage, String operationName) {
        final JsonSccp sccp = new JsonSccp();        
        sccp.setCallingPartyAddress(valueOf(mapMessage.getMAPDialog().getLocalAddress()));
        sccp.setCalledPartyAddress(valueOf(mapMessage.getMAPDialog().getRemoteAddress()));

        
        final JsonMapOperation operation = new JsonMapOperation();        
        final JsonComponents components = new JsonComponents();
        
        switch (mapMessage.getMessageType()) {
            case processUnstructuredSSRequest_Request :
                ProcessUnstructuredSSRequest pussr = (ProcessUnstructuredSSRequest) mapMessage;
                
                JsonDataCodingScheme codingScheme = new JsonDataCodingScheme();
                codingScheme.setLanguage(pussr.getDataCodingScheme().getCharacterSet().name());
                codingScheme.setCodingGroup(pussr.getDataCodingScheme().getDataCodingGroup().name());
                
                operation.setMsisdn(valueOf(pussr.getMSISDNAddressString()));
                operation.setCodingScheme(codingScheme);
                operation.setUssdString(valueOf(pussr.getUSSDString()));

                JsonMap jsonMap = new JsonMap(operationName, operation);
                JsonInvoke invoke = new JsonInvoke(mapMessage.getInvokeId(), jsonMap);

                JsonComponent component = new JsonComponent();
                component.setType("invoke");
                component.setValue(invoke);

                components.add(component);
                break;
            case  processUnstructuredSSRequest_Response :
                ProcessUnstructuredSSResponse pussrr = (ProcessUnstructuredSSResponse) mapMessage;
                
                codingScheme = new JsonDataCodingScheme();
                codingScheme.setLanguage(pussrr.getDataCodingScheme().getCharacterSet().name());
                codingScheme.setCodingGroup(pussrr.getDataCodingScheme().getDataCodingGroup().name());
                
                operation.setCodingScheme(codingScheme);
                operation.setUssdString(valueOf(pussrr.getUSSDString()));

                jsonMap = new JsonMap(operationName, operation);
                JsonReturnResultLast returnResultLast = new JsonReturnResultLast(mapMessage.getInvokeId(), jsonMap);

                component = new JsonComponent();
                component.setType("returnResultLast");
                component.setValue(returnResultLast);

                components.add(component);
                break;
            case  unstructuredSSRequest_Request :
                UnstructuredSSRequest ussr = (UnstructuredSSRequest) mapMessage;
                
                codingScheme = new JsonDataCodingScheme();
                codingScheme.setLanguage(ussr.getDataCodingScheme().getCharacterSet().name());
                codingScheme.setCodingGroup(ussr.getDataCodingScheme().getDataCodingGroup().name());
                
                operation.setMsisdn(valueOf(ussr.getMSISDNAddressString()));
                operation.setCodingScheme(codingScheme);
                operation.setUssdString(valueOf(ussr.getUSSDString()));
                
                jsonMap = new JsonMap(operationName, operation);
                invoke = new JsonInvoke(mapMessage.getInvokeId(), jsonMap);

                component = new JsonComponent();
                component.setType("invoke");
                component.setValue(invoke);

                components.add(component);
                break;
            case  unstructuredSSRequest_Response :
                UnstructuredSSResponse ussrr = (UnstructuredSSResponse) mapMessage;
                
                codingScheme = new JsonDataCodingScheme();
                codingScheme.setLanguage(ussrr.getDataCodingScheme().getCharacterSet().name());
                codingScheme.setCodingGroup(ussrr.getDataCodingScheme().getDataCodingGroup().name());
                
                operation.setCodingScheme(codingScheme);
                operation.setUssdString(valueOf(ussrr.getUSSDString()));
                
                jsonMap = new JsonMap(operationName, operation);
                returnResultLast = new JsonReturnResultLast(mapMessage.getInvokeId(), jsonMap);

                component = new JsonComponent();
                component.setType("returnResultLast");
                component.setValue(returnResultLast);

                components.add(component);
                break;
        }
        
        AddressString origReference = mapMessage.getMAPDialog().getReceivedDestReference();        
        AddressString destReference = mapMessage.getMAPDialog().getReceivedDestReference();
        
        final JsonTcapDialog tcapDialog = new JsonTcapDialog();
        
        tcapDialog.setDialogId(mapMessage.getMAPDialog().getLocalDialogId());
        tcapDialog.setOriginationReference(valueOf(origReference));
        tcapDialog.setDestinationReference(valueOf(destReference));
                
        
        final JsonTcap tcap = new JsonTcap();
        tcap.setDialog(tcapDialog);  
        tcap.setType(mapMessage.getMAPDialog().getTCAPMessageType().name());
        tcap.setComponents(components);
        
        jsonMessage = new JsonMessage();
        jsonMessage.setSccp(sccp);
        jsonMessage.setTcap(tcap);
    }
    
    /**
     * Converts SS7 address string into json format.
     * 
     * @param address
     * @return 
     */
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
    
    private JsonSccpAddress valueOf(SccpAddress address) {
        final JsonGlobalTitle gt = new JsonGlobalTitle();
        gt.setDigits(address.getGlobalTitle().getDigits());
        
        switch (address.getGlobalTitle().getGlobalTitleIndicator()) {
            case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY :
                gt.setNatureOfAddressIndicator(((GlobalTitle0001)address.getGlobalTitle()).getNatureOfAddress().name());
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY :
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME :
                gt.setEncodingSchema(((GlobalTitle0011)address.getGlobalTitle()).getEncodingScheme().getType().name());
                gt.setNumberingPlan(((GlobalTitle0011)address.getGlobalTitle()).getNumberingPlan().name());
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS :
                gt.setNatureOfAddressIndicator(((GlobalTitle0100)address.getGlobalTitle()).getNatureOfAddress().name());
                gt.setEncodingSchema(((GlobalTitle0100)address.getGlobalTitle()).getEncodingScheme().getType().name());
                gt.setNumberingPlan(((GlobalTitle0100)address.getGlobalTitle()).getNumberingPlan().name());
                break;
        }
        
        
        JsonSccpAddress value = new JsonSccpAddress();
        value.setGtIndicator(address.getGlobalTitle().getGlobalTitleIndicator().name());
        value.setGlobalTitle(gt);
        value.setPc(address.getSignalingPointCode());
        value.setSsn(address.getSubsystemNumber());
        
        return value;
    }
    
    /**
     * Converts USSD string value into UTF-8 string.
     * 
     * @param ussdString
     * @return 
     */
    private String valueOf(USSDString ussdString) {
        try {
            return ussdString.getString(Charset.forName("UTF-8"));
        } catch (Exception e) {
            return "null";
        }
    }
    
    public JsonSccp getSccp() {
        return jsonMessage.getSccp();
    }
    
    public JsonTcap getTcap() {
        return jsonMessage.getTcap();
    }
    
    @Override
    public String toString() {
        return this.jsonMessage.toString();
    }

}
