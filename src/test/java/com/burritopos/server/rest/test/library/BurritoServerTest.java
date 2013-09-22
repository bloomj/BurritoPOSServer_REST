package com.burritopos.server.rest.test.library;

import java.util.HashMap;

import com.burritopos.server.domain.User;
import com.burritopos.server.rest.library.BurritoServer;
import com.burritopos.server.rest.test.BaseTestCase;
import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.service.crypto.BCrypt;
import com.burritopos.server.service.dao.IUserSvc;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Runner class for the Burrito POS service library functionality.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class BurritoServerTest extends BaseTestCase {
	@Autowired
    private BurritoServer server;
    private ObjectMapper mapper;
    private ObjectNode rootNode;
    private JsonNode responseJson;

    // test entities
    @Autowired
    private IUserSvc userSvc;
    private User tUser;
	
    /**
     * Initializes the test case.
     */
    public BurritoServerTest() {
        super();
        
        mapper = new ObjectMapper();
    }

    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
        super.initCommonResources();
        
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
    @After
    public void tearDownCommonResources() throws Exception {
        super.tearDownCommonResources();
        
        userSvc.deleteUser(tUser.getId());
    	assertNull(userSvc.getUser(tUser.getId()).getId());
    }

    /**
     * Tests successful doLogin.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testDoLogin() throws Exception {
    	// build payload
    	rootNode = mapper.createObjectNode();
    	rootNode.put("Username", tUser.getUserName());
    	rootNode.put("Password", "password");
    	
    	String response = server.doLogin(new HashMap<String, String>(), rootNode.toString());
    	
    	responseJson = mapper.readTree(response);
    	
    	assertNotNull(responseJson.get("Success"));
    	assertEquals("true", responseJson.get("Success").asText());
    }
}
