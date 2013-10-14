package com.burritopos.server.rest.test.webresource.activiti;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import com.burritopos.server.rest.library.activiti.Definition;
import com.burritopos.server.rest.test.webresource.BaseServiceCoreTest;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

/**
 * Runner class for the Burrito POS service to test the REST functionality for Process Definition.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class WorkflowActivitiTest extends BaseServiceCoreTest {
	// TODO: Replace with REST call to delete process definition
    @Autowired
    protected Definition activitiDefinitionSvc;
	
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
        /*if (!processDefinitionId.isEmpty()) {
            processInstanceId = getProcessInstanceId(processDefinitionId, true);
            System.out.println("Got processInstanceId: " + processInstanceId);

            // ensure instances are deleted
            if (!processInstanceId.isEmpty()) {
                deleteInstance(processInstanceId);
            }
        }*/

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
     * Updates principal context
     * 
     * @param user
     * @throws Exception
     */
    protected void updatePrincipal(com.burritopos.server.domain.User user) throws Exception {
    	System.out.println("Setting Security Principal to: " + user.getUserName());
    	
        // set OAuth principal for test user
        String[] roles = new String[user.getGroupId().size()];
        for(int i=0; i<user.getGroupId().size(); i++) {
        	roles[i] = groupSvc.getGroup(user.getGroupId().get(i)).getName();
        }
        User userPrincipal = new User(user.getUserName(), user.getPassword(), true, true, true, true, AuthorityUtils.createAuthorityList(roles));
        Authentication userAuth = new UsernamePasswordAuthenticationToken(userPrincipal, user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(userAuth);
    }
    
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
    	// ensure user has admin role
        updatePrincipal(testAdmin);
        // TODO: Replace with REST call to delete process definition
        // test for success
    	activitiDefinitionSvc.deleteProcessDefinition(deploymentId);
    	
    	updatePrincipal(testUser);
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
