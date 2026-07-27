package com.hjs.study.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 运营后台管理令牌过滤器。
 *
 * <p>令牌只从环境配置读取，前端通过 {@code X-Admin-Token} 请求头传递。
 * 使用 {@link MessageDigest#isEqual(byte[], byte[])} 避免普通字符串逐字符比较带来的
 * 明显时序差异。</p>
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/gbm/admin/";
    private static final String TOKEN_HEADER = "X-Admin-Token";

    @Value("${gbm.admin.token:}")
    private String configuredToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isBlank(configuredToken)) {
            writeUnauthorized(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "后台管理令牌尚未配置");
            return;
        }
        boolean matched = requestToken != null && MessageDigest.isEqual(
                sha256(configuredToken),
                sha256(requestToken)
        );
        if (!matched) {
            writeUnauthorized(response, HttpServletResponse.SC_UNAUTHORIZED, "管理令牌无效");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 先固定长度摘要再比较，避免原始令牌长度不同导致比较耗时出现可观察差异。
     */
    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Java 标准运行时必须提供 SHA-256；缺失时拒绝继续鉴权。
            throw new IllegalStateException("当前运行环境不支持SHA-256", e);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        /*
         * 管理页面与后端在开发部署中可能使用不同端口。鉴权过滤器会在 MVC 的
         * CORS 处理之前结束请求，因此这里也要显式返回允许跨域的响应头，否则
         * 浏览器只能看到模糊的“网络异常”，无法向管理员展示准确的令牌错误。
         */
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Vary", "Origin");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"0401\",\"info\":\"" + message + "\",\"data\":null}");
    }
}
