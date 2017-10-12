/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonComponent;
import com.unifun.map.JsonInvoke;
import com.unifun.map.JsonMap;
import com.unifun.map.JsonMapOperation;
import com.unifun.map.JsonMessage;
import com.unifun.map.JsonReturnResultLast;
import com.unifun.map.JsonTcap;
import com.unifun.map.JsonTcapDialog;
import com.unifun.ussd.context.ExecutionContext;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
public class LocalChannel implements Channel {

    private final Gateway gateway;
    private final static Logger LOGGER = Logger.getLogger(LocalChannel.class);
    
    public LocalChannel(Gateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void send(String url, UssMessage msg, ExecutionContext context) {
        long dialogID = msg.getTcap().getDialog().getDialogId();
        
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("(DID:%d) ---> %s", dialogID, url));
        }
        JsonMessage ocsQuery = gateway.ocs().nextQuery();

        copyParams(msg, ocsQuery);

        //Start the asynchronous execution!
        //Asynchronous execution allows to leave methos Servlet.service() immediately
        //without holding this thread and container will be able to recycle this thread
        //for receiving incoming messages.
        //Final HTTP response will be handled later (when it will actually arrive)
        //by callback object HttpLocalContext
        try {
            Channel channel = gateway.channel("map://localhost");
            
            long dialogId = msg.getTcap().getDialog().getDialogId();
            long invokeID = invokeID(msg.getTcap());
            
            //send received message over map with async callback handler
            channel.send(null, new UssMessage(ocsQuery), new OcsContext(dialogId, invokeID, context));
        } catch (Exception e) {
            context.failed(e);
        }

    }

    @Override
    public void stop() {
    }

    private void copyParams(UssMessage m1, JsonMessage m2) {
        JsonTcap tcap1 = m1.getTcap();
        JsonTcap tcap2 = m2.getTcap();

        JsonTcapDialog dialog1 = tcap1.getDialog();
        JsonTcapDialog dialog2 = tcap2.getDialog();

        dialog2.setDestinationReference(dialog1.getDestinationReference());
        
//        dialog2.setOriginationReference(dialog1.getOriginationReference());

        if (dialog2.getMsisdn() != null) {
            dialog2.setMsisdn(dialog1.getMsisdn());
        }

        if (dialog2.getVlrAddress() != null) {
            dialog2.setVlrAddress(dialog1.getVlrAddress());
        }

        JsonMapOperation op1 = operation(tcap1);
        JsonMapOperation op2 = operation(tcap2);

//        op2.setUssdString(op1.getUssdString());
        if (op2.getMsisdn() != null) {
            op2.setMsisdn(op1.getMsisdn());
        }
    }

    private long invokeID(JsonTcap tcap) {
        JsonComponent component1 = tcap.getComponents().get(0);
        switch (component1.getType()) {
            case "invoke":
                JsonInvoke invoke = (JsonInvoke) component1.getValue();
                return invoke.getInvokeId();
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component1.getValue();
                return returnResultLast.getInvokeId();
        }
        return 0;
    }
    private JsonMapOperation operation(JsonTcap tcap) {
        JsonComponent component1 = tcap.getComponents().get(0);
        switch (component1.getType()) {
            case "invoke":
                JsonInvoke invoke = (JsonInvoke) component1.getValue();
                JsonMap map = (JsonMap) invoke.component();
                return (JsonMapOperation) map.operation();
            case "returnResultLast":
                JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component1.getValue();
                return (JsonMapOperation) (((JsonMap) returnResultLast.component()).operation());
        }
        return null;
    }

    private class OcsContext implements ExecutionContext {

        private final long dialogId;
        private final long invokeID;
        private final ExecutionContext context;

        public OcsContext(long dialogId, long invokeID, ExecutionContext context) {
            this.dialogId = dialogId;
            this.invokeID = invokeID;
            this.context = context;
        }

        @Override
        public void completed(UssMessage msg) {
            msg.getTcap().getDialog().setDialogId(dialogId);
            JsonComponent component1 = msg.getTcap().getComponents().get(0);
            switch (component1.getType()) {
                case "invoke":
                    JsonInvoke invoke = (JsonInvoke) component1.getValue();
                    invoke.setInvokeId(invokeID);
                case "returnResultLast":
                    JsonReturnResultLast returnResultLast = (JsonReturnResultLast) component1.getValue();
                    returnResultLast.setInvokeId(invokeID);
            }
            context.completed(msg);
        }

        @Override
        public void failed(Exception e) {
            context.failed(e);
        }

        @Override
        public void cancelled() {
            context.cancelled();
        }

    }
}
