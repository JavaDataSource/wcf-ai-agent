package com.wcf.wcfaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document document = Jsoup.connect(url).get();
            // 头部附带网页地址与标题：便于智能体知悉来源，也便于前端生成可点击的跳转链接
            return "[网页地址] " + url + "\n[网页标题] " + document.title() + "\n\n" + document.html();
        } catch (Exception e) {
            return "Error scraping web page：" + e.getMessage();
        }
    }

}
