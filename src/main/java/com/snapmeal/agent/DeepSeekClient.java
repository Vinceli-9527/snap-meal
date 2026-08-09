package com.snapmeal.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snapmeal.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 极简 DeepSeek（OpenAI 兼容）Chat Completions 客户端，RestTemplate + Jackson；API Key 与模型均实时取自运行时存储。 */
@Component
public class DeepSeekClient {

    private final RestTemplate http;
    private final ObjectMapper json;
    private final String baseUrl;
    private final AgentKeyStore keyStore;
    private final AgentModelStore modelStore;

    @Autowired
    public DeepSeekClient(
            @Value("${sky.agent.base-url:https://api.deepseek.com}") String baseUrl,
            AgentKeyStore keyStore,
            AgentModelStore modelStore) {
        this(baseUrl, keyStore, modelStore, new RestTemplate(configuredFactory()));
    }

    /** 测试用包私有构造函数：可注入自定义 RestTemplate 以离线脚本化网络行为。 */
    DeepSeekClient(String baseUrl, AgentKeyStore keyStore, AgentModelStore modelStore, RestTemplate http) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.keyStore = keyStore;
        this.modelStore = modelStore;
        this.json = new ObjectMapper();
        this.http = http;
    }

    private static SimpleClientHttpRequestFactory configuredFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        return factory;
    }

    public boolean configured() {
        return keyStore.configured();
    }

    public String model() {
        return modelStore.current();
    }

    public JsonNode chat(ObjectNode payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(keyStore.current());
        String body;
        try {
            body = json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException("序列化请求失败：" + e.getMessage());
        }
        ResponseEntity<String> response;
        try {
            response = http.exchange(baseUrl + "/chat/completions", HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            throw new BusinessException("无法连接 DeepSeek：" + e.getMessage());
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException("DeepSeek 接口返回异常：" + response.getStatusCode());
        }
        try {
            return json.readTree(response.getBody());
        } catch (Exception e) {
            throw new BusinessException("解析 DeepSeek 响应失败");
        }
    }

    /** 用给定 Key 探测 /models 联通性，映射常见失败原因，不发聊天请求。 */
    public TestResult testConnection(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        try {
            ResponseEntity<String> response = http.exchange(baseUrl + "/models", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return TestResult.ok("连接成功，可用模型列表已获取");
            }
            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return TestResult.fail("API Key 认证失败：请检查 Key 是否填写正确");
            }
            if (response.getStatusCode().value() == 402) {
                return TestResult.fail("账户余额不足，无法调用");
            }
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return TestResult.fail("请求过于频繁（限流），请稍后再试");
            }
            return TestResult.fail("DeepSeek 接口返回异常：" + response.getStatusCode());
        } catch (Exception e) {
            return TestResult.fail("无法连接 DeepSeek：请检查网络或服务状态");
        }
    }

    /** 连接测试结果：ok=true 可继续提问。 */
    public static class TestResult {
        public final boolean ok;
        public final String message;

        private TestResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static TestResult ok(String message) {
            return new TestResult(true, message);
        }

        public static TestResult fail(String message) {
            return new TestResult(false, message);
        }
    }
}
