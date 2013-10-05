/**
 * 
 */
package com.burritopos.server.rest.webresource;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import com.burritopos.server.rest.utilities.BurritoPOSUtils;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.reporting.GraphiteReporter;
import com.yammer.metrics.util.DeadlockHealthCheck;

/**
 * Initialize yammer.metrics health checks on webapp startup
 */
public class ContextListener implements ServletContextListener {
	private static Logger dLog = Logger.getLogger(ContextListener.class);
	
    private static String graphiteServerIP;
    private static int graphiteServerPort;
    
    public void contextInitialized(ServletContextEvent event) {
        // register health checks here
        Reflections reflections = new Reflections("com.burritopos.server.rest.metrics");

        Set<Class<? extends HealthCheck>> healthChecks = reflections.getSubTypesOf(HealthCheck.class);
        for(Class<? extends Object> check  : healthChecks) {
        	dLog.trace("Registering " + check.getSimpleName() + " with metrics HealthChecks singleton");
            try {
				HealthChecks.register((HealthCheck) check.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				dLog.trace("Unable to register " + check.getSimpleName(), e);
			}
        }
        dLog.trace("Registering DeadlockHealthCheck with metrics HealthChecks singleton");
        HealthChecks.register(new DeadlockHealthCheck());

        try {
	        // stream to Graphite
	        graphiteServerIP = BurritoPOSUtils.getProperty("graphite.server.ip");
	
	        if (graphiteServerIP != null && BurritoPOSUtils.getProperty("graphite.server.port") != null) {
	        	try {
	        		graphiteServerPort = Integer.parseInt(BurritoPOSUtils.getProperty("graphite.server.port"));
	        		dLog.trace("Initializing Graphite stream: " + graphiteServerIP + ":" + graphiteServerPort);
	                GraphiteReporter.enable(1, TimeUnit.MINUTES, graphiteServerIP, graphiteServerPort);
	            } catch (Exception e) {
	            	dLog.error("Error initializing GraphiteReporter", e);
	            }
	        }
        }
        catch(Exception e) {
        	dLog.error("Error initializing BurritoPOS Service", e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
    	
    }

}
