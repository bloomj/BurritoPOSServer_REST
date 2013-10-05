package com.burritopos.server.rest.library;

import com.burritopos.server.domain.User;
import com.burritopos.server.service.crypto.BCrypt;
import com.burritopos.server.service.dao.IUserSvc;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * 
 */
public class BurritoServer {
    protected ObjectMapper mapper;
    protected JsonNodeFactory factory;
    private static Logger dLog = Logger.getLogger(BurritoServer.class);
    
    @Autowired
    private IUserSvc userSvc;
    private User tUser;

    // metrics
    protected final Counter failedLoginCounter = Metrics.newCounter(BurritoServer.class, "failed-login-counter");
    protected final Meter usersLoggedIn = Metrics.newMeter(BurritoServer.class, "users-logged-in", "users-logged-in", TimeUnit.SECONDS);

    public BurritoServer() throws IOException {
        mapper = new ObjectMapper();
        factory = JsonNodeFactory.instance;
    }
    
    /**
     * Mimics socket login function
     * 
     * @param parameters
     * @return
     * @throws Exception
     */
    @Deprecated
    public String doLogin(Map<String, String> parameters, String payload) throws Exception { 
    	dLog.trace("payload: " + payload);
    	
    	ObjectNode rootNode = mapper.createObjectNode();
    	ResponseBuilderImpl builder = new ResponseBuilderImpl();
    	
        if (payload == null || payload.isEmpty()) {
            rootNode.put("Error", "Request payload is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        JsonNode payloadJson = mapper.readTree(payload);
    	
    	if (payloadJson.get("Username") == null) {
            rootNode.put("Error", "Username is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
    	
    	if (payloadJson.get("Password") == null) {
            rootNode.put("Error", "Password is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
    	
    	String username = payloadJson.get("Username").asText();
    	String password = payloadJson.get("Password").asText();
    	
    	
    	tUser = new User();
    	tUser.setUserName(username);
    	
    	User curUser = userSvc.getUser(username);
    	dLog.trace("Stored user: " + curUser.getUserName() + " | stored pass: " + curUser.getPassword());
    	if(curUser.getUserName().equals(tUser.getUserName()) && BCrypt.checkpw(password, curUser.getPassword())) {
    		//set password
    		tUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));

    		rootNode.put("Success", "true");

    		// update temporary metrics
    		usersLoggedIn.mark();
    	}
        
        if(tUser.getPassword() == null) {
        	rootNode.put("Success", "false");
        	
        	// update temporary metrics
        	failedLoginCounter.inc();
        }

        return rootNode.toString();
    }
}
