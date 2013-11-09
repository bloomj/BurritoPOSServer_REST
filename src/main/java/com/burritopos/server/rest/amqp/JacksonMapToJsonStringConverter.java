package com.burritopos.server.rest.amqp;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.Map;

/**
 * Core logic for converting a map to JSON and vice versa
 *
 */
public class JacksonMapToJsonStringConverter implements MessageConverter {
	private static final Logger logger = Logger.getLogger(JacksonMapToJsonStringConverter.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonMapToJsonStringConverter() {

    }
    
	@Override
	public Object fromMessage(Message message) throws MessageConversionException {
		try {
			String json = new String(message.getBody());
			logger.trace("JSON: " + json);
			
			return mapper.readValue(json, Map.class);
		} catch(Exception ex) {
			logger.error("Unable to convert message body", ex);
			throw new MessageConversionException("Unable to convert message body", ex);
		}
	}

	@Override
	public Message toMessage(Object data, MessageProperties properties)	throws MessageConversionException {
		if(data instanceof Map) {
			try {
				String json =  mapper.writeValueAsString(data);
				return new Message(json.getBytes(), properties);
			} catch(Exception ex) {
				logger.error("Unable to convert transaction event to a json string", ex);
				throw new MessageConversionException("Unable to convert map to a json string", ex);
			}
		}
		logger.error("Invalid data.  Data provided was not a transaction event");
		throw new MessageConversionException("Invalid data.  Data provided was not a map");
	}
}
