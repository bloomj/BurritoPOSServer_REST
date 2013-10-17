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
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.burritopos.server.rest.library.activiti.Instance;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.io.IOException;
import java.net.URI;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Jersey REST web-service wrapper for Activiti Process Instance
 *
 */
@Path("/processinstance")
@Api(value = "/processinstance", description = "Activiti Process Instance Methods")
@Produces({"application/json"})
public class ProcessInstance {
    private static Logger dLog = Logger.getLogger(ProcessInstance.class);    
    protected ObjectMapper mapper;
    
	private Instance instanceSvc;

    public void setInstanceSvc(Instance instanceSvc) {
        this.instanceSvc = instanceSvc;
    }
    
    /**
     * Constructor
     * @throws IOException 
     */
    public ProcessInstance() throws IOException {
        super();
        
        mapper = new ObjectMapper();
    }
    
    /**
     * Creates Process Instance in Activiti
     *
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @POST
    @ApiOperation(value = "Create Activiti Process Instance", notes = "Creates Process Instance in Activiti", httpMethod = "POST", response = Response.class)
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
				            @ApiParam(name = "payload", value = "Method Payload", allowableValues = "{ProcessDefinitionId: ''}", required = true) String payload
    ) throws Exception {
        try {
            String response = instanceSvc.createProcessInstance(payload);
            JsonNode rootNode = mapper.readTree(response);

            if (rootNode.has("ProcessInstanceList")) {
                ArrayNode array = (ArrayNode) rootNode.get("ProcessInstanceList");
                String primaryKeyValue = array.get(0).get("Id").asText();
                //use this to set location header:
                return Response.created(new URI(primaryKeyValue)).entity(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
        	dLog.error("Unable create process instance", e);
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("Error", e.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }    
    }
    
    /**
     * Deletes an Activiti deployment instance based on process definition id.
     *
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @Path("/{processInstanceId}")
    @DELETE
    @ApiOperation(value = "Delete Activiti Process Instance", notes = "Deletes Process Instance in Activiti", httpMethod = "DELETE", response = Response.class)
    @ApiResponses(value = {
      @ApiResponse(code = 204, message = "No Content"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Server Error")
    })
    @Timed
    @Produces(APPLICATION_JSON)
    public Response deleteProcessInstance(@Context HttpHeaders headers,
                                          @Context UriInfo ui,
                                          @ApiParam(name = "processInstanceId", value = "Activiti Process Instance ID", required = true) 
    									  @PathParam("processInstanceId") String processInstanceId
    ) throws Exception {
    	instanceSvc.deleteProcessInstance(processInstanceId);
        return Response.noContent().build();
    }
    
    /**
     * Gets list of Activiti process instances.
     * 
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @GET
    @ApiOperation(value = "Gets list of Activiti process instances", notes = "Gets list of Activiti process instances", httpMethod = "GET", response = Response.class)
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "OK")
    })
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getProcessInstance(@Context HttpHeaders headers,
                                       @Context UriInfo ui
    ) throws Exception {
        return Response.ok(instanceSvc.getProcessInstanceList()).build();
    }
}
