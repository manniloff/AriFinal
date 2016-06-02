package com.unifun.sigtran.smsgate.smpp.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.util.DeliveryReceiptState;

import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.smpp.ServerController;


public class SmppDeliveryReceiptTask implements Runnable {
    private final DLRQueue deliverSm;
    private final SMPPServerSession session;
    private static final Logger logger = LogManager.getLogger(SmppDeliveryReceiptTask.class);
    private long started;
    
    public SmppDeliveryReceiptTask(SMPPServerSession session,
    		DLRQueue deliverSm) {
        this.session = session;
        this.deliverSm = deliverSm;
        this.started = System.currentTimeMillis();
    }
    
    public void run() {
    	Thread.currentThread().setName("_MessageId - " + deliverSm.getMessageId());
    	String errorMessage = null;
    	try {
    		if("0".equals(deliverSm.getDlrResponseType())) {
    			ServerController.SaveDLRResponse(deliverSm, started
    					, System.currentTimeMillis(), errorMessage);
    			return;
    		}
    		if(!"1".equals(deliverSm.getDlrResponseType())) {
    			if(("2".equals(deliverSm.getState()) && "2".equals(deliverSm.getDlrResponseType()))
    					|| (!"2".equals(deliverSm.getState()) && "3".equals(deliverSm.getDlrResponseType()))) {
    				ServerController.SaveDLRResponse(deliverSm, started
    						, System.currentTimeMillis(), errorMessage);
    				return;	
    			}
    		}
    		if(session.getSessionState().isBound() 
    				&& session.getSessionState().isTransmittable()) {
    			DeliveryReceipt delRec = new DeliveryReceipt(String.valueOf(deliverSm.getMessageId()), 1, 1, deliverSm.getInserted()
    					, (deliverSm.getReceivedDLR() == null) ? new Date() : deliverSm.getReceivedDLR()
    					, DeliveryReceiptState.valueOf(Integer.valueOf(deliverSm.getState()) - 1),  null, deliverSm.getMessage());
                session.deliverShortMessage("", 
                        TypeOfNumber.valueOf(Integer.valueOf(deliverSm.getFromTON()).byteValue()), 
                        NumberingPlanIndicator.valueOf(Integer.valueOf(deliverSm.getFromNP()).byteValue()), 
                        deliverSm.getFromAD(), 
                        TypeOfNumber.valueOf(Integer.valueOf(deliverSm.getToAN()).byteValue()), 
                        NumberingPlanIndicator.valueOf(Integer.valueOf(deliverSm.getToNP()).byteValue()), 
                        String.valueOf(deliverSm.getToAD()), 
                        new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT, GSMSpecificFeature.DEFAULT), 
                        (byte)0,
                        (byte)0, 
                        new RegisteredDelivery(0), 
                        DataCodings.newInstance((byte)deliverSm.getDcs()), 
                        delRec.toString().getBytes());
        		if(System.currentTimeMillis() - started > 1000)
        			logger.warn("Process SmppDeliveryReceiptTask to long!");	
    		} else {
    			errorMessage = "Connection lost";
    		}
		} catch (IllegalArgumentException e) {
			errorMessage = "IllegalArgumentException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		} catch (PDUException e) {
			errorMessage = "PDUException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		} catch (ResponseTimeoutException e) {
			errorMessage = "ResponseTimeoutException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		} catch (InvalidResponseException e) {
			errorMessage = "InvalidResponseException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		} catch (NegativeResponseException e) {
			errorMessage = "NegativeResponseException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		} catch (IOException e) {
			errorMessage = "IOException: " + e.getMessage(); 
			logger.error(e.toString() + Arrays.toString(e.getStackTrace()));
		}
    	if(errorMessage != null) {
    		ServerController.removeConnection(session, false);
    	}
    	ServerController.SaveDLRResponse(deliverSm, started
				, System.currentTimeMillis(), errorMessage);
    }
}