package com.snapmeal.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Agent 离线集成测试：强制空 API Key，验证未配置降级、登录鉴权；不发起任何网络调用。 */
@SpringBootTest(properties = {
        "sky.agent.api-key=",
        "sky.agent.base-url=http://127.0.0.1:1",
        "sky.agent.key-file=target/agent-test-offline.env"
})
@AutoConfigureMockMvc
class AgentOfflineTests {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void agentStatusReportsNotConfigured() throws Exception {
        String token = login();
        mvc.perform(get("/api/admin/agent/status").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false));
    }

    @Test
    void chatWithoutKeyReturnsFriendlyErrorNotNetworkCall() throws Exception {
        String token = login();
        mvc.perform(post("/api/admin/agent/chat").header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"本月营收多少？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.error").isNotEmpty());
    }

    @Test
    void agentApiRequiresLogin() throws Exception {
        mvc.perform(post("/api/admin/agent/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
