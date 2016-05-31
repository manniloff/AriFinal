package com.unifun.sigtran.smsgate.hibernate;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.transform.Transformers;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;

import com.unifun.sigtran.smsgate.AlertList;
import com.unifun.sigtran.smsgate.NextAttempt;
import com.unifun.sigtran.smsgate.SmsGateWay;
import com.unifun.sigtran.smsgate.enums.AccessType;
import com.unifun.sigtran.smsgate.enums.Direction;
import com.unifun.sigtran.smsgate.hibernate.models.AlertWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.ClientDLRWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProvider;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccess;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProviderAccessCharsetList;
import com.unifun.sigtran.smsgate.hibernate.models.ContentProvidersAccessesRestrictions;
import com.unifun.sigtran.smsgate.hibernate.models.DLRQueue;
import com.unifun.sigtran.smsgate.hibernate.models.DLRSendedLog;
import com.unifun.sigtran.smsgate.hibernate.models.DataCodingList;
import com.unifun.sigtran.smsgate.hibernate.models.ErroneousRequests;
import com.unifun.sigtran.smsgate.hibernate.models.GateWaySettings;
import com.unifun.sigtran.smsgate.hibernate.models.IncomingDLRLog;
import com.unifun.sigtran.smsgate.hibernate.models.MOIncoming;
import com.unifun.sigtran.smsgate.hibernate.models.MoRoutingRules;
import com.unifun.sigtran.smsgate.hibernate.models.MsisdnBlackList;
import com.unifun.sigtran.smsgate.hibernate.models.NextAttemptWaitingList;
import com.unifun.sigtran.smsgate.hibernate.models.NumberRegEx;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientErrorLog;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPClientsGroups;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPErrorLog;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPMoResponse;
import com.unifun.sigtran.smsgate.hibernate.models.SMPPServerConfig;
import com.unifun.sigtran.smsgate.hibernate.models.SMSQueue;
import com.unifun.sigtran.smsgate.hibernate.models.SendData;
import com.unifun.sigtran.smsgate.hibernate.models.SmsData;
import com.unifun.sigtran.smsgate.hibernate.models.SourcheSubstitutionList;
import com.unifun.sigtran.smsgate.hibernate.models.TempContentProvideAccessQueueSize;

public class DataBaseLayer {
	
	private SessionFactory sessionFactory;
	private StandardServiceRegistry serviceRegistry;
	private Map<String, String> mysqlConfig;
	private static final Logger logger = LogManager.getLogger(DataBaseLayer.class);
	
	
	public DataBaseLayer(Map<String, String> mysqlConfig) {
		this.mysqlConfig = mysqlConfig; 
	}

	public boolean initHibernate() {
		sessionFactory = buildSessionFactory(mysqlConfig);
		return !sessionFactory.isClosed();
	}
	
	public boolean stop() {
		try {
//			for (int i = 0; i < sessionFactory.getStatistics().getCollectionRoleNames().length; i++) {
//				logger.info(sessionFactory.getStatistics()
//						.getCollectionStatistics(sessionFactory.getStatistics().
//								getCollectionRoleNames()[i]));				
//			}
			sessionFactory.close();	
		} catch (Exception e) {
			logger.error(e.getStackTrace());
		}
		return sessionFactory.isClosed();
	}
//	private StatelessSession getStatelessSession() {
//		return this.sessionFactory.openStatelessSession();	
//	}
	
	private Session getSession() {
		return this.sessionFactory.openSession();	
	}
	
