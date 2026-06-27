package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetCodeApplicationService;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.oned.Code128Writer;

/**
 * Asset Code / Label REST Controller
 *
 * Generates asset codes, QR codes, barcodes, and print labels.
 * QR codes and barcodes are returned as Base64-encoded data URIs.
 */
@Slf4j
@RestController
@RequestMapping("/asset-codes")
@RequiredArgsConstructor
public class AssetCodeController {

    private final AssetCodeApplicationService assetCodeService;

    // ==================== Generate Asset Code ====================

    @PostMapping("/generate")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<String> generateAssetCode(
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "0") int currentMaxSeq) {
        int nextSeq = Math.max(currentMaxSeq, assetCodeService.countExistingAssetCodes(categoryCode)) + 1;
        String code = categoryCode + "-" + String.format("%04d", nextSeq);
        return Result.success(code);
    }

    // ==================== Batch Generate Codes ====================

    @PostMapping("/generate-batch")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<List<String>> generateAssetCodes(
            @RequestParam String categoryCode,
            @RequestParam int count,
            @RequestParam(defaultValue = "0") int currentMaxSeq) {
        int startSeq = Math.max(currentMaxSeq, assetCodeService.countExistingAssetCodes(categoryCode));
        List<String> codes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            codes.add(categoryCode + "-" + String.format("%04d", startSeq + i));
        }
        return Result.success(codes);
    }

    // ==================== Generate QR Code ====================

    @GetMapping("/qrcode")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> generateQRCode(
            @RequestParam String assetCode,
            @RequestParam Long assetId,
            @RequestParam(defaultValue = "200") int size) {

        // Generate QR code content as a simple data URI (SVG-based)
        String content = "ASSET:" + assetCode + ":" + assetId;
        String qrSvg = generateSimpleQRSvg(content, size);
        String base64 = "data:image/svg+xml;base64," +
            Base64.getEncoder().encodeToString(qrSvg.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = new HashMap<>();
        result.put("qrcode", base64);
        result.put("assetCode", assetCode);
        return Result.success(result);
    }

    // ==================== Generate Barcode ====================

    @GetMapping("/barcode")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> generateBarcode(
            @RequestParam String assetCode,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "100") int height) {

        String barSvg = generateSimpleBarcodeSvg(assetCode, width, height);
        String base64 = "data:image/svg+xml;base64," +
            Base64.getEncoder().encodeToString(barSvg.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = new HashMap<>();
        result.put("barcode", base64);
        result.put("assetCode", assetCode);
        return Result.success(result);
    }

    // ==================== Parse QR Code ====================

    @PostMapping("/parse-qrcode")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Map<String, Object>> parseQRCode(@RequestParam String content) {
        Map<String, Object> result = new HashMap<>();

        if (content != null && content.startsWith("ASSET:")) {
            String[] parts = content.split(":");
            result.put("type", "ASSET");
            result.put("assetCode", parts.length > 1 ? parts[1] : "");
            result.put("assetId", parts.length > 2 ? parts[2] : "");
        } else {
            result.put("type", "UNKNOWN");
            result.put("assetCode", content);
            result.put("assetId", "");
        }

        return Result.success(result);
    }

    // ==================== Generate Label ====================

    @GetMapping("/label")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> generateLabel(
            @RequestParam String assetCode,
            @RequestParam Long assetId,
            @RequestParam String assetName,
            @RequestParam(required = false) String location) {

        String qrContent = "ASSET:" + assetCode + ":" + assetId;
        String qrSvg = generateSimpleQRSvg(qrContent, 150);
        String qrBase64 = "data:image/svg+xml;base64," +
            Base64.getEncoder().encodeToString(qrSvg.getBytes(StandardCharsets.UTF_8));

        String barSvg = generateSimpleBarcodeSvg(assetCode, 200, 60);
        String barBase64 = "data:image/svg+xml;base64," +
            Base64.getEncoder().encodeToString(barSvg.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = new HashMap<>();
        result.put("assetCode", assetCode);
        result.put("assetName", assetName);
        result.put("location", location);
        result.put("qrcode", qrBase64);
        result.put("barcode", barBase64);
        return Result.success(result);
    }

    // ==================== Batch Generate Labels ====================

    @PostMapping("/labels")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<List<Map<String, Object>>> generateLabels(@RequestBody List<Map<String, Object>> requests) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> req : requests) {
            String assetCode = (String) req.get("assetCode");
            Long assetId = req.get("assetId") != null ? ((Number) req.get("assetId")).longValue() : 0L;
            String assetName = (String) req.get("assetName");
            String location = (String) req.get("location");

            String qrContent = "ASSET:" + assetCode + ":" + assetId;
            String qrSvg = generateSimpleQRSvg(qrContent, 150);
            String qrBase64 = "data:image/svg+xml;base64," +
                Base64.getEncoder().encodeToString(qrSvg.getBytes(StandardCharsets.UTF_8));

            String barSvg = generateSimpleBarcodeSvg(assetCode, 200, 60);
            String barBase64 = "data:image/svg+xml;base64," +
                Base64.getEncoder().encodeToString(barSvg.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> label = new HashMap<>();
            label.put("assetCode", assetCode);
            label.put("assetName", assetName);
            label.put("location", location);
            label.put("qrcode", qrBase64);
            label.put("barcode", barBase64);
            results.add(label);
        }

        return Result.success(results);
    }

    // ==================== SVG Generators ====================

    /**
     * 生成真二维码 SVG (ZXing QRCodeWriter)。原占位实现是 Random(hash) 画的假图案, 扫不出来。
     * BitMatrix 按行 run-length 合并为 rect, viewBox=模块数 → width/height 缩放到请求 size。
     */
    private String generateSimpleQRSvg(String content, int size) {
        try {
            BitMatrix m = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size,
                    java.util.Map.of(EncodeHintType.MARGIN, 1));
            int w = m.getWidth(), h = m.getHeight();
            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(size)
               .append("' height='").append(size).append("' viewBox='0 0 ").append(w).append(' ').append(h).append("'>");
            svg.append("<rect width='").append(w).append("' height='").append(h).append("' fill='white'/>");
            for (int y = 0; y < h; y++) {
                int x = 0;
                while (x < w) {
                    if (m.get(x, y)) {
                        int run = 1;
                        while (x + run < w && m.get(x + run, y)) run++;
                        svg.append("<rect x='").append(x).append("' y='").append(y)
                           .append("' width='").append(run).append("' height='1' fill='black'/>");
                        x += run;
                    } else {
                        x++;
                    }
                }
            }
            svg.append("</svg>");
            return svg.toString();
        } catch (WriterException e) {
            throw new IllegalStateException("二维码生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成真 Code128 条形码 SVG (ZXing Code128Writer)。原占位是随机竖条, 扫不出来。
     * BitMatrix 1D, 合并连续黑列为竖条; 下方附人眼可读文本。
     */
    private String generateSimpleBarcodeSvg(String content, int width, int height) {
        try {
            BitMatrix m = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height);
            int w = m.getWidth();
            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
               .append("' height='").append(height + 20).append("'>");
            svg.append("<rect width='").append(width).append("' height='").append(height + 20)
               .append("' fill='white'/>");
            int x = 0;
            while (x < w) {
                if (m.get(x, 0)) {
                    int run = 1;
                    while (x + run < w && m.get(x + run, 0)) run++;
                    svg.append("<rect x='").append(x).append("' y='2' width='").append(run)
                       .append("' height='").append(height - 4).append("' fill='black'/>");
                    x += run;
                } else {
                    x++;
                }
            }
            svg.append("<text x='").append(width / 2).append("' y='").append(height + 14)
               .append("' text-anchor='middle' font-family='monospace' font-size='11'>")
               .append(escapeXml(content)).append("</text>");
            svg.append("</svg>");
            return svg.toString();
        } catch (IllegalArgumentException e) {
            // Code128 仅支持 ASCII; 内容不可编码时 Code128Writer 抛 IllegalArgumentException。
            throw new IllegalStateException("条形码生成失败 (内容含不支持字符?): " + e.getMessage(), e);
        }
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
