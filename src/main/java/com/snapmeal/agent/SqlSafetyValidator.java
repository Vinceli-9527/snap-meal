package com.snapmeal.agent;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 只读 SQL 白名单校验，是 Text2SQL Agent 的安全防线：
 * 仅允许单条 SELECT；拒绝多语句拼接、注释绕过、DDL/DML 与危险函数，并将可查询表限制到业务表白名单。
 */
@Component
public class SqlSafetyValidator {

    private static final String[] BANNED_WORDS = {
            "insert", "update", "delete", "drop", "alter", "create",
            "truncate", "grant", "revoke", "merge", "replace", "call",
            "execute", "declare", "begin", "commit", "rollback", "backup",
            // H2/MySQL 文件读写、耗资源与锁函数，以及 SELECT INTO 等写操作
            "into", "sleep", "benchmark", "get_lock", "release_lock",
            "pg_sleep", "waitfor", "csvread", "csvwrite", "file_read",
            "file_write", "load_file", "link", "shutdown"
    };
    private static final String[] BANNED_FRAGMENTS = {
            "into outfile", "into dumpfile", "load_file", "information_schema",
            "for update"
    };

    /** Agent 可查询的业务表白名单；employee（含口令）、auth_session（token 哈希）等敏感表不允许查询。 */
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "orders", "order_detail", "dish", "category", "app_user",
            "setmeal", "setmeal_dish", "address_book", "shop_state", "shopping_cart"
    ));

    /** 返回首个问题描述；合法时返回 Optional.empty()。 */
    public Optional<String> findError(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return Optional.of("生成的 SQL 为空");
        }
        String s = sql.trim();
        if (s.indexOf(';') >= 0) {
            return Optional.of("仅允许单条 SQL，禁止以分号拼接多条语句");
        }
        if (s.indexOf("--") >= 0 || s.indexOf("/*") >= 0 || s.indexOf("*/") >= 0 || s.indexOf('#') >= 0) {
            return Optional.of("不允许在 SQL 中出现注释");
        }
        for (String word : BANNED_WORDS) {
            if (wholeWord(word).matcher(s).find()) {
                return Optional.of("SQL 包含被禁止的关键字：" + word.toUpperCase());
            }
        }
        if (!selectEntry(s)) {
            return Optional.of("仅允许执行 SELECT 只读查询");
        }
        String lower = s.toLowerCase();
        for (String fragment : BANNED_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return Optional.of("SQL 包含被禁止的内容：" + fragment);
            }
        }
        Optional<String> unknown = unknownTable(lower);
        if (unknown.isPresent()) {
            return Optional.of("仅允许查询业务表，发现未知表：" + unknown.get());
        }
        return Optional.empty();
    }

    /** 必须以 SELECT 开头且后跟空白（或恰为 SELECT）。 */
    private static boolean selectEntry(String sql) {
        String entry = sql.trim().toLowerCase();
        return entry.length() == 6
                || (entry.startsWith("select") && entry.length() > 6 && Character.isWhitespace(entry.charAt(6)));
    }

    private static Pattern wholeWord(String word) {
        return Pattern.compile("(?i)(?<![a-z0-9_])" + word + "(?![a-z0-9_])");
    }

    /** 提取 FROM/JOIN 后的表名并校验白名单（表名仅含字母数字下划线）。 */
    private static Optional<String> unknownTable(String sql) {
        String normalized = sql.replace('`', ' ').replace('"', ' ');
        String[] tokens = normalized.split("[^a-z0-9_]+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (("from".equals(tokens[i]) || "join".equals(tokens[i])) && !tokens[i + 1].isEmpty()) {
                String table = tokens[i + 1];
                if (!ALLOWED_TABLES.contains(table)) {
                    return Optional.of(table);
                }
            }
        }
        return Optional.empty();
    }
}
