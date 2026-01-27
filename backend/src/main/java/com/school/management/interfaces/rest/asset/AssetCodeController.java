package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetCodeService;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 资产编码控制器
 * 提供资产编码生成和二维码/条形码生成功能
 */
@Tag(name = "资产编码", description = "资产编码和二维码管理接口")
@RestController
@RequestMapping("/asset-codes")
@RequiredArgsConstructor
public class AssetCodeController {

    private final AssetCodeService codeService;

    @Operation(summary = "生成资产编码")
    @PostMapping("/generate")
    public Result<String> generateAssetCode(
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "0") long currentMaxSeq
    ) {
        String code = codeService.generateAssetCode(categoryCode, currentMaxSeq);
        return Result.success(code);
    }

    @Operation(summary = "批量生成资产编码")
    @PostMapping("/generate-batch")
    public Result<String[]> generateAssetCodes(
            @RequestParam String categoryCode,
            @RequestParam int count,
            @RequestParam(defaultValue = "0") long currentMaxSeq
    ) {
        String[] codes = codeService.generateAssetCodes(categoryCode, count, currentMaxSeq);
        return Result.success(codes);
    }

    @Operation(summary = "生成资产二维码")
    @GetMapping("/qrcode")
    public Result<Map<String, String>> generateQRCode(
            @RequestParam String assetCode,
            @RequestParam Long assetId,
            @RequestParam(defaultValue = "200") int size
    ) {
        String qrCode = codeService.generateAssetQRCode(assetCode, assetId);

        Map<String, String> result = new HashMap<>();
        result.put("qrcode", qrCode);
        result.put("assetCode", assetCode);
        return Result.success(result);
    }

    @Operation(summary = "生成条形码")
    @GetMapping("/barcode")
    public Result<Map<String, String>> generateBarcode(
            @RequestParam String assetCode,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "100") int height
    ) {
        String barcode = codeService.generateBarcode(assetCode, width, height);

        Map<String, String> result = new HashMap<>();
        result.put("barcode", barcode);
        result.put("assetCode", assetCode);
        return Result.success(result);
    }

    @Operation(summary = "解析二维码内容")
    @PostMapping("/parse-qrcode")
    public Result<Map<String, String>> parseQRCode(@RequestParam String content) {
        String[] parts = codeService.parseQRCodeContent(content);
        if (parts == null) {
            return Result.fail("无效的二维码内容");
        }

        Map<String, String> result = new HashMap<>();
        result.put("type", parts[0]);
        result.put("assetCode", parts[1]);
        result.put("assetId", parts[2]);
        return Result.success(result);
    }

    @Operation(summary = "生成打印用的完整标签数据")
    @GetMapping("/label")
    public Result<Map<String, Object>> generateLabel(
            @RequestParam String assetCode,
            @RequestParam Long assetId,
            @RequestParam String assetName,
            @RequestParam(required = false) String location
    ) {
        Map<String, Object> label = new HashMap<>();
        label.put("assetCode", assetCode);
        label.put("assetName", assetName);
        label.put("location", location);
        label.put("qrcode", codeService.generateAssetQRCode(assetCode, assetId));
        label.put("barcode", codeService.generateBarcode(assetCode, 250, 80));

        return Result.success(label);
    }

    @Operation(summary = "批量生成标签数据")
    @PostMapping("/labels")
    public Result<java.util.List<Map<String, Object>>> generateLabels(
            @RequestBody java.util.List<LabelRequest> requests
    ) {
        java.util.List<Map<String, Object>> labels = new java.util.ArrayList<>();

        for (LabelRequest req : requests) {
            Map<String, Object> label = new HashMap<>();
            label.put("assetCode", req.getAssetCode());
            label.put("assetName", req.getAssetName());
            label.put("location", req.getLocation());
            label.put("qrcode", codeService.generateAssetQRCode(req.getAssetCode(), req.getAssetId()));
            label.put("barcode", codeService.generateBarcode(req.getAssetCode(), 250, 80));
            labels.add(label);
        }

        return Result.success(labels);
    }
}
