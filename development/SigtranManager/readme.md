### Installation instruction:
1. Create folder _/opt/unifun_
2. Change folder owner to be system regular user (ex. _chown -R unifun:unifun /opt/unifun_)
3. Transfer jdk and tomcat to _/opt/unifun_
4. Unack jdk and tomcat and create relative simlinks
``` bash
	 ~# cd /opt/unifun
	 ~# gunzip jdk-8u45-linux-x64.gz
	 ~# tar -xvf jdk-8u45-linux-x64
	 ~# tar -xvzf apache-tomcat-8.0.22.tar.gz
	 ~# ln -s jdk-8u45 java
	 ~# ln -s apache-tomcat-8.0.22 tomcat
```
5. Create JAVA_HOME and CATALINA_HOME system enviroments
``` bash
	# cd /etc/cd /etc/profile.d/
	# touch java.sh
	# chmod +x java.sh
	# vi java.sh
```
Paste the follownig context to java.sh
```	bash
	export CATALINA_HOME=/opt/unifun/tomcat
	export JAVA_HOME=/opt/unifun/java
	export PATH=$JAVA_HOME/bin:$PATH
```
6. Copy SigtranConnector-1.0.jar to _<CATALINA_HOME>/lib/commonlibs_ also copy the rest of the dependencies libs
7. Add folder from p.6 to tomcat classloader
# opent <CATALINA_HOME>/conf/catalina.properties
# append the next ,"${catalina.home}/lib/commonlibs","${catalina.home}/lib/commonlibs/*.jar" to the following line:
# common.loader="${catalina.base}/lib","${catalina.base}/lib/*.jar","${catalina.home}/lib","${catalina.home}/lib/*.jar"
8. Register SigtranConnector LifecycleListener
# open <CATALINA_HOME>/conf/server.xml
# Add the following lines under GlobalNamingResources tag
<Resource name="shared/bean/SigtranObjectFactory" auth="Container"
	          type="com.unifun.sigtran.adaptor.SigtranConnectorBean"
	          factory="com.unifun.sigtran.adaptor.SigtranObjectFactory"/>



Usage:
---
Action:
 sri - Sending MAP-SEND-ROUTING-INFO-FOR-SM request for specified msisdn (url:/LBS_CellId/LbsService?action=SRI&msisdn=<number>)
 psi -  Sending  Mobility Services, Subscriber Information services "ProvideSubscriberInfoRequest" (url:/LBS_CellId/LbsService?action=PSI&imsi=<imsi>&vlr=<vlr>)
 sripsi - not implemented yet
 ati - not implemented yet
 
 
logs:
====================/MAP-SEND-ROUTING-INFO-FOR-SM/=========================================
2015.03.23 15:59:48.934 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogAccept]: MAPDialog: LocalDialogId=1 RemoteDialogId=null MAPDialogState=ACTIVE MAPApplicationContext=MAPApplicationContext [Name=shortMsgGatewayContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 20, 3, ] TCAPDialogState=InitialSent [MAPExtensionContainer]: null
2015.03.23 15:59:48.942 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onMAPMessage]: SendRoutingInfoForSMResponse [DialogId=1, imsi=IMSI [437090103892071], locationInfoWithLMSI=LocationInfoWithLMSIImpl [networkNodeNumber=ISDNAddressString[AddressNature=international_number, NumberingPlan=ISDN, Address=996701588000]]]
2015.03.23 15:59:48.943 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onSendRoutingInfoForSMResponse]: SendRoutingInfoForSMResponse [DialogId=1, imsi=IMSI [437090103892071], locationInfoWithLMSI=LocationInfoWithLMSIImpl [networkNodeNumber=ISDNAddressString[AddressNature=international_number, NumberingPlan=ISDN, Address=996701588000]]]
2015.03.23 15:59:48.943 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogClose]: MAPDialog: LocalDialogId=1 RemoteDialogId=null MAPDialogState=ACTIVE MAPApplicationContext=MAPApplicationContext [Name=shortMsgGatewayContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 20, 3, ] TCAPDialogState=InitialSent
2015.03.23 15:59:48.943 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogRelease]: MAPDialog: LocalDialogId=1 RemoteDialogId=null MAPDialogState=EXPUNGED MAPApplicationContext=MAPApplicationContext [Name=shortMsgGatewayContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 20, 3, ] TCAPDialogState=Expunged
====================/Subscriber Information services/=========================================
2015.03.23 16:12:21.932 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogAccept]: MAPDialog: LocalDialogId=2 RemoteDialogId=null MAPDialogState=ACTIVE MAPApplicationContext=MAPApplicationContext [Name=subscriberInfoEnquiryContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 28, 3, ] TCAPDialogState=InitialSent [MAPExtensionContainer]: null
2015.03.23 16:12:21.959 [pool-1-thread-1] DEBUG [MapLayer      ]  - [ProvideSubscriberInfoResponse]ProvideSubscriberInfoResponse [subscriberInfo=SubscriberInfo [, locationInformation=LocationInformation [, ageOfLocationInformation=0, vlrNumber=ISDNAddressString[AddressNature=international_number, NumberingPlan=ISDN, Address=996701588000], locationNumber=LocationNumberMap [LocationNumber [numberingPlanIndicator=1, internalNetworkNumberIndicator=0, addressRepresentationRestrictedIndicator=3, screeningIndicator=3, natureOfAddresIndicator=4, oddFlag=0, address=996701588000]], cellGlobalIdOrServiceAreaIdOrLAI=[CellGlobalIdOrServiceAreaIdOrLAI [CellGlobalIdOrServiceAreaIdFixedLength [MCC=437, MNC=9, Lac=7139, CellId(SAC)=6533]]], mscNumber=ISDNAddressString[AddressNature=international_number, NumberingPlan=ISDN, Address=996701588000], currentLocationRetrieved, saiPresent], subscriberState=SubscriberState [subscriberStateChoice=assumedIdle]]]
2015.03.23 16:12:21.960 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogClose]: MAPDialog: LocalDialogId=2 RemoteDialogId=null MAPDialogState=ACTIVE MAPApplicationContext=MAPApplicationContext [Name=subscriberInfoEnquiryContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 28, 3, ] TCAPDialogState=InitialSent
2015.03.23 16:12:21.960 [pool-1-thread-1] DEBUG [MapLayer      ]  - [onDialogRelease]: MAPDialog: LocalDialogId=2 RemoteDialogId=null MAPDialogState=EXPUNGED MAPApplicationContext=MAPApplicationContext [Name=subscriberInfoEnquiryContext, Version=version3, Oid=0, 4, 0, 0, 1, 0, 28, 3, ] TCAPDialogState=Expunged