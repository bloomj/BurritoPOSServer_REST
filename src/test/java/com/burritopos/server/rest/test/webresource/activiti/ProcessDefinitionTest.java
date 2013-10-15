package com.burritopos.server.rest.test.webresource.activiti;

import java.io.File;

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
 * Runner class for the Burrito POS service to test the REST functionality for Process Definition.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ProcessDefinitionTest extends WorkflowActivitiTest {

    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     */
    public ProcessDefinitionTest() {
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
     * Tests for 201 response to a POST method to path /processdefinition and verifies the response payload.
     * This is a specific test for JSON accept and creates a new definition based on an ad-hoc Activiti BPMN XML definition.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionPostXML() throws Exception {
        // test create definition
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");
    }
    
    /**
     * Tests for 403 response to a POST method to path /processdefinition via invalid credentials.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testDeniedProcessDefinitionPost() throws Exception {
        System.out.println("Sending POST to path /processdefinition with invalid credentials");

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("bpmn", "test" + File.separator + "DailySalesReport.bpmn20.xml");
        sendRequest("POST", "processdefinition", "", rootNode, null, 403, testUser);
    }
    
    /**
     * Tests for 4XX response to a POST method to path /processdefinition via invalid acceptable media type and missing payload values.
     * @throws Exception 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testInvalidProcessDefinitionPost() throws Exception {
        System.out.println("Sending POST to path /processdefinition with invalid media type");

        Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(testAdmin.getUserName(), testAdmin.getPassword()));
        ws = c.resource(DEFAULT_URI).path("processdefinition");
        response = ws.type(MediaType.APPLICATION_XML).post(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        assertEquals(415, response.getStatus());

        sendRequest("POST", "processdefinition", "", null, null, 400, testAdmin);

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("bpmn", "BPMN" + File.separator + "DailySalesReport.bpmn20.xml");
        sendRequest("POST", "processdefinition", "mode", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        sendRequest("POST", "processdefinition", "bpmn", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        rootNode.put("bpmn", "BPMN" + File.separator + "DailySalesReport.bpmn20.xml");
        sendRequest("POST", "processdefinition", "name", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", "BPMN" + File.separator + "DailySalesReport.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        sendRequest("POST", "processdefinition", "filename", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        rootNode.put("bpmn", "DOESNOTEXIST.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        sendRequest("POST", "processdefinition", "name", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", "BADFILENAME");
        rootNode.put("filename", "BADFILENAME.bpmn.xml");
        rootNode.put("name", "TestBPMN");
        sendRequest("POST", "processdefinition", "name", rootNode, null, 400, testAdmin);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", createAdHocBPMNXML("Invalid.bpmn20.xml"));
        rootNode.put("filename", "BADXML.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        sendRequest("POST", "processdefinition", "name", rootNode, null, 500, testAdmin);
    }
    
    /**
     * Tests for 200 response to a DELETE method to path /processdefinition and verifies the response payload.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionDelete() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");
    }
    
    /**
     * Tests for 403 response to a DELETE method to path /processdefinition via invalid credentials.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testDeniedProcessDefinitionDelete() throws Exception {
        System.out.println("Sending DELETE to path /processdefinition with invalid credentials");

        sendRequest("DELETE", "processdefinition/INVALID_ID", "", null, null, 403, testUser);
    }

    /**
     * Tests for 404 response to a DELETE method to path /processdefinition via blank deployment id and invalid deployment id.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidProcessDefinitionDelete() throws Exception {
    	System.out.println("Sending DELETE to path /processdefinition with invalid deployment ID");
    	
        sendRequest("DELETE", "processdefinition/INVALID_ID", "", null, null, 404, testAdmin);
    }
    
    /**
     * Tests for 200 response to a GET method to path /processdefinition and verifies the response payload.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionGet() throws Exception {
        // create definition
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get list of process definitions
        getProcessDefinitionId(deploymentId, "", testUser);
        
        // get list of DailySalesReport process definitions
        getProcessDefinitionId(deploymentId, "DailySalesReport", testUser);
        
        // get list of all process definitions via test admin
        getProcessDefinitionId(deploymentId, "DailySalesReport", testAdmin);
    }
    
    /**
     * Tests for 4XX response to a GET method to path /processdefinition via invalid acceptable media type.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidProcessDefinitionGet() throws Exception {
        System.out.println("Sending GET to path /processdefinition with invalid media type");

        Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter(testUser.getUserName(), testUser.getPassword()));
        ws = c.resource(DEFAULT_URI).path("processdefinition");
        response = ws.type(MediaType.APPLICATION_XML).post(ClientResponse.class);

        // now get the response entity
        String responsePayload = response.getEntity(String.class);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        assertEquals(415, response.getStatus());
    }
}
