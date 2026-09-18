package com.phoneshop.pos;

import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.service.BarcodeService;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class BarcodeServiceTest {

    @Test
    public void testZXingBarcodeGeneration() {
        BarcodeService barcodeService = new BarcodeService();

        BufferedImage bi = barcodeService.generateBarcodeBufferedImage("880609123456", 200, 50);
        assertNotNull(bi, "Barcode image should not be null");
        assertEquals(200, bi.getWidth());
        assertEquals(50, bi.getHeight());

        // Test label tag generation
        Product product = new Product();
        product.setBarcode("880609123456");
        product.setName("iPhone 15 Pro");
        product.setStorageRam("256GB / 8GB RAM");
        product.setColor("Natural Titanium");
        product.setSellingPrice(375000.0);
        product.setWarrantyPeriod("1 Year Apple Care");

        BufferedImage label = barcodeService.generateProductPriceTagLabel(product, 320, 140);
        assertNotNull(label, "Label image should not be null");
        assertEquals(320, label.getWidth());
        assertEquals(140, label.getHeight());
    }
}
