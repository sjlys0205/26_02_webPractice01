package com.example.ksl.wsd2025.api.exception;

import org.springframework.http.HttpStatus;

// 컨트롤러에서 원하는 HTTP 상태코드로 에러를 던질 때 사용 (예: 404, 409)
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
