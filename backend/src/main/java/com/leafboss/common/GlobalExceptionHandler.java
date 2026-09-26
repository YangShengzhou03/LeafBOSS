package com.leafboss.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NullPointerException.class)
    public Result<Object> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("请求URL: {} 空指针异常", request.getRequestURL(), e);
        return Result.serverError();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("请求URL: {} 参数异常: {}", request.getRequestURL(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("请求URL: {} 参数校验失败: {}", request.getRequestURL(), msg);
        return Result.error(400, msg);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Object> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("请求URL: {} 运行时异常", request.getRequestURL(), e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("请求URL: {} 未知异常", request.getRequestURL(), e);
        return Result.serverError();
    }
}
