package com.burritopos.server.rest.test.webresource.activiti;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.ws.rs.core.MultivaluedMap;

import com.burritopos.server.domain.User;
import com.burritopos.server.rest.test.webresource.BaseServiceCoreTest;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Runner class for the Burrito POS service to test the REST functionality for Process Definition.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class WorkflowActivitiTest extends BaseServiceCoreTest {
    // global var for test cleanup
    protected String deploymentId;
    protected String processDefinitionId;
    protected String processInstanceId;

    /**
     * Initializes the service Jersey test runner and sets the context to the applicationContext.xml.
     */
    public WorkflowActivitiTest() {
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
        
        deploymentId = "";
        processDefinitionId = "";
        processInstanceId = "";
    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @After
    public void tearDownCommonResources() throws Exception {
        // make sure processInstanceId still exists
        if (!processDefinitionId.isEmpty()) {
            processInstanceId = getProcessInstanceId(processDefinitionId, true);
            System.out.println("Got processInstanceId: " + processInstanceId);

            // ensure instances are deleted
            if (!processInstanceId.isEmpty()) {
                deleteInstance(processInstanceId);
            }
        }

        // clean up definition
        if (!deploymentId.isEmpty()) {
            deleteDefinition(deploymentId);
        }

        //cleanupTests();
        
        System.out.println("   ");
        
        super.tearDownCommonResources();
    }
    
    // helper functions for all Activiti Unit Tests
    
    /**
     * Creates Activiti deployment.
     *
     * @return deployment id
     * @throws Exception
     */
    protected String createDefinition(String mode, String bpmnName) throws Exception {
        ObjectNode rootNode = mapper.createObjectNode();

        String type = bpmnName.replace(".bpmn20.xml", "");
        
        rootNode.put("mode", mode);
        rootNode.put("adminGroup", "test");
        if (mode.equals("classpath")) {
            rootNode.put("name", type + " Test");
            rootNode.put("type", type);
            rootNode.put("bpmn", "BPMN" + File.separator + bpmnName);
        } else if (mode.equals("xml")) {
            rootNode.put("name", type + " Test");
            rootNode.put("type", type);
            rootNode.put("bpmn", createAdHocBPMNXML(bpmnName));
            rootNode.put("filename", "adhoc1.bpmn20.xml");
        }
        
        // test for id, name, deployment time
        responseJson = sendRequest("POST", "processdefinition", "", rootNode, null, 201, testAdmin);

        // test for id, name, deployment time
        assertNotNull(responseJson.get("Id"));
        assertNotNull(responseJson.get("Name"));
        assertNotNull(responseJson.get("DeploymentTime"));

        System.out.println("Created deployment: " + responseJson.get("Id").asText());
        System.out.println("   ");
        
        return responseJson.get("Id").asText();
    }
    
    /**
     * Deletes Activiti deployment definition.
     *
     * @param deploymentId
     * @throws Exception
     */
    protected void deleteDefinition(String deploymentId) throws Exception {
        // test for success
        responseJson = sendRequest("DELETE", "processdefinition/" + deploymentId, "", null, null, 204, testAdmin);

        assertNull(responseJson);
    }
    
    /**
     * Gets process definition id from the deployment id.
     *
     * @param deploymentId
     * @return
     * @throws Exception 
     */
    protected String getProcessDefinitionId(String deploymentId, String type, User user) throws Exception {
        String processDefinitionId = "";

        // test we have the newly created process definition
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        if(!type.isEmpty()) {
        	params.add("Type", type);
        }
        if(user.getUserName().equals(ADMIN_USERNAME_STR)) {
        	params.add("ShowAll", true);
        }
        responseJson = sendRequest("GET", "processdefinition", "", null, params, 200, user);

        ArrayNode definitionList = (ArrayNode) responseJson.get("ProcessDefinitionList");
        assertTrue(definitionList.size() > 0);

        Iterator<JsonNode> childNodes = definitionList.getElements();
        boolean deploymentExists = false;
        while (childNodes.hasNext()) {
            JsonNode childJson = childNodes.next();
            System.out.println("Got child node: " + childJson.toString());

            System.out.println("child id: " + childJson.get("DeploymentId").asText() + " | deploymentId: " + deploymentId);
            if (deploymentId.equals(childJson.get("DeploymentId").asText())) {
                deploymentExists = true;
                processDefinitionId = childJson.get("Id").asText();
            }
        }

        if (!deploymentId.isEmpty()) {
            assertTrue(deploymentExists);
        }

        return processDefinitionId;
    }
    
    /**
     * Creates Activiti instance based of an existing Activiti deployment.
     *
     * @param deploymentId
     * @throws Exception 
     */
    protected void createInstance(String deploymentId, ObjectNode formPropJson, int statusCode) throws Exception {
        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(deploymentId, "", testUser);
        System.out.println("Got processDefinitionId: " + processDefinitionId);

        ObjectNode rootNode = getStartFormProperties();
        rootNode.put("ProcessDefinitionId", processDefinitionId);

        // add form properties
        ArrayNode embeddedArray = new ArrayNode(factory);

        embeddedArray.add(formPropJson);
        rootNode.put("StartFormProperties", embeddedArray);

        sendRequest("POST", "processinstance", "", rootNode, null, statusCode, testUser);
    }
    
    /**
     * Deletes Activiti deployment instance.
     *
     * @param processInstanceId
     * @throws Exception 
     */
    protected void deleteInstance(String processInstanceId) throws Exception {
    	// successful delete returns NO_CONTENT 204
        responseJson = sendRequest("DELETE", "processinstance/" + processInstanceId, "", null, null, 204, testUser);

        assertNull(responseJson);
    }
    
    /**
     * Gets process instance id from process definition id.
     *
     * @param processDefinitionId
     * @return
     * @throws Exception 
     */
    protected String getProcessInstanceId(String processDefinitionId, Boolean isFormComplete) throws Exception {
        String processInstanceId = "";
        
        responseJson = sendRequest("GET", "processinstance", "", null, new MultivaluedMapImpl(), 200, testUser);

        ArrayNode definitionList = (ArrayNode) responseJson.get("ProcessInstanceList");
        if (!isFormComplete) {
            assertTrue(definitionList.size() > 0);
        }

        Iterator<JsonNode> childNodes = definitionList.getElements();
        boolean instanceExists = false;
        while (childNodes.hasNext()) {
            JsonNode childJson = childNodes.next();
            System.out.println("Got child node: " + childJson.toString());

			/*Iterator<String> names = childJson.getFieldNames();
	        while (names.hasNext()) {
	        	System.out.println("Got child node element: " + names.next());
	        	assertNotNull(childJson.get(names.next()));
	        }
	        */
            System.out.println("child id: " + childJson.get("ProcessDefinitionId").asText() + " | processDefinitionId: " + processDefinitionId);
            if (processDefinitionId.equals(childJson.get("ProcessDefinitionId").asText())) {
                instanceExists = true;
                processInstanceId = childJson.get("Id").asText();
            }
        }

        if (!processDefinitionId.isEmpty() && !isFormComplete) {
            assertTrue(instanceExists);
        }

        return processInstanceId;
    }
    
    /**
     * Gets task instance id from available tasks.
     *
     * @param processDefinitionId
     * @param type
     * @return
     * @throws Exception 
     */
    protected String getTaskId(String processDefinitionId, String executionId, String type) throws Exception {
        String taskId = "";

        // setup parameters
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("Type", type);

        // test we have newly created task instance
        responseJson = sendRequest("GET", "usertask", "", null, params, 200, testUser);

        ArrayNode definitionList = (ArrayNode) responseJson.get("TaskList");
        Iterator<JsonNode> childNodes = definitionList.getElements();
        boolean instanceExists = false;
        while (childNodes.hasNext()) {
            JsonNode childJson = childNodes.next();
            //System.out.println("Got child node: " + childJson.toString());

            ArrayNode variableList = (ArrayNode) childJson.get("VariableList");
            assertTrue(variableList.size() > 0);

            // this is the check if the process instance is started through form services
            if (childJson.findValue("ExecutionId") != null) {
                System.out.println("child id: " + childJson.get("ExecutionId").asText() + " | executionId: " + executionId);
                if (executionId.equals(childJson.get("ExecutionId").asText())) {
                    instanceExists = true;
                    taskId = childJson.get("Id").asText();
                }
            }

            Iterator<JsonNode> subchildNodes = variableList.getElements();
            while (subchildNodes.hasNext()) {
                JsonNode subchildJson = subchildNodes.next();
                //System.out.println("Got sub-child node: " + subchildJson.toString());

				/*Iterator<String> names = childJson.getFieldNames();
		        while (names.hasNext()) {
		        	System.out.println("Got child node element: " + names.next());
		        	assertNotNull(childJson.get(names.next()));
		        }
		        */

                // this is the check if the process instance is started through runtime services
                if (subchildJson.findValue("ProcessDefinitionId") != null) {
                    System.out.println("sub-child id: " + subchildJson.get("ProcessDefinitionId").asText() + " | processDefinitionId: " + processDefinitionId);
                    if (processDefinitionId.equals(subchildJson.get("ProcessDefinitionId").asText())) {
                        instanceExists = true;
                        taskId = childJson.get("Id").asText();
                    }
                }
            }
        }

        if (!processDefinitionId.isEmpty()) {
            assertTrue(instanceExists);
        }

        return taskId;
    }
    
    /**
     * Completes a business process life cycle by claiming and completing user tasks.
     *
     * @param bpmnName
     * @throws Exception
     */
    protected void taskInstancePost(String bpmnName) throws Exception {
        // setup
        deploymentId = createDefinition("xml", bpmnName);

        // get Draft ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(deploymentId, "", testUser);
        System.out.println("Got Draft processDefinitionId: " + processDefinitionId);
        System.out.println("   ");

        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put("ProcessDefinitionId", processDefinitionId);

        // add form properties
        ArrayNode embeddedArray = new ArrayNode(factory);
        ObjectNode formPropJson = getStartFormProperties();

        createInstance(deploymentId, formPropJson, 201);

        // get processInstanceId
        String processInstanceId = getProcessInstanceId(processDefinitionId, false);
        System.out.println("Got processInstanceId: " + processInstanceId);

        // get task id
        String taskId = getTaskId(processDefinitionId, processInstanceId, "Available");
        System.out.println("Got task id: " + taskId);

        // Claim task
        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Claim");

        responseJson = sendRequest("PUT", "usertask/" + taskId, "", rootNode, null, 200, testUser);

        checkTaskList((ArrayNode) responseJson.get("TaskInstanceClaimedList"), taskId, testUser.getId().toString());

        // Complete first task using Draft state
        rootNode = mapper.createObjectNode();
        rootNode.put("Action", "Complete");

        // add task properties
        embeddedArray = new ArrayNode(factory);
        formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");
        formPropJson.put("usertaskDraftenum", "EnumId1");
        embeddedArray.add(formPropJson);
        rootNode.put("TaskFormProperties", embeddedArray);

        responseJson = sendRequest("PUT", "usertask/" + taskId, "", rootNode, null, 200, testUser);

        checkTaskList((ArrayNode) responseJson.get("TaskInstanceCompletedList"), taskId, testUser.getId().toString());
    }
    
    /**
     * Checks task list return JSON
     * @param listName
     * @param taskId
     * @param userId
     */
    protected void checkTaskList(ArrayNode definitionList, String taskId, String userId) {
        assertTrue(definitionList.size() > 0);
        JsonNode childJson = definitionList.getElements().next();
        assertNotNull(childJson.get("TaskId"));
        assertNotNull(childJson.get("Assignee"));
        assertEquals(childJson.get("TaskId").asText(), taskId);
        assertEquals(userId, childJson.get("Assignee").asText());
    }
    
    /**
     * Set start form properties for test BPMN XML definitions.
     * @param action
     * @return
     */
    protected ObjectNode getStartFormProperties() {
    	ObjectNode formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");
        formPropJson.put("id", "EnumId1");
        
        return formPropJson;
    }
    
    /**
     * Creates an ad-hoc Activiti BPMN XML definition.
     * @param bpmnName
     * @param validBPMN
     * @return
     * @throws IOException 
     */
    protected String createAdHocBPMNXML(String bpmnName) throws IOException {
        String bpmnXML = "";

        System.out.println("BPMN Name: " + bpmnName);
        StringWriter writer = new StringWriter();
        InputStream stream = ClassLoader.class.getResourceAsStream("/BPMN/" + bpmnName);
        IOUtils.copy(stream, writer);
        bpmnXML = writer.toString();
        
        // sub out user/groups names with their respective IDs in the BPMN XML
        bpmnXML = bpmnXML.replace("Group1", testUserGroup.getId().toString());
        bpmnXML = bpmnXML.replace("User1", testUser.getId().toString());
        
        //System.out.println("bpmn xml: " + bpmnXML);
        
        return bpmnXML;
    }
}
