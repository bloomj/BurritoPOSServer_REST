package com.burritopos.server.rest.test;

import com.burritopos.server.rest.test.BuildTests;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Abstract Base Test class for standardization
 *
 */
public abstract class BaseTestCase extends TestCase {
	@Rule
	public TestWatcher watcher = new TestWatcher() {
	  protected void starting(Description description) {
	    System.out.println(String.format("*** Starting test: %s() ***", description.getMethodName()));
	  };
	};
	
    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Initializing common resources ***");

    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @After
    public void tearDownCommonResources() throws Exception {
        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Cleaning up common resources ***");

        System.out.println("   ");
    }

    /**
     * Mock test for BuildTests category
     */
    @Test
    @Category(BuildTests.class)
    public void testBuildTestsMock() {

    }
}
