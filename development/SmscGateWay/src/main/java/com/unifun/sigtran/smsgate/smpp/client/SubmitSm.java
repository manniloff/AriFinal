package com.unifun.sigtran.smsgate.smpp.client;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
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
import org.jsmpp.extra.SessionState;

import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.smpp.ClientController;


public class SubmitSm implements Runnable {
	
	private long sleepTime;
	private SendData smsData;
	private SmppClient client;
	private static final Logger logger = LogManager.getLogger(SubmitSm.class);
	
	public SubmitSm(SendData smsData, SmppClient client, long sleepTime) {
		this.smsData = smsData;
		this.client = client;
		this.sleepTime = sleepTime;
	}

	@Override
	public void run() {
		if(smsData == null) {
			logger.warn("sms data is null");
			return;
		}
		short refNum = (short)(smsData.getMessageId() % 256);
		Thread.currentThread().setName(client.getClientConfig().getSystemId() + "_submitSM_MessageId - " 
				+ smsData.getMessageId());
		
		String remoteMessageId = null;
		String errorMessage = null;
		long started = System.currentTimeMillis();
		Charset cs = ClientController.getCharset(smsData.getDcs());
		Long remoteId = -1L;
		boolean needWaitDLR = (!"0".equals(smsData.getDlrResponseType()));
		try {
			short segments = smsData.getQuantity();
			if(client.getSmppSession().getSessionState().isBound()) {
				needWaitDLR = SessionState.BOUND_TRX.equals(client.getSmppSession().getSessionState())
						&& (!"0".equals(smsData.getDlrResponseType()));
				if(segments > 1) {
					int segmetnLength = Integer.valueOf(smsData.getSegmentLen());
					int segmentMax;
					int messageLen = smsData.getMessage().length();
					switch (client.getClientConfig().getConcatenateType()) {
					case "1":	//UDH
						byte[] udh = new byte[6];
						udh[0] = 0x05;	udh[1] = 0x00;
						udh[2] = 0x03;	udh[3] = (byte) refNum;
						udh[4] = (byte)smsData.getQuantity();
						for (int i = 0; i < smsData.getQuantity(); i++) {
							short currentStep = (short)(i + 1);
							segmentMax = ((segmetnLength * currentStep) > messageLen) ? messageLen : (segmetnLength * currentStep);
							byte[] segment = smsData.getMessage().substring(segmetnLength * (i), segmentMax).getBytes(cs);
							udh[5] = (byte)currentStep;
							byte[] message = Arrays.copyOf(udh, udh.length + segment.length);
							System.arraycopy(segment, 0, message, udh.length, segment.length);
							remoteMessageId = client.getSmppSession().submitShortMessage(client.getClientConfig().getServiceType()
									, TypeOfNumber.valueOf(Integer.valueOf(smsData.getFromTON()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getFromNP()).byteValue())
									, smsData.getFromAD()
									, TypeOfNumber.valueOf(Integer.valueOf(smsData.getToAN()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getToNP()).byteValue())
									, String.valueOf(smsData.getToAD())
									, new ESMClass(MessageMode.STORE_AND_FORWARD, MessageType.DEFAULT, GSMSpecificFeature.UDHI)
									, Integer.valueOf(smsData.getPid()).byteValue()
									, Integer.valueOf(smsData.getPriority()).byteValue()
									, null
									, ClientController.getExpiredAt(smsData.getSendUntil())
									, new RegisteredDelivery(Integer.valueOf(smsData.getDlrResponseType()))
									, (byte) 0, DataCodings.newInstance((byte) smsData.getDcs())
									, (byte) refNum
									, message
									, new OptionalParameter[0]);
							if(currentStep != smsData.getQuantity()) {
								try { Thread.sleep(sleepTime); } 
								catch (InterruptedException e) { }	
							}
						}		
						break;
					case "2":	//SAR
						OptionalParameter sarMsgRefNum = OptionalParameters.newSarMsgRefNum(refNum);
		                OptionalParameter sarTotalSegments = OptionalParameters.newSarTotalSegments(smsData.getQuantity());
						for (int i = 0; i < smsData.getQuantity(); i++) {
							short currentStep = (short)(i + 1);
							segmentMax = ((segmetnLength * currentStep) > messageLen) ? messageLen : (segmetnLength * currentStep);
							remoteMessageId = client.getSmppSession().submitShortMessage(client.getClientConfig().getServiceType()
									, TypeOfNumber.valueOf(Integer.valueOf(smsData.getFromTON()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getFromNP()).byteValue())
									, smsData.getFromAD()
									, TypeOfNumber.valueOf(Integer.valueOf(smsData.getToAN()).byteValue())
									, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getToNP()).byteValue())
									, String.valueOf(smsData.getToAD())
									, new ESMClass()
									, Integer.valueOf(smsData.getPid()).byteValue()
									, Integer.valueOf(smsData.getPriority()).byteValue()
									, null
									, ClientController.getExpiredAt(smsData.getSendUntil())
									, new RegisteredDelivery(Integer.valueOf(smsData.getDlrResponseType()))
									, (byte) 0, DataCodings.newInstance((byte) smsData.getDcs())
									, (byte) refNum
									, smsData.getMessage().substring(segmetnLength * (i), segmentMax).getBytes(cs)
									, sarMsgRefNum, OptionalParameters.newSarSegmentSeqnum(currentStep), sarTotalSegments);
//							client.getConLimit().counter.incrementAndGet();
							if(currentStep != smsData.getQuantity()) {
								try { Thread.sleep(sleepTime); } 
								catch (InterruptedException e) { }	
							}
						}		
						break;
					case "3":	//PayLoad
						remoteMessageId = client.getSmppSession().submitShortMessage(client.getClientConfig().getServiceType()
								, TypeOfNumber.valueOf(Integer.valueOf(smsData.getFromTON()).byteValue())
								, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getFromNP()).byteValue())
								, smsData.getFromAD()
								, TypeOfNumber.valueOf(Integer.valueOf(smsData.getToAN()).byteValue())
								, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getToNP()).byteValue())
								, String.valueOf(smsData.getToAD())
								, new ESMClass()
								, Integer.valueOf(smsData.getPid()).byteValue()
								, Integer.valueOf(smsData.getPriority()).byteValue(), null
								, ClientController.getExpiredAt(smsData.getSendUntil())
								, new RegisteredDelivery(Integer.valueOf(smsData.getDlrResponseType()))
								, (byte) 0, DataCodings.newInstance((byte) smsData.getDcs())
								, (byte) refNum
								, new byte[0]
								, OptionalParameters.deserialize(Tag.MESSAGE_PAYLOAD.code(), smsData.getMessage().getBytes(cs)));
