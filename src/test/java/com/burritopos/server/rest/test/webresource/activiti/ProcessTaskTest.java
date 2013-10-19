package com.burritopos.server.rest.test.webresource.activiti;

import static org.junit.Assert.assertEquals;

import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.IntegrationTests;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


/**
 * Runner class for the Burrito POS service to test the REST functionality for Process Instance.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ProcessTaskTest extends WorkflowActivitiTest {

    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     */
    public ProcessTaskTest() {
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
     * Tests for 200 response to a GET method to path /usertask and verifies the response payload.
     * @throws Exception 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testTaskInstanceGet() throws Exception {
        getTaskId("", "", "Available");
        getTaskId("", "", "Claimed");
    }

    /**
     * Tests for 4XX response to a GET method to path /usertask via invalid acceptable media type and missing payload values.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidTaskInstanceGet() throws Exception {
    	Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(testUser.getUserName(), testUser.getPassword()));
        ws = c.resource(DEFAULT_URI).path("usertask");
        response = ws.type(MediaType.APPLICATION_XML).get(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        sendRequest("GET", "usertask", "", null, new MultivaluedMapImpl(), 400, testUser);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("Type", "INVALID_TYPE");
        sendRequest("GET", "usertask", "Type", null, params, 400, testUser);
    }

    /**
     * Tests for 200 response to a PUT method to path /usertask using a BPMN XML definition without form extensions and verifies the response payload
     * @throws Exception
     */
    @Test
    @Category(IntegrationTests.class)
    public void testTaskInstancePut() throws Exception {
        taskInstancePost("DailySalesReport.bpmn20.xml");
    }

    /**
     * Tests for 4XX response to a POST method to path /usertask via invalid acceptable media type, missing payload values, and invalid task id.
     * @throws Exception 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testInvalidTaskInstancePut() throws Exception {
    	Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(testUser.getUserName(), testUser.getPassword()));
        ws = c.resource(DEFAULT_URI).path("usertask/INVALID_ID");
        response = ws.type(MediaType.APPLICATION_XML).put(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        assertEquals(415, response.getStatus());

        sendRequest("PUT", "usertask/INVALID_ID", "", null, null, 400, testUser);

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("INVALID_PARAM", "INVALID_PARAM");
        sendRequest("PUT", "usertask/INVALID_ID", "Action", rootNode, null, 400, testUser);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "INVALID_ACTION");
        sendRequest("PUT", "usertask/INVALID_ID", "Action", rootNode, null, 400, testUser);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Claim");
        sendRequest("PUT", "usertask/INVALID_ID", "UserId", rootNode, null, 404, testUser);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");
        sendRequest("PUT", "usertask/INVALID_ID", "UserId", rootNode, null, 404, testUser);
    }
}
