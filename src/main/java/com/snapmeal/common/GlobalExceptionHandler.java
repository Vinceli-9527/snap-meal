package com.snapmeal.common;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnauthorizedException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> unauthorized(UnauthorizedException e){return ApiResponse.fail(e.getMessage());}
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> business(BusinessException e){return ApiResponse.fail(e.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException e){return ApiResponse.fail(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());}
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> unknown(Exception e){return ApiResponse.fail("服务器处理失败："+e.getMessage());}
}
