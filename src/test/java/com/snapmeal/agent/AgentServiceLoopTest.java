package com.snapmeal.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 多轮自纠错循环离线端到端测试：真实 H2 + 种子数据，LLM 用 Mockito 脚本化响应，
 * 验证「SQL 校验失败 / 执行失败 → 回传错误 → 模型修正 → 成功」与「连续失败终止」。
 */
class AgentServiceLoopTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String GOOD_SQL =
            "select sum(amount) from orders where pay_status=1 and order_time>=dateadd('DAY',-7,current_timestamp)";

    // ── 场景一：校验失败（DELETE）→ 回传错误 → 模型修正 → 成功 ─────────

    @Test
    void validationErrorIsFedBackAndCorrected() throws Exception {
        List<String> fedBack = new ArrayList<>();
        AgentService agent = service(new ScriptedLlm(fedBack,
                toolCall("call_1", "delete from orders"),
                toolCall("call_2", GOOD_SQL),
                contentAnswer("最近 7 天已支付营收合计为 952.00 元。")));

        AgentService.AgentReply reply = agent.answer("最近7天营收多少？");

        assertNull(reply.error);
        assertEquals(GOOD_SQL, reply.sql);
        assertEquals(2, reply.attempts);
        assertTrue(reply.answer.contains("952"), reply.answer);
        assertEquals(1, reply.rows.size());
        assertTrue(String.valueOf(reply.rows.get(0).get(0)).contains("952"));
        assertTrue(fedBack.stream().anyMatch(m -> m.contains("DELETE")), fedBack.toString());
    }

    // ── 场景二：执行失败（列不存在）→ 回传错误 → 模型修正 → 成功 ──────

    @Test
    void executionErrorIsFedBackAndCorrected() throws Exception {
        List<String> fedBack = new ArrayList<>();
        AgentService agent = service(new ScriptedLlm(fedBack,
                toolCall("call_1", "select * from orders where bogus_column = 1"),
                toolCall("call_2", GOOD_SQL),
                contentAnswer("最近 7 天已支付营收合计为 952.00 元。")));

        AgentService.AgentReply reply = agent.answer("最近7天营收多少？");

        assertNull(reply.error);
        assertEquals(2, reply.attempts);
        assertTrue(fedBack.get(0).contains("执行失败"), fedBack.get(0));
    }

    // ── 场景三：连续 3 次失败 → 终止并返回错误 ──────────────────────

    @Test
    void exhaustedAttemptsReturnError() throws Exception {
        AgentService agent = service(new ScriptedLlm(new ArrayList<>(),
                toolCall("call_1", "delete from orders"),
                toolCall("call_2", "drop table orders"),
                toolCall("call_3", "update orders set status = 1")));

        AgentService.AgentReply reply = agent.answer("随便问");

        assertTrue(reply.error != null && reply.error.contains("连续 3 次"), String.valueOf(reply.error));
        assertEquals(3, reply.attempts);
    }

    // ── 场景四：tool_calls 协议 —— assistant(tool_calls) 必须排在 tool 结果之前 ──

    @Test
    void assistantToolCallMessagePrecedesToolResult() throws Exception {
        List<JsonNode> sent = new ArrayList<>();
        AgentService agent = service(new ScriptedLlm(new ArrayList<>(),
                toolCall("call_1", GOOD_SQL),
                contentAnswer("最近 7 天已支付营收合计为 952.00 元。")), sent);

        agent.answer("最近7天营收多少？");

        assertEquals(2, sent.size(), "第一轮 tool_calls + 第二轮最终回答");
        JsonNode messages = sent.get(1).path("messages");
        int assistantIdx = -1;
        int toolIdx = -1;
        for (int i = 0; i < messages.size(); i++) {
            JsonNode m = messages.get(i);
            if ("assistant".equals(m.path("role").asText())
                    && m.path("tool_calls").isArray() && m.path("tool_calls").size() > 0) {
                assistantIdx = i;
            }
            if ("tool".equals(m.path("role").asText()) && "call_1".equals(m.path("tool_call_id").asText())) {
                toolIdx = i;
            }
        }
        assertTrue(assistantIdx >= 0, "应回传带 tool_calls 的 assistant 消息");
        assertTrue(toolIdx >= 0, "应包含 tool 结果消息");
        assertTrue(assistantIdx < toolIdx, "assistant(tool_calls) 必须排在 tool 消息之前");
        assertEquals("call_1", messages.get(assistantIdx).path("tool_calls").path(0).path("id").asText());
    }

    @Test
    void reasoningContentPreservedOnToolCallReplay() throws Exception {
        List<JsonNode> sent = new ArrayList<>();
        AgentService agent = service(new ScriptedLlm(new ArrayList<>(),
                toolCallWithReasoning("call_1", GOOD_SQL, "思考中…"),
                contentAnswer("最近 7 天已支付营收合计为 952.00 元。")), sent);

        agent.answer("最近7天营收多少？");

        JsonNode messages = sent.get(1).path("messages");
        JsonNode assistant = null;
        for (JsonNode m : messages) {
            if ("assistant".equals(m.path("role").asText())
                    && m.path("tool_calls").isArray() && m.path("tool_calls").size() > 0) {
                assistant = m;
            }
        }
        assertTrue(assistant != null, "应回传 assistant 工具调用消息");
        assertEquals("思考中…", assistant.path("reasoning_content").asText());
    }

    // ── 工具方法 ─────────────────────────────────────────────────

    private AgentService service(ScriptedLlm llm) throws Exception {
        return service(llm, null);
    }

    private AgentService service(ScriptedLlm llm, List<JsonNode> sent) throws Exception {
        JdbcTemplate jdbc = seededJdbc();
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.configured()).thenReturn(true);
        when(client.model()).thenReturn("test");
        when(client.chat(any())).thenAnswer(inv -> {
            JsonNode payload = inv.getArgument(0);
            if (sent != null) {
                sent.add(payload);
            }
            for (JsonNode m : payload.path("messages")) {
                if ("tool".equals(m.path("role").asText())) {
                    llm.fedBack.add(m.path("content").asText());
                }
            }
            return llm.next();
        });
        return new AgentService(client, jdbc, new SqlSafetyValidator());
    }

    private static JdbcTemplate seededJdbc() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:agenttest_" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=true;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        run(jdbc, new String(Files.readAllBytes(Paths.get("src/main/resources/schema.sql")), "UTF-8"));
        run(jdbc, new String(Files.readAllBytes(Paths.get("src/main/resources/data.sql")), "UTF-8"));
        assertEquals(16, jdbc.queryForObject("select count(*) from orders", Integer.class), "seed 未完整加载");
        return jdbc;
    }

    private static void run(JdbcTemplate jdbc, String script) {
        for (String stmt : script.split(";")) {
            String s = stmt.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                jdbc.execute(s);
            } catch (org.springframework.jdbc.BadSqlGrammarException ignored) {
                // 忽略 schema 中 pre-existing 保留字兼容问题（dish_flavor.value）
            }
        }
    }

    private static ObjectNode toolCall(String id, String sql) throws Exception {
        ObjectNode root = JSON.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode message = choices.addObject().putObject("message");
        ObjectNode call = message.putArray("tool_calls").addObject();
        call.put("id", id);
        call.put("type", "function");
        ObjectNode fn = call.putObject("function");
        fn.put("name", "run_query");
        fn.put("arguments", JSON.writeValueAsString(java.util.Collections.singletonMap("sql", sql)));
        return root;
    }

    private static ObjectNode toolCallWithReasoning(String id, String sql, String reasoning) throws Exception {
        ObjectNode root = toolCall(id, sql);
        ObjectNode message = (ObjectNode) root.path("choices").get(0).path("message");
        message.put("reasoning_content", reasoning);
        return root;
    }

    private static ObjectNode contentAnswer(String text) {
        ObjectNode root = JSON.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode message = choices.addObject().putObject("message");
        message.put("content", text);
        return root;
    }

    private static class ScriptedLlm {
        final List<String> fedBack;
        final JsonNode[] responses;
        final AtomicInteger index = new AtomicInteger();

        ScriptedLlm(List<String> fedBack, JsonNode... responses) {
            this.fedBack = fedBack;
            this.responses = responses;
        }

        JsonNode next() {
            return responses[Math.min(index.getAndIncrement(), responses.length - 1)];
        }
    }
}
