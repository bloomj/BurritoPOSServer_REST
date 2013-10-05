package com.burritopos.server.rest.identity;

import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.apache.log4j.Logger;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;

@SuppressWarnings("unused")
/**
 * Utility class to help convert BurritoPOS.Domain and Activiti objects
 *
 */
public class IdentityUtils {	
	private static Logger dLog = Logger.getLogger(BurritoPOSUtils.class);
	
	static {

	}
	
    /**
     * Convert from burritopos.domain User object to Activiti User object
     * @param newUser
     * @return
     */
    public static UserEntity convertUserType(com.burritopos.server.domain.User newUser) {
    	if(validateUser(newUser)) {
	    	UserEntity user = new UserEntity();
	    	user.setId(newUser.getId().toString());
	    	user.setFirstName(newUser.getUserName());
	
	    	return user;
    	}
    	else {
    		return null;
    	}
    }
    
    /**
     * Convert from burritopos.domain Group object to Activiti Group object
     * @param newGroup
     * @return
     */
    public static GroupEntity convertGroupType(com.burritopos.server.domain.Group newGroup) {
    	if(validateGroup(newGroup)) {
	    	GroupEntity group = new GroupEntity();
	    	group.setId(newGroup.getId().toString());
	    	group.setName(newGroup.getName());
	    	
	    	return group;
    	}
    	else {
    		return null;
    	}
    }
    
    /**
     * Validate a User object
     * @param user
     * @return
     */
    public static boolean validateUser(com.burritopos.server.domain.User user) {
    	if(user != null && user.getId() != null && user.getUserName() != null) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    /**
     * Validate a Group object
     * @param group
     * @return
     */
    public static boolean validateGroup(com.burritopos.server.domain.Group group) {
    	if(group != null && group.getId() != null && group.getName() != null) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
}
