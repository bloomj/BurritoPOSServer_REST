package com.burritopos.server.rest.test.webresource;

//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ 
	CoreTests.class
	})
    public class AllTests {
	
    @BeforeClass 
    public static void setUpClass() throws JsonProcessingException, IOException {  

    }
    
    @AfterClass 
    public static void tearDownClass() { 

    }
}
