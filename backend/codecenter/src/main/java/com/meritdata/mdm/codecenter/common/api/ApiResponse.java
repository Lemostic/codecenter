package com.meritdata.mdm.codecenter.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {
    private Boolean success;
    private String message;
    private T data;
    private String errorCode;
    private Long timestamp;

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, "OK", null, null, System.currentTimeMillis());
    }
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null, System.currentTimeMillis());
    }
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, System.currentTimeMillis());
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, "CODECENTER-SYS-9999", System.currentTimeMillis());
    }
    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode, System.currentTimeMillis());
    }
}