	private SessionFactory buildSessionFactory(Map<String, String> mysql) {
		
		Configuration config = new Configuration();
		mysql.entrySet().forEach(row -> {
			config.setProperty(row.getKey(), row.getValue());
		});
//		config.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
//		config.setProperty("hibernate.connection.url",String.format("jdbc:mysql://%s:%s/%s",mysql.get("ip"),mysql.get("port"), mysql.get("defaultschema")));
//		config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
//		config.setProperty("hibernate.connection.username", mysql.get("username"));
//		config.setProperty("hibernate.connection.password", mysql.get("password")); 
//		
//		config.setProperty("hibernate.connection.CharSet", "utf8");
//		config.setProperty("hibernate.connection.characterEncoding", "utf8");
//		config.setProperty("hibernate.connection.useUnicode", "true");
////		config.setProperty("hibernate.connection.isolation", "2");
//		config.setProperty("show_sql", "true");
//				
//		config.setProperty("hibernate.jdbc.batch_size", "50");       
//		config.setProperty("hibernate.c3p0.min_size", "5");
//		config.setProperty("hibernate.c3p0.max_size", "400"); //100
//		config.setProperty("hibernate.c3p0.timeout", "60");
//		config.setProperty("hibernate.c3p0.max_statements", "100");	//50
//		config.setProperty("hibernate.c3p0.acquire_increment", "40"); //
////		config.setProperty("hibernate.c3p0.statement_cache_num_deferred_close_threads", "1"); //
//		config.setProperty("hibernate.c3p0.idle_test_period", "60");
//		config.setProperty("hibernate.hbm2ddl.auto", "update");

		config.addAnnotatedClass(AlertWaitingList.class);
		config.addAnnotatedClass(ContentProvider.class);
		config.addAnnotatedClass(ContentProviderAccess.class);
		config.addAnnotatedClass(ContentProviderAccessCharsetList.class);
		config.addAnnotatedClass(ContentProvidersAccessesRestrictions.class);
		config.addAnnotatedClass(ClientDLRWaitingList.class);
		config.addAnnotatedClass(DLRQueue.class);
		config.addAnnotatedClass(DLRSendedLog.class);
		config.addAnnotatedClass(DataCodingList.class);
		config.addAnnotatedClass(ErroneousRequests.class);
		config.addAnnotatedClass(GateWaySettings.class);
		config.addAnnotatedClass(IncomingDLRLog.class);
		config.addAnnotatedClass(MOIncoming.class);
		config.addAnnotatedClass(MoRoutingRules.class);
		config.addAnnotatedClass(MsisdnBlackList.class);
		config.addAnnotatedClass(NextAttemptWaitingList.class);
		config.addAnnotatedClass(NumberRegEx.class);
		config.addAnnotatedClass(SendData.class);
		config.addAnnotatedClass(SourcheSubstitutionList.class);
//		config.addAnnotatedClass(SMPPConnectionRestrictions.class);
//		config.addAnnotatedClass(SMPPConnections.class);
		config.addAnnotatedClass(SMPPErrorLog.class);
		config.addAnnotatedClass(SMPPMoResponse.class);
		config.addAnnotatedClass(SMPPServerConfig.class);
		config.addAnnotatedClass(SmsData.class);
		config.addAnnotatedClass(SMSQueue.class);
		config.addAnnotatedClass(SMPPClientConfig.class);
		config.addAnnotatedClass(SMPPClientsGroups.class);
		// using for select. on dbl is empty
		config.addAnnotatedClass(TempContentProvideAccessQueueSize.class);
		serviceRegistry = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
		
		return config.buildSessionFactory(serviceRegistry);
	}

	public boolean addSubmitSM(SmsData smsData) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.save(smsData);
			session.createSQLQuery(
					  "INSERT srism_request(messageId, systemId, toAD, addedInQueue) "
					+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
				.setParameter("messageId", smsData.getMessageId())
				.setParameter("systemId", smsData.getSystemId())
				.setParameter("toAD", smsData.getToAD())
				.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();
			session.getTransaction().commit();
			logger.debug("insertSmsData accepted for - " + smsData.getMessageId());
			return true;
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("messageId - " + smsData.getMessageId() + " Error Message: " + e.getStackTrace());
			return false;
		} finally {
			session.close();
		}
	}
	
	public boolean saveSMSQueue(SMSQueue req){
		Session session = getSession();
		try {
			session.beginTransaction();
			session.save(req);
			session.getTransaction().commit();
			logger.debug("insertSmsData accepted for - " + req.getMessageId());
			return true;
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("messageId - " + req.getMessageId() + " Error Message: " + e.getStackTrace());
			return false;
		} finally {
			session.close();
		}
	}

	public void saveSmppErrorLog(int sysId, String sourceAddr, long destAddr, int sequenceNumber, int errorCode,
			String errorMessage, long occurred, boolean serverError) {
		try {
			Session session = getSession();
			try {
				session.beginTransaction();
				if(serverError) {
					session.save(new SMPPErrorLog(sysId, sourceAddr, destAddr, sequenceNumber, errorCode, errorMessage, new Timestamp(occurred)));
				} else {
					session.save(new SMPPClientErrorLog(sysId, sourceAddr, destAddr, sequenceNumber, errorCode, errorMessage, new Timestamp(occurred)));
				}
				session.getTransaction().commit();
			} catch (HibernateException e) {
				if (session.getTransaction() != null)
					session.getTransaction().rollback();
				logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			} finally {
				session.close();
			}	
		} catch (HibernateException e) {
			logger.error("getting session. Error Message: " + e.getStackTrace());
		}
	}
	public void saveSmppMoResponse(SMPPMoResponse log) {
		try {
			Session session = getSession();
			try {
				session.beginTransaction();
				session.save(log);
				session.getTransaction().commit();
			} catch (HibernateException e) {
				if (session.getTransaction() != null)
					session.getTransaction().rollback();
				logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			} finally {
				session.close();
			}	
		} catch (HibernateException e) {
			logger.error("getting session. Error Message: " + e.getStackTrace());
		}
	}
	public void saveIncomingMOLog(MOIncoming log) {
		try {
			Session session = getSession();
			try {
				session.beginTransaction();
				session.save(log);
				session.getTransaction().commit();
			} catch (HibernateException e) {
				if (session.getTransaction() != null)
					session.getTransaction().rollback();
				logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			} finally {
				session.close();
			}	
		} catch (HibernateException e) {
			logger.error("getting session. Error Message: " + e.getStackTrace());
		}
	}
	
