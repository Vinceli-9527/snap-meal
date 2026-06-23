package com.snapmeal.common;
public class ApiResponse<T> {
    private final boolean success; private final String message; private final T data;
    private ApiResponse(boolean success, String message, T data) { this.success=success; this.message=message; this.data=data; }
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true,"操作成功",data); }
    public static ApiResponse<Void> ok() { return new ApiResponse<>(true,"操作成功",null); }
    public static ApiResponse<Void> fail(String message) { return new ApiResponse<>(false,message,null); }
    public boolean isSuccess(){return success;} public String getMessage(){return message;} public T getData(){return data;}
}
