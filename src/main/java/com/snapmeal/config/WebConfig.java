package com.snapmeal.config;
import com.snapmeal.auth.AuthInterceptor; import com.snapmeal.ratelimit.RateLimitInterceptor; import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*; import java.nio.file.Paths;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth; private final RateLimitInterceptor rateLimit; public WebConfig(AuthInterceptor auth,RateLimitInterceptor rateLimit){this.auth=auth;this.rateLimit=rateLimit;}
    @Override public void addInterceptors(InterceptorRegistry r){r.addInterceptor(auth).addPathPatterns("/api/admin/**").excludePathPatterns("/api/admin/auth/login");r.addInterceptor(auth).addPathPatterns("/api/user/**").excludePathPatterns("/api/user/auth/login","/api/user/catalog/**","/api/user/shop/status");r.addInterceptor(rateLimit).addPathPatterns("/api/user/orders");}
    @Override public void addResourceHandlers(ResourceHandlerRegistry r){r.addResourceHandler("/uploads/**").addResourceLocations(Paths.get("uploads").toAbsolutePath().toUri().toString());}
}
