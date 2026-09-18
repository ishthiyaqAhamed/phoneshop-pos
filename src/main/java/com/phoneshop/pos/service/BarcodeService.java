package com.phoneshop.pos.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.util.FormatUtil;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class BarcodeService {
    private static final Logger logger = LoggerFactory.getLogger(BarcodeService.class);

    public BufferedImage generateBarcodeBufferedImage(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            text = "00000000";
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text.trim(),
                    BarcodeFormat.CODE_128,
                    width,
                    height,
                    hints
            );
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            logger.error("Failed to generate barcode for text '{}': {}", text, e.getMessage());
            // Fallback image
            BufferedImage fallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = fallback.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.RED);
            g.drawString("Barcode Error", 10, height / 2);
            g.dispose();
            return fallback;
        }
    }

    public Image generateBarcodeFxImage(String text, int width, int height) {
        BufferedImage bi = generateBarcodeBufferedImage(text, width, height);
        return SwingFXUtils.toFXImage(bi, null);
    }

    /**
     * Generates a complete phone shop price & warranty barcode tag (e.g. 50mm x 35mm label)
     */
    public BufferedImage generateProductPriceTagLabel(Product product, int labelWidthPx, int labelHeightPx) {
        BufferedImage image = new BufferedImage(labelWidthPx, labelHeightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable Anti-Aliasing for crisp text
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // White background & border
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, labelWidthPx, labelHeightPx);
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawRect(0, 0, labelWidthPx - 1, labelHeightPx - 1);

        // 1. Shop Header
        g2d.setColor(new Color(15, 23, 42)); // Slate dark
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String shopName = AppConfig.getShopName();
        int shopX = (labelWidthPx - fm.stringWidth(shopName)) / 2;
        g2d.drawString(shopName, Math.max(5, shopX), 16);

        // 2. Product Name & Specs
        g2d.setColor(new Color(30, 41, 59));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        fm = g2d.getFontMetrics();
        String prodName = product.getName();
        if (prodName.length() > 24) {
            prodName = prodName.substring(0, 22) + "...";
        }
        int nameX = (labelWidthPx - fm.stringWidth(prodName)) / 2;
        g2d.drawString(prodName, Math.max(5, nameX), 33);

        // Specs (Storage / Color / Condition)
        g2d.setColor(new Color(100, 116, 139));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        fm = g2d.getFontMetrics();
        StringBuilder specs = new StringBuilder();
        if (product.getStorageRam() != null && !product.getStorageRam().isBlank()) {
            specs.append(product.getStorageRam());
        }
        if (product.getColor() != null && !product.getColor().isBlank()) {
            if (specs.length() > 0) specs.append(" | ");
            specs.append(product.getColor());
        }
        if (specs.length() == 0 && product.getBrand() != null) {
            specs.append(product.getBrand());
        }
        String specsStr = specs.toString();
        int specsX = (labelWidthPx - fm.stringWidth(specsStr)) / 2;
        g2d.drawString(specsStr, Math.max(5, specsX), 47);

        // 3. Barcode graphic in the middle
        int barcodeHeight = 36;
        int barcodeWidth = labelWidthPx - 30;
        BufferedImage barcode = generateBarcodeBufferedImage(product.getBarcode(), barcodeWidth, barcodeHeight);
        g2d.drawImage(barcode, 15, 52, null);

        // Barcode number text under barcode
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        fm = g2d.getFontMetrics();
        String codeText = product.getBarcode();
        int codeX = (labelWidthPx - fm.stringWidth(codeText)) / 2;
        g2d.drawString(codeText, Math.max(5, codeX), 98);

        // 4. Warranty Badge & Price
        // Warranty box
        g2d.setColor(new Color(241, 245, 249));
        g2d.fillRoundRect(10, 105, (labelWidthPx / 2) - 15, 24, 6, 6);
        g2d.setColor(new Color(14, 116, 144)); // Cyan/blue
        g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
        String warranty = product.getWarrantyPeriod();
        if (warranty == null || warranty.isBlank()) warranty = "No Warranty";
        if (warranty.length() > 14) warranty = warranty.substring(0, 12) + "..";
        g2d.drawString(warranty, 14, 120);

        // Price
        g2d.setColor(new Color(15, 23, 42));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        String priceText = FormatUtil.formatCurrency(product.getSellingPrice());
        fm = g2d.getFontMetrics();
        int priceX = labelWidthPx - fm.stringWidth(priceText) - 12;
        g2d.drawString(priceText, priceX, 122);

        g2d.dispose();
        return image;
    }

    public Image generateProductPriceTagFxImage(Product product, int labelWidthPx, int labelHeightPx) {
        BufferedImage bi = generateProductPriceTagLabel(product, labelWidthPx, labelHeightPx);
        return SwingFXUtils.toFXImage(bi, null);
    }
}
