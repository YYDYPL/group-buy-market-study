package com.hjs.study.test.config;

import com.hjs.study.config.AdminTokenFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 运营后台管理令牌过滤器测试。
 */
public class AdminTokenFilterTest {

    private AdminTokenFilter filter;

    @Before
    public void setUp() {
        filter = new AdminTokenFilter();
        ReflectionTestUtils.setField(filter, "configuredToken", "correct-token");
    }

    @Test
    public void shouldRejectMissingTokenWithCorsHeaders() throws Exception {
        MockHttpServletResponse response = execute(null);
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
        Assert.assertTrue(response.getContentAsString().contains("\"0401\""));
    }

    @Test
    public void shouldRejectWrongToken() throws Exception {
        Assert.assertEquals(401, execute("wrong-token").getStatus());
    }

    @Test
    public void shouldPassCorrectToken() throws Exception {
        Assert.assertEquals(200, execute("correct-token").getStatus());
    }

    @Test
    public void shouldRefuseAdminRequestsWhenServerTokenIsBlank() throws Exception {
        ReflectionTestUtils.setField(filter, "configuredToken", "");
        Assert.assertEquals(503, execute("anything").getStatus());
    }

    private MockHttpServletResponse execute(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/gbm/admin/products");
        if (token != null) request.addHeader("X-Admin-Token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
