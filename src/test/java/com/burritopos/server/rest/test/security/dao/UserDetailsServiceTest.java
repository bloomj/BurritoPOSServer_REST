package com.burritopos.server.rest.test.security.dao;

import com.burritopos.server.rest.test.BuildTests;
import com.burritopos.server.rest.test.library.BaseTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Runner class for OAuth DAO for the servlet filter.
 *
 */
public class UserDetailsServiceTest extends BaseTest {  
	private static UserDetailsService oauthDataProvider;
	
    /**
     * Setups common test code for class
     *
     * @throws Exception
     */
    @BeforeClass 
    public static void setUpClass() throws Exception {   
    	BaseTest.setUpClass();
    	
        //Spring Framework IoC
        ClassPathXmlApplicationContext beanfactory = null;
        try {
            beanfactory = new ClassPathXmlApplicationContext(SPRING_CONFIG_DEFAULT);
            oauthDataProvider = (UserDetailsService)beanfactory.getBean("oauthDataProvider");
        } catch (Exception e) {
        	System.out.println("Unable to set Spring bean");
        	e.printStackTrace();
        } finally {
            if (beanfactory != null) {
                beanfactory.close();
            }
        }

    }
	
    /**
     * Tears down common test code for class
     *
     * @throws Exception
     */
    @AfterClass 
    public static void tearDownClass() throws Exception { 
    	BaseTest.tearDownClass();
    }
    
    /**
     * Tests LoadUserByUsername function
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testLoadUserByUsername() throws Exception {
    	UserDetails user = oauthDataProvider.loadUserByUsername(USER_USERNAME_STR);
    	
    	assertNotNull(user);
    	assertNotNull(user.getUsername());
    	assertEquals(USER_USERNAME_STR, user.getUsername());
    	assertNotNull(user.getPassword());
    	assertEquals(USER_PASSWORD_STR, user.getPassword());
    }
    
    /**
     * Tests LoadUserByUsername function for handling invalid user
     * @throws Exception 
     */
    @Test(expected = UsernameNotFoundException.class)
    @Category(BuildTests.class)
    public void testInvalidLoadUserByUsername() throws Exception {
    	oauthDataProvider.loadUserByUsername("Invalid_User");
    }
}
