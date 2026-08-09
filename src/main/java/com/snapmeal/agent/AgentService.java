package com.snapmeal.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Text2SQL Agent 主服务：自然语言 → SQL → 校验 → 只读执行 → 中文解释。
 * 采用多轮 function-calling：SQL 校验/执行失败会把错误回传给模型自动修正，最多重试 MAX_SQL_ATTEMPTS 次。
 */
@Service
public class AgentService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ROUNDS = 8;
    private static final int MAX_SQL_ATTEMPTS = 3;
    private static final int MAX_ROWS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private static final String SYSTEM_PROMPT =
            "你是 snap-meal 外卖餐厅的经营分析助手。用户会用中文提出经营问题，你需要：\n" +
            "1. 仅通过 run_query 工具查询只读数据库获取事实，绝不凭记忆编造数据；\n" +
            "2. 生成的 SQL 会被严格校验（只允许单条 SELECT），请输出合规 SQL；\n" +
            "3. 根据查询结果用简洁、专业的中文给出经营解释，给出关键数值与结论；\n" +
            "4. 若数据为空或无法回答，如实说明，不要虚构。\n\n" +
            "数据库表结构（金额单位：元）：\n" +
            "- orders 订单主表：id, number(订单号), user_id, status, pay_status, pay_method, amount(实付金额), phone, address, consignee, order_time(下单时间), checkout_time(支付时间), delivery_time(送达时间), estimated_delivery_time(预计送达), cancel_reason(取消原因), rejection_reason(拒单原因), remark\n" +
            "- order_detail 订单明细：id, order_id, name(菜品快照名), dish_id, number(数量), amount(该行小计)\n" +
            "- dish 菜品：id, category_id, name, price(单价), status(1起售/0停售)\n" +
            "- category 分类：id, type(1菜品/2套餐), name\n" +
            "- app_user 顾客：id, openid, nickname(昵称), phone, create_time(注册时间)\n" +
            "- setmeal 套餐 / setmeal_dish 套餐菜品 / address_book 收货地址\n\n" +
            "关键业务语义：\n" +
            "- orders.status：1待付款, 2待接单, 3已接单, 4派送中, 5已完成, 6已取消\n" +
            "- orders.pay_status：0未支付, 1已支付。计算「营收/营业额」只统计 pay_status=1；「已完成订单数」指 status=5\n" +
            "- 「近 N 天」用 dateadd('DAY',-N,current_timestamp)<=order_time 过滤；「今天」用 cast(order_time as date)=cast(current_timestamp as date)\n" +
            "- 菜品销量按 order_detail 按 name 分组 sum(number)\n" +
            "- 只做只读查询，禁止分号、注释以及任何写操作";

    private final DeepSeekClient client;
    private final JdbcTemplate jdbc;
    private final SqlSafetyValidator validator;

    public AgentService(DeepSeekClient client, JdbcTemplate jdbc, SqlSafetyValidator validator) {
        this.client = client;
        this.jdbc = jdbc;
        this.validator = validator;
    }

    public boolean configured() {
        return client.configured();
    }

    public String model() {
        return client.model();
    }

    public AgentReply answer(String question) {
        if (!client.configured()) {
            return fail("DeepSeek API Key 未配置：请先在本页上方「API Key 设置」卡片粘贴真实 Key 并保存");
        }
        ArrayNode messages = JSON.createArrayNode();
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", SYSTEM_PROMPT);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", question);

        String lastSql = null;
        List<String> lastColumns = null;
        List<List<Object>> lastRows = null;
        boolean lastTruncated = false;
        int sqlAttempts = 0;
        int failedAttempts = 0;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            ObjectNode payload = JSON.createObjectNode();
            payload.put("model", client.model());
            payload.set("messages", messages);
            payload.set("tools", toolDef());
            payload.put("tool_choice", "auto");
            JsonNode response = client.chat(payload);
            JsonNode message = response.path("choices").path(0).path("message");
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                // OpenAI 兼容协议要求 tool 消息必须紧跟在带 tool_calls 的 assistant 消息之后；
                // V4 思考模式下还必须把 reasoning_content 原样带回，否则 DeepSeek 返回 400。
                ObjectNode assistantMsg = messages.addObject();
                assistantMsg.put("role", "assistant");
                JsonNode contentNode = message.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    assistantMsg.set("content", contentNode);
                }
                JsonNode reasoning = message.get("reasoning_content");
                if (reasoning != null && !reasoning.isNull()) {
                    assistantMsg.set("reasoning_content", reasoning);
                }
                assistantMsg.set("tool_calls", toolCalls);
                boolean anyTool = false;
                for (JsonNode call : toolCalls) {
                    if (!"run_query".equals(call.path("function").path("name").asText())) {
                        continue;
                    }
                    anyTool = true;
                    sqlAttempts++;
                    String sql = extractSql(call.path("function").path("arguments").asText());
                    lastSql = sql;
                    ObjectNode toolMessage = messages.addObject();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", call.path("id").asText());
                    Optional<String> error = validator.findError(sql);
                    if (error.isPresent()) {
                        failedAttempts++;
                        toolMessage.put("content", "SQL 校验失败：" + error.get());
                    } else {
                        try {
                            QueryResult qr = execute(sql);
                            lastColumns = qr.columns;
                            lastRows = qr.rows;
                            lastTruncated = qr.truncated;
                            toolMessage.put("content", qr.toJson());
                        } catch (Exception e) {
                            failedAttempts++;
                            toolMessage.put("content", "SQL 执行失败：" + e.getMessage());
                        }
                    }
                }
                if (!anyTool) {
                    break;
                }
                if (failedAttempts >= MAX_SQL_ATTEMPTS) {
                    AgentReply reply = fail("Agent 连续 " + MAX_SQL_ATTEMPTS + " 次生成 SQL 失败，已终止，请换个更明确的问法重试");
                    reply.sql = lastSql;
                    reply.attempts = sqlAttempts;
                    return reply;
                }
                continue;
            }
            AgentReply reply = new AgentReply();
            String content = message.path("content").asText(null);
            reply.answer = (content == null || content.trim().isEmpty())
                    ? "抱歉，我没能生成有效回答，请换个方式提问。" : content.trim();
            reply.sql = lastSql;
            reply.columns = lastColumns;
            reply.rows = lastRows;
            reply.truncated = lastTruncated;
            reply.attempts = sqlAttempts;
            return reply;
        }
        AgentReply reply = fail("Agent 对话达到轮次上限，未完成回答，请重试");
        reply.sql = lastSql;
        reply.attempts = sqlAttempts;
        return reply;
    }

    private static AgentReply fail(String error) {
        AgentReply reply = new AgentReply();
        reply.error = error;
        return reply;
    }

    private String extractSql(String arguments) {
        try {
            return JSON.readTree(arguments).path("sql").asText();
        } catch (Exception e) {
            return "";
        }
    }

    private ArrayNode toolDef() {
        ArrayNode tools = JSON.createArrayNode();
        ObjectNode tool = tools.addObject();
        tool.put("type", "function");
        ObjectNode fn = tool.putObject("function");
        fn.put("name", "run_query");
        fn.put("description", "对只读经营数据库执行一条 SELECT 查询，返回列名与最多 " + MAX_ROWS + " 行数据。仅用于查询 orders/order_detail/dish/category/app_user 等业务表。");
        ObjectNode params = fn.putObject("parameters");
        params.put("type", "object");
        ObjectNode props = params.putObject("properties");
        ObjectNode sqlProp = props.putObject("sql");
        sqlProp.put("type", "string");
        sqlProp.put("description", "只读 SELECT 语句（单条，禁止分号、注释与增删改）");
        ArrayNode required = params.putArray("required");
        required.add("sql");
        return tools;
    }

    private QueryResult execute(String sql) {
        QueryResult qr = new QueryResult();
        jdbc.execute((ConnectionCallback<Void>) conn -> {
            boolean wasReadOnly = conn.isReadOnly();
            conn.setReadOnly(true);
            try (Statement st = conn.createStatement()) {
                st.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                try (ResultSet rs = st.executeQuery(sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int count = meta.getColumnCount();
                    for (int i = 1; i <= count; i++) {
                        qr.columns.add(meta.getColumnLabel(i));
                    }
                    int seen = 0;
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= count; i++) {
                            row.add(plain(rs.getObject(i)));
                        }
                        qr.rows.add(row);
                        if (++seen >= MAX_ROWS) {
                            qr.truncated = true;
                            break;
                        }
                    }
                }
            } finally {
                conn.setReadOnly(wasReadOnly);
            }
            return null;
        });
        return qr;
    }

    private static Object plain(Object value) {
        if (value instanceof java.util.Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.util.Date) value);
        }
        return value;
    }

    private class QueryResult {
        final List<String> columns = new ArrayList<>();
        final List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;

        String toJson() {
            ObjectNode out = JSON.createObjectNode();
            ArrayNode colArr = out.putArray("columns");
            for (String c : columns) {
                colArr.add(c);
            }
            ArrayNode rowArr = out.putArray("rows");
            for (List<Object> row : rows) {
                ArrayNode ra = rowArr.addArray();
                for (Object v : row) {
                    ra.add(v == null ? NullNode.instance : JSON.valueToTree(v));
                }
            }
            out.put("truncated", truncated);
            try {
                return JSON.writeValueAsString(out);
            } catch (Exception e) {
                return "{}";
            }
        }
    }

    /** 问答回复：answer=中文经营解释；sql/columns/rows=最后一次成功查询预览；error=失败原因。 */
    public static class AgentReply {
        public String answer;
        public String sql;
        public List<String> columns;
        public List<List<Object>> rows;
        public boolean truncated;
        public int attempts;
        public String error;
    }
}
