package com.wcf.wcfaiagent.demo.invoke;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * 从 application-local.yml（已被 .gitignore 忽略，不会提交到 Git）读取本地敏感配置，
 * 供不依赖 Spring 上下文的 demo 程序使用。
 * <p>
 * 该文件位于 src/main/resources 下，本地构建时会随 classpath 一起加载；
 * 仅需解析扁平的 yml 键路径，因此直接使用 Spring Boot 自带的 snakeyaml 解析。
 */
final class ApplicationLocalConfig {

    private static final Map<String, Object> LOCAL_CONFIG = load();

    private ApplicationLocalConfig() {
    }

    /**
     * 按点分隔的键路径读取配置，如 spring.ai.dashscope.api-key
     */
    @SuppressWarnings("unchecked")
    static String get(String key) {
        Map<String, Object> current = LOCAL_CONFIG;
        String[] parts = key.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = current.get(parts[i]);
            if (!(child instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) child;
        }
        Object value = current.get(parts[parts.length - 1]);
        return value == null ? null : value.toString();
    }

    private static Map<String, Object> load() {
        try (InputStream in = ApplicationLocalConfig.class
                .getClassLoader()
                .getResourceAsStream("application-local.yml")) {
            if (in == null) {
                throw new IllegalStateException(
                        "未找到 application-local.yml，请参考 application.yml 的占位符，"
                                + "在 src/main/resources 下创建该文件并填写密钥（此文件已被 .gitignore 忽略，不会提交）");
            }
            return new Yaml().load(in);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("读取 application-local.yml 失败：" + e.getMessage(), e);
        }
    }

}
