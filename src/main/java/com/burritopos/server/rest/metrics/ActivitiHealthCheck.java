package com.burritopos.server.rest.metrics;

import com.burritopos.server.rest.library.activiti.WorkflowActiviti;
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
    private WorkflowActiviti activitiSvc;
    
    private String results = "";
    
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
            return Result.unhealthy("Cannot connect to Activiti Service: " + results);
        }
    }

    private Boolean checkActivitiRunning() throws Exception {
        Boolean result = false;

        // again with the Spring problems, lack of documentation for yammer.metrics
        // this works for now
        dLog.trace("Autowiring this bean");
        ApplicationContextUtils.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);

        results = activitiSvc.checkActivitiServices();
        if(results.isEmpty()) {
        	result = true;
        }

        return result;
    }
}