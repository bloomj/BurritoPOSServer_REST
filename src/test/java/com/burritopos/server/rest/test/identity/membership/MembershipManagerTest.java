package com.burritopos.server.rest.test.identity.membership;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.burritopos.server.rest.test.IntegrationTests;
import com.burritopos.server.rest.test.identity.IdentityCoreTest;

/**
 * Runner class for the workflow Activiti core functionality manipulation.
 *
 */
public class MembershipManagerTest extends IdentityCoreTest {
    
    /**
     * Constructor
     * @throws IOException 
     */
    public MembershipManagerTest() throws IOException {
    	super();
    }
    
    /**
     * Tests for successful membership deletion
     */
    @Test
    @Category(IntegrationTests.class)
    public void testDeleteMembership() {
    	System.out.println("   ");
    	
    	System.out.println("   ");	
    }
    
    /**
     * Tests for successful membership creation
     */
    @Test
    @Category(IntegrationTests.class)
    public void testCreateMembership() {
    	System.out.println("   ");
    	
    	System.out.println("   ");
    }
    
    /**
     * Tests for successful membership search by user and group name
     */
    @Test
    @Category(IntegrationTests.class)
    public void testGetMembership() {
    	System.out.println("   ");

    	assertNotNull(membershipManager.getMembership(testUser.getFirstName(), testGroup.getName()));
    	
    	System.out.println("   ");
    }

}
