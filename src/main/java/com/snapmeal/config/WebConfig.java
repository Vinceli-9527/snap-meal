package com.snapmeal.config;
import com.snapmeal.auth.AuthInterceptor; import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*; import java.nio.file.Paths;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth; public WebConfig(AuthInterceptor auth){this.auth=auth;}
    @Override public void addInterceptors(InterceptorRegistry r){r.addInterceptor(auth).addPathPatterns("/api/admin/**").excludePathPatterns("/api/admin/auth/login");r.addInterceptor(auth).addPathPatterns("/api/user/**").excludePathPatterns("/api/user/auth/login","/api/user/catalog/**","/api/user/shop/status");}
    @Override public void addResourceHandlers(ResourceHandlerRegistry r){r.addResourceHandler("/uploads/**").addResourceLocations(Paths.get("uploads").toAbsolutePath().toUri().toString());}
}
