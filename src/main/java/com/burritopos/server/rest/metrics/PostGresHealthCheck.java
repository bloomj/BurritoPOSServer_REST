package com.burritopos.server.rest.metrics;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.yammer.metrics.core.HealthCheck;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * This class provides yammer.metrics style health check for PostGRES database.
 * 
 *
 */
public class PostGresHealthCheck extends HealthCheck {
    private static Logger dLog = Logger.getLogger(PostGresHealthCheck.class);
    private String version = "";

    private static String postgresURL;
    private static String postgresUser;
    private static String postgresPassword;
    
    /**
     * Creates HealthCheck instance for PostGRES database.
     * @throws IOException 
     */
    public PostGresHealthCheck() throws IOException {
        super("PostGRES Database");
        	
        postgresURL = BurritoPOSUtils.getProperty("postgres.url");
        postgresUser = BurritoPOSUtils.getProperty("postgres.user");
        postgresPassword = BurritoPOSUtils.getProperty("postgres.password");
    }

    @Override
    public Result check() throws Exception {
        if (checkDBConnected()) {
            return Result.healthy(" PostGRES version: " + version);
        } else {
            return Result.unhealthy("Cannot connect to PostGRES");
        }
    }
    
    private Boolean checkDBConnected() throws Exception {
        Boolean result = false;
    	Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {
        	dLog.trace("Attempting to connect to: " + postgresURL);
            con = DriverManager.getConnection(postgresURL, postgresUser, postgresPassword);
            st = con.createStatement();
            rs = st.executeQuery("SELECT VERSION()");

            if (rs.next()) {
            	version = rs.getString(1);
            	dLog.trace("PostGRES Version: " + version);
            	result = true;
            }

        } catch (SQLException ex) {
        	dLog.error("Error in checkDBConnected", ex);
    		throw new SQLException();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException ex) {
            	dLog.error("Error in checkDBConnected", ex);
        		throw new SQLException();
            }
        }
        
        return result;
    }
}