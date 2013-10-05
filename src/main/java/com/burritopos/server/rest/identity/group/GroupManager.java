package com.burritopos.server.rest.identity.group;

import com.burritopos.server.rest.identity.IdentityUtils;
import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
//import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class is utilized to handle custom Group Manager code connecting into
 * the configured DAO.
 *
 */
public class GroupManager extends GroupEntityManager {
    private static Logger dLog = Logger.getLogger(GroupManager.class);
    private static final Random rand = new Random();
    private ObjectMapper mapper;
    
    //@Autowired --> AutoWired won't work
    private IGroupSvc groupSvc;
    
    //@Autowired --> AutoWired won't work
    private IUserSvc userSvc;
    
    /**
     * Constructor
     * @throws IOException
     */
    public GroupManager(IGroupSvc groupSvc, IUserSvc userSvc) {
    	mapper = new ObjectMapper();
    	this.groupSvc = groupSvc;
    	this.userSvc = userSvc;
    }

    /**
     * Creates a new group
     */
    @Override
    public Group createNewGroup(String groupName) {
        GroupEntity group = null;

        try {
        	com.burritopos.server.domain.Group newGroup = new com.burritopos.server.domain.Group();
    		newGroup.setId(rand.nextInt());
    		newGroup.setName(groupName);
			
    		if(groupSvc.storeGroup(newGroup)) {
    			group = IdentityUtils.convertGroupType(newGroup);
        	}
		} catch (Exception e) {
			dLog.error("Error in createNewUser", e);
		}

        return group;
    }

    /**
     * Inserts a group
     */
    @Override
    public void insertGroup(Group group) {
        createNewGroup(group.getName());
    }

    /**
     * Updates a group
     */
    @Override
    public void updateGroup(GroupEntity updatedGroup) {
        createNewGroup(updatedGroup.getName());
    }

    /**
     * Deletes a group
     */
    @Override
    public void deleteGroup(String groupName) {
        if(!groupName.isEmpty()) {
			try {
				groupSvc.deleteGroup(groupSvc.getGroup(groupName).getId());
			} catch (NumberFormatException e) {
				dLog.error("Error in deleteGroup", e);
			} catch (Exception e) {
				dLog.error("Error in deleteGroup", e);
			}
        }
    }

    /**
     * Creates a new group query
     */
    @Override
    public GroupQuery createNewGroupQuery() {
        return new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    }

    /**
     * Finds a group(s) by query criteria
     */
    @Override
    public List<Group> findGroupByQueryCriteria(GroupQueryImpl query, Page page) {
    	List<Group> groups = new ArrayList<Group>();
    	
    	try {
	        if (StringUtils.isNotEmpty(query.getId())) {
	        	dLog.trace("Querying for groups that have criteria | ID: " + query.getId());
	        	if(groupSvc == null) {
	        		dLog.error("My bad");
	        	}
	        	com.burritopos.server.domain.Group g = groupSvc.getGroup(Integer.parseInt(query.getId()));
	        	if(g == null) {
	        		dLog.error("Unable to get group");
	        	}
	            groups.add(IdentityUtils.convertGroupType(groupSvc.getGroup(new Integer(query.getId()))));
	        } else if (StringUtils.isNotEmpty(query.getName())) {
	        	dLog.trace("Querying for groups that have criteria | Name: " + query.getName());
	        	groups.add(IdentityUtils.convertGroupType(groupSvc.getGroup(query.getName())));
	        } else if (StringUtils.isNotEmpty(query.getUserId())) {
	        	dLog.trace("Querying for groups that have criteria | User ID: " + query.getUserId());
	            for(Integer i : userSvc.getUser(new Integer(query.getUserId())).getGroupId()) {
	            	groups.add(IdentityUtils.convertGroupType(groupSvc.getGroup(i)));
	            }
	        }
    	}
    	catch(Exception e) {
    		dLog.error("Error in findGroupByQueryCriteria", e);
    	}

        return groups;
    }
    
    public GroupEntity findGroupById(String groupId) {
    	GroupQueryImpl searchQuery = new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.groupId(groupId);

    	List<Group> groups = findGroupByQueryCriteria(searchQuery, null);
    	
    	if(groups.size() > 0) {
    		return (GroupEntity) groups.get(0);
    	}
    	else {
    		return null;
    	}
    }
    
    public GroupEntity findGroupByName(String groupName) {
    	GroupQueryImpl searchQuery = new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
    	searchQuery.groupName(groupName);

    	List<Group> groups = findGroupByQueryCriteria(searchQuery, null);
    	
    	if(groups.size() > 0) {
    		return (GroupEntity) groups.get(0);
    	}
    	else {
    		return null;
    	}
    }

    /**
     * Find group count by query criteria
     */
    @Override
    public long findGroupCountByQueryCriteria(GroupQueryImpl query) {
        return findGroupByQueryCriteria(query, null).size();
    }

    /**
     * Find a group(s) by user membership
     */
    @Override
    public List<Group> findGroupsByUser(String userId) {
        dLog.trace("looking for groups that have user: " + userId);
        
        if (Context.getProcessEngineConfiguration() == null) {
            String msg = "Process engine configuration is wrong";
            dLog.warn(msg);
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("Error", msg);
            throw new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.INTERNAL_SERVER_ERROR).entity(rootNode.toString()).build());
        }
        
        GroupQueryImpl searchQuery = new GroupQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
        searchQuery.groupMember(userId);

        return findGroupByQueryCriteria(searchQuery, null);
    }
}
