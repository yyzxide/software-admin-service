package com.xcappstore.admin.exception;

import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return error(statusFor(ex.getCode()), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("Admin service request validation failed: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ErrorCode.PARAM_FORMAT, firstValidationMessage(ex));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Admin service upload exceeded size limit: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ErrorCode.PARAM_FORMAT, "安装包文件过大");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipart(MultipartException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        log.warn("Admin service multipart request failed: {}", root.getMessage());
        return error(HttpStatus.BAD_REQUEST, ErrorCode.PARAM_FORMAT, "安装包上传请求格式错误");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        log.error("Admin service database error: {}", root.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "数据库表结构或SQL执行异常，请查看后端日志");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected admin service error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "系统异常");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }

    private HttpStatus statusFor(int code) {
        return switch (code) {
            case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case ErrorCode.PARAM_FORMAT -> HttpStatus.BAD_REQUEST;
            case ErrorCode.RESOURCE_NOT_FOUND,
                 ErrorCode.CATEGORY_NOT_FOUND,
                 ErrorCode.TAG_NOT_FOUND,
                 ErrorCode.SOFTWARE_NOT_FOUND,
                 ErrorCode.REVIEW_TASK_NOT_FOUND,
                 ErrorCode.OPERATION_LOG_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.DUPLICATE_RESOURCE,
                 ErrorCode.CATEGORY_NAME_EXISTS,
                 ErrorCode.TAG_NAME_EXISTS -> HttpStatus.CONFLICT;
            case ErrorCode.INVALID_STATUS_FLOW,
                 ErrorCode.SOFTWARE_INVALID_STATUS,
                 ErrorCode.REVIEW_INVALID_STATUS,
                 ErrorCode.CATEGORY_DEPTH_EXCEEDED,
                 ErrorCode.CATEGORY_HAS_CHILDREN,
                 ErrorCode.CATEGORY_HAS_APPS,
                 ErrorCode.TAG_HAS_APPS -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String firstValidationMessage(Exception ex) {
        BindingResult bindingResult = null;
        if (ex instanceof BindException bindException) {
            bindingResult = bindException.getBindingResult();
        } else if (ex instanceof MethodArgumentNotValidException argumentNotValidException) {
            bindingResult = argumentNotValidException.getBindingResult();
        }
        if (bindingResult != null && bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }
        return "参数格式错误";
    }
}
