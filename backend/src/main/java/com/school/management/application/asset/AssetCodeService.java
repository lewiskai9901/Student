package com.school.management.application.asset;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资产编码服务
 * 提供资产编码生成和二维码生成功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCodeService {

    // 编码序列号缓存（按分类前缀+年份）
    private static final Map<String, AtomicLong> SEQUENCE_CACHE = new HashMap<>();

    /**
     * 生成资产编码
     * 格式：{分类前缀}-{年份}-{5位序号}
     * 示例：DJ-2026-00001
     *
     * @param categoryCode 分类编码
     * @param currentMaxSeq 当前最大序号
     * @return 资产编码
     */
    public String generateAssetCode(String categoryCode, long currentMaxSeq) {
        String prefix = extractPrefix(categoryCode);
        int year = LocalDate.now().getYear();
        String yearStr = String.valueOf(year);

        String cacheKey = prefix + "-" + yearStr;
        AtomicLong sequence = SEQUENCE_CACHE.computeIfAbsent(cacheKey,
                k -> new AtomicLong(currentMaxSeq));

        long seq = sequence.incrementAndGet();
        return String.format("%s-%s-%05d", prefix, yearStr, seq);
    }

    /**
     * 批量生成资产编码
     */
    public String[] generateAssetCodes(String categoryCode, int count, long currentMaxSeq) {
        String[] codes = new String[count];
        String prefix = extractPrefix(categoryCode);
        int year = LocalDate.now().getYear();
        String yearStr = String.valueOf(year);

        String cacheKey = prefix + "-" + yearStr;
        AtomicLong sequence = SEQUENCE_CACHE.computeIfAbsent(cacheKey,
                k -> new AtomicLong(currentMaxSeq));

        for (int i = 0; i < count; i++) {
            long seq = sequence.incrementAndGet();
            codes[i] = String.format("%s-%s-%05d", prefix, yearStr, seq);
        }
        return codes;
    }

    /**
     * 从分类编码提取前缀（取前2-4个字符的拼音首字母）
     */
    private String extractPrefix(String categoryCode) {
        if (categoryCode == null || categoryCode.isEmpty()) {
            return "ZC"; // 默认：资产
        }
        // 如果分类编码已经是大写字母格式，直接使用
        String upper = categoryCode.toUpperCase();
        if (upper.matches("^[A-Z]{2,4}.*")) {
            return upper.substring(0, Math.min(4, upper.indexOf('-') > 0 ?
                    upper.indexOf('-') : upper.length()));
        }
        // 否则取前两个字符
        return upper.substring(0, Math.min(2, upper.length()));
    }

    /**
     * 生成二维码（Base64编码的PNG图片）
     *
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @return Base64编码的PNG图片
     */
    public String generateQRCode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate QR code for content: {}", content, e);
            return null;
        }
    }

    /**
     * 生成资产二维码
     * 内容格式：ASSET:{assetCode}:{assetId}
     */
    public String generateAssetQRCode(String assetCode, Long assetId) {
        String content = String.format("ASSET:%s:%d", assetCode, assetId);
        return generateQRCode(content, 200, 200);
    }

    /**
     * 生成条形码（Base64编码的PNG图片）
     *
     * @param content 条形码内容
     * @param width 宽度
     * @param height 高度
     * @return Base64编码的PNG图片
     */
    public String generateBarcode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.CODE_128,
                    width,
                    height,
                    hints
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate barcode for content: {}", content, e);
            return null;
        }
    }

    /**
     * 解析二维码内容
     * @return [type, assetCode, assetId] 或 null
     */
    public String[] parseQRCodeContent(String content) {
        if (content == null || !content.startsWith("ASSET:")) {
            return null;
        }
        String[] parts = content.split(":");
        if (parts.length >= 3) {
            return new String[]{parts[0], parts[1], parts[2]};
        }
        return null;
    }
}
