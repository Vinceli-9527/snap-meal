package com.snapmeal.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AgentModelStore 纯离线单测：默认模型、.env 持久化与优先级、与 API Key 共用同一文件的互不覆盖。 */
class AgentModelStoreTest {

    @TempDir
    Path tmp;

    @Test
    void defaultsToV4FlashWhenNothingSet() {
        AgentModelStore store = new AgentModelStore("", tmp.resolve("m.env").toString());
        assertEquals("deepseek-v4-flash", store.current());
    }

    @Test
    void fallsBackToEnvModelWhenNoFile() {
        AgentModelStore store = new AgentModelStore("deepseek-v4-pro", tmp.resolve("missing.env").toString());
        assertEquals("deepseek-v4-pro", store.current());
    }

    @Test
    void setPersistsAndNewStoreReadsBack() {
        String file = tmp.resolve("m.env").toString();
        AgentModelStore store = new AgentModelStore("deepseek-v4-flash", file);
        store.set("deepseek-v4-pro");
        assertEquals("deepseek-v4-pro", store.current());
        AgentModelStore reloaded = new AgentModelStore("deepseek-v4-flash", file);
        assertEquals("deepseek-v4-pro", reloaded.current(), ".env 文件优先级应高于环境变量");
    }

    @Test
    void setDoesNotClobberApiKeyLine() throws Exception {
        String file = tmp.resolve("shared.env").toString();
        AgentKeyStore keyStore = new AgentKeyStore("", file);
        keyStore.set("sk-shared-key-0000");
        AgentModelStore modelStore = new AgentModelStore("", file);
        modelStore.set("deepseek-v4-pro");

        String content = new String(Files.readAllBytes(tmp.resolve("shared.env")), StandardCharsets.UTF_8);
        assertTrue(content.contains("DEEPSEEK_API_KEY=sk-shared-key-0000"));
        assertTrue(content.contains("DEEPSEEK_MODEL=deepseek-v4-pro"));
    }

    @Test
    void isValidOnlyAcceptsOfficialModels() {
        assertTrue(AgentModelStore.isValid("deepseek-v4-flash"));
        assertTrue(AgentModelStore.isValid("deepseek-v4-pro"));
        assertFalse(AgentModelStore.isValid("deepseek-chat"));
        assertFalse(AgentModelStore.isValid(null));
        assertFalse(AgentModelStore.isValid(""));
    }
}
