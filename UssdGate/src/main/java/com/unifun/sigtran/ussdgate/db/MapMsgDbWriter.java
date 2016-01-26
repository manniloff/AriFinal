/**
 * 
 */
package com.unifun.sigtran.ussdgate.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unifun.sigtran.ussdgate.UssMessage;


/**
 * @author rbabin
 *
 */
public class MapMsgDbWriter implements Runnable {

	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", MapMsgDbWriter.class.getSimpleName(), ""));
	private DataSource ds;
	private UssMessage ussMessage;
	String dbPorcedureName;


	public MapMsgDbWriter(DataSource ds, UssMessage ussMessage, String dbPorcedureName) {
		this.ds = ds;
		this.ussMessage = new UssMessage(ussMessage);
		this.dbPorcedureName = dbPorcedureName;
	}

	@Override
	public void run() {
		logger.info(String.format("Store UssMsg: %s  to db", ussMessage.toString()));
		if (ds==null){
			logger.info(String.format("No available DataSource found" ));
			return;
		}		
		Connection connection = null;
		CallableStatement cStatement = null;						
		try {			            	
			connection = ds.getConnection();
			/*
			 * 			
			in p_dpc int,
			in p_spc int,
			in p_in_tstamp timestamp,
			in p_out_tstamp timestamp,
			in p_invoke_id int,
			in p_dialog_id bigint(20),
			in p_msisdn bigint,
			in p_ussd_text varchar(255),
			in p_message_type varchar(255),
			in p_source varchar(15),
			in p_initial_dialog_id bigint(20)
			 */

			cStatement = connection.prepareCall(String.format("{call %s(?,?,?,?,?,?,?,?,?,?,?,?)}", dbPorcedureName));
			cStatement.setInt(1, ussMessage.getDpc());
			cStatement.setInt(2, ussMessage.getOpc());
			if (ussMessage.getInTimeStamp()!=null)
				cStatement.setTimestamp(3, ussMessage.getInTimeStamp());
			else
				cStatement.setNull(3, java.sql.Types.TIMESTAMP);
			if (ussMessage.getOutTimeStamp()!=null)
				cStatement.setTimestamp(4, ussMessage.getOutTimeStamp());
			else
				cStatement.setNull(4, java.sql.Types.TIMESTAMP);
			cStatement.setLong(5, ussMessage.getInvokeId());
			cStatement.setLong(6, ussMessage.getDialogId());
			cStatement.setLong(7, Long.parseLong(ussMessage.getMsisdn()));
			cStatement.setString(8, ussMessage.getUssdText());
			cStatement.setString(9, ussMessage.getMessageType());			
			cStatement.setString(10, ussMessage.getSource());
			if (ussMessage.getInitialDialogId()!=-1)
				cStatement.setLong(11, ussMessage.getInitialDialogId());
			else
				cStatement.setNull(11, java.sql.Types.BIGINT);
			if(ussMessage.getServiceCode()!=null)
				cStatement.setString(12, ussMessage.getServiceCode());
			else
				cStatement.setNull(12, java.sql.Types.VARCHAR);
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
