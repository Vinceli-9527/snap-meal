package com.snapmeal.ratelimit;

import com.snapmeal.auth.AuthInterceptor;
import com.snapmeal.common.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitInterceptorTest {

    @Test
    void allowsUpToCapacityThenThrottles() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2);
        MockHttpServletRequest req = request("POST", 1L);
        assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), null));
        assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), null));
        assertThrows(TooManyRequestsException.class,
                () -> interceptor.preHandle(req, new MockHttpServletResponse(), null));
    }

    @Test
    void bucketsAreIsolatedPerUser() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1);
        assertTrue(interceptor.preHandle(request("POST", 1L), new MockHttpServletResponse(), null));
        assertTrue(interceptor.preHandle(request("POST", 2L), new MockHttpServletResponse(), null));
        assertThrows(TooManyRequestsException.class,
                () -> interceptor.preHandle(request("POST", 1L), new MockHttpServletResponse(), null));
    }

    @Test
    void nonPostRequestsAreNotLimited() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1);
        assertTrue(interceptor.preHandle(request("GET", 1L), new MockHttpServletResponse(), null));
    }

    private static MockHttpServletRequest request(String method, Long userId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        if (userId != null) {
            req.setAttribute(AuthInterceptor.SUBJECT_ID, userId);
        }
        return req;
    }
}
