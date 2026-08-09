package com.snapmeal.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DeepSeekClient 离线单测：注入 Mockito RestTemplate，脚本化 /models 与 /chat/completions 行为。 */
class DeepSeekClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BASE = "https://api.deepseek.com";

    private AgentKeyStore keyStore;
    private AgentModelStore modelStore;
    private RestTemplate http;
    private DeepSeekClient client;

    @BeforeEach
    void setUp() {
        keyStore = mock(AgentKeyStore.class);
        modelStore = mock(AgentModelStore.class);
        http = mock(RestTemplate.class);
        client = new DeepSeekClient(BASE, keyStore, modelStore, http);
    }

    @Test
    void configuredDelegatesToKeyStore() {
        when(keyStore.configured()).thenReturn(true);
        assertTrue(client.configured());
        when(keyStore.configured()).thenReturn(false);
        assertFalse(client.configured());
    }

    @Test
    void modelReadsFromModelStore() {
        when(modelStore.current()).thenReturn("deepseek-v4-flash");
        assertEquals("deepseek-v4-flash", client.model());
        when(modelStore.current()).thenReturn("deepseek-v4-pro");
        assertEquals("deepseek-v4-pro", client.model());
    }

    // ── testConnection 错误映射 ─────────────────────────────────

    @Test
    void testConnectionOk() {
        stubModels(new ResponseEntity<>("{\"data\":[]}", HttpStatus.OK));
        DeepSeekClient.TestResult result = client.testConnection("sk-x");
        assertTrue(result.ok);
        assertTrue(result.message.contains("成功"));
    }

    @Test
    void testConnectionMaps401ToAuthFailure() {
        stubModels(new ResponseEntity<>("{}", HttpStatus.UNAUTHORIZED));
        DeepSeekClient.TestResult result = client.testConnection("sk-bad");
        assertFalse(result.ok);
        assertTrue(result.message.contains("认证失败"));
    }

    @Test
    void testConnectionMaps402ToBalance() {
        stubModels(new ResponseEntity<>("{}", HttpStatus.PAYMENT_REQUIRED));
        DeepSeekClient.TestResult result = client.testConnection("sk-x");
        assertFalse(result.ok);
        assertTrue(result.message.contains("余额不足"));
    }

    @Test
    void testConnectionMaps429ToRateLimit() {
        stubModels(new ResponseEntity<>("{}", HttpStatus.TOO_MANY_REQUESTS));
        DeepSeekClient.TestResult result = client.testConnection("sk-x");
        assertFalse(result.ok);
        assertTrue(result.message.contains("限流"));
    }

    @Test
    void testConnectionMapsServerError() {
        stubModels(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));
        DeepSeekClient.TestResult result = client.testConnection("sk-x");
        assertFalse(result.ok);
        assertTrue(result.message.contains("接口返回异常"));
    }

    @Test
    void testConnectionMapsNetworkFailure() {
        when(http.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        DeepSeekClient.TestResult result = client.testConnection("sk-x");
        assertFalse(result.ok);
        assertTrue(result.message.contains("网络"));
    }

    // ── chat 使用 keyStore 当前 Key 作为 Bearer ────────────────

    @Test
    void chatUsesKeyStoreKeyAsBearerToken() throws Exception {
        when(keyStore.current()).thenReturn("sk-bearer-token");
        when(http.exchange(eq(BASE + "/chat/completions"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", HttpStatus.OK));

        ObjectNode payload = JSON.createObjectNode();
        payload.put("model", "deepseek-chat");
        JsonNode response = client.chat(payload);
        assertEquals("ok", response.path("choices").path(0).path("message").path("content").asText());

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(http).exchange(eq(BASE + "/chat/completions"), eq(HttpMethod.POST), captor.capture(), eq(String.class));
        assertEquals("Bearer sk-bearer-token", captor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    private void stubModels(ResponseEntity<String> response) {
        when(http.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);
    }
}
