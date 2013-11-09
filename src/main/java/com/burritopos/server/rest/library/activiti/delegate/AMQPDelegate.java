package com.burritopos.server.rest.library.activiti.delegate;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;

public class AMQPDelegate implements JavaDelegate {
    private AmqpTemplate template;

    private static String PUBLISHED_QUEUE;

    private static Logger logger = Logger.getLogger(AMQPDelegate.class);

    public void setTemplate(AmqpTemplate template) {
        this.template = template;
    }

    /**
     * Constructor
     *
     * @throws IOException
     */
    public AMQPDelegate() throws IOException {
        PUBLISHED_QUEUE = BurritoPOSUtils.getProperty("reportEventQueue.queue");
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.trace("in execute method");

        // send AMQP publish message
        logger.trace("Sending AMQP publish message");
        Date eventDate = new Date();
        final String uuid = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<String, Object>();
        event.put("date", eventDate);
        event.put("UUID", uuid);
        event.put("message", "This publication event was published on " + eventDate + " and generated UUID: " + uuid);

        logger.trace("Event to publish via AMQP queue (" + PUBLISHED_QUEUE + "): " + event);
        template.convertAndSend(PUBLISHED_QUEUE, event);
    }
}