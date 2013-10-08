package com.burritopos.server.rest.security.dao;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.burritopos.server.rest.library.BurritoServer;
import com.burritopos.server.service.dao.IGroupSvc;
import com.burritopos.server.service.dao.IUserSvc;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

public class OAuthMongoDAO implements UserDetailsService {
	private static Logger dLog = Logger.getLogger(OAuthMongoDAO.class);
	
	@Autowired
    private IUserSvc userSvc;
	@Autowired
	private IGroupSvc groupSvc;
    
    // metrics
    protected final Counter loginCounter = Metrics.newCounter(BurritoServer.class, "oauth-login-counter");

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		dLog.trace("Getting OAuth details for: " + username);
		UserDetails user = null;
		
		try {
			com.burritopos.server.domain.User tUser = userSvc.getUser(username);
			
			if(tUser != null && tUser.validate()) {
				// get all groups (e.g. roles)
				String[] roles = new String[tUser.getGroupId().size()];
				dLog.trace("Roles to add: " + tUser.getGroupId().size());
				for(int i=0; i<tUser.getGroupId().size(); i++) {
					roles[i] = groupSvc.getGroup(tUser.getGroupId().get(i)).getName();
					dLog.trace("Added role: " + roles[i]);
				}
				
				user = new User(username, tUser.getPassword(), true, true, true, true, AuthorityUtils.createAuthorityList(roles));
				loginCounter.inc();
			}
			else {
				throw new UsernameNotFoundException("Invalid user: " + username);
			}
		}
		catch(UsernameNotFoundException e) {
			throw e;
		}
		catch(Exception e) {
			dLog.trace("Error in loadUserByUsername", e);
		}
		
		return user;
	}
}
