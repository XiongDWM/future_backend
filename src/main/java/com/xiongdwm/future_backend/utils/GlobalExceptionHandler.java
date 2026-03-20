package com.xiongdwm.future_backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.utils.exception.AuthenticationFailException;
import com.xiongdwm.future_backend.utils.exception.ServiceException;



@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public ApiResponse<String> handleServiceException(ServiceException e) {
        logger.error("Service Exception: {}", e.getLocalizedMessage());
        return ApiResponse.bussiness_error(e.getLocalizedMessage());
    }

    @ExceptionHandler(AuthenticationFailException.class)
    public ApiResponse<String> handleUnAuthorizedException(AuthenticationFailException e) {
        logger.error("Unauthorized Exception: {}", e.getLocalizedMessage());
        return ApiResponse.unauthorized();
    }
    
}
