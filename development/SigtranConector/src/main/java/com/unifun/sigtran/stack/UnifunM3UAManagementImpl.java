/**
 * 
 */
package com.unifun.sigtran.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.AspFactory;
import org.mobicents.protocols.ss7.m3ua.M3UAManagementEventListener;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.impl.oam.M3UAOAMMessages;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;

import javolution.util.FastList;

/**
 * @author rbabin
 *
 */
public class UnifunM3UAManagementImpl extends  M3UAManagementImpl {
	private static final Logger logger = Logger.getLogger(UnifunM3UAManagementImpl.class);
	private boolean enableForward= false;
	private Map<Association, Association> forwardAssocGroup = new HashMap<>();	
	private MAPProvider mapProvider = null;
	private TCAPStack tcapStack = null;

	public UnifunM3UAManagementImpl(String name,  String productName) {
		super(name, productName);		
	}

	@Override
	public AspFactory createAspFactory(String aspName, String associationName) throws Exception {
		return this.createAspFactory(aspName, associationName, false);
	}

	@Override
	public AspFactory createAspFactory(String aspName, String associationName, boolean isHeartBeatEnabled)
			throws Exception {
        long aspid = 0L;
        boolean regenerateFlag = true;

        while (regenerateFlag) {
            aspid = UnifunAspFactoryImpl.generateId();
            if (aspfactories.size() == 0) {
                // Special case where this is first Asp added
                break;
            }

            for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
            	UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();
                if (aspid == aspFactoryImpl.getAspid().getAspId()) {
                    regenerateFlag = true;
                    break;
                } else {
                    regenerateFlag = false;
                }
            }// for
        }// while

        return this.createAspFactory(aspName, associationName, aspid, isHeartBeatEnabled);
	}

	@Override
	public AspFactory createAspFactory(String aspName, String associationName, long aspid, boolean isHeartBeatEnabled)
			throws Exception {
		UnifunAspFactoryImpl factory = this.getAspFactory(aspName);

        if (factory != null) {
            throw new Exception(String.format(M3UAOAMMessages.CREATE_ASP_FAIL_NAME_EXIST, aspName));
        }

        Association association = this.transportManagement.getAssociation(associationName);
        if (association == null) {
            throw new Exception(String.format(M3UAOAMMessages.NO_ASSOCIATION_FOUND, associationName));
        }

        if (association.isStarted()) {
            throw new Exception(String.format(M3UAOAMMessages.ASSOCIATION_IS_STARTED, associationName));
        }

        if (association.getAssociationListener() != null) {
            throw new Exception(String.format(M3UAOAMMessages.ASSOCIATION_IS_ASSOCIATED, associationName));
        }

        for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
            UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();
            if (aspid == aspFactoryImpl.getAspid().getAspId()) {
                throw new Exception(String.format(M3UAOAMMessages.ASP_ID_TAKEN, aspid));
            }
        }

        factory = new UnifunAspFactoryImpl(aspName, this.getMaxSequenceNumber(), aspid, isHeartBeatEnabled);
        factory.setM3UAManagement(this);
        factory.setAssociation(association);
        factory.setTransportManagement(this.transportManagement);

        aspfactories.add(factory);

        this.store();

        for (FastList.Node<M3UAManagementEventListener> n = this.managementEventListeners.head(), end = this.managementEventListeners
                .tail(); (n = n.getNext()) != end;) {
            M3UAManagementEventListener m3uaManagementEventListener = n.getValue();
            try {
                m3uaManagementEventListener.onAspFactoryCreated(factory);
            } catch (Throwable ee) {
                logger.error("Exception while invoking onAspFactoryCreated", ee);
            }
        }

        return factory;
	}
	
	private UnifunAspFactoryImpl getAspFactory(String aspName) {
        for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
            UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();
            if (aspFactoryImpl.getName().equals(aspName)) {
                return aspFactoryImpl;
            }
        }
        return null;
    }

	public void setEnableForward(boolean enableForward) {
		this.enableForward = enableForward;
	}
	
	public boolean isEnableForward() {
		return enableForward;
	}
	
	public void aspFactoryReloadForwardMode(){
		logger.debug("Reload forward mode in ASPs");
		for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
            UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();
            logger.debug(String.format("ASP: %s ", aspFactoryImpl.getName()));
            logger.debug(String.format("Forward mode: %s ", this.enableForward));
            aspFactoryImpl.setEnableForward(this.enableForward);
            Association aspFactAssoc = aspFactoryImpl.getAssociation();
            // avoid this loops
            for (Association lAssoc : this.forwardAssocGroup.keySet()){
            	Association fwdAssoc = this.forwardAssocGroup.get(lAssoc);
            	if (aspFactAssoc.getName().equals(lAssoc.getName())){
            		logger.debug(String.format("Local association: %s  Forward association: %s", lAssoc.getName(), fwdAssoc.getName()));
            		aspFactoryImpl.setLocalAssoc(lAssoc);
            		aspFactoryImpl.setForwardAssoc(fwdAssoc);
            		break;
            	}
            	if (aspFactAssoc.getName().equals(fwdAssoc.getName())){
            		logger.debug("Forward connection");
            		logger.debug(String.format("Local association: %s  Forward association: %s", lAssoc.getName(), fwdAssoc.getName()));
            		aspFactoryImpl.setLocalAssoc(lAssoc);
            		aspFactoryImpl.setForwardAssoc(fwdAssoc);
            		break;
            	}
            	
            }
            //aspFactoryImpl.setForwardAssoc(forwardAssoc);
        }
	}
	
	public void aspFactoryReloadForwardMode(Association association, boolean fwdMode){

		for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
            UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();            
            if (aspFactoryImpl.getForwardAssoc().getName().equals(association.getName())){
            	aspFactoryImpl.setEnableForward(fwdMode);
            }
            if (aspFactoryImpl.getLocalAssoc().getName().equals(association.getName())){
            	aspFactoryImpl.setEnableForward(fwdMode);
            }

        }
	}



	public void addForwardAssocs(Association local, Association forward) {
		this.forwardAssocGroup.put(local, forward);
	}
	
	public Association getAssocName(String assocName){	
			logger.debug("Looking for association: "+assocName);
			for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {
	            UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();
	            logger.debug(String.format("ASP: %s Association: %s", aspFactoryImpl.getName(), aspFactoryImpl.getAssociation().getName()));
	            if (assocName.equals(aspFactoryImpl.getAssociation().getName()))
	            	return aspFactoryImpl.getAssociation();
	        }		
			return null;
	}
	public AspFactory getAspForAssoc(String assocName){
		logger.debug("Looking for ASP for association: "+assocName);
		for (FastList.Node<AspFactory> n = aspfactories.head(), end = aspfactories.tail(); (n = n.getNext()) != end;) {			
			UnifunAspFactoryImpl aspFactoryImpl = (UnifunAspFactoryImpl) n.getValue();            
            if (assocName.equals(aspFactoryImpl.getAssociation().getName())){
            	logger.debug(String.format("ASP: %s Association: %s", aspFactoryImpl.getName(), aspFactoryImpl.getAssociation().getName()));
            	return aspFactoryImpl;
            }
        }		
		return null;
	}

	public Map<Association, Association> getForwardAssocGroup() {
		return forwardAssocGroup;
	}

	public void setMapProvider(MAPProvider mapProvider) {
		this.mapProvider = mapProvider;
	}

	public void setTcapStack(TCAPStack tcapStack) {
		this.tcapStack = tcapStack;
	}

	public MAPProvider getMapProvider() {
		return mapProvider;
	}

	public TCAPStack getTcapStack() {
		return tcapStack;
	}

	
	

}
