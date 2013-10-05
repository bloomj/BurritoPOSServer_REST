package com.burritopos.server.rest.library;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Wrapper class for interacting with Activiti Workflow engine.
 */
public class WorkflowActiviti {
	@Autowired
    protected ProcessEngine processEngine;
	@Autowired
    protected RuntimeService runtimeService;
	@Autowired
    protected TaskService taskService;
	@Autowired
    protected RepositoryService repositoryService;
	@Autowired
    protected IdentityService identityService;
	@Autowired
    protected FormService formService;
	@Autowired
    protected HistoryService historyService;
	
    protected ObjectMapper mapper;
    protected JsonNodeFactory factory;
    private static Logger dLog = Logger.getLogger(WorkflowActiviti.class);

    protected static String temporaryDirectory;
    
    // metrics
    protected final Counter inProcessWorkflows = Metrics.newCounter(WorkflowActiviti.class, "in-process-workflows");
    protected Boolean workflowsCounterInit = false;
    protected final Meter definitionsCreated = Metrics.newMeter(WorkflowActiviti.class, "definitions-created", "definitions-created", TimeUnit.SECONDS);
    protected final Meter instancesCreated = Metrics.newMeter(WorkflowActiviti.class, "instances-created", "instances-created", TimeUnit.SECONDS);
    protected final Histogram definitionListSizes = Metrics.newHistogram(WorkflowActiviti.class, "definition-list-sizes");
    protected final Histogram instanceListSizes = Metrics.newHistogram(WorkflowActiviti.class, "instance-list-sizes");
    protected final Histogram taskListSizes = Metrics.newHistogram(WorkflowActiviti.class, "task-list-sizes");

    public WorkflowActiviti() throws IOException {
        initWorkflowsCounter();

        mapper = new ObjectMapper();
        factory = JsonNodeFactory.instance;
        temporaryDirectory = BurritoPOSUtils.getProperty("temporary.directory");
    }
    
