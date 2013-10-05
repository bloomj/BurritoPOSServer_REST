package com.burritopos.server.rest.library.activiti;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.identity.Group;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.IdentityLink;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Wrapper class for interacting with Activiti Workflow engine Process Definitions.
 */
public class Definition extends WorkflowActiviti {
    private static Logger dLog = Logger.getLogger(Definition.class);

    public Definition() throws IOException {
        super();
    }
    
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
            dLog.trace("Got " + idLinks.size() + " IdLinks for definition: " + processDef.getId());
            ArrayNode candidateGroups = new ArrayNode(factory);
            for (IdentityLink iLink : idLinks) {
                List<Group> groups = identityService.createGroupQuery().groupId(iLink.getGroupId()).list();
                dLog.trace("IdentityService found " + groups.size() + " for link: " + iLink.getGroupId());
                for(Group group : groups) {
    	            ObjectNode groupJson = mapper.createObjectNode();
    	            groupJson.put("GroupID", group.getId());
    	            groupJson.put("GroupName", group.getName());
    	            
    	            candidateGroups.add(groupJson);
                }
            }
            dLog.trace("CandidateGroups: " + candidateGroups.toString());
            processDefJson.put("CandidateGroups", candidateGroups);

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
}
