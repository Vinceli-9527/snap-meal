package com.snapmeal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class SnapMealApplicationTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Test void adminCanLoginAndReadCatalog() throws Exception {
        String body=mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andReturn().getResponse().getContentAsString();
        String token=json.readTree(body).path("data").path("token").asText();
        mvc.perform(get("/api/admin/dishes").header("token",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
    @Test void publicCatalogUsesUtf8AndDoesNotRequireLogin() throws Exception {
        mvc.perform(get("/api/user/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("热销推荐"));
    }
    @Test void staleAdminTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/admin/auth/session").header("token","old-memory-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
    @Test void adminLogoutRevokesTokenImmediately() throws Exception {
        String login=mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=json.readTree(login).path("data").path("token").asText();
        mvc.perform(post("/api/admin/auth/logout").header("token",token)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/auth/session").header("token",token)).andExpect(status().isUnauthorized());
    }
    @Test void phoneAndWechatLoginExposeConfirmedMockAccounts() throws Exception {
        mvc.perform(post("/api/user/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"loginMethod\":\"PHONE\",\"phone\":\"10086\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("手机体验用户")).andExpect(jsonPath("$.data.loginmethod").value("PHONE"));
        mvc.perform(post("/api/user/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"loginMethod\":\"WECHAT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("微信体验用户")).andExpect(jsonPath("$.data.loginmethod").value("WECHAT"));
    }
    @Test void userCanCompleteCheckoutFlow() throws Exception {
        String login=mvc.perform(post("/api/user/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginMethod\":\"PHONE\",\"phone\":\"checkout-test-user\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=json.readTree(login).path("data").path("token").asText();
        mvc.perform(post("/api/user/addresses").header("authentication",token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consignee\":\"结算测试用户\",\"phone\":\"10086\",\"cityName\":\"上海市\",\"districtName\":\"杨浦区\",\"detail\":\"大学路100号\"}"))
                .andExpect(status().isOk());
        String addresses=mvc.perform(get("/api/user/addresses").header("authentication",token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long addressId=json.readTree(addresses).path("data").get(0).path("id").asLong();
        mvc.perform(delete("/api/user/cart").header("authentication",token)).andExpect(status().isOk());
        mvc.perform(post("/api/user/cart/items").header("authentication",token).contentType(MediaType.APPLICATION_JSON).content("{\"dishId\":1}"))
                .andExpect(status().isOk());
        String order=mvc.perform(post("/api/user/orders").header("authentication",token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressBookId\":"+addressId+",\"payMethod\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").isNumber()).andReturn().getResponse().getContentAsString();
        long orderId=json.readTree(order).path("data").path("id").asLong();
        mvc.perform(post("/api/user/orders/"+orderId+"/pay").header("authentication",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.paid").value(true));
    }
}
