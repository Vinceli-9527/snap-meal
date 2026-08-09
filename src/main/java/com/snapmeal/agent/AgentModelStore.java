package com.snapmeal.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DeepSeek 模型运行时存储：允许前端选择模型，立即生效并持久化到 .env（与 API Key 共用同一文件）。
 * 读取优先级：.env 中的 DEEPSEEK_MODEL 高于环境变量/application.yml。
 */
@Component
public class AgentModelStore {

    private static final String MODEL_PREFIX = "DEEPSEEK_MODEL=";
    public static final List<String> AVAILABLE = Collections.unmodifiableList(
            Arrays.asList("deepseek-v4-flash", "deepseek-v4-pro"));

    private volatile String model;
    private final String keyFile;

    public AgentModelStore(
            @Value("${sky.agent.model:deepseek-v4-flash}") String envModel,
            @Value("${sky.agent.key-file:.env}") String keyFile) {
        this.keyFile = keyFile == null ? ".env" : keyFile;
        this.model = pick(DotEnv.read(this.keyFile, MODEL_PREFIX), envModel);
    }

    /** 当前生效的模型名。 */
    public String current() {
        return model;
    }

    /** 保存新模型：立即生效并持久化到 .env。 */
    public synchronized void set(String model) {
        String trimmed = model == null ? "" : model.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        this.model = trimmed;
        DotEnv.write(keyFile, MODEL_PREFIX, trimmed);
    }

    /** 是否在官方可选模型列表内。 */
    public static boolean isValid(String model) {
        return model != null && AVAILABLE.contains(model.trim());
    }

    private static String pick(String file, String env) {
        if (file != null && !file.trim().isEmpty()) {
            return file.trim();
        }
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        return "deepseek-v4-flash";
    }
}
