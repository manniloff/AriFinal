/**
 * 
 */
package com.unifun.sigtran.caplayer.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rbabin
 *
 */
public class FetchSettings {
	
	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", FetchSettings.class.getSimpleName(), ""));
	private DataSource dataSource;
	private final String settingsGroupsQuerry = "select st_type from isup_settings group by st_type";
	private final String fetchSettingsQuerry = "select name, value from isup_settings where st_type='%s'";
	
	
	/**
	 * 
	 */
	public FetchSettings(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private List<String> getSettingsGroups() throws SQLException{
		List<String> settingGropus = new ArrayList<>();
		Connection con = this.dataSource.getConnection();
	    Statement stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery(settingsGroupsQuerry);
	    while(rs.next())
        {
	    	settingGropus.add( rs.getString("st_type"));            
        }
	    rs.close();
	    stmt.close();
	    con.close();
	    return settingGropus;
	}
	
	public Map<String, Map<String, String>> fetchSettings() throws SQLException{
		//Map<String, String> preference = new HashMap<>();
		Map<String, Map<String, String>> preference = new HashMap<>();		
		Connection con = this.dataSource.getConnection();
	    Statement stmt = con.createStatement();
	    List<String> settingGropus = null;
	    try {
			settingGropus = getSettingsGroups();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	    if (settingGropus == null){
	    	logger.error("Unable to fetch the type of preference from DB");
	    	return null;
	    }
	    settingGropus.forEach((type)->{
	    	Map<String, String> params = new HashMap<>();
	    	ResultSet rs;
			try {
				rs = stmt.executeQuery(String.format(fetchSettingsQuerry, type));			
		    while(rs.next())
	        {		    	
		    	params.put(rs.getString("name"), rs.getString("value"));
	        }
		    rs.close();
		    preference.put(type,params);
			} catch (Exception e) {
				logger.error("Unable to fetch the preference from DB");
				logger.error(e.getMessage());
				e.printStackTrace();
			}
	    });
	    
	    
	    stmt.close();
	    con.close();
		return preference;		
	}

}
