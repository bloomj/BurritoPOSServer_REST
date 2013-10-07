package com.burritopos.server.rest.test.library;

import java.util.Random;

import com.burritopos.server.domain.Group;
import com.burritopos.server.domain.User;
import com.burritopos.server.rest.test.BaseTestCase;
import com.burritopos.server.service.crypto.BCrypt;
import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Runner class for the Burrito POS service library functionality.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class BaseTest extends BaseTestCase {
    protected ObjectMapper mapper;
    protected ObjectNode rootNode;
    protected JsonNode responseJson;
    protected JsonNodeFactory factory;
    protected static final Random rand = new Random();

    // test entities
    protected static IUserSvc userSvc;
    protected static User testUser;
    protected static IGroupSvc groupSvc;
    protected static Group testUserGroup;
    protected static Group testAdminGroup;
    
    // Spring configuration
    private static final String SPRING_CONFIG_DEFAULT = "applicationContext.xml";
    
    // Static strings
    private static final String USER_ROLE_STR = "ROLE_USER";
    private static final String ADMIN_ROLE_STR = "ROLE_ADMIN";
	
    /**
     * Initializes the test case.
     */
    public BaseTest() {
        super();
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
            userSvc = (IUserSvc)beanfactory.getBean("userSvc");
            groupSvc = (IGroupSvc)beanfactory.getBean("groupSvc");

        } catch (Exception e) {
        	System.out.println("Unable to set Spring bean");
        	e.printStackTrace();
        } finally {
            if (beanfactory != null) {
                beanfactory.close();
            }
        }
        
        testUserGroup = new Group();
        testUserGroup.setId(rand.nextInt());
        testUserGroup.setName(USER_ROLE_STR);
        
        groupSvc.storeGroup(testUserGroup);
        assertNotNull(groupSvc.getGroup(testUserGroup.getId()));
        
        testAdminGroup = new Group();
        testAdminGroup.setId(rand.nextInt());
        testAdminGroup.setName(ADMIN_ROLE_STR);
        
        groupSvc.storeGroup(testAdminGroup);
        assertNotNull(groupSvc.getGroup(testAdminGroup.getId()));
        
        testUser = new User();
        testUser.setId(rand.nextInt());
        testUser.addGroupId(testUserGroup.getId());
        testUser.addGroupId(testAdminGroup.getId());
        testUser.setUserName("Test_User");
        testUser.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        
        userSvc.storeUser(testUser);
        assertNotNull(userSvc.getUser(testUser.getId()));
    }

    /**
     * Tears down common test code for class
     *
     * @throws Exception
     */
    @AfterClass 
    public static void tearDownClass() throws Exception { 
        groupSvc.deleteGroup(testUserGroup.getId());
        assertNull(groupSvc.getGroup(testUserGroup.getId()).getName());
        
        groupSvc.deleteGroup(testAdminGroup.getId());
        assertNull(groupSvc.getGroup(testAdminGroup.getId()).getName());
        
        userSvc.deleteUser(testUser.getId());
    	assertNull(userSvc.getUser(testUser.getId()).getUserName());
    }
    
    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
        super.initCommonResources();
        
        mapper = new ObjectMapper();
        factory = JsonNodeFactory.instance;
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
}
