package com.example.ksl.wsd2025.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final ServiceStatusInterceptor serviceStatusInterceptor;

    @Autowired
    public WebConfig(LoggingInterceptor loggingInterceptor, ServiceStatusInterceptor serviceStatusInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.serviceStatusInterceptor = serviceStatusInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 등록 순서대로 실행: 로깅 → 서비스 상태 확인 (503도 로그에 남도록 로깅을 먼저 등록)
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")      // 적용할 URL 패턴
                .excludePathPatterns("/health"); // 제외할 URL 패턴 (예시)

        registry.addInterceptor(serviceStatusInterceptor)
                .addPathPatterns("/api/**");
    }
}
