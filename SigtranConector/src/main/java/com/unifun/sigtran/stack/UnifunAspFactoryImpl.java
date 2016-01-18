/**
 * 
 */
package com.unifun.sigtran.stack;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.ss7.m3ua.impl.AspFactoryImpl;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.impl.message.M3UAMessageImpl;
import org.mobicents.protocols.ss7.m3ua.message.M3UAMessage;
import org.mobicents.protocols.ss7.m3ua.message.MessageClass;
import org.mobicents.protocols.ss7.m3ua.message.MessageType;
import org.mobicents.protocols.ss7.m3ua.message.transfer.PayloadData;
import org.mobicents.protocols.ss7.m3ua.parameter.ProtocolData;
import org.mobicents.protocols.ss7.mtp.Mtp3TransferPrimitive;

/**
 * @author rbabin
 *
 */
public class UnifunAspFactoryImpl extends AspFactoryImpl{	

	private static final long serialVersionUID = 1575917844509940304L;
	
	private static final int SCTP_PAYLOAD_PROT_ID_M3UA = 3;
	
	private static final Logger logger = Logger.getLogger(UnifunAspFactoryImpl.class);
	private static long ASP_ID_COUNT = 1L;	
	private boolean enableForward= false;
	private Association localAssoc = null;
	private Association forwardAssoc = null;
	private ByteBuffer txBuffer = ByteBuffer.allocateDirect(8192);
	private int[] slsTable = null;
	private int maxSequenceNumber = 256;
	
	/**
	 * 
	 */
	public UnifunAspFactoryImpl() {
		super();
		txBuffer.clear();
        txBuffer.rewind();
        txBuffer.flip();        
	}
	/**
	 * @param aspName
	 * @param maxSequenceNumber
	 * @param aspid
	 * @param isHeartBeatEnabled
	 */	
	public UnifunAspFactoryImpl(String aspName, int maxSequenceNumber, long aspid, boolean isHeartBeatEnabled) {		
		super(aspName,maxSequenceNumber,aspid,isHeartBeatEnabled);	
		txBuffer.clear();
        txBuffer.rewind();
        txBuffer.flip();        
        this.slsTable = new int[maxSequenceNumber];

	}

	protected static long generateId() {
		ASP_ID_COUNT++;
        if (ASP_ID_COUNT == 4294967295L) {
            ASP_ID_COUNT = 1L;
        }
        return ASP_ID_COUNT;
    }
	
	@Override
	protected void setAssociation(Association association) {
		super.setAssociation(association);
    }
	
	@Override
	protected void setTransportManagement(Management transportManagement) {
        super.setTransportManagement(transportManagement);
    }

	@Override
	protected void read(M3UAMessage message) {
		logger.debug(" Read from extended class");		
		switch(message.getMessageClass()){
		case MessageClass.TRANSFER_MESSAGES:
			if (forwardAssoc!=null && localAssoc != null){
				logger.debug(" Forward mode is " + enableForward);
				if(enableForward){
					//handle forwarded traffic
					if (forwardAssoc.getName().equals(this.association.getName())){
						// Write to association from where initial traffic was received
						//localWrite(message);
						fwWrite(message, localAssoc);
					}else{
						//write to forward interface
						fwWrite(message, forwardAssoc);
					}
					break;
				}			
			}
		default: 
			super.read(message);
			break;
		}
	}
	
	private void fwWrite(M3UAMessage message, Association assoc){		
		synchronized (txBuffer) {
            try {
                txBuffer.clear();
                ((M3UAMessageImpl) message).encode(txBuffer);
                txBuffer.flip();

                byte[] data = new byte[txBuffer.limit()];
                txBuffer.get(data);

                org.mobicents.protocols.api.PayloadData payloadData = null;

                switch (message.getMessageClass()) {
                    case MessageClass.ASP_STATE_MAINTENANCE:
                    case MessageClass.MANAGEMENT:
                    case MessageClass.ROUTING_KEY_MANAGEMENT:
                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, true,
                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                        break;
                    case MessageClass.TRANSFER_MESSAGES:
                        PayloadData payload = (PayloadData) message;                        
                        int seqControl = payload.getData().getSLS();
                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, false,
                                SCTP_PAYLOAD_PROT_ID_M3UA, this.slsTable[seqControl]);
                        break;
                    default:
                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, true,
                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                        break;
                }
                logger.debug(String.format("Message Class: %d, ", message.getMessageClass()));
                assoc.send(payloadData);
            } catch (Exception e) {
                logger.error(String.format("Error while trying to send PayloadData to SCTP layer. M3UAMessage=%s", message));
            }
        }
	}

	protected void setEnableForward(boolean enableForward) {
		this.enableForward = enableForward;
	}

	public void setForwardAssoc(Association forwardAssoc) {
		this.forwardAssoc = forwardAssoc;
	}

	@Override
	protected void createSLSTable(int minimumBoundStream) {
		super.createSLSTable(minimumBoundStream);
        if (minimumBoundStream == 0) { // special case - only 1 stream
            for (int i = 0; i < this.maxSequenceNumber; i++) {
                slsTable[i] = 0;
            }
        } else {
            // SCTP Stream 0 is for management messages, we start from 1
            int stream = 1;
            for (int i = 0; i < this.maxSequenceNumber; i++) {
                if (stream > minimumBoundStream) {
                    stream = 1;
                }
                slsTable[i] = stream++;
            }
        }
    }
	public void setLocalAssoc(Association localAssoc) {
		this.localAssoc = localAssoc;
	}
	public Association getLocalAssoc() {
		return localAssoc;
	}
	public Association getForwardAssoc() {
		return forwardAssoc;
	}
	
	
	@Override
	public void onPayload(Association association, org.mobicents.protocols.api.PayloadData payloadData) {
		logger.debug(payloadData.getData());
		if (forwardAssoc!=null && localAssoc != null){
			if(enableForward){				
				Mtp3TransferPrimitive localMtp3TransferPrimitive = super.getM3UAManagement().
						 getMtp3TransferPrimitiveFactory().createMtp3TransferPrimitive(payloadData.getData());
				try{
					if (forwardAssoc.getName().equals(this.association.getName())){
						this.localAssoc.send(payloadData);
					}else{
						//Forward message						
						this.forwardAssoc.send(payloadData);
					}
				} catch (Exception e) {

				}
				
				return;
			}			
		}
		super.onPayload(association, payloadData);
	}
}
