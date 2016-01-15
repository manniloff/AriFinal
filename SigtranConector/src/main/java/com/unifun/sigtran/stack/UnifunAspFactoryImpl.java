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
	
	protected void setAssociation(Association association) {
        // Unset the listener to previous association
        if (this.association != null) {
            this.association.setAssociationListener(null);
        }
        this.association = association;
        this.associationName = this.association.getName();
        // Set the listener for new association
        this.association.setAssociationListener(this);
    }
	
	protected void setTransportManagement(Management transportManagement) {
        this.transportManagement = transportManagement;
    }

	@Override
	protected void read(M3UAMessage message) {
		logger.debug(" Read from extended class");		
		try {
			logger.debug(localAssoc.getName() + " - " + forwardAssoc.getName());
		} catch (Exception e) {
			logger.debug("Error: "+ e.getMessage());
		}
		
		//write to forward interface if it is transfer message
		switch(message.getMessageClass()){
		case MessageClass.TRANSFER_MESSAGES:
		if (forwardAssoc!=null && localAssoc != null){
			logger.debug(" Forward mode is " + enableForward);
			if(enableForward){
				//handle forwarded traffic
				if (forwardAssoc.getName().equals(this.association.getName())){
					// Write to association from where initial traffic was received
					localWrite(message);
				}else{
					//write to forward interface
					fwdWrite(message);
				}
			}else {
				super.read(message);
			}
			break;
		}
		default: 
			super.read(message);
			break;
	}
	}
	
	private void fwdWrite(M3UAMessage message){
		logger.debug(" fwdWrite");
		switch (message.getMessageType()){
		case MessageType.PAYLOAD:
			try {
				PayloadData payload = (PayloadData) message;
				logger.debug(String.format("RoutingContext: %s, ", payload.getRoutingContext()));
				org.mobicents.protocols.api.PayloadData payloadData = null;
				int seqControl = payload.getData().getSLS();
				payloadData = new org.mobicents.protocols.api.PayloadData(payload.getData().getData().length, 
						payload.getData().getData(), true, false,
                    SCTP_PAYLOAD_PROT_ID_M3UA, this.slsTable[seqControl]);
              this.forwardAssoc.send(payloadData);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			break;
			default:
				logger.error(String.format("Rx : Transfer message with invalid MessageType=%d message=%s",
                        message.getMessageType(), message));
                break;
		}
//		synchronized (txBuffer) {
//            try {
//                txBuffer.clear();
//                ((M3UAMessageImpl) message).encode(txBuffer);
//                txBuffer.flip();
//
//                byte[] data = new byte[txBuffer.limit()];
//                txBuffer.get(data);
//
//                org.mobicents.protocols.api.PayloadData payloadData = null;
//
//                switch (message.getMessageClass()) {
//                    case MessageClass.ASP_STATE_MAINTENANCE:
//                    case MessageClass.MANAGEMENT:
//                    case MessageClass.ROUTING_KEY_MANAGEMENT:
//                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, true,
//                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
//                        break;
//                    case MessageClass.TRANSFER_MESSAGES:
//                        PayloadData payload = (PayloadData) message;
//                        int seqControl = payload.getData().getSLS();
//                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, false,
//                                SCTP_PAYLOAD_PROT_ID_M3UA, this.slsTable[seqControl]);
//                        break;
//                    default:
//                        payloadData = new org.mobicents.protocols.api.PayloadData(data.length, data, true, true,
//                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
//                        break;
//                }
//                logger.debug(String.format("Message Class: %d, ", message.getMessageClass()));
//                this.forwardAssoc.send(payloadData);
//            } catch (Exception e) {
//                logger.error(String.format("Error while trying to send PayloadData to SCTP layer. M3UAMessage=%s", message));
//            }
//        }
	}
	
	private void localWrite(M3UAMessage message){
		logger.debug(" localWrite");
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

                this.localAssoc.send(payloadData);
            } catch (Exception e) {
                logger.error(String.format("Error while trying to send PayloadData to SCTP layer. M3UAMessage=%s", message));
            }
        }
	}

	@Override
	protected void write(M3UAMessage message) {
		logger.info(" Write from extended class");
		super.write(message);
	}

	protected void setEnableForward(boolean enableForward) {
		this.enableForward = enableForward;
	}

	public void setForwardAssoc(Association forwardAssoc) {
		this.forwardAssoc = forwardAssoc;
	}

	protected void createSLSTable(int minimumBoundStream) {
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
	
	

}
