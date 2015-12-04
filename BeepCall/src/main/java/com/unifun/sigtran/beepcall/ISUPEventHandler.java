/**
 * 
 */
package com.unifun.sigtran.beepcall;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.mobicents.protocols.ss7.isup.ISUPEvent;
import org.mobicents.protocols.ss7.isup.ISUPListener;
import org.mobicents.protocols.ss7.isup.ISUPStack;
import org.mobicents.protocols.ss7.isup.ISUPTimeoutEvent;
import org.mobicents.protocols.ss7.isup.ParameterException;
import org.mobicents.protocols.ss7.isup.impl.ISUPProviderImpl;
import org.mobicents.protocols.ss7.isup.message.AddressCompleteMessage;
import org.mobicents.protocols.ss7.isup.message.AnswerMessage;
import org.mobicents.protocols.ss7.isup.message.ApplicationTransportMessage;
import org.mobicents.protocols.ss7.isup.message.BlockingAckMessage;
import org.mobicents.protocols.ss7.isup.message.BlockingMessage;
import org.mobicents.protocols.ss7.isup.message.CallProgressMessage;
import org.mobicents.protocols.ss7.isup.message.ChargeInformationMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupBlockingAckMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupBlockingMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupQueryMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupQueryResponseMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupResetAckMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupResetMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupUnblockingAckMessage;
import org.mobicents.protocols.ss7.isup.message.CircuitGroupUnblockingMessage;
import org.mobicents.protocols.ss7.isup.message.ConnectMessage;
import org.mobicents.protocols.ss7.isup.message.ContinuityCheckRequestMessage;
import org.mobicents.protocols.ss7.isup.message.ContinuityMessage;
import org.mobicents.protocols.ss7.isup.message.FacilityAcceptedMessage;
import org.mobicents.protocols.ss7.isup.message.FacilityRejectedMessage;
import org.mobicents.protocols.ss7.isup.message.ForwardTransferMessage;
import org.mobicents.protocols.ss7.isup.message.IdentificationRequestMessage;
import org.mobicents.protocols.ss7.isup.message.IdentificationResponseMessage;
import org.mobicents.protocols.ss7.isup.message.InitialAddressMessage;
import org.mobicents.protocols.ss7.isup.message.LoopPreventionMessage;
import org.mobicents.protocols.ss7.isup.message.LoopbackAckMessage;
import org.mobicents.protocols.ss7.isup.message.NetworkResourceManagementMessage;
import org.mobicents.protocols.ss7.isup.message.OverloadMessage;
import org.mobicents.protocols.ss7.isup.message.PassAlongMessage;
import org.mobicents.protocols.ss7.isup.message.PreReleaseInformationMessage;
import org.mobicents.protocols.ss7.isup.message.ReleaseCompleteMessage;
import org.mobicents.protocols.ss7.isup.message.ReleaseMessage;
import org.mobicents.protocols.ss7.isup.message.ResetCircuitMessage;
import org.mobicents.protocols.ss7.isup.message.ResumeMessage;
import org.mobicents.protocols.ss7.isup.message.SubsequentAddressMessage;
import org.mobicents.protocols.ss7.isup.message.SubsequentDirectoryNumberMessage;
import org.mobicents.protocols.ss7.isup.message.SuspendMessage;
import org.mobicents.protocols.ss7.isup.message.UnblockingAckMessage;
import org.mobicents.protocols.ss7.isup.message.UnblockingMessage;
import org.mobicents.protocols.ss7.isup.message.UnequippedCICMessage;
import org.mobicents.protocols.ss7.isup.message.UserPartAvailableMessage;
import org.mobicents.protocols.ss7.isup.message.UserPartTestMessage;
import org.mobicents.protocols.ss7.isup.message.UserToUserInformationMessage;
import org.mobicents.protocols.ss7.isup.message.parameter.BackwardCallIndicators;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CallingPartyCategory;
import org.mobicents.protocols.ss7.isup.message.parameter.CallingPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.mobicents.protocols.ss7.isup.message.parameter.ForwardCallIndicators;
import org.mobicents.protocols.ss7.isup.message.parameter.NatureOfConnectionIndicators;
import org.mobicents.protocols.ss7.isup.message.parameter.TransmissionMediumRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.beepcall.utils.Channel;
import com.unifun.sigtran.beepcall.utils.Channel.Circuit_states;
import com.unifun.sigtran.beepcall.utils.CicManagement;

/**
 * @author rbabin
 *
 */
