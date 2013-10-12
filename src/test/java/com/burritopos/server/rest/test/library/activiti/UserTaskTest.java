package com.burritopos.server.rest.test.library.activiti;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.BuildTests;

import javax.ws.rs.WebApplicationException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Runner class for the workflow Activiti task instance functionality manipulation.
 *
 */
public class UserTaskTest extends WorkflowActivitiTest {
    
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
    public UserTaskTest() throws IOException {
    	super();
    }

    /**
     * Tests for successful task instance retrieval.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testTaskInstanceGet() throws Exception {
        getTaskId("", "", "Available", testUser.getId().toString(), testUserGroup.getId().toString());
        getTaskId("", "", "Claimed", testUser.getId().toString(), testUserGroup.getId().toString());
    }

    /**
     * Tests for error handling for invalid task instance retrieval.
     *
     * @throws Exception
     */
    @Test(expected = WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidTaskInstanceGet() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params = new HashMap<String, String>();
        params.put("Type", "INVALID_TYPE");
        String responsePayload = activitiUserTaskSvc.getTaskInstanceList(params);
        responseJson = mapper.readTree(responsePayload);
    }

    /**
     * Tests for successful completion of user task.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testTaskInstanceCreate() throws Exception {
        taskInstanceAndComplete("DailySalesReport.bpmn20.xml", "DailySalesReport");
    }

    /**
     * Tests for error handling for invalid requests for task completion. 
     *
     * @throws Exception
     * @throws WebApplicationException
     */
    @Test(expected = WebApplicationException.class)
    @Category(BuildTests.class)
    public void testInvalidTaskInstanceCreate() throws WebApplicationException, Exception {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("INVALID_PARAM", "INVALID_PARAM");
        String responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "INVALID_ACTION");
        responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Claim");
        responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Claim");
        rootNode.put("UserId", testUser.getId().toString());
        responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");
        responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");
        rootNode.put("UserId", testUser.getId().toString());
        responsePayload = activitiUserTaskSvc.updateTask("INVALID_ID", null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
    }

    /**
     * Tests for error handling for invalid or missing Activiti form properties for task completion.
     *
     * @throws Exception
     * @throws WebApplicationException
     */
    @Test(expected = WebApplicationException.class)
    @Category(BuildTests.class)
    public void testMissingFormProperties() throws WebApplicationException, Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getId().toString(), "DailySalesReport", deploymentId, "");
        System.out.println("Got processDefinitionId: " + processDefinitionId);

        // add form properties
        ObjectNode formPropJson = mapper.createObjectNode();
        formPropJson.put("draftGroup", testUserGroup.getId().toString());

        createInstance(deploymentId, formPropJson, 400);

        // retest with bad enum value
        formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");
        formPropJson.put("id", "INVALID_ENUM");

        createInstance(deploymentId, formPropJson, 400);

        // retest with task properties
        formPropJson = getStartFormProperties();

        createInstance(deploymentId, formPropJson, 201);

        // get processInstanceId
        String processInstanceId = getProcessInstanceId(processDefinitionId, false);
        System.out.println("Got processInstanceId: " + processInstanceId);

        // get task id
        String taskId = getTaskId(processDefinitionId, processInstanceId, "Available", testUser.getId().toString(), testUserGroup.getId().toString());
        System.out.println("Got task id: " + taskId);

        // Claim task
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Claim");
        rootNode.put("UserId", testUser.getId().toString());
        rootNode.put("GroupId", testUserGroup.getId().toString());

        String responsePayload = activitiUserTaskSvc.updateTask(taskId, null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        checkTaskList("TaskInstanceClaimedList", taskId, testUser.getId().toString());

        //  force an ActivitiException in complete task
        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");
        rootNode.put("UserId", "INVALID_USER");
        rootNode.put("GroupId", testUserGroup.getId().toString());
        formPropJson = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);
        embeddedArray.add(formPropJson);
        rootNode.put("TaskFormProperties", embeddedArray);

        responsePayload = activitiUserTaskSvc.updateTask(taskId, null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        // Complete task
        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");
        rootNode.put("UserId", testUser.getId().toString());
        rootNode.put("GroupId", testUserGroup.getId());
        // add task properties
        embeddedArray = new ArrayNode(factory);
        formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");

        embeddedArray.add(formPropJson);
        rootNode.put("TaskFormProperties", embeddedArray);

        //add ProcessDefinitionInstantiate properties
        ObjectNode pDef = mapper.createObjectNode();
        pDef.put("ProcessDefinitionId", processDefinitionId);

        embeddedArray = new ArrayNode(factory);
        formPropJson = getStartFormProperties();
        embeddedArray.add(formPropJson);
        pDef.put("StartFormProperties", embeddedArray);

        rootNode.put("ProcessDefinitionInstantiate", pDef);

        responsePayload = activitiUserTaskSvc.updateTask(taskId, null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        checkTaskList("TaskInstanceCompletedList", taskId, testUser.getId().toString());
    }
}
