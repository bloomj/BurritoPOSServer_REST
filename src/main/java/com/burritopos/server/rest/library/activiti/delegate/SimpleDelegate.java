package com.burritopos.server.rest.library.activiti.delegate;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;

/**
 * Simple java delegate to be called from Activiti
 * 
 */
public class SimpleDelegate implements JavaDelegate {
    private static Logger logger = Logger.getLogger(SimpleDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
    	logger.trace("Doing some simple delegate actions");
    	
    	logger.trace("Current Activity ID: " + execution.getCurrentActivityId());
    	logger.trace("Current Activity Name: " + execution.getCurrentActivityName());
    	logger.trace("Process Definition ID: " + execution.getProcessDefinitionId());
    	logger.trace("Process Instance ID: " + execution.getProcessInstanceId());
    	for(String var : execution.getVariableNames()) {
    		logger.trace("Variable " + var + ": " + execution.getVariable(var));
    	}
    }
}