package com.snapmeal.agent;

import com.snapmeal.common.ApiResponse;
import com.snapmeal.common.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 经营问答（Text2SQL Agent）接口，挂载在 /api/admin/** 下，自动受登录鉴权保护。 */
@RestController
@RequestMapping("/api/admin/agent")
public class AgentController {

    private final AgentService agent;
    private final AgentKeyStore keyStore;
    private final AgentModelStore modelStore;
    private final DeepSeekClient client;

    public AgentController(AgentService agent, AgentKeyStore keyStore, AgentModelStore modelStore, DeepSeekClient client) {
        this.agent = agent;
        this.keyStore = keyStore;
        this.modelStore = modelStore;
        this.client = client;
    }

    public static class ChatRequest {
        public String question;
    }

    public static class KeyRequest {
        public String apiKey;
    }

    public static class ModelRequest {
        public String model;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentService.AgentReply> chat(@RequestBody ChatRequest request) {
        if (request.question == null || request.question.trim().isEmpty()) {
            throw new BusinessException("请输入经营问题");
        }
        return ApiResponse.ok(agent.answer(request.question.trim()));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(info());
    }

    @GetMapping("/key")
    public ApiResponse<Map<String, Object>> getKey() {
        return ApiResponse.ok(info());
    }

    @PostMapping("/key")
    public ApiResponse<Map<String, Object>> saveKey(@RequestBody KeyRequest request) {
        if (AgentKeyStore.isPlaceholder(request.apiKey)) {
            throw new BusinessException("请粘贴真实的 DeepSeek API Key，不要留空或使用示例占位符");
        }
        keyStore.set(request.apiKey);
        return ApiResponse.ok(info());
    }

    @PostMapping("/key/test")
    public ApiResponse<Map<String, Object>> testKey(@RequestBody KeyRequest request) {
        if (AgentKeyStore.isPlaceholder(request.apiKey)) {
            throw new BusinessException("请先粘贴要测试的 API Key");
        }
        DeepSeekClient.TestResult result = client.testConnection(request.apiKey.trim());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", result.ok);
        info.put("message", result.message);
        return ApiResponse.ok(info);
    }

    @GetMapping("/model")
    public ApiResponse<Map<String, Object>> getModel() {
        return ApiResponse.ok(modelInfo());
    }

    @PostMapping("/model")
    public ApiResponse<Map<String, Object>> saveModel(@RequestBody ModelRequest request) {
        if (!AgentModelStore.isValid(request.model)) {
            throw new BusinessException("不支持的模型，可选：" + String.join(" / ", AgentModelStore.AVAILABLE));
        }
        modelStore.set(request.model.trim());
        return ApiResponse.ok(modelInfo());
    }

    private Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("configured", keyStore.configured());
        info.put("masked", keyStore.masked());
        info.put("model", modelStore.current());
        info.put("models", AgentModelStore.AVAILABLE);
        info.put("keyfile", keyStore.keyFile());
        return info;
    }

    private Map<String, Object> modelInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("model", modelStore.current());
        info.put("models", AgentModelStore.AVAILABLE);
        return info;
    }
}
