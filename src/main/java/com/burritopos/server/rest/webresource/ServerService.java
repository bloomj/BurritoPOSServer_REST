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
package com.burritopos.server.rest.webresource;

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.yammer.metrics.annotation.Timed;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.burritopos.server.rest.library.BurritoServer;
import com.burritopos.server.rest.utilities.BurritoPOSUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.io.IOException;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Jersey REST web-service wrapper for Burrito POS Server
 *
 */
@Path("/")
public class ServerService {
    private static Logger logger = Logger.getLogger(ServerService.class);    
    protected ObjectMapper mapper;
    
    private BurritoServer server;

    public void setServer(BurritoServer server) {
        this.server = server;
    }
    
    /**
     * Constructor
     * @throws IOException 
     */
    public ServerService() throws IOException {
        super();
        
        mapper = new ObjectMapper();
    }

    // TODO: deprecate and remove this function and move authentication
    
    /**
     * Mimics the old socket login function
     *
     * @param headers
     * @param ui
     * @return
     * @throws Exception
     */
    @Path("/login")
    @POST
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response login(@Context HttpHeaders headers,
				            @Context UriInfo ui,
				            @PathParam("taskId") String taskId,
				            String payload
    ) throws Exception {
        logger.trace("Attempting to login");
        
    	if (payload == null || payload.equals("")){
    		ResponseBuilderImpl builder = new ResponseBuilderImpl();
    		ObjectNode rootNode = mapper.createObjectNode();
    		rootNode.put("Error", "Payload is required" );
    		throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
    	}

    	MultivaluedMap<String, String> queryParameters = ui.getQueryParameters();
    	Map<String, String> queryParams = BurritoPOSUtils.parseMultivaluedMap(queryParameters);
        
        return Response.ok(server.doLogin(queryParams, payload)).build();
    }
}
