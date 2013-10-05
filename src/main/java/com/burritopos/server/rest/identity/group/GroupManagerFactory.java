package com.burritopos.server.rest.identity.group;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;

import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;

/**
 * Factory class for WorkflowGroupManager
 *
 */
class GroupManagerFactory implements SessionFactory {
    private IGroupSvc groupSvc;
    
    private IUserSvc userSvc;
	
    public void setGroupSvc(IGroupSvc groupSvc) {
        this.groupSvc = groupSvc;
    }
    
    public void setUserSvc(IUserSvc userSvc) {
        this.userSvc = userSvc;
    }
    
	public Class<?> getSessionType() {
		return org.activiti.engine.impl.persistence.entity.GroupEntityManager.class;
	}

	public Session openSession() {
		return new GroupManager(groupSvc, userSvc);
	}
}