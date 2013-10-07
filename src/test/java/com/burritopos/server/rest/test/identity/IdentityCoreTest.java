package com.burritopos.server.rest.test.identity;

import com.burritopos.server.rest.identity.group.GroupManager;
import com.burritopos.server.rest.identity.membership.MembershipManager;
import com.burritopos.server.rest.identity.user.UserManager;
import com.burritopos.server.rest.test.BaseTestCase;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.MembershipEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.UUID;

/**
 * Runner class for the workflow Activiti core functionality manipulation.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class IdentityCoreTest extends BaseTestCase {
    protected static GroupManager groupManager;
    protected static UserManager userManager;
    protected static MembershipManager membershipManager;

    // static test strings
    protected static String testGUID = UUID.randomUUID().toString();
    protected static String testGroupStr = "TEST-ROLE_" + testGUID;
    protected static String testUserStr = "TEST-USER_" + testGUID;

    // test entities
    protected static GroupEntity testGroup;
    protected static UserEntity testUser;
    protected static MembershipEntity testMembership;
    
    // Spring configuration
    private static final String SPRING_CONFIG_DEFAULT = "applicationContext.xml";

    /**
     * Constructor
     *
     * @throws IOException
     */
    public IdentityCoreTest() throws IOException {

    }

    /**
     * Setups common test code for class
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        //Spring Framework IoC
        ClassPathXmlApplicationContext beanfactory = null;
        try {
            beanfactory = new ClassPathXmlApplicationContext(SPRING_CONFIG_DEFAULT);
            groupManager = (GroupManager)beanfactory.getBean("groupManager");
            userManager = (UserManager)beanfactory.getBean("userManager");
            membershipManager = (MembershipManager)beanfactory.getBean("membershipManager");

        } catch (Exception e) {
        	System.out.println("Unable to set Spring bean");
        	e.printStackTrace();
        } finally {
            if (beanfactory != null) {
                beanfactory.close();
            }
        }
        // fake out Context Process Configuration for test
        ProcessEngineConfigurationImpl conf = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        Context.setProcessEngineConfiguration(conf);

        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Initializing common resources ***");

        System.out.println("   ");
        System.out.println("Creating test group");
        testGroup = (GroupEntity) groupManager.createNewGroup(testGroupStr);
        assertNotNull(groupManager.findGroupByName(testGroup.getName()));

        System.out.println("   ");
        System.out.println("Creating test user");
        testUser = (UserEntity) userManager.createNewUser(testUserStr);
        assertNotNull(userManager.findUserByName(testUser.getFirstName()));

        System.out.println("   ");
        System.out.println("Adding test user to test group");
        membershipManager.createMembership(testUser.getId(), testGroup.getId());
        assertNotNull(membershipManager.getMembership(testUser.getFirstName(), testGroup.getName()));
    }

    /**
     * Tears down common test code for class
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        //Spring Framework IoC
        /*ClassPathXmlApplicationContext beanfactory = null;
        try {
            beanfactory = new ClassPathXmlApplicationContext(SPRING_CONFIG_DEFAULT);
            groupManager = (GroupManager)beanfactory.getBean("groupManager");
            userManager = (UserManager)beanfactory.getBean("userManager");
            membershipManager = (MembershipManager)beanfactory.getBean("membershipManager");

        } catch (Exception e) {
        	System.out.println("Unable to set Spring bean");
        	e.printStackTrace();
        } finally {
            if (beanfactory != null) {
                beanfactory.close();
            }
        }*/
        
        System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Cleaning up common resources ***");

        System.out.println("   ");
        System.out.println("Removing test user from test group");
        membershipManager.deleteMembership(testUser.getFirstName(), testGroup.getName());
        assertNull(membershipManager.getMembership(testUser.getId(), testGroup.getId()));

        System.out.println("   ");
        System.out.println("Deleting test user");
        userManager.deleteUser(testUser.getId());
        assertNull(userManager.findUserByName(testUser.getFirstName()));

        System.out.println("   ");
        System.out.println("Deleting test group");
        groupManager.deleteGroup(testGroup.getName());
        assertNull(groupManager.findGroupByName(testGroup.getName()));
    }

    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {

    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @After
    public void tearDownCommonResources() throws Exception {

    }
}
