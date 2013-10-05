package com.burritopos.server.rest.identity.user;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.burritopos.server.rest.identity.IdentityUtils;
import com.burritopos.server.service.dao.IUserSvc;
import com.burritopos.server.service.dao.IGroupSvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class is utilized to handle custom User Manager code connecting into
 * the DAO.
 * 
 *         
 */
public class UserManager extends UserEntityManager {
	private static Logger dLog = Logger.getLogger(UserManager.class);
	private static final Random rand = new Random();
	
    @Autowired
    private IUserSvc userSvc;
    
    @Autowired
    private IGroupSvc groupSvc;
    
    /**
     * Constructor
     * @throws IOException 
     */
    public UserManager() {

    }
    
    @Override
    public User createNewUser(String userName) {
    	UserEntity user = null;

        try {
        	com.burritopos.server.domain.User newUser = new com.burritopos.server.domain.User();
    		newUser.setId(rand.nextInt());
    		newUser.setUserName(userName);
			
    		if(userSvc.storeUser(newUser)) {
	        	user = IdentityUtils.convertUserType(newUser);
        	}
		} catch (Exception e) {
			dLog.error("Error in createNewUser", e);
		}
        
        return user;
    }

    @Override
    public void insertUser(User user) {
        createNewUser(user.getId());
    }

    @Override
    public void updateUser(UserEntity updatedUser) {
    	createNewUser(updatedUser.getId());
    }

    @Override
    public UserEntity findUserById(String userId) {
    	UserQueryImpl searchQuery = new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.userId(userId);

    	List<User> users = findUserByQueryCriteria(searchQuery, null);
    	
    	if(users.size() > 0) {
    		return (UserEntity) users.get(0);
    	}
    	else {
    		return null;
    	}
    }
    
    public UserEntity findUserByName(String userName) {
    	UserQueryImpl searchQuery = new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.userFirstName(userName);

    	List<User> users = findUserByQueryCriteria(searchQuery, null);
    	
    	dLog.trace("User size: " + users.size());
    	if(users.size() > 0) {
    		return (UserEntity) users.get(0);
    	}
    	else {
    		return null;
    	}
    }

    @Override
    public void deleteUser(String userId) {    	
        if(!userId.isEmpty()) {
			try {
				userSvc.deleteUser(new Integer(userId));
			} catch (NumberFormatException e) {
				dLog.error("Error in deleteUser", e);
			} catch (Exception e) {
				dLog.error("Error in deleteUser", e);
			}
        }
    }

    @Override
    public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
    	List<User> users = new ArrayList<User>();
    	
    	try {
	    	if(StringUtils.isNotEmpty(query.getId())) {
	    		users.add(IdentityUtils.convertUserType(userSvc.getUser(new Integer(query.getId()))));
	    	} else if(StringUtils.isNotEmpty(query.getFirstName())) {
	    		users.add(IdentityUtils.convertUserType(userSvc.getUser(query.getFirstName())));
	    	} else if(StringUtils.isNotEmpty(query.getGroupId())) {
	    		List<com.burritopos.server.domain.User> tUsers = userSvc.getUsers(new Integer(query.getGroupId()));
	    		
	    		for(com.burritopos.server.domain.User user : tUsers) {
	    			users.add(IdentityUtils.convertUserType(user));
	    		}
	    	}
    	}
    	catch(Exception e) {
    		dLog.error("Error in findUserByQueryCriteria", e);
    	}
    	
    	return users;
    }

    @Override
    public long findUserCountByQueryCriteria(UserQueryImpl query) {
    	return findUserByQueryCriteria(query, null).size();
    }

    @Override
    public List<Group> findGroupsByUser(String userName) {
    	List<Group> groups = new ArrayList<Group>();
    	
    	try {
    		for(Integer i : userSvc.getUser(userName).getGroupId()) {
    			groups.add(IdentityUtils.convertGroupType(groupSvc.getGroup(i)));
    		}
    	}
    	catch(Exception e) {
    		dLog.error("Error in findUserByQueryCriteria", e);
    	}

    	return groups;
    }

    @Override
    public UserQuery createNewUserQuery() {
       return new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    }

    @Override
    public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put("userId", userId);
//        parameters.put("key", key);
//        return (IdentityInfoEntity) getDbSqlSession().selectOne("selectIdentityInfoByUserIdAndKey", parameters);
        throw new ActivitiException("UserManager doesn't support finding user info by userId and key.");
    }

    @Override
    public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put("userId", userId);
//        parameters.put("type", type);
//        return (List) getDbSqlSession().getSqlSession().selectList("selectIdentityInfoKeysByUserIdAndType", parameters);
        throw new ActivitiException("UserManager doesn't support finding user info keys by userId and type.");
    }

    @Override
    public Boolean checkPassword(String userId, String password) {
//        User user = findUserById(userId);
//        if ((user != null) && (password != null) && (password.equals(user.getPassword()))) {
//            return true;
//        }
//        return false;
        throw new ActivitiException("UserManager doesn't support checking password.");
    }
    
    @Override
    public List<User> findPotentialStarterUsers(String proceDefId) {
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put("procDefId", proceDefId);
//        return (List<Group>) getDbSqlSession().selectOne("selectGroupByQueryCriteria", parameters);
        throw new ActivitiException("UserManager doesn't support find potential starter users.");
    }
}
