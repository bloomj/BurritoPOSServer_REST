package com.burritopos.server.rest.test.library.activiti;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.IntegrationTests;

import javax.ws.rs.WebApplicationException;
import java.io.*;

/**
 * Runner class for the workflow Activiti process definition functionality manipulation.
 *
 */
public class DefinitionTest extends WorkflowActivitiTest {
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
    public DefinitionTest() throws IOException {
    	super();
    }

    /**
     * Tests for get from Definition library and verifies the response payload.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionGet() throws Exception {
        // create definition
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        System.out.println("Getting process defintions for: " + testUser.getUserName());
        // get list of process definitions
        getProcessDefinitionId(testUser.getId().toString(), "DailySalesReport", deploymentId, "");
        
        System.out.println("Getting process defintions for candidateGroup: " + testUser.getUserName());
        // get list of process definitions by candidate Group
        getProcessDefinitionId(testUser.getId().toString(), "DailySalesReport", deploymentId, testUserGroup.getName());
    }

    /**
     * Tests for handling from Definition library for invalid acceptable media type.
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidProcessDefinitionGet() throws Exception {
        System.out.println("Getting process definition list with invalid parameters");

        // now get the response entity
        String responsePayload = activitiDefinitionSvc.getProcessDefinitionList("XXX", false);
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

    }

    /**
     * Tests for creation from ad-hoc xml from Definition library and verifies the response payload.
     * This is a specific test for JSON accept and creates a new definition based on an ad-hoc Activiti BPMN XML definition.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionPostXML() throws Exception {
        // test create definition
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");
    }

    /**
     * Tests for creation from classpath from Definition library and verifies the response payload.
     * This is a specific test for JSON accept and creates a new definition based on a file already in the classpath.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testProcessDefinitionPostClasspath() throws Exception {
        // test create definition
        deploymentId = createDefinition("classpath", "DailySalesReport.bpmn20.xml");
    }

    /**
     * Tests for error handling from Definition library via invalid acceptable media type and missing payload values.
     *
     * @throws Exception
     */
    @Test(expected = WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidProcessDefinitionCreate() throws Exception {
    	System.out.println("Testing blank rootNode");
        ObjectNode rootNode = mapper.createObjectNode();
        String responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        System.out.println("Testing only sending mode");
        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        System.out.println("Testing only sending mode, bpmn");
        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        rootNode.put("bpmn", "test" + File.separator + "DailySalesReport.bpmn20.xml");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", "test" + File.separator + "DailySalesReport.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "classpath");
        rootNode.put("bpmn", "DOESNOTEXIST.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", "BADFILENAME");
        rootNode.put("filename", "BADFILENAME.bpmn.xml");
        rootNode.put("name", "TestBPMN");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("mode", "xml");
        rootNode.put("bpmn", createAdHocBPMNXML("Invalid.bpmn20.xml"));
        rootNode.put("filename", "BADXML.bpmn20.xml");
        rootNode.put("name", "TestBPMN");
        responsePayload = activitiDefinitionSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
    }

    /**
     * Tests for deletion from Definition library verifies the response payload.
     *
     * @throws Exception
     */
    @Test
    @Category(IntegrationTests.class)
    public void testProcessDefinitionDelete() throws Exception {
        // setup
        deploymentId = createDefinition("classpath", "DailySalesReport.bpmn20.xml");
    }

    /**
     * Tests for WebApplicationException from Definition library via blank deployment id and invalid deployment id.
     *
     * @throws Exception
     */
    @Test(expected = WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidProcessDefinitionDelete() throws Exception {
    	activitiDefinitionSvc.deleteProcessDefinition("INVALID_ID");
    }
}
