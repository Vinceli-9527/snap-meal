package com.snapmeal.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agent Key/模型接口离线测试：MockMvc + @MockBean DeepSeekClient，不发起网络调用。
 * key-file 用随机后缀保证每次运行上下文启动时都是空文件；方法按 @Order 排序，
 * 保存 Key/模型的用例放在最后，避免污染同一上下文中的未配置断言。
 */
@SpringBootTest(properties = {
        "sky.agent.api-key=",
        "sky.agent.base-url=http://127.0.0.1:1",
        "sky.agent.key-file=target/agent-key-api-test-${random.uuid}.env"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentKeyApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @MockBean
    DeepSeekClient client;

    @Test
    @Order(1)
    void getKeyReportsUnconfigured() throws Exception {
        String body = mvc.perform(get("/api/admin/agent/key").header("token", login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.masked").value(""))
                .andReturn().getResponse().getContentAsString();
        assertEquals("deepseek-v4-flash", json.readTree(body).path("data").path("model").asText());
    }

    @Test
    @Order(2)
    void saveKeyRejectsPlaceholder() throws Exception {
        mvc.perform(post("/api/admin/agent/key").header("token", login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-your-key-here\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(3)
    void testKeyReturnsClientResult() throws Exception {
        when(client.testConnection(anyString()))
                .thenReturn(DeepSeekClient.TestResult.ok("连接成功，可用模型列表已获取"));
        mvc.perform(post("/api/admin/agent/key/test").header("token", login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-anything\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.message").isNotEmpty());
    }

    @Test
    @Order(4)
    void saveKeyPersistsAndUpdatesStatus() throws Exception {
        String token = login();
        mvc.perform(post("/api/admin/agent/key").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-saved-key-00000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.masked").value("sk-****0000"));
        mvc.perform(get("/api/admin/agent/key").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    @Order(5)
    void keyApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/agent/key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void getModelReportsDefaultAndOptions() throws Exception {
        String body = mvc.perform(get("/api/admin/agent/model").header("token", login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.data.models[0]").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.data.models[1]").value("deepseek-v4-pro"))
                .andReturn().getResponse().getContentAsString();
        assertEquals("deepseek-v4-flash", json.readTree(body).path("data").path("model").asText());
    }

    @Test
    @Order(7)
    void saveModelSwitchesAndPersists() throws Exception {
        String token = login();
        mvc.perform(post("/api/admin/agent/model").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"deepseek-v4-pro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("deepseek-v4-pro"));
        mvc.perform(get("/api/admin/agent/model").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("deepseek-v4-pro"));
    }

    @Test
    @Order(8)
    void saveModelRejectsUnsupported() throws Exception {
        mvc.perform(post("/api/admin/agent/model").header("token", login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"deepseek-chat\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(9)
    void modelApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/agent/model"))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
