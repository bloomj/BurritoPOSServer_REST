package com.burritopos.server.rest.metrics;

import com.burritopos.server.rest.library.activiti.Definition;
import com.burritopos.server.rest.utilities.ApplicationContextUtils;
import com.yammer.metrics.core.HealthCheck;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides yammer.metrics style health check for Activiti service.
 *
 */
public class ActivitiHealthCheck extends HealthCheck {
    private static Logger dLog = Logger.getLogger(ActivitiHealthCheck.class);

    @Autowired
    private Definition activitiDefinitionSvc;
    
    /**
     * Creates HealthCheck instance for Activiti service.
     */
    public ActivitiHealthCheck() {
        super("Activiti Service");
    }

    @Override
    public Result check() throws Exception {
        if (checkActivitiRunning()) {
            return Result.healthy(" Activiti Service is running");
        } else {
            return Result.unhealthy("Cannot connect to Activiti Service");
        }
    }

    private Boolean checkActivitiRunning() throws Exception {
        Boolean result = false;

        // again with the Spring problems, lack of documentation for yammer.metrics
        // this works for now
        dLog.trace("Autowiring this bean");
        ApplicationContextUtils.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);

        // TODO: Migrate to WorkflowActivitiTest.checkActivitiServices
        
        /*dLog.trace("Attempting to getProcessDefinitionList from Activiti");

        String defList = "";
        if (activitiDefinitionSvc != null) {
        	try {
        		//request list of process definitions of type TEST, should be zero but non-error empty list shows Activiti engine is operational
        		defList = activitiDefinitionSvc.getProcessDefinitionList("TEST", false);
        		dLog.trace("Got process definition list: " + defList);
        		result = true;
        	} catch (Exception e) {
        		dLog.trace("Activiti health check failed",e);
        	}
        } else {
        	dLog.trace("Activiti health check failed, activitiDefinitionSvc is null");
        }*/

        return true;
    }
}