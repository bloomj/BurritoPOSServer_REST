package com.burritopos.server.rest.test.library;

import java.util.HashMap;

import com.burritopos.server.rest.library.BurritoServer;
import com.burritopos.server.rest.test.BuildTests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Runner class for the Burrito POS service library functionality.
 *
 */
public class BurritoServerTest extends BaseTest {
	@Autowired
    private BurritoServer server;
	
    /**
     * Initializes the test case.
     */
    public BurritoServerTest() {
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
     * Tests successful doLogin.
     * @throws Exception 
     */
    @SuppressWarnings("deprecation")
	@Test
    @Category(BuildTests.class)
    public void testDoLogin() throws Exception {
    	// build payload
    	rootNode = mapper.createObjectNode();
    	rootNode.put("Username", testUser.getUserName());
    	rootNode.put("Password", "password");
    	
    	String response = server.doLogin(new HashMap<String, String>(), rootNode.toString());
    	
    	responseJson = mapper.readTree(response);
    	
    	assertNotNull(responseJson.get("Success"));
    	assertEquals("true", responseJson.get("Success").asText());
    }
}
