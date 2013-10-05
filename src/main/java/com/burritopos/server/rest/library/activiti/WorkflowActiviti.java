package com.burritopos.server.rest.library.activiti;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

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
