package com.snapmeal.common;
import org.springframework.core.MethodParameter;import org.springframework.http.*;import org.springframework.http.converter.HttpMessageConverter;import org.springframework.http.server.ServerHttpRequest;import org.springframework.http.server.ServerHttpResponse;import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;import java.util.*;
@RestControllerAdvice
public class MapKeyNormalizerAdvice implements ResponseBodyAdvice<Object> {
    public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c){return true;}
    public Object beforeBodyWrite(Object body,MethodParameter p,MediaType m,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest req,ServerHttpResponse res){normalize(body);return body;}
    @SuppressWarnings("unchecked") private void normalize(Object value){if(value instanceof ApiResponse){normalize(((ApiResponse<?>)value).getData());}else if(value instanceof Map){Map<String,Object> map=(Map<String,Object>)value;Map<String,Object> copy=new LinkedHashMap<>(map);map.clear();for(Map.Entry<String,Object> e:copy.entrySet()){normalize(e.getValue());map.put(e.getKey().toLowerCase(),e.getValue());}}else if(value instanceof Iterable)for(Object item:(Iterable<?>)value)normalize(item);}
}