//	public void addDLRSmppConnection(int systemId) {
//		Session session = getSession();
//		try {
//			SMPPConnections con = new SMPPConnections();
//			con.setSystemId(systemId);
//			con.setStarted(new Timestamp(System.currentTimeMillis()));
//			session.beginTransaction();
//			session.saveOrUpdate(con);
//			session.getTransaction().commit();
//		} catch (HibernateException e) {
//			if (session.getTransaction() != null)
//				session.getTransaction().rollback();
//			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
//		} finally {
//			session.close();
//		}
//	}
	
	public void removeDLRSmppConnection(int systemId) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.createSQLQuery("delete from smpp_transmittable_connections where systemId = :systemId")
				.setParameter("systemId", systemId).executeUpdate();
			session.getTransaction().commit();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
	}
	
//	public List<SMPPConnectionRestrictions> getConnectionRestrictions(Integer sysId) {
//		List<SMPPConnectionRestrictions> rest = null;
//		Session session = getSession();
//		try {
//			session.beginTransaction();
//			rest = session.getNamedQuery("getConnectionRestrictions")
//					.setParameter("sysId", sysId).list();
//			session.getTransaction().commit();
//		} catch (HibernateException e) {
//			if (session.getTransaction() != null)
//				session.getTransaction().rollback();
//			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
//		} finally {
//			session.close();
//		}
//		return rest;
//	}
	public List<NumberRegEx> loadNumberCheck() {
		List<NumberRegEx> result = new ArrayList<>();
		Session session = getSession();
		try {
			session.beginTransaction();
			result = session.createQuery("FROM NumberRegEx").list();
			session.getTransaction().commit();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	} 
	
	public List<ContentProvidersAccessesRestrictions> loadAccessesRestrictions() {
		List<ContentProvidersAccessesRestrictions> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM ContentProvidersAccessesRestrictions")
					.setCacheable(false).list();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	public List<ContentProviderAccessCharsetList> loadAccessesCharSetList() {
		List<ContentProviderAccessCharsetList> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM ContentProviderAccessCharsetList")
					.setCacheable(false).list();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	public List<SourcheSubstitutionList> loadSourceSubstitutionList() {
		List<SourcheSubstitutionList> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM SourcheSubstitutionList")
					.setCacheable(false).list();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	public List<MoRoutingRules> loadMoRoutingRulesList() {
		List<MoRoutingRules> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM MoRoutingRules")
					.setCacheable(false).list();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	public ContentProviderAccess getSmppConnection(String host, String login, String password, String ton, String np) {
		ContentProviderAccess rest = null;
		Session session = getSession();
		try {
			rest = (ContentProviderAccess) session.createQuery("FROM ContentProviderAccess "
					+ "WHERE login = :login AND password = :password"
					+ "  AND host = :host" 
					+ "  AND ton = :ton AND np = :np")
					.setParameter("host", host)
					.setParameter("login", login)
					.setParameter("password", password)
					.setParameter("ton", ton)
					.setParameter("np", np)
					.uniqueResult();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return rest;
	}
	
	public List<ContentProviderAccess> getSmppAccessList(AccessType accessType) {
		Session session = getSession();
		List<ContentProviderAccess> rest = new ArrayList<>();
		try {
			rest = session.createQuery("FROM ContentProviderAccess "
					+ "WHERE access_type = :accessType")
					.setParameter("accessType", accessType.name())
					.setCacheable(false).list();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return rest;
	}
	
	public void resetMAPQueues() {
		Session session = getSession();
		long started = System.currentTimeMillis();
		try {
			session.beginTransaction();
			session.getNamedQuery("resetQueues").executeUpdate();
			session.getTransaction().commit();
			logger.info("Queue Reseted");
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("Error message = " + e.getStackTrace());
		} finally {
			session.close();
		}
		logger.info("Reset Queues duration - " + (System.currentTimeMillis() - started));
	}
	
	public long getLastSMSQueueId() {
		long lastId = 0;
		Session session = getSession();
		try {
			session.beginTransaction();
			lastId = ((BigInteger)session.getNamedQuery("getLastSMSQueueId").uniqueResult()).longValue();
			session.getTransaction().commit();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("Error message = " + e.getStackTrace());
			lastId = -1;
		} finally {
			session.close();
		}
		return lastId;
	}
	
	public void moveToSMSData(int queryLimit) {
		Session session = getSession();
//		long started = System.currentTimeMillis();
		try {
			session.beginTransaction();
			session.getNamedQuery("moveToSMSData")
			.setParameter("queryLimit", queryLimit).executeUpdate();
			session.getTransaction().commit();
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > saveResponseTimeLimit)
//			logger.warn("MoveToSMSData is too long MoveToSMSDataLimit - " + saveResponseTimeLimit);
	}
	
	public void saveSRISMResponse(SendData data, String state, String errorMessage, boolean numberNotInRoaming) { 
		Long started = System.currentTimeMillis();
		Session session = getSession();
		try {
			session.beginTransaction();
			session.createSQLQuery(
				  "INSERT srism_response(messageId, dialogId, errorCode, errorMessage, receivedResponse) "
				+ "VALUES(:messageId, :dialogId, :errorCode, :errorMessage, :receivedResponse);")
			.setParameter("messageId", data.getMessageId()).setParameter("dialogId", data.getDialogId())
			.setParameter("errorCode", state).setParameter("errorMessage", errorMessage)
			.setParameter("receivedResponse", new Timestamp(started)).executeUpdate();
			if(state == null) {
				if(numberNotInRoaming) {
					session.createSQLQuery(
							  "INSERT mtfsm_request(messageId, systemId, toAD, addedInQueue) "
							+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
						.setParameter("messageId", data.getMessageId())
						.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
						.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();	
				}
			} else if("5".equals(state) || "8".equals(state)) {
				session.save(new IncomingDLRLog(data.getMessageId(), new Timestamp(started)
						, new Timestamp(started + SmsGateWay.getSendDLRUntil()), state, data.getSendAttempts()));
			} else if("10".equals(state) || "6".equals(state)) {
				session.createSQLQuery(
					  "INSERT reportsmds_request(messageId, systemId, toAD, addedInQueue) "
					+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
				.setParameter("messageId", data.getMessageId())
				.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
				.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();
				data.setSmDeliveryOutcome(("6".equals(state)) ? SMDeliveryOutcome.absentSubscriber : SMDeliveryOutcome.memoryCapacityExceeded);
				logger.info("added to RDSQueue for messageId=" + data.getMessageId() + "; dialog - " + data.getDialogId());
			} else {
				Timestamp nextAttempt = new Timestamp(System.currentTimeMillis() + SmsGateWay.getNextAttemptToReSend());
				session.save(new NextAttemptWaitingList(data.getMessageId(), data.getFromAD(), data.getFromTON()
						, data.getFromNP(), data.getToAD(), data.getToAN(), data.getToNP(), data.getMessage()
						, data.getQuantity(), data.getDcs(), data.getPid(), data.getInserted(), data.getSendUntil()
						, data.getSystemId(), data.getDlrResponseType(), data.getPriority(), data.getSegmentLen()
						, data.getSendAttempts(), "1", state
						, data.getSendUntil().after(nextAttempt) ? nextAttempt : data.getSendUntil()));
				logger.info("added to NextAttemptWaitingList for messageId=" + data.getMessageId() 
							+ " dialog - " + data.getDialogId());
			}
			session.getTransaction().commit();
		} catch (HibernateException e) { 
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > saveResponseTimeLimit)
//			logger.warn("insert is too long for dialog = " + data.getDialogId() + "! SaveResponseTimeLimit = " + saveResponseTimeLimit);
	}
	
	public void saveMTFSMResponse(SendData data, boolean isLastPart, String state, String errorMessage) {
		Long started = System.currentTimeMillis();
		Session session = getSession();
//		Timestamp nextAttempt = new Timestamp(System.currentTimeMillis() + this.nextAttempt);
		try {
			session.beginTransaction();
			session.createSQLQuery(
				  "INSERT mtfsm_response(messageId, dialogId, systemId, toAD, errorCode, errorMessage, receivedResponse) "
				+ "VALUES(:messageId, :dialogId, :systemId, :toAD, :errorCode, :errorMessage, :receivedResponse);")
			.setParameter("messageId", data.getMessageId()).setParameter("dialogId", data.getDialogId())
			.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
			.setParameter("errorCode", ("2".equals(state)) ? null : state).setParameter("errorMessage", errorMessage)
			.setParameter("receivedResponse", new Timestamp(started)).executeUpdate();
			if("2".equals(state)) {
				if(isLastPart) {
					session.save(new IncomingDLRLog(data.getMessageId(), new Timestamp(started)
							, new Timestamp(started + SmsGateWay.getSendDLRUntil()), state, data.getSendAttempts()));
					logger.info("added to DLRQueue for messageId=" + data.getMessageId() + "; state " + state);
				} else {
					session.createSQLQuery(
						  "INSERT mtfsm_request(messageId, systemId, toAD, addedInQueue) "
						+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
					.setParameter("messageId", data.getMessageId())
					.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
					.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();
				}
			} else if("31".equals(state)) {
				session.createSQLQuery(
					  "INSERT mtfsm_request(messageId, systemId, toAD, addedInQueue) "
					+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
				.setParameter("messageId", data.getMessageId())
				.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
				.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();
				logger.info("added to MTFQueue for messageId=" + data.getMessageId() + "; dialog - " + data.getDialogId());
			} else if("5".equals(state) || "8".equals(state)) {
				session.save(new IncomingDLRLog(data.getMessageId(), new Timestamp(started)
						, new Timestamp(started + SmsGateWay.getSendDLRUntil()), state, data.getSendAttempts()));
			} else if("10".equals(state) || "6".equals(state)) {
				session.createSQLQuery(
					  "INSERT reportsmds_request(messageId, systemId, toAD, addedInQueue) "
					+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
				.setParameter("messageId", data.getMessageId())
				.setParameter("systemId", data.getSystemId()).setParameter("toAD", data.getToAD())
				.setParameter("addedInQueue", new Timestamp(System.currentTimeMillis())).executeUpdate();
				logger.info("added to RDSQueue for messageId=" + data.getMessageId() 
							+ " dialog - " + data.getDialogId());
			} else {
				Timestamp nextAttempt = new Timestamp(System.currentTimeMillis() + SmsGateWay.getNextAttemptToReSend());
				session.save(new NextAttemptWaitingList(data.getMessageId(), data.getFromAD(), data.getFromTON()
						, data.getFromNP(), data.getToAD(), data.getToAN(), data.getToNP(), data.getMessage()
						, data.getQuantity(), data.getDcs(), data.getPid(), data.getInserted(), data.getSendUntil()
						, data.getSystemId(), data.getDlrResponseType(), data.getPriority(), data.getSegmentLen()
						, data.getSendAttempts(), "2", state
						, data.getSendUntil().after(nextAttempt) ? nextAttempt : data.getSendUntil()));
				logger.info("added to NextAttemptWaitingList for messageId==" + data.getMessageId() 
							+ " dialog - " + data.getDialogId());
			} 
			session.getTransaction().commit();
		} catch (HibernateException e) { 
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("For Dialog - " + data.getDialogId() + " state - " + state + ". Error Message: " + e);
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > saveResponseTimeLimit)
//			logger.warn("insert is too long for dialog - " + data.getDialogId() + " state - " + state + "! SaveResponseTimeLimit = " + saveResponseTimeLimit);
	}
	
	public void saveReportSMDSResponse(SendData data, String errorCode, String errorMessage) {
		Long started = System.currentTimeMillis();
		Session session = getSession();
		try {
			session.beginTransaction();
			session.createSQLQuery(
			      "INSERT reportsmds_response(messageId, dialogId, systemId, errorCode, errorMessage, receivedResponse) "
				+ "VALUES(:messageId, :dialogId, :systemId, :errorCode, :errorMessage, :receivedResponse);")
			.setParameter("messageId", data.getMessageId()).setParameter("dialogId", data.getDialogId())
			.setParameter("systemId", data.getSystemId()).setParameter("errorCode", errorCode)
			.setParameter("errorMessage", errorMessage)
			.setParameter("receivedResponse", new Timestamp(started)).executeUpdate();
			Timestamp nextAttempt = new Timestamp(System.currentTimeMillis() + SmsGateWay.getNextAttemptForAlert());
			if(errorCode == null) {
				session.save(new AlertWaitingList(data.getMessageId(), data.getFromAD(), data.getFromTON()
						, data.getFromNP(), data.getToAD(), data.getToAN(), data.getToNP(), data.getMessage()
						, data.getQuantity(), data.getDcs(), data.getPid(), data.getInserted(), data.getSendUntil()
						, data.getSystemId(), data.getDlrResponseType(), data.getPriority(), data.getSegmentLen()
						, data.getSendAttempts(), "3"
						, SMDeliveryOutcome.absentSubscriber.equals(data.getSmDeliveryOutcome())? "6" : "10"
						, data.getSendUntil().after(nextAttempt) ? nextAttempt : data.getSendUntil()));
				logger.info("added to AlertWaitingList for messageId=" + data.getMessageId() 
							+ " dialog - " + data.getDialogId());	
			} else {
				nextAttempt = new Timestamp(System.currentTimeMillis() + SmsGateWay.getNextAttemptToReSend());
				session.save(new NextAttemptWaitingList(data.getMessageId(), data.getFromAD(), data.getFromTON()
						, data.getFromNP(), data.getToAD(), data.getToAN(), data.getToNP(), data.getMessage()
						, data.getQuantity(), data.getDcs(), data.getPid(), data.getInserted(), data.getSendUntil()
						, data.getSystemId(), data.getDlrResponseType(), data.getPriority(), data.getSegmentLen()
						, data.getSendAttempts(), "3" , errorCode
						, data.getSendUntil().after(nextAttempt) ? nextAttempt : data.getSendUntil()));
				logger.info("added to NextAttemptWaitingList for messageId=" + data.getMessageId()
				+ " dialog - " + data.getDialogId());	
			}
			session.getTransaction().commit();
		} catch (HibernateException e) { 
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("For dialogId - " + data.getDialogId() + ". Error Message: " + e);
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > saveResponseTimeLimit)
//			logger.warn("insert is too long for dialog - " + data.getDialogId() + "! SaveResponseTimeLimit = " + saveResponseTimeLimit);
	}
	
	public void saveDLRResponse(long messageId, long started, long finished, int attempts,
	String errorMessage, boolean doNextAttempt){
		Session session = getSession();
		try {
			session.beginTransaction();
			session.getNamedQuery("saveDLRResponse").setParameter("messageId", messageId)
			.setParameter("started", new Timestamp(started)).setParameter("finished", new Timestamp(finished))
			.setParameter("attempts", attempts).setParameter("errorMessage", errorMessage)
			.setParameter("doNextAttempt", doNextAttempt).executeUpdate();
			session.getTransaction().commit();
			logger.info("result saved for MessageId - " + messageId);
		} catch (HibernateException e) { 
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error("For MessageId - " + messageId + ". Error Message: " + e);
		} finally {
			session.close();
		}
	}

	public void saveErroneousRequests(long messageId, int systemId, String errorMessage, String methodtype) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.save(new ErroneousRequests(messageId, systemId, methodtype, errorMessage));
			session.getTransaction().commit();
			logger.info("save ErroneousRequests processed");
		} catch (HibernateException e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
	}
	
	public List<SendData> getSmsData(int qLimit) {
		Session session = getSession();
		List<SendData> list = new ArrayList<>();
		Timestamp now = new Timestamp(System.currentTimeMillis());
		try {
			session.beginTransaction();
			list = session.createQuery("FROM SendData ORDER BY priority desc")
					.setMaxResults(qLimit).list();
			if(!list.isEmpty()) {
				Timestamp addedTime = new Timestamp(System.currentTimeMillis());
				list.forEach(data -> {
					if(data.getSendUntil().after(now)) {
						session.createSQLQuery(
							  "INSERT srism_request(messageId, systemId, toAD, addedInQueue) "
							+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
						.setParameter("messageId", data.getMessageId())
						.setParameter("systemId", data.getSystemId())
						.setParameter("toAD", data.getToAD())
						.setParameter("addedInQueue", addedTime).executeUpdate();
						data.setExpired(false);
					} else {
						session.save(new IncomingDLRLog(data.getMessageId(), now
								, new Timestamp(now.getTime() + SmsGateWay.getSendDLRUntil()), "3", data.getSendAttempts()));
						data.setExpired(true);
					}
					session.delete(data);
					session.flush();
				});
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			list = new ArrayList<>();													// clear list
		} finally {
			session.close();
		}
		return list;
	}
	
	public synchronized List<AlertWaitingList> processAlertRequest(List<Long> messageIds) {
		Session session = getSession();
		List<AlertWaitingList> alertList = new ArrayList<>();
		long started = System.currentTimeMillis();
		try {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			session.beginTransaction();
			alertList = session.createQuery("FROM AlertWaitingList WHERE messageId IN :ids")
			.setParameterList("ids", messageIds).list();
			session.createSQLQuery("DELETE FROM alert_waiting_list WHERE messageId IN :ids")
			.setParameterList("ids", messageIds).executeUpdate();
			alertList.forEach(sms -> {
				if(sms.getSendUntil().after(now)) {
					session.createSQLQuery(
							  "INSERT srism_request(messageId, systemId, toAD, addedInQueue) "
							+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
						.setParameter("messageId", sms.getMessageId())
						.setParameter("systemId", sms.getSystemId())
						.setParameter("toAD", sms.getToAD())
						.setParameter("addedInQueue", now).executeUpdate();
					sms.setExpired(false);
				} else {
					session.save(new IncomingDLRLog(sms.getMessageId(), now
							, new Timestamp(now.getTime() + SmsGateWay.getSendDLRUntil()), "3", sms.getSendAttempts()));
					sms.setExpired(true);
				}
			});
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			alertList = new ArrayList<>();
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > requsetTimeLimit)
//			logger.warn("processing is too long! RequsetTimeLimit = " + requsetTimeLimit);
		return alertList;
	}
	
	public synchronized List<NextAttemptWaitingList> processNextAttemptRequest(List<Long> messageIds) {
		Session session = getSession();
		List<NextAttemptWaitingList> nextList = new ArrayList<>();
		long started = System.currentTimeMillis();
		try {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			session.beginTransaction();
			nextList = session.createQuery("FROM NextAttemptWaitingList WHERE messageId IN :ids")
			.setParameterList("ids", messageIds).list();
			session.createSQLQuery("DELETE FROM next_attempt_waiting_list WHERE messageId IN :ids")
			.setParameterList("ids", messageIds).executeUpdate();
			nextList.forEach(sms -> {
				if(sms.getSendUntil().after(now)) {
					session.createSQLQuery(
							  "INSERT srism_request(messageId, systemId, toAD, addedInQueue) "
							+ "VALUES(:messageId, :systemId, :toAD, :addedInQueue);")
						.setParameter("messageId", sms.getMessageId())
						.setParameter("systemId", sms.getSystemId())
						.setParameter("toAD", sms.getToAD())
						.setParameter("addedInQueue", now).executeUpdate();
					sms.setExpired(false);
				} else {
					session.save(new IncomingDLRLog(sms.getMessageId(), now
							, new Timestamp(now.getTime() + SmsGateWay.getSendDLRUntil()), "3", sms.getSendAttempts()));
					sms.setExpired(true);
				}
			});
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
			nextList = new ArrayList<>();
		} finally {
			session.close();
		}
//		if((System.currentTimeMillis() - started) > requsetTimeLimit)
//			logger.warn("processing is too long! RequsetTimeLimit = " + requsetTimeLimit);
		return nextList;
	}
	
	public int getDBQueue() {
		Session session = getSession();
		int result = 0;
		try {
			session.beginTransaction();
			result = ((BigInteger)session.createSQLQuery("select count(messageId) from send_waiting_list").uniqueResult()).intValue();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	
	public List<TempContentProvideAccessQueueSize> checkContentProviderAccessSmsQueueSize() {
		List<TempContentProvideAccessQueueSize> result = new ArrayList();
		Session session = getSession();
		try {
			result = session.createSQLQuery("SELECT systemId, COUNT(systemId) as size FROM smsqueue GROUP BY systemId")
					.setResultTransformer(Transformers.aliasToBean(TempContentProvideAccessQueueSize.class)).list();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}
	
	public void moteToArchive() {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.getNamedQuery("moveToArchive").executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
	}

	public List<GateWaySettings> getGateWaySetting() {
		Session session = getSession();
		List<GateWaySettings> settings = new ArrayList<>();
		try {
			settings = session.createQuery("FROM GateWaySettings").setCacheable(false).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return settings;
	}
	
	public List<NumberRegEx> getNumberRegEx() {
		Session session = getSession();
		List<NumberRegEx> expressions = null;
		try {
			expressions = session.createQuery("FROM NumberRegEx").setCacheable(false).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return expressions;
	}

	public List<SMPPServerConfig> getSmppServerList() {
		Session session = getSession();
		List<SMPPServerConfig> servers = new ArrayList<>();
		try {
			servers = session.createQuery("FROM SMPPServerConfig").setCacheable(false).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return servers;
	}

	public List<DataCodingList> getDataCodingList() {
		Session session = getSession();
		List<DataCodingList> result = new ArrayList<>();
		try {
			result = session.createQuery("FROM DataCodingList").setCacheable(false).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}

	
	public List<AlertList> resetAlertQueue() {
		Session session = getSession();
		List<AlertList> alertList = new ArrayList<>();
		try {
			alertList = session.createSQLQuery("SELECT messageId, toAd AS msisdn, nextAttempt "
					+ "FROM alert_waiting_list").setCacheable(false)
					.setResultTransformer(Transformers.aliasToBean(AlertList.class)).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return alertList;
	}

	public List<NextAttempt> resetNextAttemptQueue() {
		Session session = getSession();
		List<NextAttempt> nextAttemptList = new ArrayList<>();
		try {
			nextAttemptList = session.createSQLQuery("SELECT messageId, sendSMAttempts AS attempts, nextAttempt "
					+ "FROM next_attempt_waiting_list").setCacheable(false)
					.setResultTransformer(Transformers.aliasToBean(NextAttempt.class)).list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return nextAttemptList;
	}
	
	public List<DLRQueue> resetDLRQueue() {
		Session session = getSession();
		List<DLRQueue> dlrList = new ArrayList<>();
		try {
			dlrList = (List<DLRQueue>)session.createSQLQuery("SELECT	i.messageId, d.systemId, d.fromAD, d.fromTON, d.fromNP, d.toAD, d.toAN"
					+ ", d.toNP, i.state, '' as message , d.dcs, d.inserted, now() as nextAttempt, 1 as attempts"
					+ ", d.dlrResponseType, i.sendDLRUntil, i.receivedDLR "
					+ "FROM smpp_dlr_sended_log s "
					+ "RIGHT JOIN smpp_incoming_dlr_log	i on i.messageId = s.messageId "
					+ "JOIN	smsdata						d on d.messageId = i.messageId "
					+"WHERE	s.messageId is null").addEntity(DLRQueue.class)
					.list();
		} catch (Exception e) {
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return dlrList;
	}

	public boolean blackListManipulations(long number, Direction direction, boolean add) {
		Session session = getSession();
		boolean result = false;
		try {
			session.beginTransaction();
			if(add) {
				MsisdnBlackList row;
				row = (MsisdnBlackList)session.createQuery("FROM MsisdnBlackList WHERE msisdn = :msisdn")
						.setParameter("msisdn", number).uniqueResult();
				if(row == null) {
					row = new MsisdnBlackList(number, direction, new Timestamp(System.currentTimeMillis()));	
				} else {
					row.setDirection(direction);
				}
				session.save(row);
			} else {
				session.createSQLQuery("DELETE FROM msisdn_black_list where msisdn = " + number).executeUpdate();
			}
			session.getTransaction().commit();
			result = true;
		} catch (Exception e) {
			if(session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		return result;
	}

	public List<SMPPClientConfig> getSmppClientConnectionList(int clientId) {
		List<SMPPClientConfig> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM SMPPClientConfig "
					+ "WHERE enabled = :enabled AND (id = :id OR :id = 0)")
					.setParameter("id", clientId).setParameter("enabled", true).list();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		return result;
	}

	public List<SMPPClientsGroups> getSmppClientConnectionGroups() {
		List<SMPPClientsGroups> result = new ArrayList<>();
		Session session = getSession();
		try {
			result = session.createQuery("FROM SMPPClientsGroups "
					+ "WHERE enabled = 1").list();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		return result;
	}

	public void saveClientDlrWaitingList(Long remoteId, int clientId, long messageId, String errorMessage,
			Timestamp started, Timestamp finished, Timestamp sendUntil) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.getNamedQuery("saveSMPPClientSubmitLog")
			.setParameter("remoteId", remoteId).setParameter("clientId", clientId)
			.setParameter("messageId", messageId).setParameter("errorMessage", errorMessage)
			.setParameter("started", started).setParameter("finished", finished)
			.setParameter("sendUntil", sendUntil).executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}		
	}

	public void saveSMPPIncomingDLRLog(Long remoteId, String state, int clientId) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.createSQLQuery("INSERT smpp_client_incoming_dlr(remoteId, state, clientId, occurred) "
					+ "VALUES(:remoteId, :state, :clientId, now())")
			.setParameter("remoteId", remoteId).setParameter("state", state)
			.setParameter("clientId", clientId).executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}		
	}
	
	public DLRQueue processClientDlrRequest(Long remoteId, String state, int clientId) {
		Session session = getSession();
		DLRQueue result = null;
		try {
			session.beginTransaction();
			result = (DLRQueue) session.createSQLQuery("CALL spSMPPClientProcessDLRRequest(:remoteId, :state, :clientId, :occurred, :sendDLRUntil)")
					.addEntity(DLRQueue.class)
					.setParameter("remoteId", remoteId).setParameter("clientId", clientId)
					.setParameter("state", state).setParameter("occurred", new Timestamp(System.currentTimeMillis()))
					.setParameter("sendDLRUntil", new Timestamp(System.currentTimeMillis() + SmsGateWay.getSendDLRUntil()))
					.uniqueResult();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
		return result;
	}

	public boolean addSMPPServerIncomingLog(long messageId, String state, long started, int sendAttempts) {
		Session session = getSession();
		boolean result = false;
		try {
			session.beginTransaction();
			session.save(new IncomingDLRLog(messageId, new Timestamp(started)
					, new Timestamp(started + SmsGateWay.getSendDLRUntil()), state, sendAttempts));
			logger.info("added to DLRQueue for messageId=" + messageId + "; state " + state);
			session.getTransaction().commit();
			result = true;
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}		
		return result;
	}

	public List<ClientDLRWaitingList> getClientAwaitingDLRQueue() {
		Session session = getSession();
		List<ClientDLRWaitingList> result = new ArrayList<>();
		try {
			session.beginTransaction();
			result = session.createQuery("FROM ClientDLRWaitingList").list();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}		
		return result;
	}
	
	public boolean processCancelSmRequest(long messageId, int accessId) {
		Session session = getSession();
		boolean result = false;
		try {
			session.beginTransaction();
			result = (boolean)session.getNamedQuery("addCancelSMRequest")
					.setParameter("messageId", messageId)
					.setParameter("accessId", accessId).uniqueResult();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}		
		return result;
	}
	
	public void processCancelMessage(long messageId) {
		Session session = getSession();
		try {
			session.beginTransaction();
			session.getNamedQuery("CancelMessage")
					.setParameter("messageId", messageId)
					.executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null)
				session.getTransaction().rollback();
			logger.error(e.getMessage() + "; StackTrace: " + Arrays.toString(e.getStackTrace()));
		} finally {
			session.close();
		}
	}
}