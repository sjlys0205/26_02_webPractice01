package com.example.ksl.wsd2025.config;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(START_TIME, System.currentTimeMillis()); // 처리 시간 측정용

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        System.out.println("[LoggingInterceptor] 요청 시작: " + method + " " + uri +
                (query != null ? "?" + query : "") +
                " (client=" + request.getRemoteAddr() + ")");
        return true; // false로 하면 컨트롤러로 안 넘어감
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        long elapsed = System.currentTimeMillis() - (Long) request.getAttribute(START_TIME);
        System.out.println("[LoggingInterceptor] 요청 완료: " + request.getMethod() + " " + request.getRequestURI() +
                ", status=" + response.getStatus() + ", time=" + elapsed + "ms");
    }
}
