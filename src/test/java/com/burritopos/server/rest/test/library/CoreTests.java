package com.burritopos.server.rest.test.library;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.burritopos.server.rest.test.library.activiti.DefinitionTest;
import com.burritopos.server.rest.test.library.activiti.InstanceTest;
import com.burritopos.server.rest.test.library.activiti.UserTaskTest;
import com.burritopos.server.rest.test.library.activiti.WorkflowActivitiTest;

@RunWith(Suite.class)
@SuiteClasses({ 
	BurritoServerTest.class,
	DefinitionTest.class,
	InstanceTest.class,
	UserTaskTest.class,
	WorkflowActivitiTest.class
	})
    public class CoreTests {

    @BeforeClass 
    public static void setUpClass() {      

    }

    @AfterClass 
    public static void tearDownClass() { 

    }

}
