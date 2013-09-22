package com.burritopos.server.rest.metrics;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.yammer.metrics.core.HealthCheck;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * This class provides yammer.metrics style health check for MongoDB database.
 * 
 *
 */
@SuppressWarnings("unused")
public class MongoDBHealthCheck extends HealthCheck {
    private static Logger logger = Logger.getLogger(MongoDBHealthCheck.class);
    private String version = "";

    private static String mongodbURL;
    private static String mongodbUser;
    private static String mongodbPassword;
    
    /**
     * Creates HealthCheck instance for MongoDB database.
     * @throws IOException 
     */
    public MongoDBHealthCheck() throws IOException {
        super("mongodb Database");
        	
        mongodbURL = BurritoPOSUtils.getProperty("mongo.url");
        mongodbUser = BurritoPOSUtils.getProperty("mongo.user");
        mongodbPassword = BurritoPOSUtils.getProperty("mongo.password");
    }

    @Override
    public Result check() throws Exception {
        if (checkDBConnected()) {
            return Result.healthy(" MongoDB version: " + version);
        } else {
            return Result.unhealthy("Cannot connect to Mongo Database: neatoBurrito");
        }
    }
    
    private Boolean checkDBConnected() throws Exception {
        Boolean result = false;
    	Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {
        	logger.trace("Attempting to connect to: " + mongodbURL);
        	MongoClient m = new MongoClient(mongodbURL);
    		DB db = m.getDB("neatoBurrito");
    		version = m.getVersion();
    		result=true;

        } catch (Exception ex) {
        	logger.error("Error in checkDBConnected: " + ex);
    		throw new Exception();
        } finally {
        }
        
        return result;
    }
}