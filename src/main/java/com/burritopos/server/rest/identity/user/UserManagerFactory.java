package com.burritopos.server.rest.identity.user;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;

class UserManagerFactory implements SessionFactory {  
    public Class<?> getSessionType() {
    	return org.activiti.engine.impl.persistence.entity.UserEntityManager.class;
    }

    public Session openSession() {
    	return new UserManager();
    }
}