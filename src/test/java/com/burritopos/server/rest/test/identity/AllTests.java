package com.burritopos.server.rest.test.identity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.burritopos.server.rest.test.identity.group.GroupManagerTest;
import com.burritopos.server.rest.test.identity.membership.MembershipManagerTest;
import com.burritopos.server.rest.test.identity.user.UserManagerTest;

@RunWith(Suite.class)
@SuiteClasses({ 
	GroupManagerTest.class, 
	MembershipManagerTest.class, 
	UserManagerTest.class
	})
    public class AllTests {

    @BeforeClass 
    public static void setUpClass() {      

    }

    @AfterClass 
    public static void tearDownClass() { 

    }

}
