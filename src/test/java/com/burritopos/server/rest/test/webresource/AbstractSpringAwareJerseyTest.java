package com.burritopos.server.rest.test.webresource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

/**
 * Test class which will wire itSelf into your the Spring context which
 * is configured on the WebAppDecriptor built for your tests.
 * Ensure you configure annotation-aware support into your contexts,
 * and annotate any auto-wire properties on your test class
 * @author George McIntosh
 *
 */
public abstract class AbstractSpringAwareJerseyTest extends JerseyTest {
	@Rule
	public TestWatcher watcher = new TestWatcher() {
	  protected void starting(Description description) {
	    System.out.println(String.format("*** Starting test: %s() ***", description.getMethodName()));
	  };
	};

    protected static String DEFAULT_URI = "http://localhost:9998";
    protected static ObjectMapper mapper;
    protected JsonNodeFactory factory;
    protected JsonNode responseJson;

    protected WebResource ws;
    protected ClientResponse response;
    
    private SpringAwareGrizzlyTestContainerFactory containerFactory;
	
	public AbstractSpringAwareJerseyTest(WebAppDescriptor wad) {
		super(wad);
		
		//System.out.println("Got baseURI: " + containerFactory.getBaseUri());
		//DEFAULT_URI = containerFactory.getBaseUri().toString();
	}
	
	protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
		containerFactory = new SpringAwareGrizzlyTestContainerFactory(this);
		return containerFactory;
	}
	
    @BeforeClass 
    public static void setUpClass() throws Exception {     	

    }
    
    @AfterClass
    public static void tearDownClass() { 

    }
    
    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
    	System.out.println("   ");
        System.out.println("   ");
        System.out.println("*** Initializing common resources ***");
        
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
        System.out.println("   ");
        System.out.println("*** Cleaning up common resources ***");

        
        System.out.println("   ");
    }
}