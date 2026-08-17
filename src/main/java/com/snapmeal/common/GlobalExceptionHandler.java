package com.snapmeal.common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(UnauthorizedException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> unauthorized(UnauthorizedException e){return ApiResponse.fail(e.getMessage());}
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> business(BusinessException e){return ApiResponse.fail(e.getMessage());}
    @ExceptionHandler(TooManyRequestsException.class) @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> tooManyRequests(TooManyRequestsException e){return ApiResponse.fail(e.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException e){return ApiResponse.fail(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());}
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> unknown(Exception e){log.error("未处理异常",e);return ApiResponse.fail("服务器内部错误，请稍后重试");}
}
