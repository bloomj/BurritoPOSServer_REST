package com.burritopos.server.rest.library.activiti;

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.activiti.engine.*;
import org.activiti.engine.identity.Group;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Wrapper class for interacting with Activiti Workflow engine Process Instances.
 */
public class Instance extends WorkflowActiviti {
    private static Logger dLog = Logger.getLogger(Instance.class);

    public Instance() throws IOException {
        super();
    }
    
    /**
     * Creates a new process instances from a specific process definition via form services.
     *
     * @param payload
     * @return JSON Array of process instances created
     * @throws Exception
     */
    public String createProcessInstance(String payload) throws Exception {
        dLog.trace("in create process instance");

        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();

        initWorkflowsCounter();

        ObjectMapper mapper = new ObjectMapper();
        if (payload == null || payload.isEmpty()) {
            rootNode.put("Error", "Request payload is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        JsonNode payloadJson = mapper.readTree(payload);

        if (!payloadJson.has("ProcessDefinitionId")) {
            rootNode.put("Error", "ProcessDefinitionId is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        String processDefinitionId = payloadJson.get("ProcessDefinitionId").asText();
        // ensure id is valid
        List<ProcessDefinition> processDefList = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).list();
        if (processDefList.isEmpty()) {
            rootNode.put("Error", "Process Definition not found for ProcessDefinitionId: " + processDefinitionId);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        ProcessInstance pi = null;
        Map<String, String> formProperties = new HashMap<>();
        Iterator<String> names = payloadJson.getFieldNames();
        while (names.hasNext()) {
            String fieldName = names.next();

            //put vars in form properties for form services to access via JavaDelegate as well
            formProperties.put(fieldName, payloadJson.get(fieldName).asText());
        }

        //get list of form properties to enforce
        Map<String, String> reqProp = getRequiredFormProperties("startForm", processDefinitionId);

        dLog.trace("setting form properties for deployment id: " + processDefinitionId);
        if (payloadJson.findValue("StartFormProperties") != null) {
            Iterator<JsonNode> childNodes = (Iterator<JsonNode>) payloadJson.get("StartFormProperties").getElements();

            while (childNodes.hasNext()) {
                JsonNode childJson = childNodes.next();
                dLog.trace("Got child node: " + childJson.toString());

                names = childJson.getFieldNames();
                while (names.hasNext()) {
                    String fieldName = names.next();
                    formProperties.put(fieldName, childJson.get(fieldName).asText());
                    dLog.trace("Set Form Property | Fieldname: " + fieldName + " | value: " + childJson.get(fieldName).asText());

                    // remove property to ensure all are set
                    if (reqProp.containsKey(fieldName)) {
                        reqProp.remove(fieldName);
                    }
                }
            }
        }

        // if we still have properties at this point, send error back to client
        dLog.trace("Number of required form properties missing: " + reqProp.size());
        if (!reqProp.isEmpty()) {
            ArrayNode enumArray = new ArrayNode(factory);
            for (Entry<String, String> enumEntry : reqProp.entrySet()) {
                ObjectNode enumJson = mapper.createObjectNode();

                enumJson.put("Id", enumEntry.getKey());
                enumJson.put("Name", enumEntry.getValue());

                enumArray.add(enumJson);
            }

            rootNode.put("Required Form Properties", enumArray);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        dLog.trace("going to call startProcessInstanceById: " + processDefinitionId);
        try {
            //pi = runtimeService.startProcessInstanceById(id, vars);
            pi = formService.submitStartFormData(processDefinitionId, formProperties);
        } catch (ActivitiException ae) {
        	dLog.error("ActivitiException submitting start form", ae);
            rootNode.put("ActivitiException submitting start form", ae.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        } catch (Exception e) {
        	dLog.error("Exception submitting start form: ", e);
            rootNode.put("Exception submitting start form", e.toString());
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        ArrayNode embeddedArray = new ArrayNode(factory);
        if (pi != null) {
            ObjectNode processDefJson = mapper.createObjectNode();
            processDefJson.put("Id", pi.getId());
            processDefJson.put("ProcessInstanceId", pi.getProcessInstanceId());
            processDefJson.put("ProcessDefinitionId", pi.getProcessDefinitionId());
            processDefJson.put("isEnded", pi.isEnded());//never returns true (unless only automatics) since if ended, process instance is gone/null
            processDefJson.put("isSuspended", pi.isSuspended());
            embeddedArray.add(processDefJson);
        } else {
            rootNode.put("Error", "Null process instance returned from FormService.submitStartFormData");
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }

        rootNode.put("ProcessInstanceList", embeddedArray);

        instancesCreated.mark();
        inProcessWorkflows.inc();

        return rootNode.toString();
    }

    /**
     * Deletes a specific process instance.
     *
     * @param processInstanceId
     * @throws Exception
     */
    public void deleteProcessInstance(String processInstanceId) throws Exception {
        dLog.trace("in delete process instance");
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();

        initWorkflowsCounter();

        dLog.trace("going to call deleteProcessInstance");
        List<ProcessInstance> processInstList = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list();

        dLog.trace("ProcessInstanceId: " + processInstanceId);
        if (processInstList.isEmpty()) {
            rootNode.put("Error", "Invalid ProcessInstanceId: " + processInstanceId);
            throw new WebApplicationException(builder.status(Response.Status.NOT_FOUND).entity(rootNode.toString()).build());
        }

        try {
            //delete process instance
            runtimeService.deleteProcessInstance(processInstanceId, "Client request");
            inProcessWorkflows.dec();
        } catch (ActivitiException ae) {
        	dLog.error("Unable to delete instance", ae);
            rootNode.put("ActivitiException deleting instance", ae.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        } catch (Exception e) {
        	dLog.error("Error in runtimeService.deleteProcessInstance()", e);
            rootNode.put("Exception", e.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }
    }

    /**
     * Gets a list of all process instances available via runtime services.
     *
     * @return JSON Array of process instances
     */
    public String getProcessInstanceList() {
    	dLog.trace("Getting list of process instances");
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);

        initWorkflowsCounter();

        //get list of process instance
        List<ProcessInstance> processInstList = runtimeService.createProcessInstanceQuery().list();

        for (ProcessInstance processInst : processInstList) {
            ObjectNode processInstJson = mapper.createObjectNode();

            processInstJson.put("Id", processInst.getId());
            processInstJson.put("BusinessKey", processInst.getBusinessKey());
            processInstJson.put("ProcessDefinitionId", processInst.getProcessDefinitionId());
            processInstJson.put("InstanceId", processInst.getProcessInstanceId());

            // add candidateGroup to Json so we know who can claim the tasks
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInst.getId()).list();
            dLog.trace("Got " + taskList.size() + " tasks for process instance: " + processInst.getId());
            if (!taskList.isEmpty()) {
            	dLog.trace("Task Name: " + taskList.get(0).getName());
            	
                List<IdentityLink> idLinks = taskService.getIdentityLinksForTask(taskList.get(0).getId());
                dLog.trace("Got " + idLinks.size() + " IdLinks for task: " + taskList.get(0).getId());
                ArrayNode candidateGroups = new ArrayNode(factory);
                for (IdentityLink iLink : idLinks) {
                	if(iLink.getGroupId() != null) {
	                    List<Group> groups = identityService.createGroupQuery().groupId(iLink.getGroupId()).list();
	                    for(Group group : groups) {
	        	            ObjectNode groupJson = mapper.createObjectNode();
	        	            groupJson.put("GroupID", group.getId());
	        	            groupJson.put("GroupName", group.getName());
	        	            
	        	            candidateGroups.add(groupJson);
	                    }
                	}
                }
                processInstJson.put("CandidateGroups", candidateGroups);
            }

            //get parameters used by query filter?
            embeddedArray.add(processInstJson);
        }
        rootNode.put("ProcessInstanceList", embeddedArray);

        instanceListSizes.update(embeddedArray.size());

        return rootNode.toString();
    }
}
