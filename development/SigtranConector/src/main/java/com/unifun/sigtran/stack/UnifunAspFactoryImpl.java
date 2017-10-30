/**
 * 
 */
package com.unifun.sigtran.stack;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.ss7.m3ua.impl.AspFactoryImpl;
import org.mobicents.protocols.ss7.m3ua.impl.message.M3UAMessageImpl;
import org.mobicents.protocols.ss7.m3ua.message.M3UAMessage;
import org.mobicents.protocols.ss7.m3ua.message.MessageClass;
import org.mobicents.protocols.ss7.m3ua.message.MessageType;
import org.mobicents.protocols.ss7.m3ua.message.transfer.PayloadData;
import org.mobicents.protocols.ss7.m3ua.parameter.ProtocolData;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.mtp.Mtp3EndCongestionPrimitive;
import org.mobicents.protocols.ss7.mtp.Mtp3StatusCause;
import org.mobicents.protocols.ss7.mtp.Mtp3StatusPrimitive;
import org.mobicents.protocols.ss7.sccp.message.SccpMessage;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ParseException;
import org.mobicents.protocols.ss7.tcap.asn.TcapFactory;
import org.mobicents.protocols.ss7.tcap.asn.Utils;
import org.mobicents.protocols.ss7.tcap.asn.comp.TCAbortMessage;
import org.mobicents.protocols.ss7.tcap.asn.comp.TCBeginMessage;
import org.mobicents.protocols.ss7.tcap.asn.comp.TCContinueMessage;
import org.mobicents.protocols.ss7.tcap.asn.comp.TCEndMessage;
import org.mobicents.protocols.ss7.tcap.asn.comp.TCUniMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import javolution.util.FastMap;

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
	private FastMap<Integer, AtomicInteger> congDpcList = new FastMap<Integer, AtomicInteger>().shared();
	private UnifunM3UAManagementImpl m3uaMng;

	
	/**
	 * 
	 */
	public UnifunAspFactoryImpl() {
		super();
		txBuffer.clear();
        txBuffer.rewind();
        txBuffer.flip();
        this.m3uaMng = (UnifunM3UAManagementImpl)this.getM3UAManagement();
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
		switch(message.getMessageClass()){
		case MessageClass.TRANSFER_MESSAGES:			
			if (forwardAssoc!=null && localAssoc != null){
//				if(enableForward){
//					if (( this.m3uaMng.getMapProvider() != null) && (this.m3uaMng.getTcapStack() != null)){
//						if (this.m3uaMng.getTcapStack().getCounterProvider().getCurrentDialogsCount() > 0){
//							//Parse transactional id from m3ua message
//							if (message.getMessageType() == MessageType.PAYLOAD){
//								PayloadData pyload = (PayloadData)message;
//								ProtocolData protocolData = pyload.getData();
//								ByteArrayInputStream bais = new ByteArrayInputStream(protocolData.getData());
//								DataInputStream in = new DataInputStream(bais);
//								int mt = 0;
//								try {
//									mt = in.readUnsignedByte();
//								} catch (IOException e1) {
//									logger.warn("Some error while decoding");
//									e1.printStackTrace();
//								}
//								byte[] data = null;
//								try{
//									switch (mt) {
//									case SccpMessage.MESSAGE_TYPE_UDT:
//									case SccpMessage.MESSAGE_TYPE_UDTS: {  
//										in.read(); //second param
//										int cpaPointer = in.read() & 0xff;
//										in.mark(in.available());
//										in.skip(cpaPointer - 1);
//										int len = in.read() & 0xff;
//										byte[] buffer = new byte[len];
//										in.read(buffer);
//										//super.calledParty = createAddress(buffer,factory,sccpProtocolVersion);
//										in.reset();
//										cpaPointer = in.read() & 0xff;
//										in.mark(in.available());
//										in.skip(cpaPointer - 1);
//										len = in.read() & 0xff;
//										buffer = new byte[len];
//										in.read(buffer);
//										//super.callingParty = createAddress(buffer,factory,sccpProtocolVersion);
//										in.reset();
//										cpaPointer = in.read() & 0xff;
//
//										in.skip(cpaPointer - 1);
//										len = in.read() & 0xff;
//
//										data = new byte[len];
//										in.read(data);
//									}
//									break;
//
//									case SccpMessage.MESSAGE_TYPE_XUDT:
//									case SccpMessage.MESSAGE_TYPE_XUDTS: {
//										//this.setSecondParamaterData(in.read(), sccpProtocolVersion);
//										in.read();
//										//									this.hopCounter = new HopCounterImpl((byte) in.read());
//										in.read();
//										//									if (this.hopCounter.getValue() > HopCounter.COUNT_HIGH
//										//											|| this.hopCounter.getValue() <= HopCounter.COUNT_LOW) {
//										//										throw new IOException("Hop Counter must be between 1 and 15, it is: " + this.hopCounter);
//										//									}
//
//										int pointer = in.read() & 0xff;
//										in.mark(in.available());
//										//									if (pointer - 1 != in.skip(pointer - 1)) {
//										//										throw new IOException("Not enough data in buffer");
//										//									}
//										int len = in.read() & 0xff;
//
//										byte[] buffer = new byte[len];
//										in.read(buffer);
//
//										//calledParty = createAddress(buffer,factory,sccpProtocolVersion);
//
//										in.reset();
//
//										pointer = in.read() & 0xff;
//
//										in.mark(in.available());
//
//										//									if (pointer - 1 != in.skip(pointer - 1)) {
//										//										throw new IOException("Not enough data in buffer");
//										//									}
//										len = in.read() & 0xff;
//
//										buffer = new byte[len];
//										in.read(buffer);
//
//										//callingParty = createAddress(buffer,factory,sccpProtocolVersion);
//
//										in.reset();
//										pointer = in.read() & 0xff;
//										in.mark(in.available());
//										//									if (pointer - 1 != in.skip(pointer - 1)) {
//										//										throw new IOException("Not enough data in buffer");
//										//									}
//										len = in.read() & 0xff;
//
//										data = new byte[len];
//										in.read(data);
//
//										in.reset();
//										pointer = in.read() & 0xff;
//										in.mark(in.available());
//
//										if (pointer == 0) {
//											// we are done
//											//return;
//											break;
//										}
//										//									if (pointer - 1 != in.skip(pointer - 1)) {
//										//										throw new IOException("Not enough data in buffer");
//										//									}
//
//										int paramCode = 0;
//										// EOP
//										//									while ((paramCode = in.read() & 0xFF) != 0) {
//										//										len = in.read() & 0xff;
//										//										buffer = new byte[len];
//										//										in.read(buffer);
//										//										this.decodeOptional(paramCode, buffer, sccpProtocolVersion);
//										//									}
//									}
//									break;
//
//									case SccpMessage.MESSAGE_TYPE_LUDT:
//									case SccpMessage.MESSAGE_TYPE_LUDTS: {
//										//this.setSecondParamaterData(in.read(), sccpProtocolVersion);
//										in.read();
//										in.read();
//										//									this.hopCounter = new HopCounterImpl((byte) in.read());
//										//									if (this.hopCounter.getValue() > HopCounter.COUNT_HIGH
//										//											|| this.hopCounter.getValue() <= HopCounter.COUNT_LOW) {
//										//										throw new IOException("Hop Counter must be between 1 and 15, it is: " + this.hopCounter);
//										//									}
//
//										int pointer = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
//										in.mark(in.available());
//										if (pointer - 1 != in.skip(pointer - 1)) {
//											//throw new IOException("Not enough data in buffer");
//											break;
//										}
//										int len = in.read() & 0xff;
//										byte[] buffer = new byte[len];
//										in.read(buffer);
//										//calledParty = createAddress(buffer,factory,sccpProtocolVersion);
//
//										in.reset();
//										pointer = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
//										in.mark(in.available());
//										if (pointer - 1 != in.skip(pointer - 1)) {
//											//throw new IOException("Not enough data in buffer");
//											break;
//										}
//										len = in.read() & 0xff;
//										buffer = new byte[len];
//										in.read(buffer);
//										//callingParty = createAddress(buffer,factory,sccpProtocolVersion);
//
//										in.reset();
//										pointer = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
//										in.mark(in.available());
//										if (pointer - 1 != in.skip(pointer - 1)) {
//											//throw new IOException("Not enough data in buffer");
//											break;
//										}
//										len = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
//										data = new byte[len];
//										in.read(data);
//
//										in.reset();
//										pointer = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
//										in.mark(in.available());
//
//										if (pointer == 0) {
//											// we are done
//											//return;
//											break;
//										}
//										if (pointer - 1 != in.skip(pointer - 1)) {
//											//throw new IOException("Not enough data in buffer");
//											break;
//										}
//
//										int paramCode = 0;
//										// EOP
//										//									while ((paramCode = in.read() & 0xFF) != 0) {
//										//										len = in.read() & 0xff;
//										//										buffer = new byte[len];
//										//										in.read(buffer);
//										//										this.decodeOptional(paramCode, buffer, sccpProtocolVersion);
//										//									}
//									}
//									break;
//									}
//								}catch(Exception e){
//									logger.warn("Some error while decoding: "+e.getMessage());
//								}
//								//End Decoding
//								AsnInputStream ais = new AsnInputStream(data);								
//								long dialogId = 0;
//								try {
//									int tag = ais.readTag();
//									switch (tag) {
//						            // continue first, usually we will get more of those. small perf
//						            // boost
//						            case TCContinueMessage._TAG:
//						                TCContinueMessage tcm = null;
//						                try {
//						                    tcm = TcapFactory.createTCContinueMessage(ais);
//						                } catch (ParseException e) {
//						                    logger.error("ParseException when parsing TCContinueMessage: " + e.toString(), e);
//						                    break;
//						                }
//						                dialogId = Utils.decodeTransactionId(tcm.getDestinationTransactionId());						                
//						                break;
//
//						            case TCBeginMessage._TAG:
//						                TCBeginMessage tcb = null;
//						                try {
//						                    tcb = TcapFactory.createTCBeginMessage(ais);
//						                } catch (ParseException e) {
//						                    logger.error("ParseException when parsing TCBeginMessage: " + e.toString(), e);
//						                    break;
//						                }
//						                dialogId = 0L;
//						                break;
//						                
//						            case TCEndMessage._TAG:
//						                TCEndMessage teb = null;
//						                try {
//						                    teb = TcapFactory.createTCEndMessage(ais);
//						                } catch (ParseException e) {
//						                    logger.error("ParseException when parsing TCEndMessage: " + e.toString(), e);
//						                    break;
//						                }
//						                dialogId = Utils.decodeTransactionId(teb.getDestinationTransactionId());						               
//						                break;
//
//						            case TCAbortMessage._TAG:
//						                TCAbortMessage tub = null;
//						                try {
//						                    tub = TcapFactory.createTCAbortMessage(ais);
//						                } catch (ParseException e) {
//						                    logger.error("ParseException when parsing TCAbortMessage: " + e.toString(), e);
//						                    break;
//						                }
//
//						                dialogId = Utils.decodeTransactionId(tub.getDestinationTransactionId());						                
//						                break;
//
//						            case TCUniMessage._TAG:
//						                TCUniMessage tcuni;
//						                try {
//						                    tcuni = TcapFactory.createTCUniMessage(ais);
//						                } catch (ParseException e) {
//						                    logger.error("ParseException when parsing TCUniMessage: " + e.toString(), e);
//						                    break;
//						                }
//						                dialogId = 0L;
//						                break;
//						            default:
//						            	dialogId = 0L;
//						                break;
//						            }
//								} catch (Exception e) {
//									logger.warn("Error while decoding AsnInputStream");
//									e.printStackTrace();
//								}
//								if (this.m3uaMng.getMapProvider().getMAPDialog(dialogId) != null){
//									super.read(message);
//									return;
//								}
//							}
//						}
//					}
//				}
				if(forwardAssoc.isUp() && enableForward){									
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
				if(!forwardAssoc.isUp())
					logger.info(String.format("Association: %s is down, unable to forward message", forwardAssoc.getName()));
			}
		default: 
			super.read(message);
			break;
		}
	}
	
	private void fwWrite(M3UAMessage message, Association assoc){		
		try {
            ByteBufAllocator byteBufAllocator = assoc.getByteBufAllocator();
            ByteBuf byteBuf;
            if (byteBufAllocator != null) {
                byteBuf = byteBufAllocator.buffer();
            } else {
                byteBuf = Unpooled.buffer();
            }

            ((M3UAMessageImpl) message).encode(byteBuf);

            org.mobicents.protocols.api.PayloadData payloadData = null;

            if (this.m3UAManagementImpl.isSctpLibNettySupport()) {
                switch (message.getMessageClass()) {
                    case MessageClass.ASP_STATE_MAINTENANCE:
                    case MessageClass.MANAGEMENT:
                    case MessageClass.ROUTING_KEY_MANAGEMENT:
                        payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), byteBuf, true, true,
                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                        break;
                    case MessageClass.TRANSFER_MESSAGES:
                        PayloadData payload = (PayloadData) message;
                        int seqControl = payload.getData().getSLS();
                        payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), byteBuf, true,
                                false, SCTP_PAYLOAD_PROT_ID_M3UA, this.slsTable[seqControl]);
                        break;
                    default:
                        payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), byteBuf, true, true,
                                SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                        break;
                }

                assoc.send(payloadData);

                // congestion control - we will send MTP-PAUSE every 8 messages
                int congLevel = assoc.getCongestionLevel();
                if (message instanceof PayloadData) {
                    PayloadData payloadData2 = (PayloadData) message;
                    if (congLevel > 0) {
                        sendCongestionInfoToMtp3Users(congLevel, payloadData2.getData().getDpc());
                    } else {
                        sendCongestionEndInfoToMtp3Users(congLevel, payloadData2.getData().getDpc());
                    }
                }
            } else {
                byte[] bf = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bf);
                synchronized (txBuffer) {
                    switch (message.getMessageClass()) {
                        case MessageClass.ASP_STATE_MAINTENANCE:
                        case MessageClass.MANAGEMENT:
                        case MessageClass.ROUTING_KEY_MANAGEMENT:
                            payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), bf, true, true,
                                    SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                            break;
                        case MessageClass.TRANSFER_MESSAGES:
                            PayloadData payload = (PayloadData) message;
                            int seqControl = payload.getData().getSLS();
                            payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), bf, true, false,
                                    SCTP_PAYLOAD_PROT_ID_M3UA, this.slsTable[seqControl]);
                            break;
                        default:
                            payloadData = new org.mobicents.protocols.api.PayloadData(byteBuf.readableBytes(), bf, true, true,
                                    SCTP_PAYLOAD_PROT_ID_M3UA, 0);
                            break;
                    }

                    assoc.send(payloadData);
                }
            }
        } catch (Throwable e) {
            logger.error(String.format("Error while trying to send PayloadData to SCTP layer. M3UAMessage=%s", message), e);
        }
	}
	
	private void sendCongestionInfoToMtp3Users(int congLevel, int dpc) {
        AtomicInteger ai = congDpcList.get(dpc);
        if (ai == null) {
            ai = new AtomicInteger();
            congDpcList.put(dpc, ai);
        }
        if (ai.incrementAndGet() % 8 == 0) {
            Mtp3StatusPrimitive statusPrimitive = new Mtp3StatusPrimitive(dpc, Mtp3StatusCause.SignallingNetworkCongested,
                    congLevel, 0);
            this.m3UAManagementImpl.sendStatusMessageToLocalUser(statusPrimitive);
        }
    }
	
    private void sendCongestionEndInfoToMtp3Users(int congLevel, int dpc) {
        AtomicInteger ai = congDpcList.get(dpc);
        if (ai == null) {
            return;
        }

        ai = new AtomicInteger();
        congDpcList.remove(dpc);
        Mtp3EndCongestionPrimitive endCongestionPrimitive = new Mtp3EndCongestionPrimitive(dpc);
        this.m3UAManagementImpl.sendEndCongestionMessageToLocalUser(endCongestionPrimitive);
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
	
	
	
//	@Override
//	public void onPayload(Association association, org.mobicents.protocols.api.PayloadData payloadData) {
//		logger.debug(payloadData.getData());
//		if (forwardAssoc!=null && localAssoc != null){
//			if(enableForward){				
//				Mtp3TransferPrimitive localMtp3TransferPrimitive = super.getM3UAManagement().
//						 getMtp3TransferPrimitiveFactory().createMtp3TransferPrimitive(payloadData.getData());
//				try{
//					if (forwardAssoc.getName().equals(this.association.getName())){
//						this.localAssoc.send(payloadData);
//					}else{
//						//Forward message						
//						this.forwardAssoc.send(payloadData);
//					}
//				} catch (Exception e) {
//
//				}
//				
//				return;
//			}			
//		}
//		super.onPayload(association, payloadData);
//	}
}
