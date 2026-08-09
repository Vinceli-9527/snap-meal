package com.snapmeal.agent;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 只读 SQL 白名单校验，是 Text2SQL Agent 的安全防线：
 * 仅允许单条 SELECT；拒绝多语句拼接、注释绕过、DDL/DML 与危险函数。
 */
@Component
public class SqlSafetyValidator {

    private static final String[] BANNED_WORDS = {
            "insert", "update", "delete", "drop", "alter", "create",
            "truncate", "grant", "revoke", "merge", "replace", "call",
            "execute", "declare", "begin", "commit", "rollback", "backup"
    };
    private static final String[] BANNED_FRAGMENTS = {
            "into outfile", "into dumpfile", "load_file", "information_schema"
    };

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
        if (!Pattern.matches("(?is)^select\\b.*", s)) {
            return Optional.of("仅允许执行 SELECT 只读查询");
        }
        String lower = s.toLowerCase();
        for (String fragment : BANNED_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return Optional.of("SQL 包含被禁止的内容：" + fragment);
            }
        }
        return Optional.empty();
    }

    private static Pattern wholeWord(String word) {
        return Pattern.compile("(?i)(?<![a-z0-9_])" + word + "(?![a-z0-9_])");
    }
}