public class ISUPEventHandler implements ISUPListener {
	public static final Logger logger = LoggerFactory.getLogger(String.format("%1$-15s] ", "[ISUPEventHandler"));
	protected ISUPStack stack;
    protected ISUPProviderImpl provider;
    CicManagement cicMgm;
    
    /**
	 * 
	 */
	public ISUPEventHandler(ISUPStack stack) {
		this.stack = stack;
		this.provider = (ISUPProviderImpl) stack.getIsupProvider();
	}
	
	public void init(){
		logger.info("Initiating ISUP listiner");
		cicMgm = new CicManagement();
		allocateChannels();
		this.provider.addListener(this);
		logger.info("ISUP Listiner addet to ISUP provider");
	}
	
	private void allocateChannels(){
		long[] circuitIds = this.stack.getCircuitManager().getChannelIDs();
		for (long circuitId : circuitIds) {
			int cic = this.stack.getCircuitManager().getCIC(circuitId);
			int dpc = this.stack.getCircuitManager().getDPC(circuitId);
			this.cicMgm.addChannel(cic, dpc);
			logger.info(String.format("Channel with cic: %d and dpc: %d added to CircuitManager", cic, dpc));
		}
	}
	
	public void destroy(){
		this.provider.removeListener(this);
	}

	@Override
	public void onEvent(ISUPEvent event) {
		logger.debug("[onEvent] " + event.getMessage());		
		logger.info("Received Message, code: "+event.getMessage().getMessageType().getCode());
		String eventName = null;
		switch(event.getMessage().getMessageType().getCode())
		{
		case AnswerMessage.MESSAGE_CODE:
			eventName = "ANSWER";
			break;
		case ApplicationTransportMessage.MESSAGE_CODE:
			eventName = "APPLICATION_TRANSPORT";
			break;
		//TODO ACM
		case AddressCompleteMessage.MESSAGE_CODE:
			eventName = "ADDRESS_COMPLETE";
			break;
		case BlockingMessage.MESSAGE_CODE:
			eventName = "BLOCKING";
			break;			
		case BlockingAckMessage.MESSAGE_CODE:
			eventName = "BLOCKING_ACK";
			break;
		//TODO CPG
		case CallProgressMessage.MESSAGE_CODE:
			eventName = "CALL_PROGRESS";
			break;
		case ChargeInformationMessage.MESSAGE_CODE:
			eventName = "CHARGE_INFORMATION";
			break;
		case CircuitGroupBlockingMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_BLOCKING";
			break;
		case CircuitGroupBlockingAckMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_BLOCKING_ACK";
			break;
		case CircuitGroupQueryMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_QUERY";
			break;
		case CircuitGroupQueryResponseMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_QUERY_RESPONSE";
			break;
		case CircuitGroupResetMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_RESET";
			break;
		case CircuitGroupResetAckMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_RESET_ACK";
			break;
		case CircuitGroupUnblockingMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_UNBLOCKING";
			break;
		case CircuitGroupUnblockingAckMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_UNBLOCKING_ACK";
			break;
		case ConnectMessage.MESSAGE_CODE:
			eventName = "CONNECT";
			break;
		case ContinuityCheckRequestMessage.MESSAGE_CODE:
			eventName = "CONTINUITY_CHECK_REQUEST";
			break;
		case ContinuityMessage.MESSAGE_CODE:
			eventName = "CONTINUITY";
			break;
		case FacilityAcceptedMessage.MESSAGE_CODE:
			eventName = "FACILITY_ACCPETED";
			break;
		case FacilityRejectedMessage.MESSAGE_CODE:
			eventName = "FACILITY_REJECTED";
			break;
		case ForwardTransferMessage.MESSAGE_CODE:
			eventName = "FORWARD_TRANSFER";
			break;
		case IdentificationRequestMessage.MESSAGE_CODE:
			eventName = "IDENTIFICATION_REQUEST";
			break;
		case IdentificationResponseMessage.MESSAGE_CODE:
			eventName = "IDENTIFICATION_RESPONSE";
			break;
		//TODO IAM
		case InitialAddressMessage.MESSAGE_CODE:
			eventName = "INITIAL_ADDRESS_MESSAGE";
			//Get Circuit State
			//
			try {
				onIAM(event);
			} catch (Exception e) {
				logger.warn("Unable to handel onIAM event: [%s]", e.getMessage());
				e.printStackTrace();
			}
			break;
		case LoopPreventionMessage.MESSAGE_CODE:
			eventName = "LOOP_PREVENTION";
			break;
		case LoopbackAckMessage.MESSAGE_CODE:
			eventName = "LOOPBACK_ACK";
			break;
		case NetworkResourceManagementMessage.MESSAGE_CODE:
			eventName = "NETWORK_RESOURCE_MANAGEMENT";
			break;
		case OverloadMessage.MESSAGE_CODE:
			eventName = "OVERLOAD";
			break;
		case PassAlongMessage.MESSAGE_CODE:
			eventName = "PASS_ALONG";
			break;
		case PreReleaseInformationMessage.MESSAGE_CODE:
			eventName = "PRERELEASE_INFORMATION";
			break;
		//TODO RLC
		case ReleaseCompleteMessage.MESSAGE_CODE:
			eventName = "RELEASE_COMPLETE";
			try{
				onRLC(event);
			}catch(Exception e){
				logger.warn(String.format("Unable to handle RLC [%s] ",e.getMessage()));
			}
			break;
		//TODO REL 12
		case ReleaseMessage.MESSAGE_CODE:
			eventName = "RELEASE";
			onREL(event);
			break;
		//TODO Message Code 18
		case ResetCircuitMessage.MESSAGE_CODE:
			eventName = "RESET_CIRCUIT";
			//FIXME 
			onREL(event);
			break;
		case ResumeMessage.MESSAGE_CODE:
			eventName = "RESUME";
			break;
		case SubsequentAddressMessage.MESSAGE_CODE:
			eventName = "SUBSEQUENT_ADDRESS";
			break;
		case SubsequentDirectoryNumberMessage.MESSAGE_CODE:
			eventName = "SUBSEQUENT_DIRECTORY_NUMBER";
			break;
		case SuspendMessage.MESSAGE_CODE:
			eventName = "SUSPEND";
			break;
		case UnblockingMessage.MESSAGE_CODE:
			eventName = "UNBLOCKING";
			break;
		case UnblockingAckMessage.MESSAGE_CODE:
			eventName = "UNBLOCKING_ACK";
			break;
		case UnequippedCICMessage.MESSAGE_CODE:
			eventName = "UNEQUIPPED_CIC";
			break;
		case UserToUserInformationMessage.MESSAGE_CODE:
			eventName = "USER_TO_USER_INFORMATION";
			break;
		case UserPartAvailableMessage.MESSAGE_CODE:
			eventName = "USER_PART_AVAILABLE";
			break;
		case UserPartTestMessage.MESSAGE_CODE:
			eventName = "USER_PART_TEST";
			break;
		default:
			logger.info("Received unkown event code: "+event.getMessage().getMessageType().getCode());			
			return;			
		}		
		
		
	}

