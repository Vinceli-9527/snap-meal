package com.snapmeal.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API Key 运行时存储：允许通过前端写入 Key，立即生效并持久化到 .env，
 * 无需重启服务。读取优先级：.env 文件中的 DEEPSEEK_API_KEY 高于环境变量/application.yml。
 */
@Component
public class AgentKeyStore {

    private static final String KEY_PREFIX = "DEEPSEEK_API_KEY=";
    private static final String PLACEHOLDER = "sk-your-key-here";

    private volatile String apiKey;
    private final String keyFile;

    public AgentKeyStore(
            @Value("${sky.agent.api-key:}") String envKey,
            @Value("${sky.agent.key-file:.env}") String keyFile) {
        this.keyFile = keyFile == null ? ".env" : keyFile;
        this.apiKey = pick(DotEnv.read(this.keyFile, KEY_PREFIX), envKey);
    }

    /** 当前生效的 API Key（可能为占位符或空串）。 */
    public String current() {
        return apiKey;
    }

    public String keyFile() {
        return keyFile;
    }

    /** 是否已配置真实 Key（非空、非占位符）。 */
    public boolean configured() {
        return !isPlaceholder(apiKey);
    }

    /** 保存新 Key：立即生效并持久化到 keyFile。 */
    public synchronized void set(String key) {
        String trimmed = key == null ? "" : key.trim();
        apiKey = trimmed;
        DotEnv.write(keyFile, KEY_PREFIX, trimmed);
    }

    /** 脱敏显示：sk-****后四位。 */
    public String masked() {
        return mask(apiKey);
    }

    public static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }

    /** 空串 / 占位符都视为未配置。 */
    public static boolean isPlaceholder(String key) {
        return key == null || key.trim().isEmpty() || key.trim().equalsIgnoreCase(PLACEHOLDER);
    }

    private static String pick(String file, String env) {
        if (file != null && !file.trim().isEmpty()) {
            return file.trim();
        }
        return env == null ? "" : env.trim();
    }
}