//						for (int i = 0; i < smsData.getQuantity(); i++) {
//							client.getConLimit().counter.incrementAndGet();	
//						}
						break;
					}	
				} else {
					remoteMessageId = client.getSmppSession().submitShortMessage(client.getClientConfig().getServiceType()
							, TypeOfNumber.valueOf(Integer.valueOf(smsData.getFromTON()).byteValue())
							, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getFromNP()).byteValue())
							, smsData.getFromAD()
							, TypeOfNumber.valueOf(Integer.valueOf(smsData.getToAN()).byteValue())
							, NumberingPlanIndicator.valueOf(Integer.valueOf(smsData.getToNP()).byteValue())
							, String.valueOf(smsData.getToAD())
							, new ESMClass()
							, Integer.valueOf(smsData.getPid()).byteValue()
							, Integer.valueOf(smsData.getPriority()).byteValue(), null
							, ClientController.getExpiredAt(smsData.getSendUntil())
							, new RegisteredDelivery(Integer.valueOf(smsData.getDlrResponseType()))
							, (byte) 0, DataCodings.newInstance((byte) smsData.getDcs())
							, (byte) 0
							, smsData.getMessage().getBytes(cs.name())
							, new OptionalParameter[0]);
				}
			}
			if (remoteMessageId != null) {
				switch (client.getClientConfig().getRemoteIdType()) {
			    case "HEX":
			    	remoteId = new BigInteger(remoteMessageId, 16).longValueExact(); 
			     break;
			    case "LONG":
			    	remoteId = Long.valueOf(remoteMessageId); 
			     break;
			    default:
			    	errorMessage = "Unsupported RemoteIdType - " + client.getClientConfig().getRemoteIdType();
			     break;
			    }
			} else {
				errorMessage = "remote message is null";
			}
			logger.info("SubmitSM processed. remoteId - " + remoteMessageId + " | " + remoteId + "; Duration - " + (System.currentTimeMillis() - started));
		} catch (IllegalArgumentException e) {
			remoteId = -2L;
			errorMessage = "IllegalArgumentException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (PDUException e) {
			remoteId = -3L;
			errorMessage = "PDUException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (ResponseTimeoutException e) {
			remoteId = -4L;
			errorMessage = "ResponseTimeoutException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (InvalidResponseException e) {
			remoteId = -5L;
			errorMessage = "InvalidResponseException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (NegativeResponseException e) {
			if(e.getCommandStatus() == 88) { //Throttling error try to re send
				remoteId = -6L;
			} else {
				remoteId = -7L;	
			}
			errorMessage = "NegativeResponseException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (IOException e) {
			remoteId = -8L;
			errorMessage = "IOException. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		} catch (Exception e) {
			errorMessage = "Exception. ErrorMessage: " + e.getMessage();
			logger.error(e.getMessage() + Arrays.toString(e.getStackTrace()));
		}
		if(errorMessage != null) {
			logger.error(errorMessage);
		}
		
		ClientController.addDlrWaitingList(remoteId, client.getClientConfig().getId()
				, smsData, errorMessage, new Timestamp(started)
				, new Timestamp(System.currentTimeMillis()), needWaitDLR);	
	}	
}
