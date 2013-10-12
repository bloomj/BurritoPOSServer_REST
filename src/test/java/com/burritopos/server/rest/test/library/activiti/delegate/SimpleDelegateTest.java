package com.burritopos.server.rest.test.library.activiti.delegate;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.library.activiti.WorkflowActivitiTest;

/**
 * Runner class for the Activiti Execution Listeners.  Execution Listeners are
 * difficult to mock and test outside of the Activiti environment so this 
 * will test simple BPMN that only has the execution listener defined.
 *
 */
public class SimpleDelegateTest extends WorkflowActivitiTest {
    
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
    public SimpleDelegateTest() throws IOException {
    	super();
    }
    
    /**
     * Tests for successful completion of BPMN process that has SimpleDelegate service task defined
     *
     * @throws Exception
     */
    @Test
    @Category(BuildTests.class)
    public void testSimpleDelegateBPMN() throws Exception {
        taskInstanceAndComplete("SimpleDelegate.bpmn20.xml","SimpleDelegate");
    }
}
