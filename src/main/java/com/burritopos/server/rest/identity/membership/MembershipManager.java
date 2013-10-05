package com.burritopos.server.rest.identity.membership;

import org.activiti.engine.impl.persistence.entity.MembershipEntity;
import org.activiti.engine.impl.persistence.entity.MembershipEntityManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.burritopos.server.rest.identity.IdentityUtils;
import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;

import java.io.IOException;
import java.util.Random;

/**
 * This class is utilized to handle custom Membership Manager code connecting into
 * the DAO.  MembershipEntity is what ties the UserEntity to the GroupEntity.
 *         
 */
public class MembershipManager extends MembershipEntityManager {
	private static Logger dLog = Logger.getLogger(MembershipManager.class);
    private static final Random rand = new Random();
    
    @Autowired
    private IGroupSvc groupSvc;
    
    @Autowired
    private IUserSvc userSvc;
    
    /**
     * Constructor
     * @throws IOException 
     */
    public MembershipManager() {

    }
    
    /**
     * Creates membership in Activiti by adding user to group
     */
	@Override
	public void createMembership(String userId, String groupId) { 
        try {
        	com.burritopos.server.domain.User user = userSvc.getUser(new Integer(userId));
    		user.addGroupId(new Integer(groupId));
			
    		userSvc.storeUser(user);
		} catch (Exception e) {
			dLog.error("Error in createMembership", e);
		}
	}
	
	// TODO: Add MembershipQueryImpl to mirror User/Group?
	/**
	 * Gets membership based on user name and group name
	 * @param userName
	 * @param groupName
	 * @return
	 */
	public MembershipEntity getMembership(String userName, String groupName) {
		MembershipEntity membership = null;
		
        try {
        	com.burritopos.server.domain.User user = userSvc.getUser(userName);
        	com.burritopos.server.domain.Group group = groupSvc.getGroup(groupName);
        	
        	if(IdentityUtils.validateUser(user) && IdentityUtils.validateGroup(group)) {
	        	membership = new MembershipEntity();
	        	membership.setId(Integer.toString(rand.nextInt()));
	        	membership.setGroupId(group.getId().toString());
	        	membership.setUserId(user.getId().toString());
        	}
		} catch (Exception e) {
			dLog.trace("Error in getMembership", e);
		}
        
        return membership;
	}
	
	/**
     * Deletes membership in Activiti by removing user from group
     */
	@Override
	public void deleteMembership(String userName, String groupName) {
        try {
        	com.burritopos.server.domain.User user = userSvc.getUser(userName);
    		user.removeGroupId(groupSvc.getGroup(groupName).getId());
			
    		dLog.trace("Should have removed: " + groupSvc.getGroup(groupName).getId());
    		for(Integer i : user.getGroupId()) {
    			dLog.trace("Group: " + i);
    		}
    		
    		userSvc.storeUser(user);
		} catch (Exception e) {
			dLog.error("Error in deleteMembership", e);
		}
	}
}
