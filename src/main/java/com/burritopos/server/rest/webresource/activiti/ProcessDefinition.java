/*
 * 
 * Copyright (c) 2013, James Bloom
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
 *   in the documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.burritopos.server.rest.webresource.activiti;

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.wordnik.swagger.annotations.*;
import com.yammer.metrics.annotation.Timed;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.burritopos.server.rest.library.activiti.Definition;
import com.burritopos.server.rest.utilities.BurritoPOSUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Jersey REST web-service wrapper for Activiti Process Definitions
 *
 */
@Path("/processdefinition")
@Api(value = "/processdefinition", description = "Activiti Process Definition Methods")
@Produces({"application/json"})
public class ProcessDefinition {
    private static Logger dLog = Logger.getLogger(ProcessDefinition.class);    
    protected ObjectMapper mapper;
    
	private Definition definitionSvc;

    public void setDefinitionSvc(Definition definitionSvc) {
        this.definitionSvc = definitionSvc;
    }
    
    /**
     * Constructor
     * @throws IOException 
     */
    public ProcessDefinition() throws IOException {
        super();
        
        mapper = new ObjectMapper();
    }
    
    /**
     * Creates Process Definition in Activiti
     *
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @POST
    @ApiOperation(value = "Create Activiti Process Definition", notes = "Creates Process Definition in Activiti", httpMethod = "POST", response = Response.class)
    @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 500, message = "Server Error")
    })
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createProcessDefinition(@Context HttpHeaders headers,
				            @Context UriInfo ui,
				            @ApiParam(name = "payload", value = "Method Payload", allowableValues = "{bpmn: '', mode: 'classpath|xml'}", required = true) String payload
    ) throws Exception {
    	dLog.trace("In createProcessDefinition");
    	if (payload == null || payload.equals("")){
    		ResponseBuilderImpl builder = new ResponseBuilderImpl();
    		ObjectNode rootNode = mapper.createObjectNode();
    		rootNode.put("Error", "Payload is required" );
    		throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
    	}

    	Map<String, String> queryParams = BurritoPOSUtils.parseMultivaluedMap(ui.getQueryParameters());

    	String response = definitionSvc.createProcessDefinition(queryParams, payload);
    	JsonNode rootNode = mapper.readTree(response);

    	if (rootNode.has("Id")) {
    		String primaryKeyValue = rootNode.get("Id").asText();

    		return Response.created(new URI(primaryKeyValue)).entity(response).build();
    	} else {
    		return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    	}     
    }
}
