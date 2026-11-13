package com.smartagri.common.exception;

import com.smartagri.common.result.Result;
import com.smartagri.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.neo4j.driver.exceptions.SecurityException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: {}", exception.getMessage());
        return fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return fail(ResultCode.BAD_REQUEST.getCode(), "缺少请求参数: " + exception.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return fail(ResultCode.BAD_REQUEST.getCode(), "不支持的请求方法: " + exception.getMethod());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Result<Void>> handleNeo4jSecurityException(SecurityException exception) {
        log.warn("Neo4j 认证失败: {}", exception.getMessage());
        String hint = "Neo4j 用户名或密码错误：请设置环境变量 SMART_AGRI_NEO4J_PASSWORD（或 NEO4J_PASSWORD），"
                + "与 Neo4j 实例或 docker-compose 中 NEO4J_AUTH 的密码部分完全一致。"
                + " 若数据卷曾用其它密码初始化，仅改 compose 不会改库内口令，需用原密码或删卷重建。"
                + " 若提示尝试次数过多，请重启 Neo4j 容器后再试。";
        return fail(ResultCode.SYSTEM_ERROR.getCode(), hint);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("系统异常", exception);
        return fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage());
    }

    private ResponseEntity<Result<Void>> fail(Integer code, String message) {
        Integer resolvedCode = code == null ? ResultCode.SYSTEM_ERROR.getCode() : code;
        HttpStatus status = HttpStatus.resolve(resolvedCode);
        if (status == null || status.is2xxSuccessful() || status.is3xxRedirection()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(Result.fail(resolvedCode, message));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + Objects.requireNonNullElse(fieldError.getDefaultMessage(), "参数校验失败");
    }
}
