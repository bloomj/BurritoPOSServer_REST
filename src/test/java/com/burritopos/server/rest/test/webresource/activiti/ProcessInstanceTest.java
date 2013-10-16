package com.burritopos.server.rest.test.webresource.activiti;

import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.IntegrationTests;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;


/**
 * Runner class for the Burrito POS service to test the REST functionality for Process Instance.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ProcessInstanceTest extends WorkflowActivitiTest {

    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     */
    public ProcessInstanceTest() {
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
     * Tests for 201 response to a POST method to path /processinstance and verifies the response payload.
     * @throws Exception 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testProcessInstancePost() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(deploymentId, "", testUser);
        System.out.println("Got processDefinitionId: " + processDefinitionId);
        
        ObjectNode formPropJson = getStartFormProperties();
        		
        createInstance(deploymentId, formPropJson, 201);
    }

    /**
     * Tests for 4XX response to a POST method to path /processinstance via invalid acceptable media type, missing payload values, and invalid process definition id.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidProcessInstancePost() throws Exception {
    	Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(testUser.getUserName(), testUser.getPassword()));
        ws = c.resource(DEFAULT_URI).path("processinstance");
        response = ws.type(MediaType.APPLICATION_XML).post(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        assertEquals(415, response.getStatus());

        sendRequest("POST", "processinstance", "", null, null, 400, testUser);

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("INVALID_PARAM", "INVALID_PARAM");
        sendRequest("POST", "processinstance", "ProcessDefinitionId", rootNode, null, 400, testUser);

        rootNode = mapper.createObjectNode();
        rootNode.put("ProcessDefinitionId", "INVALID_PARAM");
        sendRequest("POST", "processinstance", "ProcessDefinitionId", rootNode, null, 400, testUser);
    }
}
