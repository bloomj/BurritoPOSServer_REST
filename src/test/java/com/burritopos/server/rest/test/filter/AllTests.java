package com.burritopos.server.rest.test.filter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	RequestFilterTest.class
	})
    public class AllTests {

    @BeforeClass 
    public static void setUpClass() {
        //including the following property assignments allows filter/AllTests to complete successfully when run separately
        /*System.setProperty("javax.net.ssl.keyStore", certLocation +"C:\\burritopos.ks");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStore", certLocation + "C:\\burritopos.ks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");*/
    }

    @AfterClass 
    public static void tearDownClass() { 

    }

}
