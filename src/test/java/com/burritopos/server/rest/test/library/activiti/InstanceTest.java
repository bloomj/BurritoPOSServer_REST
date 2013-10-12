package com.burritopos.server.rest.test.library.activiti;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.BuildTests;

import javax.ws.rs.WebApplicationException;

/**
 * Runner class for the workflow Activiti process instance functionality manipulation.
 *
 */
public class InstanceTest extends WorkflowActivitiTest {
    
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
     * Constructor
     *
     * @throws IOException
     */
    public InstanceTest() throws IOException {
    	super();
    }

    /**
     * Tests for successful process instance retrieval.
     *
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     * @throws IOException
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessInstanceGet() throws UnsupportedEncodingException, JsonProcessingException, IOException {
        getProcessInstanceId("", true);
    }

    /**
     * Tests for successful process instance creation.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessInstanceCreate() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getId().toString(), "DailySalesReport", deploymentId, "");
        System.out.println("Got processDefinitionId: " + processDefinitionId);
        
        ObjectNode formPropJson = getStartFormProperties();

        createInstance(deploymentId, formPropJson, 201);
    }

    /**
     * Tests for error handling for invalid process instance creation.
     * @throws Exception 
     */
    @Test(expected=WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidProcessInstanceCreate() throws Exception {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("INVALID_PARAM", "INVALID_PARAM");
        String responsePayload = activitiInstanceSvc.createProcessInstance(rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

        rootNode = mapper.createObjectNode();
        rootNode.put("ProcessDefinitionId", "INVALID_PARAM");
        responsePayload = activitiInstanceSvc.createProcessInstance(rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");
    }

    /**
     * Tests for successful process instance deletion.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessInstanceDelete() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        ObjectNode formPropJson = getStartFormProperties();

        createInstance(deploymentId, formPropJson, 201);
    }

    /**
     * Tests for error handling for invalid process instance deletion.
     * @throws Exception 
     */
    @Test(expected=WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidProcessInstanceDelete() throws Exception {
    	activitiInstanceSvc.deleteProcessInstance("INVALID_ID");
    }
}
