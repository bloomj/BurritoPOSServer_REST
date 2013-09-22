package com.burritopos.server.rest.test.filter;

import com.burritopos.server.rest.filter.RequestFilter;
import com.burritopos.server.rest.test.IntegrationTests;
import com.burritopos.server.rest.test.BaseTestCase;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.mock.web.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
/**
 * Test class for RequestFilter filter class
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class RequestFilterTest extends BaseTestCase {
    /**
     * Tests for successful doFilter method of servlet filter
     *
     * @throws Exception
     */
    @Test
    @Category(IntegrationTests.class)
    public void testDoFilter() throws Exception {
        final MockRequestDispatcher requestDispatcher = new MockRequestDispatcher("");

        // http servlet mocked for mocked dispatcher
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public RequestDispatcher getRequestDispatcher(String mappingURI) {
                setAttribute("mappingURI", mappingURI);
                return requestDispatcher;
            }
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterConfig config = new MockFilterConfig();
        final RequestFilter initFilter = new RequestFilter();

        FilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                try {
                    initFilter.doFilter(req, res, new MockFilterChain());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        request.setRequestURI("http://localhost:8443/BurritoPOSServer_REST/login");

        //verify doFilter did not throw an error.
        initFilter.init(config);

        initFilter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }
}
