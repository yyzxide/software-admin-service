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
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        log.warn("Admin service request validation failed: {}", ex.getMessage());
        return ApiResponse.error(ErrorCode.PARAM_FORMAT, firstValidationMessage(ex));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Admin service upload exceeded size limit: {}", ex.getMessage());
        return ApiResponse.error(ErrorCode.PARAM_FORMAT, "安装包文件过大");
    }

    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipart(MultipartException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        log.warn("Admin service multipart request failed: {}", root.getMessage());
        return ApiResponse.error(ErrorCode.PARAM_FORMAT, "安装包上传请求格式错误");
    }

    @ExceptionHandler(DataAccessException.class)
    public ApiResponse<Void> handleDataAccess(DataAccessException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        log.error("Admin service database error: {}", root.getMessage(), ex);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "数据库表结构或SQL执行异常，请查看后端日志");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        log.error("Unexpected admin service error", ex);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统异常");
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
