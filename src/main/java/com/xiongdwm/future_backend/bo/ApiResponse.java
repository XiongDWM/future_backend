package com.xiongdwm.future_backend.bo;

public class ApiResponse<T>{
    
    private final T data;
    private final int code;
    private final boolean success;

    public ApiResponse(T data, int code, boolean success) {
        this.data = data;
        this.code = code;
        this.success = success;
    }

    public enum ApiResponseCode {
        INTERAL_SERVER_ERROR(500,"Internal Server Error"),
        SUCCESS(200,"Success"),
        BUSSINESS_ERROR(555,"Business Error"),
        KEY_EXPIRED(556,"Key Expired"),
        UNAUTHORIZED(401,"Unauthorized");

        private final int code;
        private final String message;

        ApiResponseCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
    }

    public static ApiResponse<String> success() {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getMessage(), ApiResponseCode.SUCCESS.getCode(), true);
    }

    public static ApiResponse<String> key_expired() {
        return new ApiResponse<>(ApiResponseCode.KEY_EXPIRED.getMessage(), ApiResponseCode.KEY_EXPIRED.getCode(), false);
    }

    public static<T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiResponseCode.SUCCESS.getCode(), true);
    }

    public static<T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(data, ApiResponseCode.INTERAL_SERVER_ERROR.getCode(), false);
    }

    public static ApiResponse<String> error() {
        return new ApiResponse<>(ApiResponseCode.INTERAL_SERVER_ERROR.getMessage(), ApiResponseCode.INTERAL_SERVER_ERROR.getCode(), false);
    }

    public static<T> ApiResponse<T> bussiness_error(T data){
        return new ApiResponse<>(data, ApiResponseCode.BUSSINESS_ERROR.getCode(), false);
    } 
    
    public static ApiResponse<String> unauthorized(){
        return new ApiResponse<>(ApiResponseCode.UNAUTHORIZED.getMessage(), ApiResponseCode.UNAUTHORIZED.getCode(), false);
    }

    public static ApiResponse<String> unauthorized(String message){
        return new ApiResponse<>(message, ApiResponseCode.UNAUTHORIZED.getCode(), false);
    }

    public T getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public boolean isSuccess() {
        return success;
    }
}
