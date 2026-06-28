package com.edumanager.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具
 */
public final class QrCodeUtil {

    private QrCodeUtil() {}

    /**
     * 生成二维码并返回 Base64 PNG
     *
     * @param content 二维码内容（JSON 字符串如 {"scheduleId":1,"token":"abc"}）
     * @param size    图片尺寸（像素）
     * @return Base64 编码的 PNG 图片 data URI
     */
    public static String generateBase64(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.BLACK);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (matrix.get(x, y)) {
                        image.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("二维码生成失败", e);
        }
    }
}
