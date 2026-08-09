package com.snapmeal.agent;

import com.snapmeal.common.BusinessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** 极简 .env 文件读写工具：按 `PREFIX=value` 行读取/替换，保留其他行。 */
final class DotEnv {

    private DotEnv() {
    }

    /** 读取 `PREFIX=value` 的值；文件不存在或未命中返回 null。 */
    static String read(String file, String prefix) {
        try {
            Path path = Paths.get(file);
            if (!Files.exists(path)) {
                return null;
            }
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String l = line.trim();
                if (l.startsWith(prefix) && !l.endsWith("#")) {
                    String value = l.substring(prefix.length()).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (IOException ignored) {
            // 读取失败按「文件不存在」处理，回退到环境变量
        }
        return null;
    }

    /** 写入/替换 `PREFIX=value` 行（保留文件其余行）；写失败抛 BusinessException。 */
    static void write(String file, String prefix, String value) {
        try {
            Path path = Paths.get(file);
            List<String> lines = new ArrayList<>();
            if (Files.exists(path)) {
                lines.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
            }
            int idx = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith(prefix)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                lines.set(idx, prefix + value);
            } else {
                lines.add(prefix + value);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("写入配置文件失败：" + e.getMessage());
        }
    }
}
