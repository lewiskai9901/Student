package com.school.management.interfaces.rest.asset;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

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

    private final JdbcTemplate jdbc;

    // ==================== Generate Asset Code ====================

    @PostMapping("/generate")
    public Result<String> generateAssetCode(
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "0") int currentMaxSeq) {

        // Find the next sequence number
        int nextSeq = currentMaxSeq;
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset WHERE asset_code LIKE ? AND deleted = 0",
            Long.class, categoryCode + "-%");
        if (count != null) {
            nextSeq = Math.max(nextSeq, count.intValue());
        }
        nextSeq++;

        String code = categoryCode + "-" + String.format("%04d", nextSeq);
        return Result.success(code);
    }

    // ==================== Batch Generate Codes ====================

    @PostMapping("/generate-batch")
    public Result<List<String>> generateAssetCodes(
            @RequestParam String categoryCode,
            @RequestParam int count,
            @RequestParam(defaultValue = "0") int currentMaxSeq) {

        int startSeq = currentMaxSeq;
        Long existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset WHERE asset_code LIKE ? AND deleted = 0",
            Long.class, categoryCode + "-%");
        if (existing != null) {
            startSeq = Math.max(startSeq, existing.intValue());
        }

        List<String> codes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            codes.add(categoryCode + "-" + String.format("%04d", startSeq + i));
        }
        return Result.success(codes);
    }

    // ==================== Generate QR Code ====================

    @GetMapping("/qrcode")
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
     * Generate a simple placeholder QR code SVG.
     * In production, use a real QR code library (e.g. ZXing).
     */
    private String generateSimpleQRSvg(String content, int size) {
        // Generate a deterministic pattern from content hash
        int hash = content.hashCode();
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(size)
           .append("' height='").append(size).append("' viewBox='0 0 25 25'>");
        svg.append("<rect width='25' height='25' fill='white'/>");

        // Position detection patterns (the three large squares)
        drawFinderPattern(svg, 0, 0);
        drawFinderPattern(svg, 18, 0);
        drawFinderPattern(svg, 0, 18);

        // Fill some data modules based on hash
        Random rng = new Random(hash);
        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 25; x++) {
                if (isFinderArea(x, y)) continue;
                if (rng.nextBoolean()) {
                    svg.append("<rect x='").append(x).append("' y='").append(y)
                       .append("' width='1' height='1' fill='black'/>");
                }
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private void drawFinderPattern(StringBuilder svg, int ox, int oy) {
        // Outer 7x7 black border
        svg.append("<rect x='").append(ox).append("' y='").append(oy)
           .append("' width='7' height='7' fill='black'/>");
        // Inner 5x5 white
        svg.append("<rect x='").append(ox + 1).append("' y='").append(oy + 1)
           .append("' width='5' height='5' fill='white'/>");
        // Center 3x3 black
        svg.append("<rect x='").append(ox + 2).append("' y='").append(oy + 2)
           .append("' width='3' height='3' fill='black'/>");
    }

    private boolean isFinderArea(int x, int y) {
        if (x < 8 && y < 8) return true;
        if (x >= 17 && y < 8) return true;
        if (x < 8 && y >= 17) return true;
        return false;
    }

    /**
     * Generate a simple placeholder barcode SVG (Code 128-like).
     */
    private String generateSimpleBarcodeSvg(String content, int width, int height) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
           .append("' height='").append(height + 20).append("'>");
        svg.append("<rect width='").append(width).append("' height='").append(height + 20)
           .append("' fill='white'/>");

        // Generate bars from content
        Random rng = new Random(content.hashCode());
        double x = 10;
        double maxX = width - 10;
        while (x < maxX) {
            double barWidth = 1 + rng.nextInt(3);
            if (rng.nextBoolean()) {
                svg.append("<rect x='").append(String.format("%.1f", x))
                   .append("' y='2' width='").append(String.format("%.1f", barWidth))
                   .append("' height='").append(height - 4).append("' fill='black'/>");
            }
            x += barWidth + 1;
        }

        // Add text
        svg.append("<text x='").append(width / 2).append("' y='").append(height + 14)
           .append("' text-anchor='middle' font-family='monospace' font-size='11'>")
           .append(escapeXml(content)).append("</text>");

        svg.append("</svg>");
        return svg.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
