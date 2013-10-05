package com.burritopos.server.rest.library.activiti;

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Wrapper class for interacting with Activiti Workflow engine TaskService.
 */
public class UserTask extends WorkflowActiviti {
    private static Logger dLog = Logger.getLogger(UserTask.class);

    public UserTask() throws IOException {
        super();
    }
    
    protected enum UpdateTaskActionEnum {
        CLAIM, UNCLAIM, COMPLETE
    }

    /**
     * Retrieves the valid update task actions in JSON format
     * @return
     */
    protected Map<String, JsonNode> getValidUpdateTaskActionPayload() {
        ArrayNode enumArray = new ArrayNode(factory);
        ObjectNode enumJson = mapper.createObjectNode();

        enumJson.put("Id", "Action");
        enumJson.put("Value", EnumSet.allOf(UpdateTaskActionEnum.class).toString());

        enumArray.add(enumJson);

        HashMap<String, JsonNode> map = new HashMap<String, JsonNode>();
        map.put("Valid values", (JsonNode) enumArray);
        return map;
    }

    protected enum TaskInstanceTypeEnum {
        AVAILABLE, CLAIMED, ALL
    }

    /**
     * Retrieves the valid task instance types in JSON format
     * @return
     */
    protected Map<String, JsonNode> getValidTaskInstanceTypePayload() {
        ArrayNode enumArray = new ArrayNode(factory);
        ObjectNode enumJson = mapper.createObjectNode();

        enumJson.put("Id", "Action");
        enumJson.put("Value", EnumSet.allOf(TaskInstanceTypeEnum.class).toString());

        enumArray.add(enumJson);

        HashMap<String, JsonNode> map = new HashMap<String, JsonNode>();
        map.put("Valid values", (JsonNode) enumArray);
        return map;
    }
    
