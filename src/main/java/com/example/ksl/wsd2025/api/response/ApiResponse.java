package com.example.ksl.wsd2025.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// 표준 응답 포맷
// 성공: { "status": "success", "code": 200, "message": "...", "data": { ... } }
// 실패: { "status": "error", "code": 404, "message": "...", "errors": { ... } }
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에서 제외
public class ApiResponse<T> {
    private final String status;
    private final Integer code;
    private final String message;
    private final T data;
    private final Object errors;

    private ApiResponse(String status, Integer code, String message, T data, Object errors) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    // 200 OK
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", 200, null, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", 200, message, data, null);
    }

    // 201 Created
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>("success", 201, message, data, null);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>("error", code, message, null, null);
    }

    public static ApiResponse<Void> error(int code, String message, Object errors) {
        return new ApiResponse<>("error", code, message, null, errors);
    }

    // getter
    public String getStatus() { return status; }
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Object getErrors() { return errors; }
}
