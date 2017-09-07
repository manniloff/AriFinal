/**
 *
 */
package com.unifun.sigtran.ussdgate.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
//        this.ussMessage = new UssMessage(ussMessage);
        this.dbPorcedureName = dbPorcedureName;
    }

    @Override
    public void run() {
        logger.info(String.format("Store UssMsg: %s  to db", ussMessage.toString()));
        if (ds == null) {
            logger.info(String.format("No available DataSource found"));
            return;
        }
        Connection connection = null;
        PreparedStatement statement = null;
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

            statement = connection.prepareStatement(String.format("{call %s(?,?,?,?,?,?,?,?,?,?,?,?)}", dbPorcedureName));
            statement.setInt(1, ussMessage.getDpc());
            statement.setInt(2, ussMessage.getOpc());
            if (ussMessage.getInTimeStamp() != null) {
                statement.setTimestamp(3, ussMessage.getInTimeStamp());
            } else {
                statement.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (ussMessage.getOutTimeStamp() != null) {
                statement.setTimestamp(4, ussMessage.getOutTimeStamp());
            } else {
                statement.setNull(4, java.sql.Types.TIMESTAMP);
            }
            statement.setLong(5, ussMessage.getInvokeId());
            statement.setLong(6, ussMessage.getDialogId());
            statement.setLong(7, Long.parseLong(ussMessage.getMsisdn()));
            statement.setString(8, ussMessage.getUssdText());
            statement.setString(9, ussMessage.getMessageType());
            statement.setString(10, ussMessage.getSource());
            if (ussMessage.getInitialDialogId() != -1) {
                statement.setLong(11, ussMessage.getInitialDialogId());
            } else {
                statement.setNull(11, java.sql.Types.BIGINT);
            }
            if (ussMessage.getServiceCode() != null) {
                statement.setString(12, ussMessage.getServiceCode());
            } else {
                statement.setNull(12, java.sql.Types.VARCHAR);
            }
            statement.execute();
        } catch (SQLException e) {
            logger.error("storeError:SQLException: " + e.getMessage() + " errCode=" + e.getErrorCode());
            logger.error(ussMessage.toString());
        } finally {
            try {
                if (null != statement && !statement.isClosed()) {
                    statement.close();
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