	@Override
	public void onTimeout(ISUPTimeoutEvent event) {
		logger.debug("[onTimeout] " + event.getMessage());
		logger.debug("[CIC] "+ event.getMessage().getCircuitIdentificationCode().getCIC());
		
	}
	
	
	private void onIAM(ISUPEvent event) throws Exception{		
		logger.info(String.format("Rx: ISUP IAM CIC: %d", event.getMessage().getCircuitIdentificationCode().getCIC()));		
		if (!(event.getMessage() instanceof InitialAddressMessage)){
			logger.warn("The event [ "+event+" ] is not instance of IAM");
			return;
		}
		InitialAddressMessage iam = (InitialAddressMessage) event.getMessage();
		int cic = iam.getCircuitIdentificationCode().getCIC();
		Channel ch = null;
		try{
			ch = cicMgm.getChannelByCic(cic);
		}catch (Exception e){
			logger.info(String.format("Getting IAM with cic: % but it is not present in circuit manager attempt to add it", cic));
			this.cicMgm.addChannel(cic, event.getDpc());
			ch = cicMgm.getChannelByCic(cic);
		}
		logger.debug(String.format("cic : %d channel state: %s", cic, ch.getState()));
		if (ch.getState() != Circuit_states.ST_IDLE){
			logger.warn(String.format("cic : %d channel state: %s", cic, ch.getState()));
			logger.warn("Send REL");						
			sendREL(cic, event.getDpc(), CauseIndicators._CV_NO_CIRCUIT_AVAILABLE);
			return;
		}
		ch.setState(Circuit_states.ST_GOT_IAM);
		logger.debug("Generate ACM");
		//TODO send ACM
	}
	private void onREL(ISUPEvent event){
		sendRLC(event.getMessage().getCircuitIdentificationCode().getCIC(), event.getDpc());
	}
	
