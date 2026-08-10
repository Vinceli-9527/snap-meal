package com.snapmeal.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "sky.rate-limit.orders-per-minute=2",
        "spring.datasource.url=jdbc:h2:mem:ratelimit_it;MODE=MySQL;DATABASE_TO_UPPER=true;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void thirdOrderSubmitWithinMinuteReturns429() throws Exception {
        String token = login("rate-limit-user");
        long addressId = address(token);
        String payload = "{\"addressBookId\":" + addressId + ",\"payMethod\":1}";

        submit(token, payload);
        submit(token, payload);
        mvc.perform(post("/api/user/orders").header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void submit(String token, String payload) throws Exception {
        mvc.perform(delete("/api/user/cart").header("authentication", token)).andExpect(status().isOk());
        mvc.perform(post("/api/user/cart/items").header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dishId\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/user/orders").header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }

    private String login(String phone) throws Exception {
        String body = mvc.perform(post("/api/user/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginMethod\":\"PHONE\",\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }

    private long address(String token) throws Exception {
        mvc.perform(post("/api/user/addresses").header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consignee\":\"测试用户\",\"phone\":\"10086\",\"cityName\":\"上海市\",\"districtName\":\"杨浦区\",\"detail\":\"大学路100号\"}"))
                .andExpect(status().isOk());
        String body = mvc.perform(get("/api/user/addresses").header("authentication", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").get(0).path("id").asLong();
    }
}
