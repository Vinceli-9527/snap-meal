package com.snapmeal.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AgentKeyStore 纯离线单测：占位符/脱敏/configured，以及 .env 持久化与读取优先级。 */
class AgentKeyStoreTest {

    private static final String REAL_KEY = "sk-abcdefghijklmnopqrstuvwxyz7890";
    private static final String LAST4 = "7890";

    @TempDir
    Path tmp;

    // ── 占位符与脱敏 ─────────────────────────────────────────────

    @Test
    void placeholderDetection() {
        assertTrue(AgentKeyStore.isPlaceholder(null));
        assertTrue(AgentKeyStore.isPlaceholder(""));
        assertTrue(AgentKeyStore.isPlaceholder("   "));
        assertTrue(AgentKeyStore.isPlaceholder("sk-your-key-here"));
        assertTrue(AgentKeyStore.isPlaceholder("SK-YOUR-KEY-HERE"));
        assertFalse(AgentKeyStore.isPlaceholder(REAL_KEY));
    }

    @Test
    void maskShowsPrefixAndLast4() {
        assertEquals("", AgentKeyStore.mask(""));
        assertEquals("****", AgentKeyStore.mask("short"));
        assertEquals("sk-****" + LAST4, AgentKeyStore.mask(REAL_KEY));
    }

    @Test
    void configuredOnlyForRealKey() {
        AgentKeyStore store = new AgentKeyStore("", keyFile());
        assertFalse(store.configured());
        store.set("sk-your-key-here");
        assertFalse(store.configured(), "占位符不算已配置");
        store.set(REAL_KEY);
        assertTrue(store.configured());
    }

    // ── .env 持久化与重启读取 ────────────────────────────────────

    @Test
    void setPersistsAndNewStoreReadsBack() {
        AgentKeyStore store = new AgentKeyStore("", keyFile());
        store.set(REAL_KEY);
        assertEquals(REAL_KEY, store.current());
        assertEquals("sk-****" + LAST4, store.masked());

        // 模拟重启：同一 key-file 新建 store 应读到持久化 Key
        AgentKeyStore reloaded = new AgentKeyStore("env-should-lose", keyFile());
        assertEquals(REAL_KEY, reloaded.current(), ".env 文件优先级应高于环境变量");
    }

    @Test
    void fileOverridesEnvKey() {
        AgentKeyStore store = new AgentKeyStore("sk-env-key-0000", keyFile());
        store.set(REAL_KEY);
        AgentKeyStore reloaded = new AgentKeyStore("sk-env-key-0000", keyFile());
        assertEquals(REAL_KEY, reloaded.current());
    }

    @Test
    void fallsBackToEnvKeyWhenNoFile() {
        Path missing = tmp.resolve("missing.env");
        AgentKeyStore store = new AgentKeyStore("sk-env-only-1234", missing.toString());
        assertEquals("sk-env-only-1234", store.current());
        assertTrue(store.configured());
    }

    @Test
    void setReplacesExistingLineInFile() throws Exception {
        AgentKeyStore store = new AgentKeyStore("", keyFile());
        store.set("sk-first-key-1111");
        store.set(REAL_KEY);
        String content = new String(Files.readAllBytes(tmp.resolve("key.env")), StandardCharsets.UTF_8);
        assertEquals(1, countLinesWith(content, "DEEPSEEK_API_KEY="), "重复 set 不应追加多行");
        assertTrue(content.contains("DEEPSEEK_API_KEY=" + REAL_KEY));
        assertFalse(content.contains("sk-first-key-1111"));
    }

    private String keyFile() {
        return tmp.resolve("key.env").toString();
    }

    private static long countLinesWith(String content, String prefix) {
        long count = 0;
        for (String line : content.split("\n")) {
            if (line.trim().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }
}
