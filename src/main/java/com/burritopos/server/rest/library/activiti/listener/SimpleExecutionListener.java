package com.burritopos.server.rest.library.activiti.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.apache.log4j.Logger;

/**
 * Simple implementation of a custom Activiti Execution Listener.
 *
 */
public class SimpleExecutionListener implements TaskListener, ExecutionListener {
	private static final long serialVersionUID = 3742115912374545324L;
	private static Logger logger = Logger.getLogger(SimpleExecutionListener.class);
    private static final String EOL = System.getProperty("line.separator");
    private StringBuilder builder;
    
    /**
     * Constructor
     */
    public SimpleExecutionListener() throws Exception {

    }
    
    /**
     * 
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        logDelegateTask(delegateTask);
    }

    /**
     * 
     */
    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        builder = new StringBuilder();
        logDelegateExecution(builder, delegateExecution);
        logger.trace(builder.toString());
    }
    
    /**
     * 
     * @param delegateTask
     */
    private void logDelegateTask(DelegateTask delegateTask) {
    	logger.trace("delegateTask class: " + delegateTask.getClass().getCanonicalName());
    	builder = new StringBuilder();

        builder.append("delegateTask.getAssignee(): " + delegateTask.getAssignee()).append(EOL);
        builder.append("delegateTask.getCreateTime(): " + delegateTask.getCreateTime()).append(EOL);
        builder.append("delegateTask.getDescription(): " + delegateTask.getDescription()).append(EOL);
        builder.append("delegateTask.getDueDate(): " + delegateTask.getDueDate()).append(EOL);
        builder.append("delegateTask.getEventName(): " + delegateTask.getEventName()).append(EOL);
        {
            builder.append("DelegateExecution{").append(EOL);
            DelegateExecution delegateExecution = delegateTask.getExecution();
            logDelegateExecution(builder, delegateExecution);
        }
        builder.append("delegateTask.getExecutionId(): " + delegateTask.getExecutionId()).append(EOL);
        builder.append("delegateTask.getId(): " + delegateTask.getId()).append(EOL);
        builder.append("delegateTask.getName(): " + delegateTask.getName()).append(EOL);
        builder.append("delegateTask.getOwner(): " + delegateTask.getOwner()).append(EOL);
        builder.append("delegateTask.getPriority(): " + delegateTask.getPriority()).append(EOL);
        builder.append("delegateTask.getProcessDefinitionId(): " + delegateTask.getProcessDefinitionId()).append(EOL);
        builder.append("delegateTask.getProcessInstanceId(): " + delegateTask.getProcessInstanceId()).append(EOL);
        builder.append("delegateTask.getTaskDefinitionKey(): " + delegateTask.getTaskDefinitionKey()).append(EOL);
        {
            builder.append("Variables: {").append(EOL);
            for(String variableName : delegateTask.getVariableNames()) {
                builder.append(variableName + " = " + delegateTask.getVariable(variableName)).append(EOL);
            }
            builder.append("}").append(EOL);
        }
        {
            builder.append("Variables local: {").append(EOL);
            for(String variableName : delegateTask.getVariableNamesLocal()) {
                builder.append(variableName + " = " + delegateTask.getVariable(variableName)).append(EOL);
            }
            builder.append("}").append(EOL);
        }
        logger.trace(builder.toString());
    }
    
    /**
     * 
     * @param builder
     * @param delegateExecution
     */
    private void logDelegateExecution(StringBuilder builder, DelegateExecution delegateExecution) {
        builder.append("delegateExecution.getId(): " + delegateExecution.getId()).append(EOL);
        builder.append("delegateExecution.getProcessBusinessKey(): " + delegateExecution.getProcessBusinessKey()).append(EOL);
        builder.append("delegateExecution.getProcessInstanceId(): " + delegateExecution.getProcessInstanceId()).append(EOL);
        {
            builder.append("Variables: {").append(EOL);
            for(String variableName : delegateExecution.getVariableNames()) {
                builder.append(variableName + " = " + delegateExecution.getVariable(variableName)).append(EOL);
            }
            builder.append("}").append(EOL);
        }
        {
            builder.append("Variables local{").append(EOL);
            for(String variableName : delegateExecution.getVariableNamesLocal()) {
                builder.append(variableName + " = " + delegateExecution.getVariable(variableName)).append(EOL);
            }
            builder.append("}").append(EOL);
        }
    }
}
