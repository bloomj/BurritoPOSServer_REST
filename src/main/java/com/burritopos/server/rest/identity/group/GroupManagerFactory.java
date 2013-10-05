package com.burritopos.server.rest.identity.group;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;

/**
 * Factory class for WorkflowGroupManager
 *
 */
class GroupManagerFactory implements SessionFactory {
	public Class<?> getSessionType() {
		return org.activiti.engine.impl.persistence.entity.GroupEntityManager.class;
	}

	public Session openSession() {
		return new GroupManager();
	}
}