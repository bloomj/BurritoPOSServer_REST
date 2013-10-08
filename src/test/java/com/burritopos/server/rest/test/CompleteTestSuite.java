package com.burritopos.server.rest.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	com.burritopos.server.rest.test.metrics.AllTests.class,
    com.burritopos.server.rest.test.webresource.AllTests.class,
    com.burritopos.server.rest.test.library.AllTests.class,
    com.burritopos.server.rest.test.filter.AllTests.class,
    com.burritopos.server.rest.test.identity.AllTests.class,
    com.burritopos.server.rest.test.security.dao.AllTests.class
    })
public class CompleteTestSuite {

    @BeforeClass
    public static void setUpClass() {
        System.out.println("Master setup");

    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("Master tearDown");
    }

}
