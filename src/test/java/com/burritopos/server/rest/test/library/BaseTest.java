package com.burritopos.server.rest.test.library;

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
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

    // test entities
    @Autowired
    protected IUserSvc userSvc;
    protected User testUser;
    
    @Autowired
    protected IGroupSvc groupSvc;
    protected Group testGroup;
	
    /**
     * Initializes the test case.
     */
    public BaseTest() {
        super();
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

        testGroup = new Group();
        testGroup.setId(56);
        testGroup.setName("Test_Group");
        
        groupSvc.storeGroup(testGroup);
        assertNotNull(groupSvc.getGroup(testGroup.getId()));
        
        testUser = new User();
        testUser.setId(123);
        testUser.addGroupId(testGroup.getId());
        testUser.setUserName("Test_User");
        testUser.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        
        userSvc.storeUser(testUser);
        assertNotNull(userSvc.getUser(testUser.getId()));
    }

    /**
     * Tears down common setup
     *
     * @throws Exception if it cannot tear down the test.
     */
    @After
    public void tearDownCommonResources() throws Exception {
        super.tearDownCommonResources();
        
        groupSvc.deleteGroup(testGroup.getId());
        assertNull(groupSvc.getGroup(testGroup.getId()).getName());
        
        userSvc.deleteUser(testUser.getId());
    	assertNull(userSvc.getUser(testUser.getId()).getUserName());
    }
}
