package com.wcf.wcfaiagent.demo.invoke;

/**
 * 仅用于测试获取API Key
 * <p>
 * 密钥已移出源码，统一保存在 application-local.yml 中（该文件已被 .gitignore 忽略，不会提交到 Git）。
 * 需要包含的配置项：
 * spring.ai.dashscope.api-key        —— 阿里云百炼 API Key
 * spring.ai.dashscope.workspace-id   —— 阿里云百炼业务空间 ID
 */
public interface TestApiKey {

    // 替换为你的API Key => 配置于 application-local.yml 的 spring.ai.dashscope.api-key
    String API_KEY = ApplicationLocalConfig.get("spring.ai.dashscope.api-key");

    // 替换为你的业务空间ID => 配置于 application-local.yml 的 spring.ai.dashscope.workspace-id
    String WORK_SPACE_ID = ApplicationLocalConfig.get("spring.ai.dashscope.workspace-id");

}
