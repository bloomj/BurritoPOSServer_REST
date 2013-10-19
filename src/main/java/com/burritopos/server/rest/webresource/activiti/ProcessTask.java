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

import com.burritopos.server.rest.library.activiti.UserTask;
import com.burritopos.server.rest.utilities.BurritoPOSUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.io.IOException;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Jersey REST web-service wrapper for Activiti User Tasks
 *
 */
@Path("/usertask")
@Api(value = "/usertask", description = "Activiti User Task Methods")
@Produces({"application/json"})
public class ProcessTask {
    private static Logger dLog = Logger.getLogger(ProcessTask.class);    
    protected ObjectMapper mapper;
    
	private UserTask taskSvc;

    public void setTaskSvc(UserTask taskSvc) {
        this.taskSvc = taskSvc;
    }
    
    /**
     * Constructor
     * @throws IOException 
     */
    public ProcessTask() throws IOException {
        super();
        
        mapper = new ObjectMapper();
    }
    
    /**
     * Gets list of user tasks
     * 
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @GET
    @ApiOperation(value = "Gets List of Activiti User Tasks", notes = "Gets List of Activiti User Tasks", httpMethod = "GET", response = Response.class)
    @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 500, message = "Server Error")
    })
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getUserTaskList(@Context HttpHeaders headers,
                                    @Context UriInfo ui
    ) throws Exception {
    	dLog.trace("Getting user tasks");
        MultivaluedMap<String, String> queryParameters = ui.getQueryParameters();
        Map<String, String> queryParams = BurritoPOSUtils.parseMultivaluedMap(queryParameters);

        String response = taskSvc.getTaskInstanceList(queryParams);

        JsonNode rootNode = mapper.readTree(response);

        //use this to set location header:
        if (rootNode.has("TaskList")) {
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }
    
    /**
     * Updates Activiti User task.
     * 
     * @param headers
     * @param ui
     * @param taskId
     * @param payload
     * @return
     * @throws Exception
     */
    @Path("/{taskId}")
    @PUT
    @ApiOperation(value = "Updates Activiti User task", notes = "Updates Activiti User task", httpMethod = "PUT", response = Response.class)
    @ApiResponses(value = {
      @ApiResponse(code = 200, message = "OK"),
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Server Error")
    })
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response updateUserTask(@Context HttpHeaders headers,
                                   @Context UriInfo ui,
                                   @ApiParam(name = "taskId", value = "Activiti Task ID", required = true) 
                                   @PathParam("taskId") String taskId,
                                   @ApiParam(name = "payload", value = "Method Payload", required = true) 
                                   String payload
    ) throws Exception {
        MultivaluedMap<String, String> queryParameters = ui.getQueryParameters();
        Map<String, String> queryParams = BurritoPOSUtils.parseMultivaluedMap(queryParameters);

        try {
            return Response.ok(taskSvc.updateTask(taskId, queryParams, payload)).build();
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
        	dLog.error("Unable to update user task", e);
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("Error", e.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }
    }
}
