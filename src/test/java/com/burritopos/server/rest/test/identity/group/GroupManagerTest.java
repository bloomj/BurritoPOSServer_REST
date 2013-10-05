package com.burritopos.server.rest.test.identity.group;

import java.io.IOException;
import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.IntegrationTests;
import com.burritopos.server.rest.test.identity.IdentityCoreTest;
/**
 * Runner class for the Activiti core functionality manipulation.
 *
 */
public class GroupManagerTest extends IdentityCoreTest {
    
    /**
     * Constructor
     * @throws IOException 
     */
    public GroupManagerTest() throws IOException {
    	super();
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testDeleteGroup() {
    	System.out.println("   ");
    	
    	System.out.println("   ");	
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testCreateGroup() {
    	System.out.println("   ");
    	
    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindGroupById() {
    	System.out.println("   ");

    	assertNotNull(groupManager.findGroupById(testGroup.getId()));
    	
    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindGroupByName() {
    	System.out.println("   ");

    	assertNotNull(groupManager.findGroupByName(testGroup.getName()));
    	
    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testCreateNewGroupQuery() {
    	System.out.println("   ");

    	assertNotNull(groupManager.createNewGroupQuery());
    	assertEquals(GroupQueryImpl.class, groupManager.createNewGroupQuery().getClass());

    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindGroupByQueryCriteria() {
    	System.out.println("   ");

    	GroupQueryImpl searchQuery = new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.groupName(testGroup.getName());
        
    	List<Group> groups = groupManager.findGroupByQueryCriteria(searchQuery, null);
    	
    	assertEquals(groups.size(), 1);
    	GroupEntity group = (GroupEntity) groups.get(0);
    	assertEquals(testGroup.getName(), group.getName());
    	
    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testFindGroupCountByQueryCriteria() {
    	System.out.println("   ");

    	GroupQueryImpl searchQuery = new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.groupName(testGroup.getName());
        
    	assertEquals(1, groupManager.findGroupCountByQueryCriteria(searchQuery));
    	
    	System.out.println("   ");
    }
    
    /**
     * 
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
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testInsertGroup() {
    	System.out.println("   ");

    	GroupEntity group = new GroupEntity("TEST-INSERT-ROLE");
    	group.setName("TEST-INSERT-ROLE");
    	groupManager.insertGroup(group);
    	group = groupManager.findGroupByName("TEST-INSERT-ROLE");
    	assertNotNull(group);
    	groupManager.deleteGroup(group.getName());
    	assertNull(groupManager.findGroupByName("TEST-INSERT-ROLE"));
    	
    	System.out.println("   ");
    }
    
    /**
     * 
     */
    @Test
    @Category(IntegrationTests.class)
    public void testUpdateGroup() {
    	System.out.println("   ");

    	GroupEntity group = new GroupEntity("TEST-UPDATE-ROLE");
    	group.setName("TEST-UPDATE-ROLE");
    	System.out.println("Updating group");
    	groupManager.updateGroup(group);
    	System.out.println("Finding group by name");
    	group = groupManager.findGroupByName("TEST-UPDATE-ROLE");
    	assertNotNull(group);
    	groupManager.deleteGroup(group.getName());
    	assertNull(groupManager.findGroupByName("TEST-UPDATE-ROLE"));
    	
    	System.out.println("   ");
    }

}
