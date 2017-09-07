package com.unifun.sigtran.beepcall.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsupEventPersistence implements Runnable{
	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", IsupEventPersistence.class.getSimpleName(), ""));	
	private DataSource dataSource;
	String dbCallableObj;

	//insert params
	int sessionId;
	int cic;
	int dpc;
	String callingParty;
	String calledParty;
	Timestamp timestamp;
	String message_type;
	int statusIndicator;
	int causeIndicator;

	public IsupEventPersistence(DataSource dataSource, String dbCallableObj ,int sessionId, int cic, int dpc, String callingParty, String calledParty,
			Timestamp timestamp, String message_type, int statusIndicator, int causeIndicator) {
		this.dataSource = dataSource;
		this.dbCallableObj = dbCallableObj;
		this.sessionId = sessionId;
		this.cic = cic;
		this.dpc = dpc;
		this.callingParty = callingParty;
		this.calledParty = calledParty;
		this.timestamp = timestamp;
		this.message_type = message_type;
		this.statusIndicator = statusIndicator;
		this.causeIndicator = causeIndicator;
	}

	@Override
	public void run() {
		logger.info(String.format("write sessionId: %d cic: %d , dpc: %d, msg_type:%s", sessionId,cic,dpc,message_type));
		if (dataSource==null){
			logger.info(String.format("No available DataSource found" ));
			return;
		}		
		Connection connection = null;
		CallableStatement cStatement = null;						
		try {			            	
			connection = dataSource.getConnection();
			/*
            	in p_session_id int,
            	in p_cic int,
            	in p_dpc int,
            	in p_calling_party bigint, -- msisdnA
            	in p_called_party bigint, -- msisdnB
            	in p_timestamp timestamp,
            	in p_message_type varchar(5),
            	in p_statu_indicator tinyint,
            	in p_cause_indicator tinyint
			 */

			cStatement = connection.prepareCall(String.format("{call %s(?,?,?,?,?,?,?,?,?)}", dbCallableObj));
			cStatement.setInt(1, sessionId);
			cStatement.setInt(2, cic);
			cStatement.setInt(3, dpc);	               
			if (callingParty == null)
				cStatement.setNull(4, java.sql.Types.BIGINT);
			else
				cStatement.setLong(4, Long.parseLong(callingParty));
			if (calledParty == null)
				cStatement.setNull(5, java.sql.Types.BIGINT);
			else
				cStatement.setLong(5, Long.parseLong(calledParty));
			cStatement.setTimestamp(6, timestamp);
			cStatement.setString(7, message_type);
			if (statusIndicator == -1)
				cStatement.setNull(8, java.sql.Types.TINYINT);
			else
				cStatement.setInt(8, statusIndicator);
			if (causeIndicator == -1)
				cStatement.setNull(9, java.sql.Types.TINYINT);
			else
				cStatement.setInt(9, causeIndicator);
			cStatement.execute();
		} catch (SQLException e) {
			logger.error("storeError:SQLException: " + e.getMessage() + " errCode=" + e.getErrorCode());
		} finally {
			try {
				if (null != cStatement && !cStatement.isClosed()) {
					cStatement.close();
				}
				if (null != connection && connection.isValid(1)) {
					//connection.commit();
					connection.close();
				}
			} catch (SQLException ex) {
				logger.error("Error closing statement or connection. " + ex.getMessage());
			}	

		}
	}

}
