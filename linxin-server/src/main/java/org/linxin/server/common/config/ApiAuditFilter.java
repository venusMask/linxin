package org.linxin.server.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@Order(1)
public class ApiAuditFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 包装请求和响应以支持多次读取内容
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String method = requestWrapper.getMethod();
            String uri = requestWrapper.getRequestURI();
            String queryString = requestWrapper.getQueryString();
            int status = responseWrapper.getStatus();

            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            // 打印到专用日志文件中
            log.info("\n--- API AUDIT START ---\n" +
                    "Time: {} ms\n" +
                    "Endpoint: {} {}\n" +
                    "Query: {}\n" +
                    "Request: {}\n" +
                    "Status: {}\n" +
                    "Response: {}\n" +
                    "--- API AUDIT END ---",
                    duration, method, uri, queryString, requestBody, status, responseBody);

            // 必须把内容拷贝回原响应，否则客户端收不到数据
            responseWrapper.copyBodyToResponse();
        }
    }
}
