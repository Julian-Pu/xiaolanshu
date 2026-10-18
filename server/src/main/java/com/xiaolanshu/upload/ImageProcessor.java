package com.xiaolanshu.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 图片处理管线（开发期本机 ImageIO 实现；生产可替换为对象存储 + 服务端图片处理）：
 * - 缩略图：jpg/jpeg/png 等比缩放到 maxSide（默认 600px），存 uploads/thumbs/
 * - 水印：右下角叠加半透明「小蓝书」文字水印（可配置开关）
 * - gif（动图）/ svg / webp 不做处理，thumbUrl 回落为原图 URL
 */
@Component
public class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);
    private static final Set<String> PROCESSABLE = Set.of("jpg", "jpeg", "png");
    private static final int DEFAULT_MAX_SIDE = 600;

    private final int maxSide;
    private final boolean watermarkEnabled;
    private final Font watermarkFont;

    public ImageProcessor(
            @Value("${xiaolanshu.image.thumb-max-side:600}") int maxSide,
            @Value("${xiaolanshu.image.watermark:true}") boolean watermarkEnabled) {
        this.maxSide = maxSide;
        this.watermarkEnabled = watermarkEnabled;
        this.watermarkFont = findChineseFont();
    }

    /** 是否支持生成缩略图 */
    public boolean processable(String ext) {
        return PROCESSABLE.contains(ext.toLowerCase(Locale.ROOT));
    }

    /** 由原图 URL 推导缩略图 URL；不支持的格式返回原图 URL（约定式，无需改库） */
    public String thumbnailOf(String url) {
        if (url == null) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        if (slash < 0) {
            return url;
        }
        String name = url.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return url;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!processable(ext)) {
            return url;
        }
        return "/uploads/thumbs/thumb-" + name;
    }

    /** 生成缩略图文件；失败时静默（调用方回落原图），返回是否成功 */
    public boolean thumbnail(File src, File dest) {
        try {
            BufferedImage img = ImageIO.read(src);
            if (img == null) {
                return false;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double scale = Math.min(1.0, (double) maxSide / Math.max(w, h));
            int nw = Math.max(1, (int) (w * scale));
            int nh = Math.max(1, (int) (h * scale));
            BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            if (watermarkEnabled) {
                applyWatermark(out);
            }
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            return ImageIO.write(out, "jpg", dest);
        } catch (IOException | RuntimeException e) {
            log.warn("缩略图生成失败，回落原图: {}", e.getMessage());
            return false;
        }
    }

    private void applyWatermark(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        String text = "小蓝书";
        int fontSize = Math.max(12, img.getWidth() / 18);
        Font font = watermarkFont.deriveFont(Font.PLAIN, fontSize);
        g.setFont(font);
        int pad = Math.max(6, fontSize / 2);
        int w = g.getFontMetrics().stringWidth(text);
        int x = img.getWidth() - w - pad;
        int y = img.getHeight() - pad;
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRoundRect(x - pad / 2, y - fontSize - pad / 2 + 4, w + pad, fontSize + pad, fontSize / 2, fontSize / 2);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(text, x, y);
        g.dispose();
    }

    /** 找一个能渲染中文的字体（Windows 常见字体） */
    private Font findChineseFont() {
        String[] names = {"微软雅黑", "Microsoft YaHei", "SimSun", "宋体", "PingFang SC"};
        for (String n : names) {
            if (java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames().length > 0
                    && contains(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getAvailableFontFamilyNames(), n)) {
                return new Font(n, Font.PLAIN, 16);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    }

    private static boolean contains(String[] arr, String target) {
        for (String s : arr) {
            if (s.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}