    // PROCESS DEFINITIONS
    /**
     * Creates a new process definition from either ad-hoc BPMN XML or already defined classpath resource.
     *
     * @param parameters
     * @param payload
     * @return
     * @throws Exception
     */
    public String createProcessDefinition(Map<String, String> parameters, String payload) throws Exception {
        dLog.trace("payload: " + payload);

        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        ObjectNode rootNode = mapper.createObjectNode();
        if (payload == null || payload.isEmpty()) {
            rootNode.put("Error", "Request payload is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }

        JsonNode payloadJson = mapper.readTree(payload);

        if (!payloadJson.has("mode")) {
            rootNode.put("Error", "Mode is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
        String mode = payloadJson.get("mode").asText();

        if (!payloadJson.has("bpmn")) {
            rootNode.put("Error", "BPMN is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
        String bpmn = payloadJson.get("bpmn").asText();

        if (!payloadJson.has("name")) {
            rootNode.put("Error", "Name is required");
            throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
        }
        String processDefinitionName = payloadJson.get("name").asText();

        dLog.trace("processDefinitionName: " + processDefinitionName + " | bpmn: " + bpmn + " | mode: " + mode);
        Deployment deployment = null;

        if (!processDefinitionName.isEmpty()) {
            if (mode.equals("classpath") && !bpmn.isEmpty()) {
                try {
                    deployment = repositoryService.createDeployment().addClasspathResource(bpmn).name(processDefinitionName).deploy();
                } catch (Exception e) {
                	dLog.error("Classpath Resource Does Not Exist",e);
                    rootNode.put("Classpath Resource Does Not Exist", e.getMessage());
                }
            } else if (mode.equals("xml")) {
                if (!payloadJson.has("filename")) {
                    rootNode.put("Error", "File name is required");
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }
                String fileName = payloadJson.get("filename").asText();

                // ensure filename has .bpmn20.xml file type
                if (!fileName.endsWith(".bpmn20.xml")) {
                    rootNode.put("Error", "BPMN XML file names must end in .bpmn20.xml");
                    throw new WebApplicationException(builder.status(Response.Status.BAD_REQUEST).entity(rootNode.toString()).build());
                }

                BurritoPOSUtils.writeFile(bpmn, temporaryDirectory, fileName);
                String filePath = temporaryDirectory + fileName;
                dLog.trace("filePath: " + filePath);

                // Attempt to deploy ad-hoc XML and return 400 if Activiti cannot parse it
                try {
                    deployment = repositoryService.createDeployment().addInputStream(filePath, new FileInputStream(filePath)).name(processDefinitionName).deploy();
                } catch (ActivitiException ae) {
                	dLog.error("ActivitiException Parsing BPMN XML", ae);
                    rootNode.put("Error", ae.getMessage());
                    throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
                }
            }

            if (deployment != null) {
                rootNode.put("Id", deployment.getId());
                rootNode.put("Name", deployment.getName());
                rootNode.put("DeploymentTime", deployment.getDeploymentTime().toString());
                //rootNode.put("Category", deployment.getCategory());//later Activiti version
            }
        }

        return rootNode.toString();
    }

    /**
     * Deletes an Activiti deployment based on deployment id.
     *
     * @param deploymentId
     * @throws Exception
     */
    public void deleteProcessDefinition(String deploymentId) throws Exception {
    	dLog.trace("in delete process definition");
        ObjectNode rootNode = mapper.createObjectNode();
        ResponseBuilderImpl builder = new ResponseBuilderImpl();

        List<ProcessDefinition> processDefList = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).list();

        dLog.trace("DeploymentId: " + deploymentId);
        if (processDefList.isEmpty()) {
            rootNode.put("Error", "Invalid ProcessDefinitionId: " + deploymentId);
            throw new WebApplicationException(builder.status(Response.Status.NOT_FOUND).entity(rootNode.toString()).build());
        }

        try {
            //delete deployment of process definition
            repositoryService.deleteDeployment(deploymentId);
        } catch (ActivitiException ae) {
        	dLog.error("ActivitiException deleting process definition", ae);
            rootNode.put("ActivitiException deleting process definition", ae.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        } catch (Exception e) {
        	dLog.error("Error in deleteProcessDefinition()", e);
            rootNode.put("Exception", e.getMessage());
            throw new WebApplicationException(builder.status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }
    }
 
    /**
     * Gets a list of all process definitions deployed to the Activiti repository service.
     *
     * @return JSON array of all process definitions
     */
    public String getProcessDefinitionList(String userId, String type) {
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode embeddedArray = new ArrayNode(factory);

        //get list of process definition
        List<ProcessDefinition> processDefList = new ArrayList<ProcessDefinition>();
        if(userId.isEmpty()) {
        	processDefList = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%" + type +"%").list();
        }
        else {
        	processDefList = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%" + type +"%").startableByUser(userId).list();
        }

        dLog.trace("Got " + processDefList.size() + " " + type + " process definitions for user: " + userId);
        for (ProcessDefinition processDef : processDefList) {
            ObjectNode processDefJson = mapper.createObjectNode();

            processDefJson.put("Id", processDef.getId());
            processDefJson.put("Key", processDef.getKey());
            processDefJson.put("Category", processDef.getCategory());
            processDefJson.put("DeploymentId", processDef.getDeploymentId());
            processDefJson.put("Name", processDef.getName());
            processDefJson.put("ResourceName", processDef.getResourceName());
            List<IdentityLink> idLinks = repositoryService.getIdentityLinksForProcessDefinition(processDef.getId());
            //logger.trace("Got " + idLinks.size() + " IdLinks for definition: " + processDef.getId());
            List<String> candidateGroups = new ArrayList<String>();
            for (IdentityLink iLink : idLinks) {
                candidateGroups.add(iLink.getGroupId());
            }
            processDefJson.put("CandidateGroups", candidateGroups.toString());

            //get list of form properties
            List<FormProperty> propList = formService.getStartFormData(processDef.getId()).getFormProperties();
            processDefJson.put("StartFormProperties", getFormProperties(propList));

            //get parameters used by query filter?
            embeddedArray.add(extractProcessDefinitionDetails(processDef));
        }
        rootNode.put("ProcessDefinitionList", embeddedArray);

        definitionListSizes.update(embeddedArray.size());

        return rootNode.toString();
    }
    
    /**
     * Compiles process definition details into JSON object
     * @param processDef
     * @return
     */
    private JsonNode extractProcessDefinitionDetails(ProcessDefinition processDef) {
        ObjectNode processDefJson = mapper.createObjectNode();

        processDefJson.put("Id", processDef.getId());
        processDefJson.put("Key", processDef.getKey());
        processDefJson.put("Category", processDef.getCategory());
        processDefJson.put("DeploymentId", processDef.getDeploymentId());
        processDefJson.put("Name", processDef.getName());
        processDefJson.put("ResourceName", processDef.getResourceName());

        //get list of form properties
        List<FormProperty> propList = formService.getStartFormData(processDef.getId()).getFormProperties();
        processDefJson.put("StartFormProperties", getFormProperties(propList));

        return processDefJson;
    }
    
    // PROCESS INSTANCES
    /**
     * Creates a new process instances from a specific process definition via form services.
     *
     * @param token
     * @param payload
     * @return JSON Array of process instances created
     * @throws Exception
     */
    public String createProcessInstance(String token, String payload) throws Exception {
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
        // add original payload for later use if needed
        //formProperties.put("OriginalPayload", payload);

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
            //dLog.trace("Got " + taskList.size() + " tasks for process instance: " + processInst.getId());
            if (!taskList.isEmpty()) {
                List<IdentityLink> idLinks = taskService.getIdentityLinksForTask(taskList.get(0).getId());
                //dLog.trace("Got " + idLinks.size() + " IdLinks for task: " + taskList.get(0).getId());
                ArrayNode candidateGroups = new ArrayNode(factory);
                for (IdentityLink iLink : idLinks) {
                    List<Group> groups = identityService.createGroupQuery().groupId(iLink.getGroupId()).list();
                    for(Group group : groups) {
        	            ObjectNode groupJson = mapper.createObjectNode();
        	            groupJson.put("GroupID", group.getId());
        	            groupJson.put("GroupName", group.getName());
        	            
        	            candidateGroups.add(groupJson);
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
    
    // TASK INSTANCES
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
    
    // TODO: Figure out if we can get back a validly rendered form and what config is necessary to do this
    /**
     * Calls Activiti form service getRenderForm function and returns the form object.
     *
     * @param parameters Map of url parameters, parameter Id and FormType are expected
     * @return Activiti Form Object
     * @throws Exception
     */
    public String getRenderedForm(Map<String, String> parameters) throws Exception {
        dLog.trace("In getRenderedForm");
        ObjectNode rootNode = mapper.createObjectNode();

        // get rendered form based on id and form type
        String formId = parameters.get("Id");
        String formType = parameters.get("FormType");

        dLog.trace("Id: " + formId + " | FormType: " + formType);
        Object form = null;
        if ("Start".equals(formType)) {
            dLog.trace("Getting rendered start form");
            form = formService.getRenderedStartForm(formId);
        } else if ("Task".equals(formType)) {
            dLog.trace("Getting rendered task form");
            form = formService.getRenderedTaskForm(formId);
        }

        if (form != null) {
            dLog.trace("Got the rendered " + formType + " form");
            dLog.trace("Form: " + form.toString());
            rootNode.put("FormObject", form.toString());
        } else {
            dLog.trace("Unable to get rendered form");
            rootNode.put("FormObject", "");
        }

        return rootNode.toString();
    }

    /**
     * Returns required form properties that do not have default values specified.
     *
     * @param id Process Definition Id or Task Id
     * @return Map<String, String> of required form properties
     */
    protected Map<String, String> getRequiredFormProperties(String type, String id) {
        Map<String, String> map = new HashMap<String, String>();

        //get list of form properties
        List<FormProperty> propList = null;
        if (type.equals("startForm")) {
            propList = formService.getStartFormData(id).getFormProperties();
        } else if (type.equals("taskForm")) {
            propList = formService.getTaskFormData(id).getFormProperties();
        }

        if (propList != null) {
            for (FormProperty prop : propList)
                if (prop.isRequired() && prop.getValue() == null)
                    map.put(prop.getId(), prop.getName());
        }

        return map;
    }

    /**
     * Checks a to see if a given property is defined on a specific process definition.
     *
     * @param processDefinitionId Process definition ID
     * @param propName            Property name to check
     * @return true/false
     */
    public Boolean isFormProperty(String processDefinitionId, String propName) {
        Boolean result = false;

        try {
            //get list of form properties
            List<FormProperty> propList = formService.getStartFormData(processDefinitionId).getFormProperties();

            for (FormProperty prop : propList) {
                if (prop.getId().equals(propName)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            dLog.error("Error in isFormProperty",e);
        }

        return result;
    }
    
    /**
     * Formats form properties into JSON representation.
     *
     * @param propList List of FormProperty objects
     * @return ArrayNode of Form Properties
     */
    @SuppressWarnings("unchecked")
    protected ArrayNode getFormProperties(List<FormProperty> propList) {
        //get list of form properties
        ArrayNode propArray = new ArrayNode(factory);

        for (FormProperty prop : propList) {
            ObjectNode propJson = mapper.createObjectNode();

            propJson.put("Id", prop.getId());
            propJson.put("Name", prop.getName());
            // get type properties
            if (prop.getType() != null) {
                String type = "Unknown";
                if (prop.getType().getName() != null) {
                    type = prop.getType().getName();
                }
                
                propJson.put("Type", type);

                if (type == "enum") {
                    //logger.trace("Reformatting enum property: " + prop.getId());
                    //logger.trace("Values: " + prop.getType().getInformation("values").toString());

                    ArrayNode enumArray = new ArrayNode(factory);
                    Map<String, String> values = (Map<String, String>) prop.getType().getInformation("values");

                    if (values != null) {
                        for (Entry<String, String> enumEntry : values.entrySet()) {
                            ObjectNode enumJson = mapper.createObjectNode();

                            enumJson.put("Id", enumEntry.getKey());
                            enumJson.put("Name", enumEntry.getValue());

                            enumArray.add(enumJson);
                        }
                    }

                    propJson.put("EnumValues", enumArray);
                }

                if (type == "date") {
                    propJson.put("DatePattern", prop.getType().getInformation("datePattern").toString());
                }
            }
            propJson.put("Value", prop.getValue());
            propJson.put("isReadable", prop.isReadable());
            propJson.put("isRequired", prop.isRequired());
            propJson.put("isWritable", prop.isWritable());

            propArray.add(propJson);
        }

        return propArray;
    }

    /**
     * Initializes yammer.metrics in-process-workflows Counter.
     */
    protected void initWorkflowsCounter() {
        if (!workflowsCounterInit) {
            // instantiate current in-process-workflows Counter
            try {
                // TODO: figure out why the runtime service is null here on constructor
                dLog.trace("Instantiating current in-process-workflows Counter");
                inProcessWorkflows.clear();
                if (runtimeService != null) {
                    List<ProcessInstance> processInstList = runtimeService.createProcessInstanceQuery().list();
                    dLog.trace("Instantiate current in-process-workflows Counter | Current In-Process Workflows: " + processInstList.size());
                    inProcessWorkflows.inc(processInstList.size());

                    workflowsCounterInit = true;
                } else {
                    dLog.warn("runtime services is null");
                }
            } catch (Exception e) {
                dLog.error("Unable to instantiate current in-process-workflows Counter",e);
            }
        }
    }

}
