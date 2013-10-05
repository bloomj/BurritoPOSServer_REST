package com.burritopos.server.rest.test.identity.user;

import java.io.IOException;
import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.IntegrationTests;
import com.burritopos.server.rest.test.identity.IdentityCoreTest;

/**
 * Runner class for the workflow Activiti core functionality manipulation.
 *
 */
public class UserManagerTest extends IdentityCoreTest {
    
    /**
     * Constructor
     * @throws IOException 
     */
    public UserManagerTest() throws IOException {
    	super();
    }
    
    /**
     * Tests for successful user deletion
     */
    @Test
    @Category(IntegrationTests.class)
    public void testDeleteUser() {
    	System.out.println("   ");
    	
    	System.out.println("   ");	
    }
    
    /**
     * Tests for successful user creation
     */
    @Test
    @Category(IntegrationTests.class)
    public void testCreateUser() {
    	System.out.println("   ");
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful user search by user id
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindUserById() {
    	System.out.println("   ");

    	assertNotNull(userManager.findUserById(testUser.getId()));
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful user search by user name
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindUserByName() {
    	System.out.println("   ");

    	assertNotNull(userManager.findUserByName(testUser.getFirstName()));
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful user query creation
     */
    @Test
    @Category(IntegrationTests.class)
    public void testCreateNewUserQuery() {
    	System.out.println("   ");

    	assertNotNull(userManager.createNewUserQuery());
    	assertEquals(UserQueryImpl.class, userManager.createNewUserQuery().getClass());

    	System.out.println("   ");
    }
    
    /**
     * Tests for successful user search by query criteria
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindUserByQueryCriteria() {
    	System.out.println("   ");

    	UserQueryImpl searchQuery = new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.userFirstName(testUser.getFirstName());
        
    	List<User> users = userManager.findUserByQueryCriteria(searchQuery, null);
    	
    	assertEquals(users.size(), 1);
    	UserEntity user = (UserEntity) users.get(0);
    	assertEquals(testUser.getFirstName(), user.getFirstName());
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful user search count by query criteria
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindUserCountByQueryCriteria() {
    	System.out.println("   ");

    	UserQueryImpl searchQuery = new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.userFirstName(testUser.getFirstName());
        
    	assertEquals(1, userManager.findUserCountByQueryCriteria(searchQuery));
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful group search by user name
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindGroupsByUser() {
    	System.out.println("   ");

    	List<Group> groups = groupManager.findGroupsByUser(testUser.getId());
    	
    	assertEquals(groups.size(), 1);
    	GroupEntity group = (GroupEntity) groups.get(0);
    	assertEquals(testGroup.getName(), group.getName());
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful insertion of user
     */
    @Test
    @Category(IntegrationTests.class)
    public void testInsertUser() {
    	System.out.println("   ");

    	String newUserStr = "TEST-INSERT-SUBJECT";
    	UserEntity user = new UserEntity(newUserStr);
    	user.setFirstName(newUserStr);
    	userManager.insertUser(user);
    	user = userManager.findUserByName(newUserStr);
    	assertNotNull(user);
    	userManager.deleteUser(user.getId());
    	assertNull(userManager.findUserById(newUserStr));
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful update of user
     */
    @Test
    @Category(IntegrationTests.class)
    public void testUpdateUser() {
    	System.out.println("   ");

    	String newUserStr = "TEST-UPDATE-SUBJECT";
    	UserEntity user = new UserEntity(newUserStr);
    	user.setFirstName(newUserStr);
    	userManager.updateUser(user);
    	user = userManager.findUserByName(newUserStr);
    	assertNotNull(user);
    	userManager.deleteUser(user.getId());
    	assertNull(userManager.findUserById(newUserStr));
    	
    	System.out.println("   ");
    }

}