	private void onRLC(ISUPEvent event) throws Exception{
		ReleaseCompleteMessage rlc = (ReleaseCompleteMessage) event.getMessage();
		int cic = rlc.getCircuitIdentificationCode().getCIC();
		if (rlc.getCauseIndicators().getCauseValue() != CauseIndicators._CV_NO_CIRCUIT_AVAILABLE){
			this.cicMgm.getChannelByCic(cic).setState(Circuit_states.ST_IDLE);
		}
	}
	
	private void sendACM(int cic, int dpc){
		logger.info(String.format("ACM cic: %d, dpc: %d", cic, dpc));		
		AddressCompleteMessage msg = this.provider.getMessageFactory().createACM(cic);
		BackwardCallIndicators bci = this.provider.getParameterFactory().createBackwardCallIndicators();
		msg.setBackwardCallIndicators(bci);
        msg.setSls(cic);
        try {        	
        	this.provider.sendMessage(msg,dpc);
        	this.cicMgm.getChannelByCic(cic).setState(Circuit_states.ST_SENT_ACM);
            } catch (Exception e) {
            	logger.warn(e.getMessage());
        		e.printStackTrace();
        }
	}
	
	private void sendREL(int cic, int dpc, int causeValue){		
		ReleaseMessage msg = this.provider.getMessageFactory().createREL(cic);
		msg.setSls(cic);
		CauseIndicators cause = this.provider.getParameterFactory().createCauseIndicators();
		cause.setCauseValue(causeValue);
		msg.setCauseIndicators(cause);
		try {
			// just to play with stack, send smth
		   	this.provider.sendMessage(msg,dpc);
		} catch (Exception e) {
        	// TODO Auto-generated catch block
		   	e.printStackTrace();
		}
	}
	
	private void sendRLC(int cic, int dpc){
		logger.info(String.format("RLC cic: %d, dpc: %d", cic, dpc));
		ReleaseCompleteMessage msg = this.provider.getMessageFactory().createRLC(cic);
        msg.setSls(cic);

        try {
        	// just to play with stack, send smth
        	this.provider.sendMessage(msg,dpc);
            } catch (Exception e) {
            	// TODO Auto-generated catch block
        		e.printStackTrace();
        }
	}
	public void sendIAM(String msisdnA, String msisdnB) throws Exception {
		String msisdnCalling = msisdnA;
		String msisdnCalled = msisdnB;
		logger.info(String.format("Generate IAM msisndA: %s msisdnB: %s", msisdnCalling, msisdnCalled));
		int cic = this.cicMgm.getIdleCic();		
		Channel ch = this.cicMgm.getChannelByCic(cic);
		ch.setState(Circuit_states.ST_BUSY);
		int dpc = this.cicMgm.getChannelByCic(cic).getDpc();		
		logger.info(String.format("cic: %d, seted to busy", cic));
		InitialAddressMessage msg = provider.getMessageFactory().createIAM(cic);
        NatureOfConnectionIndicators nai = provider.getParameterFactory().createNatureOfConnectionIndicators();
        //nai.
        ForwardCallIndicators fci = provider.getParameterFactory().createForwardCallIndicators();
        //fci.
        CallingPartyCategory cpg = provider.getParameterFactory().createCallingPartyCategory();
        cpg.setCallingPartyCategory((byte) 10);
        TransmissionMediumRequirement tmr = provider.getParameterFactory().createTransmissionMediumRequirement();
        tmr.setTransimissionMediumRequirement(0);
        CalledPartyNumber cpn = provider.getParameterFactory().createCalledPartyNumber();
        cpn.setAddress(msisdnCalled);
        cpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
        cpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_INTERNATIONAL_NUMBER);

        msg.setNatureOfConnectionIndicators(nai);
        msg.setForwardCallIndicators(fci);
        msg.setCallingPartCategory(cpg);
        msg.setCalledPartyNumber(cpn);
        msg.setTransmissionMediumRequirement(tmr);
        msg.setSls(0);
        CallingPartyNumber cpa = provider.getParameterFactory().createCallingPartyNumber();
        cpa.setNumberingPlanIndicator(CallingPartyNumber._NPI_ISDN);
        cpa.setNatureOfAddresIndicator(CallingPartyNumber._NAI_INTERNATIONAL_NUMBER);
        cpa.setAddress(msisdnCalling);
        cpa.setScreeningIndicator(0);
        msg.setCallingPartyNumber(cpa);
        
        try {
            provider.sendMessage(msg, dpc);
            ch.setState(Circuit_states.ST_SENT_IAM);

        } catch (ParameterException | IOException e) {
            logger.error("Failed to send message: " + e);
        }
    }

}
