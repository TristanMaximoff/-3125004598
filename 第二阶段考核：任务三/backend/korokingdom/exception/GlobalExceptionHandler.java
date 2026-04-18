package com.game.korokingdom.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice           //spring框架的全局异常处理
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)      //bussinessexception异常
    public Map<String, Object> handleBusinessException(BusinessException e) {

        Map<String, Object> result = new HashMap<>();
        result.put("code", e.getCode());
        result.put("message", e.getMessage());
        result.put("success", false);

        return result;
    }

    @ExceptionHandler(Exception.class)      //所有异常
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "系统错误：" + e.getMessage());
        result.put("success", false);
        return result;
    }
}