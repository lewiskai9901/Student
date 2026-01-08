package com.school.management.service.impl;

import com.lowagie.text.pdf.BaseFont;
import com.school.management.dto.export.ExportPreviewDTO;
import com.school.management.dto.export.ExportTemplateDTO;
import com.school.management.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 文档生成服务
 */
@Slf4j
@Service
public class DocumentGeneratorService {

    /**
     * HTML转PDF
     */
    public byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // 设置中文字体
            ITextFontResolver fontResolver = renderer.getFontResolver();
            try {
                // 尝试加载系统中文字体
                fontResolver.addFont("C:/Windows/Fonts/simsun.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                fontResolver.addFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                log.warn("加载系统字体失败，尝试使用备用字体: {}", e.getMessage());
                // 尝试其他字体路径
                try {
                    fontResolver.addFont("C:/Windows/Fonts/msyh.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                } catch (Exception ex) {
                    log.warn("加载备用字体也失败: {}", ex.getMessage());
                }
            }

            // 将 HTML 转换为 XHTML 格式（ITextRenderer 需要 XHTML）
            String xhtml = convertToXhtml(html);

            // 添加中文字体样式
            xhtml = xhtml.replace(
                    "font-family: SimSun, serif;",
                    "font-family: SimSun, 'Microsoft YaHei', sans-serif;"
            );

            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new BusinessException("生成PDF失败，请稍后重试");
        }
    }

    /**
     * 将 HTML 转换为 XHTML 格式
     * 主要处理自闭合标签，使其符合 XML 规范
     */
    private String convertToXhtml(String html) {
        if (html == null) return "";

        String xhtml = html;

        // 处理自闭合标签，添加 />
        // hr 标签
        xhtml = xhtml.replaceAll("<hr([^>]*)(?<!/)>", "<hr$1 />");
        // br 标签
        xhtml = xhtml.replaceAll("<br([^>]*)(?<!/)>", "<br$1 />");
        // img 标签
        xhtml = xhtml.replaceAll("<img([^>]*)(?<!/)>", "<img$1 />");
        // input 标签
        xhtml = xhtml.replaceAll("<input([^>]*)(?<!/)>", "<input$1 />");
        // meta 标签
        xhtml = xhtml.replaceAll("<meta([^>]*)(?<!/)>", "<meta$1 />");
        // link 标签
        xhtml = xhtml.replaceAll("<link([^>]*)(?<!/)>", "<link$1 />");

        // 处理 HTML 注释中的特殊字符（可能导致 XML 解析错误）
        xhtml = xhtml.replaceAll("<!--[\\s\\S]*?-->", "");

        // 确保有 XML 声明和正确的 DOCTYPE
        if (!xhtml.contains("<?xml")) {
            xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" " +
                    "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                    "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                    "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head>\n" +
                    "<body>\n" + xhtml + "\n</body>\n</html>";
        }

        return xhtml;
    }

