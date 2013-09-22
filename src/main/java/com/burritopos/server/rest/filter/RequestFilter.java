package com.burritopos.server.rest.filter;


import org.apache.log4j.Logger;

import javax.servlet.*;
import java.io.IOException;

/**
 * Filter example
 *
 */
public class RequestFilter implements Filter {
    private static Logger logger = Logger.getLogger(RequestFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        logger.trace("Doing some filtering");

        // TODO: do any pre-processing of the incoming requests to the REST services here
        
        
        // Make sure to hand off to the next filter in the chain, defined in web.xml
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}