package com.burritopos.server.rest.test.webresource;

import java.util.Random;

import com.burritopos.server.domain.Group;
import com.burritopos.server.domain.User;
import com.burritopos.server.service.crypto.BCrypt;
import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;
import com.burritopos.server.service.dao.mongo.GroupSvcImpl;
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
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
	
	// Spring configuration
    private static final String SPRING_CONFIG_DEFAULT = "applicationContext.xml";
	
    public static final String PACKAGE_NAME = "com.burritopos.server.rest";
    protected ObjectNode rootNode;
    protected static final Random rand = new Random();
    
    // test entities
    protected static IUserSvc userSvc;
    protected static User testUser;
    protected static User testAdmin;
    protected static IGroupSvc groupSvc;
    protected static Group testUserGroup;
    protected static Group testAdminGroup;
    
    // Static strings
    protected static final String USER_USERNAME_STR = "Test_User";
    protected static final String USER_PASSWORD_STR = BCrypt.hashpw("password", BCrypt.gensalt());
    protected static final String ADMIN_USERNAME_STR = "Test_Admin";
    protected static final String ADMIN_PASSWORD_STR = USER_PASSWORD_STR;
    protected static final String USER_ROLE_STR = "ROLE_USER";
    protected static final String ADMIN_ROLE_STR = "ROLE_ADMIN";
    
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

        //Spring Framework IoC
        ClassPathXmlApplicationContext beanfactory = null;
        try {
            beanfactory = new ClassPathXmlApplicationContext(SPRING_CONFIG_DEFAULT);
            userSvc = (UserSvcImpl)beanfactory.getBean("userSvc");
            groupSvc = (GroupSvcImpl)beanfactory.getBean("groupSvc");

        } catch (Exception e) {
        	System.out.println("Unable to set Spring bean");
        	e.printStackTrace();
        } finally {
            if (beanfactory != null) {
                beanfactory.close();
            }
        }
        
        testUserGroup = new Group();
        testUserGroup.setId(rand.nextInt());
        testUserGroup.setName(USER_ROLE_STR);
        
        groupSvc.storeGroup(testUserGroup);
        assertNotNull(groupSvc.getGroup(testUserGroup.getId()));
        
        testAdminGroup = new Group();
        testAdminGroup.setId(rand.nextInt());
        testAdminGroup.setName(ADMIN_ROLE_STR);
        
        groupSvc.storeGroup(testAdminGroup);
        assertNotNull(groupSvc.getGroup(testAdminGroup.getId()));
        
        testUser = new User();
        testUser.setId(rand.nextInt());
        testUser.addGroupId(testUserGroup.getId());
        testUser.setUserName(USER_USERNAME_STR);
        testUser.setPassword(USER_PASSWORD_STR);
        
        userSvc.storeUser(testUser);
        assertNotNull(userSvc.getUser(testUser.getId()));
        
        testAdmin = new User();
        testAdmin.setId(rand.nextInt());
        testAdmin.addGroupId(testUserGroup.getId());
        testAdmin.addGroupId(testAdminGroup.getId());
        testAdmin.setUserName(ADMIN_USERNAME_STR);
        testAdmin.setPassword(USER_PASSWORD_STR);
        
        userSvc.storeUser(testAdmin);
        assertNotNull(userSvc.getUser(testAdmin.getId()));
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
            groupSvc.deleteGroup(testUserGroup.getId());
            assertNull(groupSvc.getGroup(testUserGroup.getId()).getName());
            
            groupSvc.deleteGroup(testAdminGroup.getId());
            assertNull(groupSvc.getGroup(testAdminGroup.getId()).getName());
            
            userSvc.deleteUser(testUser.getId());
        	assertNull(userSvc.getUser(testUser.getId()).getUserName());
        	
            userSvc.deleteUser(testAdmin.getId());
        	assertNull(userSvc.getUser(testAdmin.getId()).getUserName());
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
    protected JsonNode sendRequest(String method, String path, String parameter, ObjectNode rootNode, MultivaluedMap<String, String> params, int statusCode, User user) throws Exception {
        //path = DEFAULT_URI + path;
    	System.out.println("URI: " + DEFAULT_URI);
    	System.out.println("path: " + path);
        Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(user.getUserName(), user.getPassword()));
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
