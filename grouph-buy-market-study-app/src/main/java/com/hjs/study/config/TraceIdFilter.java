package com.hjs.study.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 为每个 HTTP 请求建立日志链路标识的过滤器。
 *
 * <p>过滤器继承 {@link OncePerRequestFilter}，保证一次请求派发只执行一次。生成的 trace-id 写入
 * SLF4J MDC 后，会被 Logback 日志格式中的 {@code %X{trace-id}} 自动输出，便于关联同一请求的
 * 多条日志。</p>
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    /** MDC 中保存链路标识的键，需与 logback-spring.xml 的日志模板保持一致。 */
    private static final String TRACE_ID = "trace-id";

    /**
     * 在过滤器链执行期间绑定 trace-id，并在请求结束时清理线程上下文。
     *
     * <p>Servlet 容器会复用工作线程，因此必须在 {@code finally} 中清空 MDC，防止后续请求
     * 继承前一个请求的链路标识。</p>
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain 后续 Servlet 过滤器及目标控制器组成的调用链
     * @throws ServletException 下游 Servlet 处理失败
     * @throws IOException      请求或响应读写失败
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 每个请求生成独立 UUID，并在进入业务代码前放入当前线程的 MDC。
            String traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            // 无论业务成功还是异常都清理 MDC，避免线程池复用导致链路串号。
            MDC.clear();
        }
    }

}