    /**
     * HTML转Word（基于HTML内容解析，支持rowspan/colspan）
     */
    public byte[] htmlToWord(String html, List<ExportPreviewDTO.StudentRecordDTO> records,
                             ExportTemplateDTO.TableConfig tableConfig) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             XWPFDocument document = new XWPFDocument()) {

            // 解析HTML并转换为Word
            parseHtmlToWord(document, html);

            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成Word失败", e);
            throw new BusinessException("生成Word失败，请稍后重试");
        }
    }

    /**
     * 解析HTML内容并转换为Word文档
     */
    private void parseHtmlToWord(XWPFDocument document, String html) {
        if (html == null || html.isEmpty()) {
            return;
        }

        log.debug("开始解析HTML转Word, HTML长度: {}", html.length());

        // 提取body内容（使用贪婪匹配）
        String content = html;
        java.util.regex.Pattern bodyPattern = java.util.regex.Pattern.compile(
                "<body[^>]*>([\\s\\S]*)</body>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher bodyMatcher = bodyPattern.matcher(html);
        if (bodyMatcher.find()) {
            content = bodyMatcher.group(1);
            log.debug("提取到body内容, 长度: {}", content.length());
        } else {
            // 如果没有body标签，尝试去除XML声明和DOCTYPE
            content = html.replaceAll("(?i)<\\?xml[^>]*\\?>", "")
                    .replaceAll("(?i)<!DOCTYPE[^>]*>", "")
                    .replaceAll("(?i)<html[^>]*>", "")
                    .replaceAll("(?i)</html>", "")
                    .replaceAll("(?i)<head>[\\s\\S]*?</head>", "")
                    .trim();
            log.debug("无body标签，清理后内容长度: {}", content.length());
        }

        // 使用贪婪匹配找出所有表格（处理嵌套情况）
        java.util.List<int[]> tablePositions = findTablePositions(content);
        log.debug("找到 {} 个表格", tablePositions.size());

        int lastEnd = 0;
        for (int[] pos : tablePositions) {
            // 处理表格前的文本
            if (pos[0] > lastEnd) {
                String beforeTable = content.substring(lastEnd, pos[0]);
                parseTextContent(document, beforeTable);
            }

            // 处理表格
            String tableHtml = content.substring(pos[0], pos[1]);
            log.debug("处理表格, 长度: {}", tableHtml.length());
            parseTableWithMerge(document, tableHtml);

            lastEnd = pos[1];
        }

        // 处理剩余的文本
        if (lastEnd < content.length()) {
            parseTextContent(document, content.substring(lastEnd));
        }
    }

    /**
     * 查找所有顶层表格的位置（处理嵌套表格）
     */
    private java.util.List<int[]> findTablePositions(String content) {
        java.util.List<int[]> positions = new java.util.ArrayList<>();
        String lowerContent = content.toLowerCase();

        int searchStart = 0;
        while (true) {
            int tableStart = lowerContent.indexOf("<table", searchStart);
            if (tableStart == -1) break;

            // 找到配对的 </table>（考虑嵌套）
            int depth = 0;
            int pos = tableStart;
            int tableEnd = -1;

            while (pos < content.length()) {
                int nextOpen = lowerContent.indexOf("<table", pos + 1);
                int nextClose = lowerContent.indexOf("</table>", pos);

                if (nextClose == -1) break;

                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    pos = nextOpen;
                } else {
                    if (depth == 0) {
                        tableEnd = nextClose + "</table>".length();
                        break;
                    } else {
                        depth--;
                        pos = nextClose + 1;
                    }
                }
            }

            if (tableEnd > tableStart) {
                positions.add(new int[]{tableStart, tableEnd});
                searchStart = tableEnd;
            } else {
                searchStart = tableStart + 1;
            }
        }

        return positions;
    }

    /**
     * 查找指定标签的所有位置（通用方法）
     */
    private java.util.List<int[]> findTagPositions(String content, String tagName) {
        java.util.List<int[]> positions = new java.util.ArrayList<>();
        String lowerContent = content.toLowerCase();
        String openTag = "<" + tagName.toLowerCase();
        String closeTag = "</" + tagName.toLowerCase() + ">";

        int searchStart = 0;
        while (true) {
            int tagStart = lowerContent.indexOf(openTag, searchStart);
            if (tagStart == -1) break;

            // 确保是完整的标签开始（不是其他标签如 <tr> vs <track>）
            int tagNameEnd = tagStart + openTag.length();
            if (tagNameEnd < content.length()) {
                char nextChar = content.charAt(tagNameEnd);
                if (nextChar != '>' && nextChar != ' ' && nextChar != '\t' && nextChar != '\n' && nextChar != '\r') {
                    searchStart = tagStart + 1;
                    continue;
                }
            }

            // 找到配对的结束标签（考虑嵌套）
            int depth = 0;
            int pos = tagStart;
            int tagEnd = -1;

            while (pos < content.length()) {
                int nextOpen = lowerContent.indexOf(openTag, pos + 1);
                int nextClose = lowerContent.indexOf(closeTag, pos);

                if (nextClose == -1) break;

                // 检查nextOpen是否是完整的标签
                boolean validOpen = false;
                if (nextOpen != -1 && nextOpen < nextClose) {
                    int checkEnd = nextOpen + openTag.length();
                    if (checkEnd < content.length()) {
                        char c = content.charAt(checkEnd);
                        validOpen = (c == '>' || c == ' ' || c == '\t' || c == '\n' || c == '\r');
                    }
                }

                if (validOpen) {
                    depth++;
                    pos = nextOpen;
                } else {
                    if (depth == 0) {
                        tagEnd = nextClose + closeTag.length();
                        break;
                    } else {
                        depth--;
                        pos = nextClose + 1;
                    }
                }
            }

            if (tagEnd > tagStart) {
                positions.add(new int[]{tagStart, tagEnd});
                searchStart = tagEnd;
            } else {
                searchStart = tagStart + 1;
            }
        }

        return positions;
    }

    /**
     * 解析行中的所有单元格
     */
    private java.util.List<CellData> parseCells(String rowContent, boolean inThead) {
        java.util.List<CellData> cells = new java.util.ArrayList<>();

        // 查找所有 td 和 th 标签
        java.util.List<int[]> tdPositions = findTagPositions(rowContent, "td");
        java.util.List<int[]> thPositions = findTagPositions(rowContent, "th");

        // 合并并按位置排序
        java.util.List<Object[]> allCells = new java.util.ArrayList<>();
        for (int[] pos : tdPositions) {
            allCells.add(new Object[]{pos, "td"});
        }
        for (int[] pos : thPositions) {
            allCells.add(new Object[]{pos, "th"});
        }
        allCells.sort((a, b) -> ((int[]) a[0])[0] - ((int[]) b[0])[0]);

        for (Object[] cellInfo : allCells) {
            int[] pos = (int[]) cellInfo[0];
            String tagType = (String) cellInfo[1];
            String cellHtml = rowContent.substring(pos[0], pos[1]);

            // 提取属性
            int attrStart = cellHtml.indexOf('<') + 1 + tagType.length();
            int attrEnd = cellHtml.indexOf('>');
            String attrs = "";
            if (attrEnd > attrStart) {
                attrs = cellHtml.substring(attrStart, attrEnd);
            }

            // 提取内容
            int contentStart = cellHtml.indexOf('>') + 1;
            int contentEnd = cellHtml.lastIndexOf('<');
            String cellContent = "";
            if (contentEnd > contentStart) {
                cellContent = stripHtmlTags(cellHtml.substring(contentStart, contentEnd)).trim();
            }

            // 解析rowspan
            int rowspan = 1;
            java.util.regex.Matcher rsMatcher = java.util.regex.Pattern.compile(
                    "rowspan\\s*=\\s*[\"']?(\\d+)[\"']?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs);
            if (rsMatcher.find()) {
                rowspan = Integer.parseInt(rsMatcher.group(1));
            }

            // 解析colspan
            int colspan = 1;
            java.util.regex.Matcher csMatcher = java.util.regex.Pattern.compile(
                    "colspan\\s*=\\s*[\"']?(\\d+)[\"']?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs);
            if (csMatcher.find()) {
                colspan = Integer.parseInt(csMatcher.group(1));
            }

            boolean isHeader = "th".equals(tagType) || inThead;
            cells.add(new CellData(cellContent, rowspan, colspan, isHeader));
        }

        return cells;
    }

    /**
     * 解析文本内容（段落、标题等）
     */
    private void parseTextContent(XWPFDocument document, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // 检查是否有水平线
        String[] parts = text.split("(?i)<hr[^>]*/?>");

        for (int partIdx = 0; partIdx < parts.length; partIdx++) {
            String part = parts[partIdx];

            // 按<p>, <div>, <br>等分割为段落
            String[] paragraphs = part.split("(?i)</p>|</div>|<br\\s*/?>");

            for (String para : paragraphs) {
                String cleanText = stripHtmlTags(para).trim();
                if (cleanText.isEmpty()) {
                    continue;
                }

                XWPFParagraph paragraph = document.createParagraph();

                // 检测对齐方式
                if (para.contains("text-align: center") || para.contains("text-align:center")) {
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                } else if (para.contains("text-align: right") || para.contains("text-align:right")) {
                    paragraph.setAlignment(ParagraphAlignment.RIGHT);
                }

                // 检测是否是标题
                boolean isH1 = para.toLowerCase().contains("<h1");
                boolean isH2 = para.toLowerCase().contains("<h2");
                boolean isBold = para.contains("<strong") || para.contains("<b>");

                // 检测颜色
                String color = extractColor(para);

                // 检测字体大小
                int fontSize = extractFontSize(para);
                if (fontSize == 0) {
                    fontSize = 12; // 默认字体大小
                }

                XWPFRun run = paragraph.createRun();
                run.setText(cleanText);
                run.setFontFamily("宋体");

                if (isH1) {
                    run.setBold(true);
                    run.setFontSize(22);
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                } else if (isH2) {
                    run.setBold(true);
                    run.setFontSize(18);
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                } else if (isBold) {
                    run.setBold(true);
                    run.setFontSize(fontSize);
                } else {
                    run.setFontSize(fontSize);
                }

                // 设置颜色
                if (color != null && !color.isEmpty()) {
                    run.setColor(color);
                }

                // 检测缩进
                if (para.contains("text-indent: 2em") || para.contains("text-indent:2em")) {
                    paragraph.setIndentationFirstLine(480);
                }
            }

            // 在部分之间添加水平线（除了最后一个部分）
            if (partIdx < parts.length - 1) {
                addHorizontalLine(document);
            }
        }
    }

    /**
     * 从HTML中提取颜色
     */
    private String extractColor(String html) {
        // 匹配 color: red 或 color: #ff0000 或 color: rgb(255,0,0)
        java.util.regex.Pattern colorPattern = java.util.regex.Pattern.compile(
            "color\\s*:\\s*([^;\"']+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = colorPattern.matcher(html);
        if (matcher.find()) {
            String colorValue = matcher.group(1).trim().toLowerCase();
            // 转换颜色名称为十六进制
            if (colorValue.equals("red")) return "FF0000";
            if (colorValue.equals("blue")) return "0000FF";
            if (colorValue.equals("green")) return "008000";
            if (colorValue.equals("black")) return "000000";
            if (colorValue.equals("white")) return "FFFFFF";
            if (colorValue.equals("gray") || colorValue.equals("grey")) return "808080";
            if (colorValue.startsWith("#")) {
                return colorValue.substring(1).toUpperCase();
            }
            if (colorValue.startsWith("rgb")) {
                // 解析 rgb(r, g, b)
                java.util.regex.Matcher rgbMatcher = java.util.regex.Pattern.compile(
                    "rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)").matcher(colorValue);
                if (rgbMatcher.find()) {
                    int r = Integer.parseInt(rgbMatcher.group(1));
                    int g = Integer.parseInt(rgbMatcher.group(2));
                    int b = Integer.parseInt(rgbMatcher.group(3));
                    return String.format("%02X%02X%02X", r, g, b);
                }
            }
        }
        return null;
    }

    /**
     * 从HTML中提取字体大小
     */
    private int extractFontSize(String html) {
        // 匹配 font-size: 16px 或 font-size: 12pt
        java.util.regex.Pattern sizePattern = java.util.regex.Pattern.compile(
            "font-size\\s*:\\s*(\\d+)\\s*(px|pt|em)?", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = sizePattern.matcher(html);
        if (matcher.find()) {
            int size = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            if (unit != null && unit.equalsIgnoreCase("px")) {
                // px转pt: 大约 px * 0.75
                return (int) Math.round(size * 0.75);
            }
            return size;
        }
        return 0;
    }

    /**
     * 添加水平线
     */
    private void addHorizontalLine(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        // 添加一个空行来显示边框
        XWPFRun run = paragraph.createRun();
        run.setText("");
    }

    /**
     * 单元格信息类
     */
    private static class CellData {
        String content;
        int rowspan;
        int colspan;
        boolean isHeader;
        boolean isMergedCell; // 是否是被合并的单元格（不需要输出内容）

        CellData(String content, int rowspan, int colspan, boolean isHeader) {
            this.content = content;
            this.rowspan = rowspan;
            this.colspan = colspan;
            this.isHeader = isHeader;
            this.isMergedCell = false;
        }
    }

    /**
     * 解析HTML表格（支持rowspan和colspan）
     */
    private void parseTableWithMerge(XWPFDocument document, String tableHtml) {
        log.debug("开始解析表格HTML, 长度: {}", tableHtml.length());

        // 使用更可靠的方式查找所有行
        java.util.List<int[]> rowPositions = findTagPositions(tableHtml, "tr");
        log.debug("找到 {} 个表格行", rowPositions.size());

        if (rowPositions.isEmpty()) {
            log.warn("表格中没有找到任何行");
            return;
        }

        // 先收集所有原始单元格数据（包括空行）
        java.util.List<java.util.List<CellData>> rawRows = new java.util.ArrayList<>();
        String lowerHtml = tableHtml.toLowerCase();

        for (int[] pos : rowPositions) {
            String rowHtml = tableHtml.substring(pos[0], pos[1]);

            // 检查是否在thead中
            String beforeRow = lowerHtml.substring(0, pos[0]);
            boolean inThead = beforeRow.lastIndexOf("<thead") > beforeRow.lastIndexOf("</thead");

            // 提取tr标签内的内容
            String rowContent = rowHtml;
            int contentStart = rowHtml.indexOf('>');
            int contentEnd = rowHtml.lastIndexOf('<');
            if (contentStart >= 0 && contentEnd > contentStart) {
                rowContent = rowHtml.substring(contentStart + 1, contentEnd);
            }

            // 解析单元格 - 使用更可靠的方式
            java.util.List<CellData> rowCells = parseCells(rowContent, inThead);

            // 不跳过空行！空行可能是rowspan的占位行
            rawRows.add(rowCells);
            log.debug("行 {} 包含 {} 个单元格", rawRows.size(), rowCells.size());
        }

        log.debug("共解析到 {} 行（包括空行）", rawRows.size());

        // 计算实际的列数（考虑colspan）
        int maxCols = 0;
        for (int i = 0; i < rawRows.size(); i++) {
            java.util.List<CellData> row = rawRows.get(i);
            int colCount = 0;
            for (CellData cell : row) {
                colCount += cell.colspan;
            }
            if (colCount > maxCols) {
                maxCols = colCount;
                log.debug("更新maxCols={}, 来自行{}", maxCols, i + 1);
            }
        }
        log.debug("计算出最大列数: {}", maxCols);

        if (maxCols == 0) {
            log.warn("表格没有任何列数据");
            return;
        }

        // 构建完整的单元格网格（处理rowspan和colspan）
        CellData[][] grid = new CellData[rawRows.size()][maxCols];
        log.debug("创建网格: {}行 x {}列", rawRows.size(), maxCols);

        for (int rowIdx = 0; rowIdx < rawRows.size(); rowIdx++) {
            java.util.List<CellData> rawRow = rawRows.get(rowIdx);
            int colIdx = 0;
            int rawColIdx = 0;

            while (colIdx < maxCols && rawColIdx < rawRow.size()) {
                // 跳过已被上方rowspan占用的单元格
                while (colIdx < maxCols && grid[rowIdx][colIdx] != null) {
                    colIdx++;
                }

                if (colIdx >= maxCols) break;

                CellData cell = rawRow.get(rawColIdx);

                // 填充当前单元格及其rowspan/colspan范围
                for (int r = 0; r < cell.rowspan && rowIdx + r < rawRows.size(); r++) {
                    for (int c = 0; c < cell.colspan && colIdx + c < maxCols; c++) {
                        if (r == 0 && c == 0) {
                            grid[rowIdx + r][colIdx + c] = cell;
                        } else {
                            // 创建占位单元格
                            CellData placeholder = new CellData("", 1, 1, cell.isHeader);
                            placeholder.isMergedCell = true;
                            grid[rowIdx + r][colIdx + c] = placeholder;
                        }
                    }
                }

                if (cell.rowspan > 1 || cell.colspan > 1) {
                    log.debug("单元格[{},{}] rowspan={}, colspan={}, 内容: {}",
                        rowIdx, colIdx, cell.rowspan, cell.colspan,
                        cell.content.length() > 20 ? cell.content.substring(0, 20) + "..." : cell.content);
                }

                colIdx += cell.colspan;
                rawColIdx++;
            }
        }

        // 验证网格填充情况
        int filledCells = 0;
        int nullCells = 0;
        for (int r = 0; r < rawRows.size(); r++) {
            for (int c = 0; c < maxCols; c++) {
                if (grid[r][c] != null) filledCells++;
                else nullCells++;
            }
        }
        log.debug("网格填充情况: 已填充={}, 空={}", filledCells, nullCells);

        // 创建Word表格
        int rowCount = rawRows.size();
        XWPFTable table = document.createTable(rowCount, maxCols);

        // 获取表格CTTbl
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();

        // 设置表格属性
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr tblPr = ctTbl.getTblPr();
        if (tblPr == null) {
            tblPr = ctTbl.addNewTblPr();
        }

        // 设置表格宽度为100%（5000 = 100%，twips单位）
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblWidth = tblPr.addNewTblW();
        tblWidth.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.PCT);
        tblWidth.setW(java.math.BigInteger.valueOf(5000));

        // 设置表格网格（定义列宽）
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid tblGrid = ctTbl.getTblGrid();
        if (tblGrid == null) {
            tblGrid = ctTbl.addNewTblGrid();
        }
        // 清除现有的gridCol
        while (tblGrid.sizeOfGridColArray() > 0) {
            tblGrid.removeGridCol(0);
        }
        // 平均分配列宽
        int colWidth = 9000 / maxCols; // 总宽度约为9000 twips
        for (int i = 0; i < maxCols; i++) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol gridCol = tblGrid.addNewGridCol();
            gridCol.setW(java.math.BigInteger.valueOf(colWidth));
        }

        // 设置表格边框
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders borders = tblPr.addNewTblBorders();
        setBorder(borders.addNewTop());
        setBorder(borders.addNewBottom());
        setBorder(borders.addNewLeft());
        setBorder(borders.addNewRight());
        setBorder(borders.addNewInsideH());
        setBorder(borders.addNewInsideV());

        // 填充数据并处理合并
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            XWPFTableRow row = table.getRow(rowIdx);

            for (int colIdx = 0; colIdx < maxCols; colIdx++) {
                XWPFTableCell cell = row.getCell(colIdx);
                if (cell == null) {
                    cell = row.createCell();
                }

                CellData cellData = grid[rowIdx][colIdx];
                if (cellData == null) {
                    continue;
                }

                // 获取或创建单元格属性（只创建一次）
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc ctTc = cell.getCTTc();
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = ctTc.getTcPr();
                if (tcPr == null) {
                    tcPr = ctTc.addNewTcPr();
                }

                // 处理垂直合并 (rowspan)
                if (cellData.rowspan > 1 && !cellData.isMergedCell) {
                    // 这是合并的起始单元格
                    tcPr.addNewVMerge().setVal(
                            org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.RESTART);
                } else if (cellData.isMergedCell) {
                    // 被合并的单元格 - 只需添加vMerge元素，不设置val（等同于CONTINUE）
                    tcPr.addNewVMerge();
                }

                // 处理水平合并 (colspan)
                if (cellData.colspan > 1 && !cellData.isMergedCell) {
                    tcPr.addNewGridSpan().setVal(java.math.BigInteger.valueOf(cellData.colspan));
                }

                // 设置单元格内容
                if (cell.getParagraphs().size() > 0) {
                    cell.removeParagraph(0);
                }

                XWPFParagraph cellPara = cell.addParagraph();
                cellPara.setAlignment(ParagraphAlignment.CENTER);
                // 设置垂直居中
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

                if (!cellData.isMergedCell) {
                    XWPFRun cellRun = cellPara.createRun();
                    cellRun.setText(cellData.content);
                    cellRun.setFontFamily("宋体");
                    cellRun.setFontSize(10);

                    if (cellData.isHeader) {
                        cell.setColor("F5F5F5");
                        cellRun.setBold(true);
                    }
                }
            }
        }

        // 添加空行
        document.createParagraph();
    }

    /**
     * 设置表格边框样式
     */
    private void setBorder(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border) {
        border.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
        border.setSz(java.math.BigInteger.valueOf(4));
        border.setColor("000000");
    }

    /**
     * 解析HTML表格（旧方法，保留兼容）
     */
    @Deprecated
    private void parseTable(XWPFDocument document, String tableHtml) {
        parseTableWithMerge(document, tableHtml);
    }

    /**
     * 去除HTML标签
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        // 先处理特殊字符
        String text = html.replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"");
        // 去除HTML标签
        text = text.replaceAll("<[^>]+>", "");
        // 合并多个空格
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    /**
     * 生成Excel
     */
    public byte[] generateExcel(List<ExportPreviewDTO.StudentRecordDTO> records,
                                 ExportTemplateDTO.TableConfig tableConfig) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("违纪学生名单");

            List<ExportTemplateDTO.ColumnConfig> columns = tableConfig.getColumns();
            if (columns == null || columns.isEmpty()) {
                columns = List.of(
                        createColumn("index", "序号"),
                        createColumn("studentNo", "学号"),
                        createColumn("studentName", "姓名"),
                        createColumn("className", "班级"),
                        createColumn("deductionItemName", "扣分项")
                );
            }

            // 创建表头样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 表头行
            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).getLabel());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, columns.get(i).getWidth() != null ? columns.get(i).getWidth() * 50 : 4000);
            }

            // 数据行
            int rowIndex = 1;
            for (ExportPreviewDTO.StudentRecordDTO record : records) {
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowIndex);
                for (int i = 0; i < columns.size(); i++) {
                    org.apache.poi.xssf.usermodel.XSSFCell cell = row.createCell(i);
                    cell.setCellValue(getFieldValue(record, columns.get(i).getField(), rowIndex));
                }
                rowIndex++;
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成Excel失败", e);
            throw new BusinessException("生成Excel失败，请稍后重试");
        }
    }

    private ExportTemplateDTO.ColumnConfig createColumn(String field, String label) {
        ExportTemplateDTO.ColumnConfig col = new ExportTemplateDTO.ColumnConfig();
        col.setField(field);
        col.setLabel(label);
        col.setWidth(80);
        return col;
    }

    private String getFieldValue(ExportPreviewDTO.StudentRecordDTO record, String field, int index) {
        if (record == null || field == null) return "";
        return switch (field) {
            case "index" -> String.valueOf(index);
            case "studentNo" -> record.getStudentNo() != null ? record.getStudentNo() : "";
            case "studentName" -> record.getStudentName() != null ? record.getStudentName() : "";
            case "gender" -> record.getGender() != null ? record.getGender() : "";
            case "className" -> record.getClassName() != null ? record.getClassName() : "";
            case "gradeName" -> record.getGradeName() != null ? record.getGradeName() : "";
            case "departmentName" -> record.getDepartmentName() != null ? record.getDepartmentName() : "";
            case "buildingName" -> record.getBuildingName() != null ? record.getBuildingName() : "";
            case "roomNo" -> record.getRoomNo() != null ? record.getRoomNo() : "";
            case "categoryName" -> record.getCategoryName() != null ? record.getCategoryName() : "";
            case "relationType" -> record.getRelationType() != null ? record.getRelationType() : "";
            case "deductionItemName" -> record.getDeductionItemName() != null ? record.getDeductionItemName() : "";
            case "deductMode" -> record.getDeductMode() != null ? record.getDeductMode() : "";
            case "deductScore" -> record.getDeductScore() != null ? String.valueOf(record.getDeductScore()) : "";
            case "originalTotalScore" -> record.getOriginalTotalScore() != null ? String.valueOf(record.getOriginalTotalScore()) : "";
            case "weightedDeductScore" -> record.getWeightedDeductScore() != null ? String.valueOf(record.getWeightedDeductScore()) : "";
            case "totalWeightedDeductScore" -> record.getTotalWeightedDeductScore() != null ? String.valueOf(record.getTotalWeightedDeductScore()) : "";
            case "categoryOriginalScore" -> record.getCategoryOriginalScore() != null ? String.valueOf(record.getCategoryOriginalScore()) : "";
            case "categoryWeightedScore" -> record.getCategoryWeightedScore() != null ? String.valueOf(record.getCategoryWeightedScore()) : "";
            case "classOriginalScore" -> record.getClassOriginalScore() != null ? String.valueOf(record.getClassOriginalScore()) : "";
            case "classWeightedScore" -> record.getClassWeightedScore() != null ? String.valueOf(record.getClassWeightedScore()) : "";
            case "remark" -> record.getRemark() != null ? record.getRemark() : "";
            case "checkerName" -> record.getCheckerName() != null ? record.getCheckerName() : "";
            case "personCount" -> record.getPersonCount() != null ? String.valueOf(record.getPersonCount()) : "1";
            default -> "";
        };
    }
}
