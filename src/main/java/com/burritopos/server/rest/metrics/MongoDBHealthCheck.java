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
    private static Logger dLog = Logger.getLogger(MongoDBHealthCheck.class);
    private String version = "";

    private static String MONGO_IP;
    private static String MONGO_USER;
    private static String MONGO_PASSWORD;
    private static String MONGO_DB;
    
    /**
     * Creates HealthCheck instance for MongoDB database.
     * @throws IOException 
     */
    public MongoDBHealthCheck() throws IOException {
        super("mongodb Database");
        	
        MONGO_IP = BurritoPOSUtils.getProperty("mongo.ip");
        MONGO_USER = BurritoPOSUtils.getProperty("mongo.user");
        MONGO_PASSWORD = BurritoPOSUtils.getProperty("mongo.password");
        MONGO_DB = BurritoPOSUtils.getProperty("mongo.databasename");
    }

    @Override
    public Result check() throws Exception {
        if (checkDBConnected()) {
            return Result.healthy(" MongoDB version: " + version);
        } else {
            return Result.unhealthy("Cannot connect to Mongo Database: " + MONGO_DB);
        }
    }
    
    private Boolean checkDBConnected() throws Exception {
        Boolean result = false;

        try {
        	dLog.trace("Attempting to connect to: " + MONGO_IP);
        	MongoClient m = new MongoClient(MONGO_IP);
    		DB db = m.getDB(MONGO_DB);
    		version = m.getVersion();
    		result=true;

        } catch (Exception ex) {
        	dLog.error("Error in checkDBConnected", ex);
    		throw new Exception();
        } 
        
        return result;
    }
}