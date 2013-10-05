package com.burritopos.server.rest.test.metrics;

import java.io.IOException;

import com.burritopos.server.rest.metrics.*;
import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.BaseTestCase;
import com.burritopos.server.rest.test.IntegrationTests;
import com.yammer.metrics.core.HealthCheck.Result;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Runner class for the burrito server service to test the yammer.metrics functionality.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class MetricsTest extends BaseTestCase {  
    /**
     * Tests MongoDBHealthCheck class
     * @throws IOException 
     */
    @Test
    @Category(BuildTests.class)
    public void testMongoDBHealthCheck() throws IOException {
    	MongoDBHealthCheck check = new MongoDBHealthCheck();
    	
    	Result result = check.execute();
    	
    	System.out.println("Result: " + result.getMessage());
    	
    	assertTrue(result.isHealthy());
    }
    
    /**
     * Tests ActivitiHealthCheck class
     */
    @Test
    @Category(IntegrationTests.class)
    public void testActivitiHealthCheck() {
    	ActivitiHealthCheck check = new ActivitiHealthCheck();
    	
    	Result result = check.execute();
    	
    	System.out.println("Result: " + result.getMessage());
    	
    	assertTrue(result.isHealthy());
    }
    
    /**
     * Tests DatabaseHealthCheck class
     * @throws IOException 
     */
    @Test
    @Category(BuildTests.class)
    public void testPostGresHealthCheck() throws IOException {
    	PostGresHealthCheck check = new PostGresHealthCheck();
    	
    	Result result = check.execute();
    	
    	System.out.println("Result: " + result.getMessage());
    	
    	assertTrue(result.isHealthy());
    }
}
