package com.burritopos.server.rest.test.library;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.burritopos.server.rest.library.WorkflowActiviti;
import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.IntegrationTests;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Runner class for the workflow Activiti core functionality manipulation.
 *
 */
public class WorkflowActivitiTest extends BaseTest {
    @Autowired
    protected WorkflowActiviti activitiSvc;

    // global var for test cleanup
    protected String deploymentId;
    protected String processDefinitionId;
    protected String processInstanceId;
    
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
        super.tearDownCommonResources();
        
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
    }

    /**
     * Constructor
     *
     * @throws IOException
     */
    public WorkflowActivitiTest() throws IOException {

    }

    /**
     * Tests Activiti render form.
     *
     * @throws Exception
     */
    @Test
    @Category(IntegrationTests.class)
    public void testRenderFormGet() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getUserName(), "DailySalesReport", deploymentId, "");
        System.out.println("Got processDefinitionId: " + processDefinitionId);

        // get rendered form

        // setup parameters
        Map<String, String> params = new HashMap<String, String>();
        params.put("Id", processDefinitionId);
        params.put("FormType", "Start");

        activitiSvc.getRenderedForm(params);
    }

    /**
     * Tests Activiti render form with invalid parameters.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testInvalidRenderFormGet() throws Exception {
        // get invalid rendered form

        // setup parameters
        Map<String, String> params = new HashMap<String, String>();
        params.put("Id", "");
        params.put("FormType", "");

        activitiSvc.getRenderedForm(params);
    }
    
    // helper methods for common test functions

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
            rootNode.put("bpmn", "test" + File.separator + bpmnName);
        } else if (mode.equals("xml")) {
            rootNode.put("name", type + " Test");
            rootNode.put("type", type);
            rootNode.put("bpmn", createAdHocBPMNXML(bpmnName));
            rootNode.put("filename", "adhoc1.bpmn20.xml");
        }

        // parameters are not used yet so null is acceptable for now
        String responsePayload = activitiSvc.createProcessDefinition(null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

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
    	activitiSvc.deleteProcessDefinition(deploymentId);
    }

    /**
     * Creates Activiti instance based of an existing Activiti deployment.
     *
     * @param deploymentId
     * @throws Exception
     */
    protected void createInstance(String deploymentId, ObjectNode formPropJson, int statusCode) throws Exception {
        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getUserName(), "", deploymentId, "");
        System.out.println("Got processDefinitionId: " + processDefinitionId);

        ObjectNode rootNode = getStartFormProperties();
        rootNode.put("ProcessDefinitionId", processDefinitionId);

        // add form properties
        ArrayNode embeddedArray = new ArrayNode(factory);
        embeddedArray.add(formPropJson);
        rootNode.put("StartFormProperties", embeddedArray);

        String responsePayload = activitiSvc.createProcessInstance(testUser.getUserName(), rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");
    }

    /**
     * Deletes Activiti deployment instance.
     *
     * @param processInstanceId
     * @throws Exception
     */
    protected void deleteInstance(String processInstanceId) throws Exception {
        // test for success
    	activitiSvc.deleteProcessInstance(processInstanceId);
    }

    /**
     * Gets process definition id from the deployment id.
     * 
     * @param userId
     * @param definitionType
     * @param deploymentId
     * @return
     * @throws Exception
     */
    protected String getProcessDefinitionId(String userId, String definitionType, String deploymentId, String candidateGroup) throws Exception {
        String processDefinitionId = "";

        // test we have the newly created process definition
        System.out.println("Getting process definition list for user: " + userId + " | definition types: " + definitionType + " | candidate group: " + candidateGroup);
        String responsePayload = activitiSvc.getProcessDefinitionList(userId, definitionType);
        System.out.println("Response: " + responsePayload);
        responseJson = mapper.readTree(responsePayload);

        ArrayNode definitionList = (ArrayNode) responseJson.get("ProcessDefinitionList");
        assertTrue(definitionList.size() > 0);

        Iterator<JsonNode> childNodes = definitionList.getElements();
        boolean deploymentExists = false;
        while (childNodes.hasNext()) {
            JsonNode childJson = childNodes.next();
            System.out.println("Got child node: " + childJson.toString());

			/*Iterator<String> names = childJson.getFieldNames();
            while (names.hasNext()) {
	        	System.out.println("Got child node element: " + names.next());
	        	assertNotNull(childJson.get(names.next()));
	        }
	        */
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
     * Gets process instance id from process definition id.
     *
     * @param processDefinitionId
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     * @throws IOException
     */
    protected String getProcessInstanceId(String processDefinitionId, Boolean isFormComplete) throws UnsupportedEncodingException, JsonProcessingException, IOException {
        String processInstanceId = "";

        // test we have the newly created process instance
        String responsePayload = activitiSvc.getProcessInstanceList();
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

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
     * @param executionId
     * @param type
     * @param userId
     * @param groupId
     * @return
     * @throws Exception
     */
    protected String getTaskId(String processDefinitionId, String executionId, String type, String userId, String groupId) throws Exception {
        String taskId = "";

        // setup parameters
        Map<String, String> params = new HashMap<String, String>();
        params.put("UserId", userId);
        params.put("GroupId", groupId);
        params.put("Type", type);

        // test we have newly created task instance
        String responsePayload = activitiSvc.getTaskInstanceList(params);
        responseJson = mapper.readTree(responsePayload);
        System.out.println("Returned: " + responsePayload);
        System.out.println("   ");

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
    protected void taskInstancePost(String bpmnName, String action) throws Exception {
        // setup
        deploymentId = createDefinition("xml", bpmnName);

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getUserName(), "DailySalesReport", deploymentId, "");
        System.out.println("Got processDefinitionId: " + processDefinitionId);
        System.out.println("   ");

        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put("ProcessDefinitionId", processDefinitionId);
        rootNode.put("UserId", testUser.getUserName());
        rootNode.put("GroupId", testUser.getId());

        // add form properties
        ArrayNode embeddedArray = new ArrayNode(factory);
        ObjectNode formPropJson = getStartFormProperties();

        createInstance(deploymentId, formPropJson, 201);

        // get processInstanceId
        String processInstanceId = getProcessInstanceId(processDefinitionId, false);
        System.out.println("Got processInstanceId: " + processInstanceId);

        // get task id
        String taskId = getTaskId(processDefinitionId, processInstanceId, "Available", testUser.getUserName(), testGroup.getId().toString());
        System.out.println("Got task id: " + taskId);

        // Claim task
        rootNode = mapper.createObjectNode();

        rootNode.put("Action", "Claim");
        rootNode.put("UserId", testUser.getUserName());
        rootNode.put("GroupId", testUser.getId());

        String responsePayload = activitiSvc.updateTask(taskId, null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);
        
        //ArrayNode definitionList = (ArrayNode) responseJson.get("TaskInstanceClaimedList");
        checkTaskList("TaskInstanceClaimedList", taskId, testUser.getUserName());
        
        // Complete first task
        rootNode = mapper.createObjectNode();

        rootNode.put("Action", "Complete");
        rootNode.put("UserId", testUser.getUserName());
        rootNode.put("GroupId", testUser.getId());
        rootNode.put("TaskDefinitionKey", "usertaskDraft");
        // add task properties
        embeddedArray = new ArrayNode(factory);
        formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");
        formPropJson.put("usertaskDraftenum", "EnumId1");
        embeddedArray.add(formPropJson);
        rootNode.put("TaskFormProperties", embeddedArray);

        responsePayload = activitiSvc.updateTask(taskId, null, rootNode.toString());
        responseJson = mapper.readTree(responsePayload);

        //definitionList = (ArrayNode) responseJson.get("TaskInstanceCompletedList");
        checkTaskList("TaskInstanceCompletedList", taskId, testUser.getUserName());
    }
    
    /**
     * Read a resource file based on file name
     *
     * @param fileName Relative path and name of test resource file
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    protected String readByFileName(String fileName) throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource(fileName);
        File file = new File(url.toURI());
        return readFile(file);
    }

    /**
     * Read a resource File and return as string
     *
     * @param file File instance to read from
     * @return
     * @throws IOException
     */
    private String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        reader.close();

        return stringBuilder.toString();
    }

    /**
     * Creates an XML document containing the required elements for a template POST including the BPMN/Freemarker template
     *
     * @return
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    @SuppressWarnings("unused")
    protected String createTemplateXml(String type, String description, String bpmnContent) throws TransformerException, SAXException, ParserConfigurationException, IOException {
//        <?xml version="1.0" encoding="UTF-8"?>
//        <processTemplate>
//            <type>Draft</type>
//            <description>Draft template</description>
//            <bpmnxml>
//                  template
//            </bpmnxml>
//        </processTemplate>

        String bpmnXML = "";

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root element
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("processTemplate");
        doc.appendChild(rootElement);

        // create startEvent element
        Element typeElement = doc.createElement("type");
        typeElement.appendChild(doc.createTextNode(type));
        rootElement.appendChild(typeElement);

        // create description element
        Element descriptionElement = doc.createElement("description");
        descriptionElement.appendChild(doc.createTextNode(description));
        rootElement.appendChild(descriptionElement);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document templateDoc = builder.parse(new InputSource(new StringReader(bpmnContent)));

        // create bpmn xml element with imported template document
        Element bpmnXmlElement = doc.createElement("bpmnxml");
        rootElement.appendChild(bpmnXmlElement);
        NodeList nodeList = templateDoc.getDocumentElement().getChildNodes();
        Node root1 = templateDoc.getFirstChild();
        bpmnXmlElement.appendChild(doc.importNode(root1, true));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        // Output to console for testing
        transformer.transform(source, result);
        bpmnXML = writer.toString();
        System.out.println("full xml: " + bpmnXML);

        return bpmnXML;
    }
    
    /**
     * Set start form properties for test BPMN XML definitions.
     * @param action
     * @return
     */
    protected ObjectNode getStartFormProperties() {
    	ObjectNode formPropJson = mapper.createObjectNode();
        formPropJson.put("testVal", "Test Value 123");
        formPropJson.put("draftGroup", "Group1");
        formPropJson.put("rejectGroup", "Group3");
        
        return formPropJson;
    }
    
    /**
     * Checks task list return JSON
     * @param listName
     * @param id
     * @param user
     */
    protected void checkTaskList(String listName, String id, String user) {
    	ArrayNode definitionList = (ArrayNode) responseJson.get(listName);
        assertTrue(definitionList.size() > 0);
        JsonNode childJson = definitionList.getElements().next();
        assertNotNull(childJson.get("TaskId"));
        assertNotNull(childJson.get("Assignee"));
        assertEquals(childJson.get("TaskId").asText(), id);
        assertEquals(childJson.get("Assignee").asText(), user);
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
        bpmnXML = bpmnXML.replace("Group1", testGroup.getId().toString());
        bpmnXML = bpmnXML.replace("User1", testUser.getId().toString());
        
        //System.out.println("bpmn xml: " + bpmnXML);
        
        return bpmnXML;
    }
}
