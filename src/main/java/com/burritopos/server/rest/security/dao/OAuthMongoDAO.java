package com.burritopos.server.rest.security.dao;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.burritopos.server.rest.library.BurritoServer;
import com.burritopos.server.service.dao.IUserSvc;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

public class OAuthMongoDAO implements UserDetailsService {
	private static Logger dLog = Logger.getLogger(OAuthMongoDAO.class);
	
	@Autowired
    private IUserSvc userSvc;
    private ArrayList<com.burritopos.server.domain.User> users;
    
    // metrics
    protected final Counter loginCounter = Metrics.newCounter(BurritoServer.class, "oauth-login-counter");

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		dLog.trace("Getting OAuth details for: " + username);
		UserDetails user = null;
		
    	// get all users
    	try {
			users = userSvc.getAllUsers();
		} catch (Exception e2) {
			dLog.error("Unable to get all users", e2);
		}
    	dLog.trace("Got " + users.size() + " users");
		
    	for(int n=0; n<users.size(); n++) {
    		com.burritopos.server.domain.User curUser = users.get(n);
        	dLog.trace("Stored user: " + curUser.getUserName() + " | stored pass: " + curUser.getPassword());
            if(curUser.getUserName().equals(username)) {
				// default all users to ROLE_USER for now
				user = new User(username, curUser.getPassword(), true, true, true, true, AuthorityUtils.createAuthorityList("ROLE_USER"));
				loginCounter.inc();
				
				break;
            }
    	}
		return user;
	}

	

}
