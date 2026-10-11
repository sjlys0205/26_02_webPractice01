package com.example.ksl.wsd2025.config;

import com.example.ksl.wsd2025.api.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

// 서비스 상태 확인 목적 미들웨어
// 1) 점검 모드(app.maintenance-mode=true)이면 모든 API 요청을 503으로 차단
// 2) 테스트용 헤더 X-Simulate-Error: 500 또는 503 으로 서버 에러 응답 확인
@Component
public class ServiceStatusInterceptor implements HandlerInterceptor {

    private final boolean maintenanceMode;
    private final ObjectMapper objectMapper;

    public ServiceStatusInterceptor(
            @Value("${app.maintenance-mode:false}") boolean maintenanceMode,
            ObjectMapper objectMapper
    ) {
        this.maintenanceMode = maintenanceMode;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        String simulate = request.getHeader("X-Simulate-Error");

        if (maintenanceMode || "503".equals(simulate)) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Service is under maintenance. Please try again later."));
            return false; // 컨트롤러로 넘기지 않음
        }

        if ("500".equals(simulate)) {
            // GlobalExceptionHandler가 500 응답으로 변환
            throw new IllegalStateException("Simulated server error");
        }

        return true;
    }
}
