package com.burritopos.server.rest.identity.membership;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;

/**
 * Factory class for WorkflowMembershipManager
 *
 */
class MembershipManagerFactory implements SessionFactory {
    public Class<?> getSessionType() {
    	return org.activiti.engine.impl.persistence.entity.MembershipEntityManager.class;
    }

    public Session openSession() {
    	return new MembershipManager();
    }
}