    /**
     * Gets the list of task instances for the specified group.
     *
     * @param parameters
     * @return JSON Array of Tasks
     * @throws Exception
     */
    public String getTaskInstanceList(Map<String, String> parameters) throws Exception {
        ObjectNode rootNode = mapper.createObjectNode();
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ArrayNode embeddedArray = new ArrayNode(factory);

        // get username from OAuth principal
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = loggedUser.getUsername();

        if (parameters.get("Type") == null) {
            rootNode.put("Error", "Type is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        TaskInstanceTypeEnum type = null;
        try {
            type = TaskInstanceTypeEnum.valueOf(parameters.get("Type").toUpperCase());
        } catch (Exception e) {
        	dLog.error("Invalid type", e);
            rootNode.put("Error", "Invalid Type: " + parameters.get("Type"));
            rootNode.putAll(getValidTaskInstanceTypePayload());
            throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        List<Task> taskList = getTaskList(userId, type);

        if (taskList != null) {
            dLog.trace("Found " + taskList.size() + " for User: " + userId);

            embeddedArray = processTaskList(taskList);
            rootNode.put("TaskList", embeddedArray);
            taskListSizes.update(embeddedArray.size());
        }

        dLog.trace("Returning: " + rootNode.toString());
        return rootNode.toString();
    }
    
    /**
     * Get task list from the Activiti task list
     * @param userId
     * @param type
     * @return
     */
    protected List<Task> getTaskList(String userId, TaskInstanceTypeEnum type) {
        List<Task> taskList;
        
        switch (type) {
            case AVAILABLE:
                taskList = taskService.createTaskQuery().taskCandidateUser(userId).taskUnassigned().list();
                break;
            case CLAIMED:
                taskList = taskService.createTaskQuery().taskAssignee(userId).list();
                break;
            case ALL:
                taskList = taskService.createTaskQuery().taskCandidateUser(userId).list();
                break;
            default:
            	ObjectNode rootNode = mapper.createObjectNode();
            	ResponseBuilderImpl builder = new ResponseBuilderImpl();
                rootNode.put("Error", "Unexpected Type:" + type.name());
                throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
        
        return taskList;
    }
    
    /**
     * Process the task list from Activiti and trim based on queue type, if passed in
     * @param taskList
     * @param queue
     * @return
     * @throws Exception 
     */
    protected ArrayNode processTaskList(List<Task> taskList) throws Exception {
    	ArrayNode embeddedArray = new ArrayNode(factory);
    	
        for (Task task : taskList) {
            ObjectNode processDefJson = mapper.createObjectNode();

            processDefJson.put("Id", task.getId());
            String taskStatus = "AVAILABLE";
            if(task.getAssignee() != null) {
            	taskStatus = "CLAIMED";
            }
            processDefJson.put("Status", taskStatus);
            processDefJson.put("Assignee", task.getAssignee());
            processDefJson.put("Description", task.getDescription());
            processDefJson.put("ExecutionId", task.getExecutionId());
            processDefJson.put("Name", task.getName());
            processDefJson.put("TaskDefinitionKey", task.getTaskDefinitionKey());

            //get list of task form properties
            List<FormProperty> propList = formService.getTaskFormData(task.getId()).getFormProperties();
            processDefJson.put("TaskFormProperties", getFormProperties(propList));

            Map<String, Object> map = runtimeService.getVariables(task.getExecutionId());
            ArrayNode vars = new ArrayNode(factory);
            for (String fieldName : map.keySet()) {
                ObjectNode varNode = mapper.createObjectNode();
                varNode.put(fieldName, map.get(fieldName).toString());
                vars.add(varNode);
            }
            
            processDefJson.put("VariableList", vars);
        }
    	
    	return embeddedArray;
    }

    /**
     * Updates a task by processing the action of either claim or complete.
     *
     * @param token
     * @param taskId
     * @param parameters
     * @param payload
     * @return Response from specific task update method
     * @throws Exception
     */
    public String updateTask(String taskId, Map<String, String> parameters, String payload) throws WebApplicationException, Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        if (payload == null || payload.isEmpty()) {
            rootNode.put("Error", "Payload is required when updating a Task");
            rootNode.putAll(getValidUpdateTaskActionPayload());
            throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        JsonNode payloadJson = mapper.readTree(payload);

        if (!payloadJson.has("Action")) {
            rootNode.put("Error", "Action is required");
            rootNode.putAll(getValidUpdateTaskActionPayload());
            throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        UpdateTaskActionEnum action = null;
        try {
            action = UpdateTaskActionEnum.valueOf(payloadJson.get("Action").getTextValue().toUpperCase());
        } catch (Exception e) {
        	dLog.error("Invalid Action", e);
            rootNode.put("Error", "Invalid Action: " + payloadJson.get("Action").getTextValue());
            rootNode.putAll(getValidUpdateTaskActionPayload());
            throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        dLog.trace("In updateTask: " + action);
        switch (action) {
            case CLAIM:
                return this.claimTask(taskId);
            case UNCLAIM:
                return this.unclaimTask(taskId, payload);
            case COMPLETE:
                return this.completeTask(taskId, payload);
            default:
                //throw unexpected Action as valid json error, shouldn't get here, but just in case
                rootNode.put("Error", "Unhandled Action: " + action);
                rootNode.putAll(getValidUpdateTaskActionPayload());
                throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
    }

    /**
     * Claims an Activiti task.
     *
     * @param taskId
     * @param payload
     * @return JSON Array of claimed tasks
     * @throws Exception
     */
    private String claimTask(String taskId) throws Exception {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);

        // get username from OAuth principal
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = loggedUser.getUsername();

        UserQuery user = identityService.createUserQuery().userFirstName(userId);
        if (user.count() == 0) {
            rootNode.put("Error", "Invalid user id: " + userId);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        // check to see if task exists and is available to UserId
        List<Task> taskList = taskService.createTaskQuery().taskCandidateUser(userId).taskId(taskId).list();

        if (taskList.isEmpty()) {
            //find task without candidateuser(userId) constraint
            taskList = taskService.createTaskQuery().taskId(taskId).list();
            if (taskList.isEmpty()) {
                rootNode.put("Error", "TaskId is invalid: " + taskId);
                throw new WebApplicationException(builder.status(Response.Status.NOT_FOUND).entity(rootNode.toString()).build());
            } else {
                if (taskList.get(0).getAssignee() == null) {
                    //task is unassigned, but specified user is not a candidate user for the task
                    rootNode.put("Error", "TaskId " + taskId + " is unavailable to UserId: " + userId);
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }

                if (taskList.get(0).getAssignee().equals(userId)) {
                    rootNode.put("Error", "TaskId " + taskId + " is already assigned to UserId: " + userId);
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }
            }
        }

        if (taskList.get(0).getAssignee() != null) {
            //task is already assigned
            rootNode.put("Error", "TaskId " + taskId + " is already assigned to Assignee: " + taskList.get(0).getAssignee());
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        taskService.claim(taskId, userId);

        ObjectNode processDefJson = mapper.createObjectNode();
        processDefJson.put("TaskId", taskId);
        processDefJson.put("Assignee", userId);
        embeddedArray.add(processDefJson);

        rootNode.put("TaskInstanceClaimedList", embeddedArray);

        return rootNode.toString();
    }

    /**
     * Unclaims an Activiti task.
     *
     * @param taskId
     * @return JSON Array of unclaimed tasks
     * @throws Exception
     */
    private String unclaimTask(String taskId, String payload) throws Exception {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);

        // check to see if task exists
        List<Task> taskList = taskService.createTaskQuery().taskId(taskId).list();
        if (taskList.isEmpty()) {
            rootNode.put("Error", "TaskId is invalid: " + taskId);
            throw new WebApplicationException(builder.status(Response.Status.NOT_FOUND).entity(rootNode.toString()).build());
        }
        if (taskList.get(0).getAssignee() == null) {
            rootNode.put("Error", "TaskId is not claimed: " + taskId);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
        //for now, don't restrict unclaimTask to current assigned, allow anyone with permission to unclaim
//        if (!taskList.get(0).getAssignee().equals(userId)) {
//            rootNode.put("Error", "TaskId " + taskId + " is not claimed by: " + userId);
//            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
//        }

        taskService.setAssignee(taskId, null);

        ObjectNode processDefJson = mapper.createObjectNode();
        processDefJson.put("TaskId", taskId);
        processDefJson.put("Assignee", "");
        embeddedArray.add(processDefJson);

        rootNode.put("TaskInstanceUnclaimedList", embeddedArray);

        return rootNode.toString();
    }

    /**
     * Completes an Activiti task.
     *
     * @param taskId
     * @param payload
     * @return JSON Array of completed tasks
     * @throws Exception
     */
    private String completeTask(String taskId, String payload) throws Exception {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);

        initWorkflowsCounter();

        JsonNode payloadJson = mapper.readTree(payload);
        
        // get username from OAuth principal
        User loggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = loggedUser.getUsername();

        UserQuery user = identityService.createUserQuery().userFirstName(userId);
        if (user.count() == 0) {
            rootNode.put("Error", "Invalid user id: " + userId);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        //verify taskid is currently assigned to user?
        //verify task is valid

        // check to see if task exists
        List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).taskId(taskId).list();
        if (taskList.isEmpty()) {
            //find task without taskAssignee(userId) constraint
            taskList = taskService.createTaskQuery().taskId(taskId).list();
            if (taskList.isEmpty()) {
                rootNode.put("Error", "TaskId is invalid: " + taskId);
                throw new WebApplicationException(builder.status(Response.Status.NOT_FOUND).entity(rootNode.toString()).build());
            } else {
                if (taskList.get(0).getAssignee() == null) {
                    //task is unassigned, but specified user is not a candidate user for the task
                    rootNode.put("Error", "TaskId " + taskId + " is unassigned and cannot be completed without being claimed");
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }

                if (!taskList.get(0).getAssignee().equals(userId)) {
                    rootNode.put("Error", "TaskId " + taskId + " is not currently assigned to UserId: " + userId);
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }
            }
        }

        //get list of form properties to enforce
        Map<String, String> reqProp = getRequiredFormProperties("taskForm", taskId);
        Boolean isTaskForm = false;
        if (!reqProp.isEmpty()) {
            isTaskForm = true;
        }

        Map<String, Object> vars = new HashMap<>();//Map<String, Object> used by taskService.complete()
        Map<String, String> varsStringMap = new HashMap<>();//Map<String, String> used by formService.submitTaskFormData()
        Iterator<String> names = payloadJson.getFieldNames();
        while (names.hasNext()) {
            String fieldName = names.next();
            String stringValue;
            if (payloadJson.get(fieldName) instanceof TextNode) {
                stringValue = payloadJson.get(fieldName).getTextValue();
            } else if (payloadJson.get(fieldName) instanceof ArrayNode) {
                stringValue = ((ArrayNode) payloadJson.get(fieldName)).toString();
            } else {
                stringValue = ((ObjectNode) payloadJson.get(fieldName)).toString();
            }
            vars.put(fieldName, stringValue);
            varsStringMap.put(fieldName, stringValue);
        }

        //get list of any secondary required properties
        Map<String, String> secondaryProp = new HashMap<String, String>();
        if (payloadJson.get("ProcessDefinitionInstantiate") != null) {
            JsonNode defJson = payloadJson.get("ProcessDefinitionInstantiate");

            if (!defJson.has("ProcessDefinitionId")) {
                rootNode.put("Error", "Missing property: ProcessDefinitionInstantiate.ProcessDefinitionId");
                throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
            }
            if (defJson.get("ProcessDefinitionId") != null) {
                secondaryProp = getRequiredFormProperties("startForm", defJson.get("ProcessDefinitionId").getTextValue());

                if (payloadJson.findValue("StartFormProperties") != null) {
                    ArrayNode taskList1 = (ArrayNode) defJson.get("StartFormProperties");
                    Iterator<JsonNode> childNodes = taskList1.getElements();

                    while (childNodes.hasNext()) {
                        JsonNode userTaskJson = childNodes.next();
                        dLog.trace("Got secondary start form properties node: " + userTaskJson.toString());

                        names = userTaskJson.getFieldNames();
                        while (names.hasNext()) {
                            String fieldName = names.next();
                            vars.put(fieldName, (Object) userTaskJson.get(fieldName).asText());
                            dLog.trace("Set Secondary Start Form Property | Fieldname: " + fieldName + " | value: " + userTaskJson.get(fieldName).asText());

                            // remove property to ensure all are set
                            if (secondaryProp.containsKey(fieldName))
                                secondaryProp.remove(fieldName);
                        }
                    }
                }
            }
        }

        // secondary start form properties could be need even if the primary form doesn't have extensions
        dLog.trace("Number of required secondary start form properties missing: " + secondaryProp.size());
        if (!secondaryProp.isEmpty()) {
            ArrayNode enumArray = new ArrayNode(factory);
            for (Entry<String, String> enumEntry : secondaryProp.entrySet()) {
                ObjectNode enumJson = mapper.createObjectNode();

                enumJson.put("Id", enumEntry.getKey());
                enumJson.put("Name", enumEntry.getValue());

                enumArray.add(enumJson);
            }

            rootNode.put("Required ProcessDefinitionInstantiate Start Form Properties", enumArray);
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        if (isTaskForm) {
            Map<String, String> taskProperties = new HashMap<>();
            if (payloadJson.findValue("TaskFormProperties") != null) {
                ArrayNode taskList1 = (ArrayNode) payloadJson.get("TaskFormProperties");
                Iterator<JsonNode> childNodes = taskList1.getElements();

                while (childNodes.hasNext()) {
                    JsonNode userTaskJson = childNodes.next();
                    dLog.trace("Got task properties node: " + userTaskJson.toString());

                    Iterator<String> names1 = userTaskJson.getFieldNames();
                    while (names1.hasNext()) {
                        String fieldName = names1.next();
                        taskProperties.put(fieldName, userTaskJson.get(fieldName).asText());
                        dLog.trace("Set Task Property | Fieldname: " + fieldName + " | value: " + userTaskJson.get(fieldName).asText());

                        // remove property to ensure all are set
                        if (reqProp.containsKey(fieldName))
                            reqProp.remove(fieldName);
                    }
                }
            }

            // if we still have properties at this point, send error back to client
            dLog.trace("Number of required task form properties missing: " + reqProp.size());
            if (!reqProp.isEmpty()) {
                ArrayNode enumArray = new ArrayNode(factory);
                for (Entry<String, String> enumEntry : reqProp.entrySet()) {
                    ObjectNode enumJson = mapper.createObjectNode();

                    enumJson.put("Id", enumEntry.getKey());
                    enumJson.put("Name", enumEntry.getValue());

                    enumArray.add(enumJson);
                }

                rootNode.put("Required Task Form Properties", enumArray);
                throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
            } else {
            	dLog.trace("Submitting task form data via form service: " + taskId);

                taskProperties.putAll(varsStringMap);
                try {
                    formService.submitTaskFormData(taskId, taskProperties);
                } catch (ActivitiException ae) {
                	dLog.error("ActivitiException submitting task form data: " + ae.getMessage());
                    rootNode.put("ActivitiException submitting task form data", ae.getMessage());
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                } catch (Exception e) {
                	dLog.error("Exception submitting task form data: " + e.getMessage());
                    rootNode.put("Exception submitting task form data", e.getMessage());
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }
            }
        } else {
            // no task form properties required, complete through task service
        	dLog.trace("Completing user task via task service: " + taskId);

            try {
                taskService.complete(taskId, vars);
            } catch (ActivitiException ae) {
            	dLog.error("ActivitiException completing task: " + ae.getMessage());
                rootNode.put("ActivitiException completing task", ae.getMessage());
                throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
            } catch (Exception e) {
                dLog.error("Exception completing task: " + e.getMessage());
                rootNode.put("Exception completing task", e.getMessage());
                throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
            }
        }

        ObjectNode processDefJson = mapper.createObjectNode();

        processDefJson.put("TaskId", taskId);
        processDefJson.put("Assignee", userId);
        embeddedArray.add(processDefJson);

        rootNode.put("TaskInstanceCompletedList", embeddedArray);

        inProcessWorkflows.dec();

        return rootNode.toString();
    }

}
