package com.burritopos.server.rest.test.amqp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.burritopos.server.rest.amqp.JacksonMapToJsonStringConverter;
import com.burritopos.server.rest.test.BaseTestCase;
import com.burritopos.server.rest.test.BuildTests;


/**
 * Runner class for the AMQP Converter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class JacksonMapToJsonStringConverterTest extends BaseTestCase {
	private final ObjectMapper mapper = new ObjectMapper();
	private JacksonMapToJsonStringConverter messageConverter;
	private Map<String, Object> event;
	ObjectNode rootNode;
	
    /**
     * Sets up the necessary code to run the tests.
     *
     * @throws Exception if it cannot set up the test.
     */
    @Before
    public void initCommonResources() throws Exception {
    	super.initCommonResources();
        
        messageConverter = new JacksonMapToJsonStringConverter();
        
        Date eventDate = new Date();
        final String uuid = UUID.randomUUID().toString();

        event = new HashMap<String, Object>();
        event.put("date", eventDate);
        event.put("UUID", uuid);
        event.put("message", "This publication event was published on " + eventDate + " and generated UUID: " + uuid);
        
        rootNode = mapper.createObjectNode();
        rootNode.put("date", eventDate.toString());
        rootNode.put("UUID", uuid);
        rootNode.put("message", "This publication event was published on " + eventDate + " and generated UUID: " + uuid);
    }
    
    /**
     * Tests fromMessage conversion class
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testFromMessage() throws Exception {
    	Message testMessage = new Message(rootNode.toString().getBytes(), new MessageProperties());
    	messageConverter.fromMessage(testMessage);
    }
    
    /**
     * Tests fromMessage conversion class
     * @throws Exception 
     */
    @Test
    @Category(BuildTests.class)
    public void testToMessage() throws Exception {
    	messageConverter.toMessage(event, new MessageProperties());
    }
}
