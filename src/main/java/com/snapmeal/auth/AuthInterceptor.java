package com.snapmeal.auth;
import org.springframework.stereotype.Component; import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.*;
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SUBJECT_ID="subjectId"; private final TokenService tokens;
    public AuthInterceptor(TokenService tokens){this.tokens=tokens;}
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){boolean admin=request.getRequestURI().startsWith("/api/admin/");TokenService.Session s=tokens.verify(request.getHeader(admin?"token":"authentication"),admin?"ADMIN":"USER");request.setAttribute(SUBJECT_ID,s.getSubjectId());return true;}
}
