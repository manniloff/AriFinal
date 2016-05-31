package com.unifun.sigtran.smsgate.smpp.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.Tag;
import org.jsmpp.bean.OptionalParameters;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.util.DeliveryReceiptState;

import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.hibernate.models.DataCodingList;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.smpp.ServerController;

public class SmppMOReceiptTask implements Runnable {
	private String state;
	private long started;
	private long sleepTime;
    private final SmsData moSm;
    private String serviceType;
    private short concatenateType;
    private final SMPPServerSession session;
    private static final Logger logger = LogManager.getLogger(SmppMOReceiptTask.class);
    
    public SmppMOReceiptTask(SMPPServerSession session,
    		SmsData moSm, short concatenateType, String serviceType) {
    	this.moSm = moSm;
    	this.session = session;
    	this.serviceType = serviceType;
    	this.concatenateType = concatenateType;
    	started = System.currentTimeMillis();
    	state = DeliveryReceiptState.REJECTD.name();
    }
    
    public void run() {
    	Thread.currentThread().setName("_MessageId - " + moSm.getMessage());
    	String errorMessage = null;
    	try {
    		if(session.getSessionState().isBound() 
    				&& session.getSessionState().isTransmittable()) {
    			Charset cs = StandardCharsets.UTF_8;
    			DataCodingList charSet = SmsGateWay.getDataCodingList().get(moSm.getDcs());
    			if(charSet != null)
    				cs = Charset.forName(charSet.getCharset());
    			if(moSm.getQuantity() == 1) {
    				session.deliverShortMessage(
    						serviceType, 
	                        TypeOfNumber.valueOf(Integer.valueOf(moSm.getFromTON()).byteValue()), 
	                        NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getFromNP()).byteValue()), 
	                        moSm.getFromAD(), 
	                        TypeOfNumber.valueOf(Integer.valueOf(moSm.getToAN()).byteValue()), 
	                        NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getToNP()).byteValue()), 
	                        String.valueOf(moSm.getToAD()), 
	                        new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.DEFAULT), 
	                        (byte)moSm.getPid(),
	                        Integer.valueOf(moSm.getPriority()).byteValue(),
	                        new RegisteredDelivery(0), 
	                        DataCodings.newInstance((byte)moSm.getDcs()),
	                        moSm.getMessage().getBytes(cs),
	                        new OptionalParameter[0]);	
    			} else if (moSm.getQuantity() > 1) {
    				int segmetnLength = Integer.valueOf(moSm.getSegmentLen());
					int segmentMax;
					int messageLen = moSm.getMessage().length();
					short refNum = (short)(moSm.getMessageId() % 256);
					switch (concatenateType) {
					case 1: // UDH
						byte[] udh = new byte[6];
						udh[0] = 0x05;	udh[1] = 0x00;
						udh[2] = 0x03;	udh[3] = (byte) refNum;
						udh[4] = (byte)moSm.getQuantity();
						for (int i = 0; i < moSm.getQuantity(); i++) {
	    					short currentStep = (short)(i + 1);
							segmentMax = ((segmetnLength * currentStep) > messageLen) ? messageLen : (segmetnLength * currentStep);
							byte[] segment = moSm.getMessage().substring(segmetnLength * (i), segmentMax).getBytes(cs);
							udh[5] = (byte)currentStep;
							byte[] message = Arrays.copyOf(udh, udh.length + segment.length);
							System.arraycopy(segment, 0, message, udh.length, segment.length);
	    					session.deliverShortMessage(serviceType,TypeOfNumber.valueOf(Integer.valueOf(moSm.getFromTON()).byteValue()), 
	    	                        NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getFromNP()).byteValue()), 
	    	                        moSm.getFromAD(), 
	    	                        TypeOfNumber.valueOf(Integer.valueOf(moSm.getToAN()).byteValue()), 
	    	                        NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getToNP()).byteValue()), 
	    	                        String.valueOf(moSm.getToAD()), 
	    	                        new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI), 
	    	                        (byte)moSm.getPid(),
	    	                        Integer.valueOf(moSm.getPriority()).byteValue(),
	    	                        new RegisteredDelivery(0), 
	    	                        DataCodings.newInstance((byte)moSm.getDcs()),
	    	                        message,
	    	                        new OptionalParameter[0]);	
	    					if(currentStep != moSm.getQuantity()) {
								try { Thread.sleep(sleepTime); } 
								catch (InterruptedException e) { }	
							}
						}
						break;
					case 2:	//SAR
						OptionalParameter sarMsgRefNum = OptionalParameters.newSarMsgRefNum(refNum);
		                OptionalParameter sarTotalSegments = OptionalParameters.newSarTotalSegments(moSm.getQuantity());
						for (int i = 0; i < moSm.getQuantity(); i++) {
							short currentStep = (short)(i + 1);
							segmentMax = ((segmetnLength * currentStep) > messageLen) ? messageLen : (segmetnLength * currentStep);
							session.deliverShortMessage(serviceType
									, TypeOfNumber.valueOf(Integer.valueOf(moSm.getFromTON()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getFromNP()).byteValue())
									, moSm.getFromAD()
									, TypeOfNumber.valueOf(Integer.valueOf(moSm.getToAN()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getToNP()).byteValue())
									, String.valueOf(moSm.getToAD())
									, new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.DEFAULT)
									, Integer.valueOf(moSm.getPid()).byteValue()
									, Integer.valueOf(moSm.getPriority()).byteValue()
									, new RegisteredDelivery(0)
									, DataCodings.newInstance((byte) moSm.getDcs())
									, moSm.getMessage().substring(segmetnLength * (i), segmentMax).getBytes(cs)
									, sarMsgRefNum, OptionalParameters.newSarSegmentSeqnum(currentStep), sarTotalSegments);
							if(currentStep != moSm.getQuantity()) {
								try { Thread.sleep(sleepTime); } 
								catch (InterruptedException e) { }	
							}
						}		
						break;
					case 3:	//PayLoad
						session.deliverShortMessage(serviceType
								, TypeOfNumber.valueOf(Integer.valueOf(moSm.getFromTON()).byteValue())
								, NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getFromNP()).byteValue())
								, moSm.getFromAD()
								, TypeOfNumber.valueOf(Integer.valueOf(moSm.getToAN()).byteValue())
								, NumberingPlanIndicator.valueOf(Integer.valueOf(moSm.getToNP()).byteValue())
								, String.valueOf(moSm.getToAD())
								, new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.DEFAULT)
								, Integer.valueOf(moSm.getPid()).byteValue()
								, Integer.valueOf(moSm.getPriority()).byteValue()
								, new RegisteredDelivery(0)
								, DataCodings.newInstance((byte) moSm.getDcs())
								, new byte[0]
								, OptionalParameters.deserialize(Tag.MESSAGE_PAYLOAD.code(), moSm.getMessage().getBytes(cs)));
						break;
					}
    			} else {
    				errorMessage = "Unsupported quantity value";
    				logger.warn(errorMessage);
    			}				
    		} else {
    			state = DeliveryReceiptState.UNDELIV.name();
    			errorMessage = "Connection lost";
    		}
    		
		} catch (IllegalArgumentException e) {
			errorMessage = "IllegalArgumentException: " + e.getMessage(); 
			logger.error(e.toString() + " " + Arrays.toString(e.getStackTrace()));
		} catch (PDUException e) {
			errorMessage = "PDUException: " + e.getMessage(); 
			logger.warn(e.toString() + " " + Arrays.toString(e.getStackTrace()));
		} catch (ResponseTimeoutException e) {
			errorMessage = "ResponseTimeoutException: " + e.getMessage(); 
			logger.warn(e.toString() + " " + Arrays.toString(e.getStackTrace()));
		} catch (InvalidResponseException e) {
			errorMessage = "InvalidResponseException: " + e.getMessage(); 
			logger.warn(e.toString() + " " + Arrays.toString(e.getStackTrace()));
		} catch (NegativeResponseException e) {
			errorMessage = "NegativeResponseException: " + e.getMessage(); 
			logger.warn(e.toString() + " " +  Arrays.toString(e.getStackTrace()));
		} catch (IOException e) {
			errorMessage = "IOException: " + e.getMessage(); 
			logger.error(e.toString() + " " +  Arrays.toString(e.getStackTrace()));
		} catch (Exception e) {
			errorMessage = "Exception: " + e.getMessage(); 
			logger.error(e.toString() + " " +  Arrays.toString(e.getStackTrace()));
		}
    	if(errorMessage != null) {
    		ServerController.removeConnection(session, false);
    	} else {
    		state = DeliveryReceiptState.DELIVRD.name();
    	}
    	ServerController.SaveMOResponse(moSm.getMessageId(), new Timestamp(started), new Timestamp(System.currentTimeMillis()), errorMessage, state, moSm.getSystemId());
    }
}