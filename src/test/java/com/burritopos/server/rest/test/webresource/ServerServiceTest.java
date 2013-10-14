package com.burritopos.server.rest.test.webresource;

import com.burritopos.server.rest.test.BuildTests;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Runner class for the Burrito POS service to test the REST functionality.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ServerServiceTest extends BaseServiceCoreTest {

    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     */
    public ServerServiceTest() {
        super();
    }

    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
        super.initCommonResources();
    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @After
    public void tearDownCommonResources() throws Exception {
        super.tearDownCommonResources();
    }

    /**
     * Tests for 410 response to a POST method to path /login and verifies the response payload.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testLoginPOST() throws Exception {
    	// build payload
    	rootNode = mapper.createObjectNode();
    	rootNode.put("Username", testUser.getUserName());
    	rootNode.put("Password", testUser.getPassword());
    	
    	responseJson = sendRequest("POST", "login", "", rootNode, new MultivaluedMapImpl(), 410, testUser);
    	
    	assertNotNull(responseJson.get("Status"));
    	assertEquals("Login has been deprecated in favor of OAuth", responseJson.get("Status").asText());
    }

    /**
     * Tests for 401 response to a POST method to path /login without Basic Authentication.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidLoginPOST() throws Exception {
    	String path = DEFAULT_URI + "/login";
        ws = resource().path(path);

        System.out.println("Sending GET to path " + path + " without Basic Authentication for Oauth");

        ClientResponse response = ws.accept(MediaType.APPLICATION_XML).post(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + response.getStatus() + " " + responsePayload);
        System.out.println("   ");

        assertEquals(401, response.getStatus());
        
        /*String path = DEFAULT_URI + "login";
        ws = resource().path(path);

        System.out.println("Sending GET to path " + path + " with invalid media type");

        ClientResponse response = ws.accept(MediaType.APPLICATION_XML).post(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + response.getStatus() + " " + responsePayload);
        System.out.println("   ");

        assertEquals(406, response.getStatus());
        
    	responseJson = sendRequest("POST", "login", "", null, new MultivaluedMapImpl(), 400);
    	
    	assertNotNull(responseJson.get("Error"));
    	assertEquals("Payload is required", responseJson.get("Error").asText());
    	
    	// build payload
    	rootNode = mapper.createObjectNode();
    	rootNode.put("Password", "XXX");
    	
    	responseJson = sendRequest("POST", "login", "", rootNode, new MultivaluedMapImpl(), 400);
    	
    	assertNotNull(responseJson.get("Error"));
    	assertEquals("Username is required", responseJson.get("Error").asText());
    	
    	// build payload
    	rootNode = mapper.createObjectNode();
    	rootNode.put("Username", "XXX");
    	
    	responseJson = sendRequest("POST", "login", "", rootNode, new MultivaluedMapImpl(), 400);
    	
    	assertNotNull(responseJson.get("Error"));
    	assertEquals("Password is required", responseJson.get("Error").asText());*/
    }
}
