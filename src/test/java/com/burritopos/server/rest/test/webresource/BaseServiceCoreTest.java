package com.burritopos.server.rest.test.webresource;

import com.burritopos.server.domain.User;
import com.burritopos.server.service.crypto.BCrypt;
import com.burritopos.server.service.dao.IUserSvc;
import com.burritopos.server.service.dao.mongo.UserSvcImpl;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

import com.sun.jersey.test.framework.WebAppDescriptor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

/**
 * Runner class for the BurritoPOS service to test the REST functionality.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class BaseServiceCoreTest extends AbstractSpringAwareJerseyTest {
	@Rule
	public TestWatcher watcher = new TestWatcher() {
	  protected void starting(Description description) {
	    System.out.println(String.format("*** Starting test: %s() ***", description.getMethodName()));
	  };
	};
	
    public static final String PACKAGE_NAME = "com.burritopos.server.rest";
    protected ObjectNode rootNode;
    
    // test entities
    protected static IUserSvc userSvc;
    protected static User tUser;
    
    
    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @BeforeClass
    public static void setUpClass() throws Exception {  
        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Setting up test class ***");
        
        userSvc = new UserSvcImpl();
        
        tUser = new User();
        tUser.setId(123);
        tUser.setUserName("Test_User");
        tUser.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        
        userSvc.storeUser(tUser);
        assertNotNull(userSvc.getUser(tUser.getId()));
    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @AfterClass
    public static void tearDownClass() { 
        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Tearing down test class ***");

        try {
        	userSvc.deleteUser(tUser.getId());
        	assertNull(userSvc.getUser(tUser.getId()).getId());
        }
        catch(Exception e) {
        	fail(e.getMessage());
        }
        
        System.out.println("   ");
    }
    
    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     * 
     */
    public BaseServiceCoreTest() {
        super(new WebAppDescriptor.Builder(PACKAGE_NAME)
                .contextPath("")
                .contextParam("contextConfigLocation", "classpath:applicationContext.xml")
                .servletClass(SpringServlet.class)
                .contextListenerClass(ContextLoaderListener.class)
                .requestListenerClass(RequestContextListener.class)
                .addFilter(org.springframework.web.filter.DelegatingFilterProxy.class, "springSecurityFilterChain")
                .addFilter(com.burritopos.server.rest.filter.RequestFilter.class, "RESTInitFilter")
                .build());
    }
    
    // helper methods for common test functions

    /**
     * Sends single GET/POST/DELETE request
     *
     * @param path      REST endpoint path
     * @param parameter Invalid parameter name to check
     * @param rootNode  ObjectNode to pass as entity
     * @param params    REST parameters
     * @throws Exception 
     */
    protected JsonNode sendRequest(String method, String path, String parameter, ObjectNode rootNode, MultivaluedMap<String, String> params, int statusCode) throws Exception {
        //path = DEFAULT_URI + path;
    	System.out.println("URI: " + DEFAULT_URI);
    	System.out.println("path: " + path);
        Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(tUser.getUserName(), tUser.getPassword()));
        ws = c.resource(DEFAULT_URI).path(path);
        
    	if (method.equals("DELETE")) {
            System.out.println("Sending DELETE to path " + path);
            response = ws.type(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
        } else if (rootNode == null) {
            System.out.println("Sending " + method + " to path " + path);

            if (method.equals("POST")) {
            	response = ws.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
            }
            else if (method.equals("GET")) {
            	ws = c.resource(DEFAULT_URI).path(path).queryParams(params);
                response = ws.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            } else {
                fail("Invalid method");
            }
        } else {
            System.out.println("Sending " + method + "  to path " + path);
            System.out.println("Sending: " + rootNode.toString());

            if (method.equals("POST")) {
            	response = ws.type(MediaType.APPLICATION_JSON).entity(rootNode.toString()).post(ClientResponse.class);
            }
            else {
                fail("Invalid method");
            }
        }

    	// now get the response entity
    	String responsePayload = response.getEntity(String.class);

    	if(responsePayload != null) {
    		try {
	    		responseJson = mapper.readTree(responsePayload);
	    		System.out.println("Returned: " + response.getStatus() + " " + responsePayload);
	    		System.out.println("   ");
	
	    		assertNotNull(responsePayload);
	    		assertTrue(!responsePayload.isEmpty());
    		}
    		catch(JsonParseException e) {
    			responseJson = null;
        		System.out.println("Returned: " + response.getStatus() + " " + Response.Status.fromStatusCode(response.getStatus()).name());
        		System.out.println("   ");
    		}
    	}
    	else {
    		responseJson = null;
    		System.out.println("Returned: " + response.getStatus() + " " + Response.Status.fromStatusCode(response.getStatus()).name());
    		System.out.println("   ");
    	}

        assertEquals(statusCode, response.getStatus());
        
        if (rootNode != null && statusCode == 400 && responseJson.findValue("Missing Payload Value") != null) {
            assertNotNull(responseJson.get("Missing Payload Value"));
            assertEquals(responseJson.get("Missing Payload Value").asText(), parameter);
        }

        return responseJson;
    }
}
