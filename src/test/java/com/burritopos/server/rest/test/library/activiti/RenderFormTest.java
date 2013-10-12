package com.burritopos.server.rest.test.library.activiti;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.BuildTests;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Runner class for the workflow Activiti form rendering services.
 *
 */
public class RenderFormTest extends WorkflowActivitiTest {
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
    public RenderFormTest() throws IOException {
    	super();
    }

    /**
     * Tests Activiti render form.
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testRenderFormGet() throws Exception {
        // setup
        deploymentId = createDefinition("xml", "DailySalesReport.bpmn20.xml");

        // get ProcessDefinitionId
        processDefinitionId = getProcessDefinitionId(testUser.getId().toString(), "DailySalesReport", deploymentId, "");
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
}
