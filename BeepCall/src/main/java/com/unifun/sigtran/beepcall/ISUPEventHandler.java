/**
 * 
 */
package com.unifun.sigtran.beepcall;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

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
import org.mobicents.protocols.ss7.isup.message.parameter.RangeAndStatus;
import org.mobicents.protocols.ss7.isup.message.parameter.TransmissionMediumRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.beepcall.persistence.IsupCallPersistence;
import com.unifun.sigtran.beepcall.persistence.IsupEventPersistence;
import com.unifun.sigtran.beepcall.utils.Channel;
import com.unifun.sigtran.beepcall.utils.Channel.CircuitStates;
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
    private final AtomicInteger id = new AtomicInteger(1);
    private IsupEventPersistence eventPersistence;
    private DataSource ds;
    private ExecutorService dbWorker;
    private Map<String,String> isupPreference;
    
    public int nextId() {
		int id = this.id.getAndIncrement();
    	if ( id == Integer.MAX_VALUE)
			this.id.set(1);
    	return id;
	}

	public CicManagement getCicMgm() {
		return cicMgm;
	}

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
		logger.info("Initiate Isupe Event Persistence");
		//eventPersistence = new IsupEventPersistence();
		//logger.info("Send RSC for each circuit");
		//resetAllChannels();
		//Send GRS
		//groupResetAllChannels();
	}
	
	private void allocateChannels(){
		long[] circuitIds = this.stack.getCircuitManager().getChannelIDs();
		for (long circuitId : circuitIds) {
			try{
			int cic = this.stack.getCircuitManager().getCIC(circuitId);
			int dpc = this.stack.getCircuitManager().getDPC(circuitId);
			this.cicMgm.addChannel(cic, dpc,circuitId);
			logger.info(String.format("Channel with cic: %d and dpc: %d added to CircuitManager", cic, dpc));
			}catch(Exception e){
				logger.warn("Some error ocure while allocating channels "+e.getMessage());
			}
		}
	}
	
	public void destroy(){		
		this.cicMgm.resetAllChannels();
		this.provider.removeListener(this);
	}

	@Override
	public void onEvent(ISUPEvent event) {
		logger.debug("[onEvent] " + event.getMessage());		
		String eventName = null;
		switch(event.getMessage().getMessageType().getCode())
		{
		case AnswerMessage.MESSAGE_CODE:
			eventName = "ANSWER";
			try{
				onANM(event);
			}catch(Exception e){
				logger.warn(String.format("Unable to handel onANM event: [ %s ]", e.getMessage()));
				e.printStackTrace();
			}
			break;
		case ApplicationTransportMessage.MESSAGE_CODE:
			eventName = "APPLICATION_TRANSPORT";
			break;
		case AddressCompleteMessage.MESSAGE_CODE:
			eventName = "ADDRESS_COMPLETE";
			try {
				onACM(event);
			} catch (Exception e) {
				logger.warn(String.format("Unable to handel onACM event: [ %s ]", e.getMessage()));
				e.printStackTrace();
			}
			break;
		case BlockingMessage.MESSAGE_CODE:
			eventName = "BLOCKING";
			break;			
		case BlockingAckMessage.MESSAGE_CODE:
			eventName = "BLOCKING_ACK";
			break;
		case CallProgressMessage.MESSAGE_CODE:
			eventName = "CALL_PROGRESS";
			try {
				onCPG(event);
			} catch (Exception e) {
				logger.warn(String.format("Unable to handel onCPG event: [ %s ]", e.getMessage()));
				e.printStackTrace();
			}
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
			try {
				onGRS(event);
			} catch (Exception e) {
				logger.warn(String.format("Unable to handel onGRS event: " + e.getMessage()));
				e.printStackTrace();
			}
			break;
		case CircuitGroupResetAckMessage.MESSAGE_CODE:
			eventName = "CIRCUIT_GROUP_RESET_ACK";
			try {
				onGRA(event);
			} catch (Exception e) {
				logger.warn(String.format("Unable to handel onGRA event: [ %s ]", e.getMessage()));
				e.printStackTrace();
			}
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
		case InitialAddressMessage.MESSAGE_CODE:
			eventName = "INITIAL_ADDRESS_MESSAGE";
			//Get Circuit State
			//
			try {
				onIAM(event);
			} catch (Exception e) {
				logger.warn(String.format("Unable to handel onIAM event: [ %s ]", e.getMessage()));
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
		case ReleaseCompleteMessage.MESSAGE_CODE:
			eventName = "RELEASE_COMPLETE";
			try{
				onRLC(event);
			}catch(Exception e){
				logger.warn(String.format(String.format("Unable to handle RLC [ %s ] ",e.getMessage())));
			}
			break;
		case ReleaseMessage.MESSAGE_CODE:
			eventName = "RELEASE";
			try {
				onREL(event);
			} catch (Exception e) {
				logger.warn(String.format(String.format("Unable to handle incommning REL [ %s ] ",e.getMessage())));
				e.printStackTrace();
			}
			break;
		case ResetCircuitMessage.MESSAGE_CODE:
			eventName = "RESET_CIRCUIT";			
			try {
				onRSC(event);
			} catch (Exception e) {
				logger.warn(String.format(String.format("Unable to handle incommning RCM [ %s ] ",e.getMessage())));
				e.printStackTrace();
			}
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
			try {
				onUBA(event);
			} catch (Exception e) {
				logger.warn(String.format(String.format("Unable to handle incommning UBA [ %s ] ",e.getMessage())));
				e.printStackTrace();
			}
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

	/**
	 * @param event
	 */
	private void onGRA(ISUPEvent event) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param event
	 */
	private void onGRS(ISUPEvent event) {
		logger.debug("onGRS " + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);		
		CircuitGroupResetMessage grs = (CircuitGroupResetMessage)event.getMessage();
		RangeAndStatus rangeAndStatus = null;
		try{
			rangeAndStatus = grs.getRangeAndStatus();
		}catch(Exception e){
			logger.warn("Range and status parameter missing. "+ e.getMessage());
		}		
		sendGRA(cic, dpc, channelId, rangeAndStatus);
	}
	
	public void sendGRA(int cic, int dpc, long channelId, RangeAndStatus rangeAndStatus){	
		logger.debug(String.format("sendGRA cic: %d, dpc: %d", cic,dpc));
		CircuitGroupResetAckMessage msg = this.provider.getMessageFactory().createGRA(cic);
		msg.setSls(cic);
		RangeAndStatus rAnds = this.provider.getParameterFactory().createRangeAndStatus();
		byte range = (byte)0x01;
		byte[] status ;
		try{
			range = rangeAndStatus.getRange();
			//status = ra
		}catch(Exception e){
			logger.warn("Range or status missing. "+e.getMessage());
			e.printStackTrace();
		}
		int len = (range + 1) / 8;
        if ((range + 1) % 8 != 0) {
            len++;
        }
        status = new byte[len];
        for (int i = 0; i<len; i++){
        	status[i] = (byte)0x00;
        }
		int rangeVal = range & 0xff;
		//status = {(byte)0x00};
		int endCic = cic + rangeVal;
		rAnds.setRange(range);
		rAnds.setStatus(status);
		msg.setRangeAndStatus(rAnds);
		try{
			this.provider.sendMessage(msg, dpc);
			logger.debug(String.format("GRA Sended to startcic: %d, endcic: %d ", cic, endCic ));
			//set CIC to IDLE
			for (int i = cic; i<= endCic; i++){
				logger.debug(String.format("Set channel to idle cic: %d, dpc: %d ", i, dpc ));
				cicMgm.setIdle(this.stack.getCircuitManager().getChannelID(i, dpc));
			}
		}catch(Exception e){
			logger.error(e.getMessage());
		}
	}

	private void onANM(ISUPEvent event) throws Exception {
		logger.debug("onANM " + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);
		Calendar calendar = Calendar.getInstance();
		AnswerMessage anm = (AnswerMessage) event.getMessage();
		int statusIndicator = anm.getBackwardCallIndicators().getCalledPartysStatusIndicator();
		this.cicMgm.setGotAcm(channelId);
		if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
			IsupEventPersistence isupEvPer = new IsupEventPersistence(
							ds, 
							isupPreference.get("events_proc"), 
							this.cicMgm.getChannelById(channelId).getSessionId(), 
							cic, 
							dpc, 
							null, 
							null, 
							new java.sql.Timestamp(calendar.getTime().getTime()),
							"ANM", 
							statusIndicator, 
							-1	);
			try {
				dbWorker.submit(isupEvPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		sendREL(cic, dpc, channelId, CauseIndicators._CV_ALL_CLEAR);
		
	}

	/**
	 * @param event
	 * @throws Exception 
	 */
	private void onRSC(ISUPEvent event) throws Exception {	
		logger.debug("onRSC" + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);
		this.cicMgm.setGotRsc(channelId);
		if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
			IsupEventPersistence isupEvPer = new IsupEventPersistence(
							ds, 
							isupPreference.get("events_proc"), 
							this.cicMgm.getChannelById(channelId).getSessionId(), 
							cic, 
							dpc, 
							null, 
							null, 
							new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
							"RSC", 
							-1, 
							-1	);
			try {
				dbWorker.submit(isupEvPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		sendRLC(cic, dpc, channelId);
	}

	/**
	 * @param event
	 * @throws Exception 
	 */
	private void onCPG(ISUPEvent event) throws Exception {
		logger.debug("onCPG" + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);
		CallProgressMessage cpg = (CallProgressMessage) event.getMessage();
		Calendar calendar = Calendar.getInstance();
		this.cicMgm.setGotCpg(channelId);
		int statusIndicator = cpg.getBackwardCallIndicators().getCalledPartysStatusIndicator();
		if (statusIndicator!=0){
			//it means that beep was perform
			//sending rel
			sendREL(cic, dpc, channelId, CauseIndicators._CV_ALL_CLEAR);			
		}
		// Write to db the result
		if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
			IsupEventPersistence isupEvPer = new IsupEventPersistence(
							ds, 
							isupPreference.get("events_proc"), 
							this.cicMgm.getChannelById(channelId).getSessionId(), 
							cic, 
							dpc, 
							null, 
							null, 
							new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
							"CPG", 
							statusIndicator, 
							-1	);
			try {
				dbWorker.submit(isupEvPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}

	/**
	 * @param event
	 * @throws Exception 
	 */
	private void onACM(ISUPEvent event) throws Exception {		
		logger.debug("onACM" + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);
		AddressCompleteMessage acm = (AddressCompleteMessage)event.getMessage();		
		this.cicMgm.setGotAcm(channelId);
		Calendar calendar = Calendar.getInstance();
		//get called_partys_status_indicator
		//if it-is 1 release circuit, if 1 wait for cpg
		/*
		 * bits	D	C:	Called party’s status indicator
			0	0	no indication
			0	1	subscriber free
			1	0	connect when free @
			1	1	spare
		 */
		int statusIndicator = acm.getBackwardCallIndicators().getCalledPartysStatusIndicator();
		if (statusIndicator!=0){
			//it means that beep was perform
			//sending rel
			sendREL(cic, dpc, channelId, CauseIndicators._CV_ALL_CLEAR);			
		}
		// wirte to db the resul
		if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
			IsupEventPersistence isupEvPer = new IsupEventPersistence(
							ds, 
							isupPreference.get("events_proc"), 
							this.cicMgm.getChannelById(channelId).getSessionId(), 
							cic, 
							dpc, 
							null, 
							null, 
							new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
							"ACM", 
							statusIndicator, 
							-1	);
			try {
				dbWorker.submit(isupEvPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}			
	}

	@Override
	public void onTimeout(ISUPTimeoutEvent event) {
		logger.debug("[onTimeout] " + event.getMessage());
		logger.debug("[CIC] "+ event.getMessage().getCircuitIdentificationCode().getCIC());
		//TODO Handle here each timeout
		logger.debug("[Timer Id] "+ event.getTimerId());
	}
	
	/**
	 * On IAM message we send back REL till we not have support of incoming IAM messages.
	 * @param event
	 * @throws Exception
	 */
	private void onIAM(ISUPEvent event) throws Exception{	
		logger.debug("onIAM" + event.getMessage());
		logger.info(String.format("Rx: ISUP IAM CIC: %d", event.getMessage().getCircuitIdentificationCode().getCIC()));		
		if (!(event.getMessage() instanceof InitialAddressMessage)){
			logger.warn("The event [ "+event+" ] is not instance of IAM");
			return;
		}
		InitialAddressMessage iam = (InitialAddressMessage) event.getMessage();
		int cic = iam.getCircuitIdentificationCode().getCIC();
		int dpc = event.getDpc();
		long channelId= this.stack.getCircuitManager().getChannelID(cic, dpc);
		Channel ch = null;
		try{
			ch = cicMgm.getChannelById(channelId);
		}catch (Exception e){
			logger.info(String.format("Getting IAM with cic: % but it is not present in circuit manager attempt to add it", cic));
			this.cicMgm.addChannel(cic, dpc, channelId);
			ch = cicMgm.getChannelById(channelId);
		}
		logger.debug(String.format("cic : %d channel state: %s", cic, ch.getState()));
		if (ch.getState() != CircuitStates.ST_IDLE){
			logger.warn(String.format("cic : %d channel state: %s", cic, ch.getState()));
			logger.warn("Send REL");						
			sendREL(cic, dpc, channelId, CauseIndicators._CV_NO_CIRCUIT_AVAILABLE);
			return;
		}
		ch.setState(CircuitStates.ST_GOT_IAM);
		logger.debug("Generate REL [Incoming IAM is not supported yet]");		
		sendREL(cic, dpc, channelId, CauseIndicators._CV_ALL_CLEAR);
		
	}
	private void onREL(ISUPEvent event) throws Exception{
		logger.debug("onREL" + event.getMessage());
		int dpc = event.getDpc();
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);		
		Calendar calendar = Calendar.getInstance();
		ReleaseMessage rel = (ReleaseMessage) event.getMessage();		
		// write to db	
		int causeIndicator;
		try {
			causeIndicator= rel.getCauseIndicators().getCauseValue();
		} catch (Exception e) {
			causeIndicator = -1;
		}
		this.cicMgm.getChannelById(channelId).setCauseIndicator(causeIndicator);
		this.cicMgm.setGotRel(channelId);
		if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
			IsupEventPersistence isupEvPer = new IsupEventPersistence(
							ds, 
							isupPreference.get("events_proc"), 
							this.cicMgm.getChannelById(channelId).getSessionId(), 
							cic, 
							dpc, 
							null, 
							null, 
							new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
							"REL", 
							-1, 
							causeIndicator	);
			try {
				dbWorker.submit(isupEvPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		sendRLC(cic, dpc, channelId);
	}
	
	private void onRLC(ISUPEvent event) throws Exception{
		logger.debug("onRLC" + event.getMessage());
		ReleaseCompleteMessage rlc = (ReleaseCompleteMessage) event.getMessage();
		int cic = rlc.getCircuitIdentificationCode().getCIC();
		int dpc = event.getDpc();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);		
		Channel chn = this.cicMgm.getChannelById(channelId);
		chn.setEndDate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		int causeIndicator;
		try {
			causeIndicator= rlc.getCauseIndicators().getCauseValue();
			chn.setCauseIndicator(causeIndicator);
		} catch (Exception e) {
			causeIndicator = -1;
		}
		//Write to call history
		
		if ("1".equalsIgnoreCase(isupPreference.get("log_calls"))){
			IsupCallPersistence isupCallPer = new IsupCallPersistence(
							ds, 
							isupPreference.get("calls_proc"), 
							this.cicMgm.getChannelById(channelId).getStatrtDate(), 
							this.cicMgm.getChannelById(channelId).getEndDate(), 
							this.cicMgm.getChannelById(channelId).getCallingParty(),
							this.cicMgm.getChannelById(channelId).getCalledParty(),
							this.cicMgm.getChannelById(channelId).getCauseIndicator()
					);
			try {
				dbWorker.submit(isupCallPer);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		//FIXME some leak i feel can be here
//		switch(chn.getState()){
//		case ST_SENT_IAM:
//		case ST_GOT_IAM:
//		case ST_SENT_REL:
//		case ST_SENT_RSC:	
//		case ST_BUSY:
//		case ST_CONCHECK:
			if(chn.getSessionId()!=0){
				if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
					IsupEventPersistence isupEvPer = new IsupEventPersistence(
									ds, 
									isupPreference.get("events_proc"), 
									this.cicMgm.getChannelById(channelId).getSessionId(), 
									cic, 
									dpc, 
									null, 
									null, 
									new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
									"RLC", 
									causeIndicator, 
									-1	);
					try {
						dbWorker.submit(isupEvPer);
					} catch (Exception e) {
						logger.error(e.getMessage());
					}
				}
			}
			cicMgm.setIdle(channelId);
//			break;
//		default:
//			break;
//		}
	}
	
	private void sendACM(int cic, int dpc, long channelId){
		logger.info(String.format("ACM cic: %d, dpc: %d", cic, dpc));		
		AddressCompleteMessage msg = this.provider.getMessageFactory().createACM(cic);
		BackwardCallIndicators bci = this.provider.getParameterFactory().createBackwardCallIndicators();
		msg.setBackwardCallIndicators(bci);
        msg.setSls(cic);
        try {
        	this.cicMgm.setSentAcm(channelId);
        	this.provider.sendMessage(msg,dpc);        	
            } catch (Exception e) {
            	logger.warn(e.getMessage());
        		e.printStackTrace();
        }
	}
	
	public void sendUBL(int cic, int dpc, long channelId){
		logger.info(String.format("Unblocking cic: %d, dpc: %d", cic, dpc));
		UnblockingMessage ubl = this.provider.getMessageFactory().createUBL(cic);
		ubl.setSls(cic);
		try {
			this.provider.sendMessage(ubl, dpc);
		}catch (Exception e) {
        	logger.warn(e.getMessage());
		   	e.printStackTrace();
		}
	}
	private void onUBA(ISUPEvent event){
		logger.debug("onUBA" + event.getMessage());		
		int cic = event.getMessage().getCircuitIdentificationCode().getCIC();
		int dpc = event.getDpc();
		long channelId = this.stack.getCircuitManager().getChannelID(cic, dpc);
		//Send RSC to channel
		sendRSC(cic, dpc, channelId);
	}
	public void sendREL(int cic, int dpc, long channelId, int causeValue){	
		logger.debug(String.format("sendREL cic: %d, dpc: %d, causeValue: %d", cic,dpc,causeValue));
		ReleaseMessage msg = this.provider.getMessageFactory().createREL(cic);
		msg.setSls(cic);
		CauseIndicators cause = this.provider.getParameterFactory().createCauseIndicators();
		cause.setCauseValue(causeValue);
		msg.setCauseIndicators(cause);
		try {
			this.cicMgm.getChannelById(channelId).setCauseIndicator(causeValue);
			if (causeValue != CauseIndicators._CV_NO_CIRCUIT_AVAILABLE)
		   		cicMgm.setSentRel(channelId);
		   	this.provider.sendMessage(msg,dpc);	   	
			if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
				IsupEventPersistence isupEvPer = new IsupEventPersistence(
								ds, 
								isupPreference.get("events_proc"), 
								this.cicMgm.getChannelById(channelId).getSessionId(), 
								cic, 
								dpc, 
								null, 
								null, 
								new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
								"REL", 
								-1, 
								causeValue	);
				try {
					dbWorker.submit(isupEvPer);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}	
		} catch (Exception e) {
        	logger.warn(e.getMessage());
		   	e.printStackTrace();
		}
	}
	
	public void sendRSC(int cic, int dpc, long channelId){
		logger.info(String.format("sendRSC to cic: %d, dpc: %d", cic, dpc));
		ResetCircuitMessage msg = this.provider.getMessageFactory().createRSC(cic);
		msg.setSls(cic);
		try {
			this.cicMgm.setSentRsc(channelId);
			this.provider.sendMessage(msg, dpc);			
		} catch (Exception e) {
			logger.warn(e.getMessage());
		   	e.printStackTrace();
		}
	}
	public void sendGRS(int cic, int dpc, long channelId) throws ParameterException{
		logger.info(String.format("sendGRS cic: %d, dpc: %d", cic, dpc));
		CircuitGroupResetMessage msg = this.provider.getMessageFactory().createGRS(cic);
		//RangeAndStatus RS = (RangeAndStatus) msg.getParameter(RangeAndStatus._PARAMETER_CODE);
		RangeAndStatus RS = this.provider.getParameterFactory().createRangeAndStatus();
		//RS.s
		msg.setRangeAndStatus(RS);
		msg.setSls(cic);
		try {
			this.cicMgm.setSentGrs(channelId);
			this.provider.sendMessage(msg, dpc);			
		} catch (Exception e) {
			logger.warn(e.getMessage());
		   	e.printStackTrace();
		}
	}
	private void sendRLC(int cic, int dpc, long channelId){
		logger.info(String.format("sendRLC cic: %d, dpc: %d", cic, dpc));
		ReleaseCompleteMessage msg = this.provider.getMessageFactory().createRLC(cic);
		msg.setSls(cic);
		try { 
			this.cicMgm.getChannelById(channelId).setEndDate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
			this.provider.sendMessage(msg,dpc);			
			//Write to call history
			if ("1".equalsIgnoreCase(isupPreference.get("log_calls"))){
				IsupCallPersistence isupCallPer = new IsupCallPersistence(
								ds, 
								isupPreference.get("calls_proc"), 
								this.cicMgm.getChannelById(channelId).getStatrtDate(), 
								this.cicMgm.getChannelById(channelId).getEndDate(), 
								this.cicMgm.getChannelById(channelId).getCallingParty(),
								this.cicMgm.getChannelById(channelId).getCalledParty(),
								this.cicMgm.getChannelById(channelId).getCauseIndicator()
						);
				try {
					dbWorker.submit(isupCallPer);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}			
			if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
				IsupEventPersistence isupEvPer = new IsupEventPersistence(
								ds, 
								isupPreference.get("events_proc"), 
								this.cicMgm.getChannelById(channelId).getSessionId(), 
								cic, 
								dpc, 
								null, 
								null, 
								new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
								"RLC", 
								-1, 
								-1	);
				try {
					dbWorker.submit(isupEvPer);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			this.cicMgm.setIdle(channelId);
		} catch (Exception e) {
			logger.warn(String.format("[Send RLC]", e.getMessage()));
			e.printStackTrace();
		}
	}
	public int sendIAM(String msisdnA, String msisdnB) throws Exception {
		String msisdnCalling = msisdnA;
		String msisdnCalled = msisdnB;
		logger.info(String.format("Generate IAM msisndA: %s msisdnB: %s", msisdnCalling, msisdnCalled));
		long channelId = this.cicMgm.getIdleChannel();		
		this.cicMgm.setBussy(channelId);
		Channel ch = this.cicMgm.getChannelById(channelId);
		ch.setSessionId(nextId());		
		//ch.setState(CircuitStates.ST_BUSY);
		int cic = ch.getCic();
		int dpc = ch.getDpc();		
		logger.info(String.format("cic: %d, dpc: %d, seted to busy", cic, dpc));
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
        //msg.setSls(0);
        msg.setSls(cic);
        CallingPartyNumber cpa = provider.getParameterFactory().createCallingPartyNumber();
        cpa.setNumberingPlanIndicator(CallingPartyNumber._NPI_ISDN);
        cpa.setNatureOfAddresIndicator(CallingPartyNumber._NAI_INTERNATIONAL_NUMBER);
        cpa.setAddress(msisdnCalling);
        cpa.setScreeningIndicator(0);
        msg.setCallingPartyNumber(cpa);
        
        try {
        	Calendar calendar = Calendar.getInstance();            
            ch.setState(CircuitStates.ST_SENT_IAM);
            ch.setStatrtDate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            ch.setCallingParty(msisdnCalling);
            ch.setCalledParty(msisdnCalled);
            provider.sendMessage(msg, dpc);
            // write to db the result
        	if ("1".equalsIgnoreCase(isupPreference.get("log_events"))){
    			IsupEventPersistence isupEvPer = new IsupEventPersistence(
    							ds, 
    							isupPreference.get("events_proc"), 
    							this.cicMgm.getChannelById(channelId).getSessionId(), 
    							cic, 
    							dpc, 
    							msisdnCalling, 
    							msisdnCalled, 
    							new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()),
    							"IAM", 
    							-1, 
    							-1	);
    			try {
    				dbWorker.submit(isupEvPer);
    			} catch (Exception e) {
    				logger.error(e.getMessage());
    			}
    		}
            // return sessionid
            return ch.getSessionId();
        } catch (ParameterException | IOException e) {
            logger.error("Failed to send message: " + e);
        }
        return -1;
    }
	
	public void resetAllChannels(){
		ConcurrentHashMap<Long, Channel> channels = this.cicMgm.getChannels();
		channels.keySet().forEach(channelId -> {
			Channel ch = channels.get(channelId);
			//this.getCicMgm().setSentRel(channelId);
			sendRSC(ch.getCic(), ch.getDpc(), channelId);
		});
	}
	
	public void groupResetAllChannels(){
		ConcurrentHashMap<Long, Channel> channels = this.cicMgm.getChannels();
		channels.keySet().forEach(channelId -> {
			Channel ch = channels.get(channelId);
			//this.getCicMgm().setSentRel(channelId);
			
			try {
				sendGRS(ch.getCic(), ch.getDpc(), channelId);
			} catch (Exception e) {
				logger.warn(e.getMessage());
				e.printStackTrace();
			}
		});
	}

	public ISUPStack getStack() {
		return stack;
	}

	public IsupEventPersistence getEventPersistence() {
		return eventPersistence;
	}

	public void setEventPersistence(IsupEventPersistence eventPersistence) {
		this.eventPersistence = eventPersistence;
	}

	public DataSource getDs() {
		return ds;
	}

	public void setDs(DataSource ds) {
		this.ds = ds;
	}

	public ExecutorService getDbWorker() {
		return dbWorker;
	}

	public void setDbWorker(ExecutorService dbWorker) {
		this.dbWorker = dbWorker;
	}

	public Map<String, String> getIsupPreference() {
		return isupPreference;
	}

	public void setIsupPreference(Map<String, String> isupPreference) {
		this.isupPreference = isupPreference;
	}

}
