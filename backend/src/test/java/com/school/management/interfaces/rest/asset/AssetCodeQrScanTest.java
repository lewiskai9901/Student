package com.school.management.interfaces.rest.asset;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 区块3-A 验收: 资产二维码必须是**真能扫出来的** QR (原占位是 Random 画的假图案, 永远扫不出)。
 *
 * <p>直接调控制器私有 generateSimpleQRSvg → 解析它输出的 SVG 反推回 BitMatrix → 用 ZXing
 * QRCodeReader 解码, 断言解出的文本 == 原始内容。这同时验证了 ① ZXing 编码正确 ② 我的
 * BitMatrix→SVG run-length 转换忠实 (端到端, 非只测库)。
 */
class AssetCodeQrScanTest {

    private static final Pattern BLACK_RECT = Pattern.compile(
            "<rect x='(\\d+)' y='(\\d+)' width='(\\d+)' height='1' fill='black'/>");
    private static final Pattern VIEWBOX = Pattern.compile("viewBox='0 0 (\\d+) (\\d+)'");

    @Test
    @DisplayName("资产二维码 SVG 可被 ZXing 扫码解出原文 (真 QR 非假图案)")
    void qrSvgIsScannable() throws Exception {
        AssetCodeController controller = new AssetCodeController(null);
        String content = "ASSET:AC-2026-001:1907334028000000001";

        String svg = (String) ReflectionTestUtils.invokeMethod(controller, "generateSimpleQRSvg", content, 200);
        assertThat(svg).isNotNull();

        BitMatrix matrix = parseSvgToMatrix(svg);
        var result = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(MatrixToImageWriter.toBufferedImage(matrix)))));

        assertThat(result.getText()).isEqualTo(content);
    }

    /** 把控制器输出的 SVG (viewBox=模块数 + 每个黑 run 一个 rect) 反解析为 BitMatrix。 */
    private BitMatrix parseSvgToMatrix(String svg) {
        Matcher vb = VIEWBOX.matcher(svg);
        assertThat(vb.find()).as("SVG 必须有动态 viewBox (=ZXing 模块数, 证明非固定 25x25 假图)").isTrue();
        int n = Integer.parseInt(vb.group(1));
        BitMatrix matrix = new BitMatrix(n, n);
        Matcher m = BLACK_RECT.matcher(svg);
        int rects = 0;
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            int w = Integer.parseInt(m.group(3));
            for (int i = 0; i < w; i++) matrix.set(x + i, y);
            rects++;
        }
        assertThat(rects).as("应有多个黑模块").isGreaterThan(10);
        return matrix;
    }
}
