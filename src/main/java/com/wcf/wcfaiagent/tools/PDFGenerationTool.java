package com.wcf.wcfaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.wcf.wcfaiagent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 生成工具
 */
@Slf4j
public class PDFGenerationTool {

    // 标题行：1~6 个 # 开头
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s*(.*)$");

    // **加粗** 语法
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");

    // Windows 系统常见中文字体（按优先级排列；ttc 字体集合默认取第 0 个字体）
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/msyh.ttc,0",    // 微软雅黑（常规）
            "C:/Windows/Fonts/simsun.ttc,0",  // 宋体
            "C:/Windows/Fonts/simhei.ttf",    // 黑体
    };

    // 对应的加粗字重（与 FONT_CANDIDATES 顺序一一对应；最后一项表示没有独立粗体，用常规字体模拟）
    private static final String[] BOLD_FONT_CANDIDATES = {
            "C:/Windows/Fonts/msyhbd.ttc,0",  // 微软雅黑 Bold
            "C:/Windows/Fonts/simhei.ttf",    // 黑体（视觉上偏粗，可作宋体的粗体替代）
            "",
    };

    @Tool(description = "Generate a PDF file with given content. "
            + "The content supports simple Markdown syntax: lines starting with '#'/'##'/'###' are rendered as headings, "
            + "text wrapped in **...** is rendered bold. "
            + "Write the content in the same language as the user (usually Simplified Chinese).", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF (Markdown-lite, same language as the user)") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 优先使用系统自带中文字体（中英文渲染都正常），找不到时才退回内置 CJK 字体
                PdfFont font = createFont(FONT_CANDIDATES, "STSongStd-Light");
                // 加粗字体：与常规字体同源；找不到独立粗体字重时用常规字体模拟
                PdfFont boldFont = createFont(BOLD_FONT_CANDIDATES, null);
                if (boldFont == null) {
                    boldFont = font;
                }
                document.setFont(font);
                // 渲染内容（支持简单 Markdown）
                renderContent(document, font, boldFont, content);
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 按候选顺序加载第一个可用的系统字体；全部失败时返回 fallbackName 对应的 iText 内置字体。
     * 注意：内置 CJK 字体（如 STSongStd-Light）对拉丁字符的字距处理很差，
     * 会出现 "W eekly" 这类字符间异常空隙，因此只作为兜底方案。
     *
     * @param candidates   字体文件路径候选（ttc 可带 ",索引" 后缀）
     * @param fallbackName 兜底内置字体名，null 表示无兜底（返回 null 由调用方处理）
     */
    private PdfFont createFont(String[] candidates, String fallbackName) throws IOException {
        for (int i = 0; i < candidates.length; i++) {
            String candidate = candidates[i];
            String realPath = candidate.split(",")[0];
            if (realPath.isEmpty() || !new File(realPath).exists()) {
                continue;
            }
            try {
                return PdfFontFactory.createFont(candidate, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e) {
                log.warn("加载字体失败，尝试下一个候选: {} ({})", candidate, e.getMessage());
            }
        }
        if (fallbackName == null) {
            return null;
        }
        log.warn("系统字体均不可用，回退到 iText 内置 CJK 字体: {}", fallbackName);
        return PdfFontFactory.createFont(fallbackName, "UniGB-UCS2-H");
    }

    /**
     * 渲染内容：支持简单 Markdown —— # 标题、**加粗**、- 列表项。
     * 避免模型的 Markdown 标记（###、**）原样出现在 PDF 里。
     */
    private void renderContent(Document document, PdfFont font, PdfFont boldFont, String content) {
        String[] lines = content.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            // 标题行
            Matcher heading = HEADING_PATTERN.matcher(line);
            if (heading.matches()) {
                document.add(new Paragraph(heading.group(1).trim())
                        .setFont(boldFont)
                        .setFontSize(16)
                        .setMarginTop(12));
                continue;
            }
            // 列表项 / 普通段落
            Paragraph paragraph = new Paragraph();
            String text = line;
            if (line.startsWith("-")) {
                paragraph.add("• ");
                text = line.substring(1).trim();
            }
            appendWithBold(paragraph, font, boldFont, text);
            document.add(paragraph);
        }
    }

    /** 按 **加粗** 语法拆分文本，生成粗体/常规混排的段落 */
    private void appendWithBold(Paragraph paragraph, PdfFont font, PdfFont boldFont, String text) {
        Matcher matcher = BOLD_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                paragraph.add(text.substring(last, matcher.start()));
            }
            paragraph.add(new Text(matcher.group(1)).setFont(boldFont));
            last = matcher.end();
        }
        if (last < text.length()) {
            paragraph.add(text.substring(last));
        }
    }

}
