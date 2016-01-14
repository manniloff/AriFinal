/**
 * 
 */
package com.unifun.sigtran.beepcall.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rbabin
 *
 */
public class IsupCallPersistence implements Runnable{
	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", IsupCallPersistence.class.getSimpleName(), ""));
	private ExecutorService executorServices = null;
	private DataSource dataSource;
	private String dbCallableObj;
	/*
	in p_start_date timestamp,
	in P_end_date timestamp,
	in p_calling_party bigint, -- msisdnA
	in p_called_party bigint, -- msisdnB
	in p_reason int
	 */
	private Timestamp stratDate;
	private Timestamp endDate;
	String callingParty;
	String calledParty;
	int reason;

	public IsupCallPersistence(DataSource dataSource, String dbCallableObj, Timestamp stratDate, Timestamp endDate, String callingParty, String calledParty,int reason) {
		this.dataSource = dataSource;
		this.dbCallableObj = dbCallableObj;
		this.stratDate = stratDate;
		this.endDate = endDate;
		this.callingParty = callingParty;
		this.calledParty = calledParty;
		this.reason = reason;
	}

	@Override
	public void run() {
		logger.info(String.format("write call, calledParty: %s callingParty: %s , reason: %d", calledParty,callingParty,reason));
		if (dataSource==null){
			logger.info(String.format("No available DataSource found" ));
			return;
		}		
		Connection connection = null;
		CallableStatement cStatement = null;						
		try {			            	
			connection = dataSource.getConnection();
			/*
			in p_start_date timestamp,
			in P_end_date timestamp,
			in p_calling_party bigint, -- msisdnA
			in p_called_party bigint, -- msisdnB
			in p_reason int
			 */

			cStatement = connection.prepareCall(String.format("{call %s(?,?,?,?,?)}", dbCallableObj));
			cStatement.setTimestamp(1, stratDate);
			cStatement.setTimestamp(2, endDate);	               
			cStatement.setLong(3, Long.parseLong(callingParty));
			cStatement.setLong(4, Long.parseLong(calledParty));
			cStatement.setInt(5, reason);			
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
