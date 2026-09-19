package com.phoneshop.pos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.model.SaleItem;
import com.phoneshop.pos.util.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ReceiptService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);
    private final BarcodeService barcodeService = new BarcodeService();

    /**
     * Generates a printable thermal receipt text format (80mm width)
     */
    public String generateThermalReceiptText(Sale sale) {
        StringBuilder sb = new StringBuilder();
        int width = 42;
        String line = "=".repeat(width);
        String dash = "-".repeat(width);

        sb.append(centerText(AppConfig.getShopName(), width)).append("\n");
        sb.append(centerText(AppConfig.getShopBranch(), width)).append("\n");
        sb.append(centerText(AppConfig.getShopAddress(), width)).append("\n");
        sb.append(centerText("Tel: " + AppConfig.getShopPhone(), width)).append("\n");
        sb.append(line).append("\n");

        sb.append("INVOICE: ").append(sale.getInvoiceNumber()).append("\n");
        sb.append("DATE:    ").append(FormatUtil.formatDateTime(sale.getCreatedAt())).append("\n");
        sb.append("CASHIER: ").append(sale.getCashierName()).append("\n");
        if (sale.getCustomerName() != null && !sale.getCustomerName().isBlank()) {
            sb.append("CUSTOMER:").append(sale.getCustomerName()).append("\n");
        }
        if (sale.getCustomerPhone() != null && !sale.getCustomerPhone().isBlank()) {
            sb.append("PHONE:   ").append(sale.getCustomerPhone()).append("\n");
        }
        sb.append(dash).append("\n");
        sb.append(String.format("%-22s %3s %14s\n", "ITEM / IMEI", "QTY", "TOTAL"));
        sb.append(dash).append("\n");

        for (SaleItem item : sale.getItems()) {
            sb.append(wrapText(item.getProductName(), width)).append("\n");
            if (item.getImei() != null && !item.getImei().isBlank() && !item.getImei().equalsIgnoreCase("N/A")) {
                sb.append("  IMEI/SN:  ").append(item.getImei()).append("\n");
            }
            if (item.getWarrantyPeriod() != null && !item.getWarrantyPeriod().isBlank()) {
                sb.append("  Warranty: ").append(item.getWarrantyPeriod()).append("\n");
            }
            sb.append(String.format("  %-18s %3d %16s\n",
                    FormatUtil.formatCurrency(item.getUnitPrice()),
                    item.getQuantity(),
                    FormatUtil.formatCurrency(item.getSubtotal())));
        }

        sb.append(dash).append("\n");
        sb.append(String.format("%-26s %15s\n", "SUBTOTAL:", FormatUtil.formatCurrency(sale.getSubtotal())));
        if (sale.getDiscountAmount() > 0) {
            sb.append(String.format("%-26s %15s\n", "DISCOUNT:", "-" + FormatUtil.formatCurrency(sale.getDiscountAmount())));
        }
        if (sale.getTaxAmount() > 0) {
            sb.append(String.format("%-26s %15s\n", "TAX:", FormatUtil.formatCurrency(sale.getTaxAmount())));
        }
        sb.append(line).append("\n");
        sb.append(String.format("%-24s %17s\n", "NET TOTAL:", FormatUtil.formatCurrency(sale.getTotalAmount())));
        sb.append(line).append("\n");

        sb.append(String.format("%-26s %15s\n", "PAYMENT (" + sale.getPaymentMethod() + "):", FormatUtil.formatCurrency(sale.getPaidAmount())));
        if (sale.getChangeAmount() > 0) {
            sb.append(String.format("%-26s %15s\n", "CHANGE DUE:", FormatUtil.formatCurrency(sale.getChangeAmount())));
        }

        sb.append(dash).append("\n");
        sb.append(centerText("** WARRANTY NOTICE **", width)).append("\n");
        sb.append(wrapText(AppConfig.getWarrantyTerms(), width)).append("\n");
        sb.append(line).append("\n");
        sb.append(centerText("Thank you for choosing us!", width)).append("\n");
        sb.append(centerText("Goods sold are covered under terms above.", width)).append("\n");

        return sb.toString();
    }

    /**
     * Exports a professional PDF invoice
     */
    public boolean exportPdfInvoice(Sale sale, File targetFile) {
        Document document = new Document(PageSize.A5, 20, 20, 25, 25);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Colors
            Color primaryColor = new Color(15, 23, 42); // Slate 900
            Color secondaryColor = new Color(71, 85, 105); // Slate 600
            Color accentColor = new Color(14, 116, 144); // Cyan 700

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, primaryColor);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryColor);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, secondaryColor);

            // 1. Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(AppConfig.getShopName(), titleFont));
            leftCell.addElement(new Paragraph(AppConfig.getShopBranch() + "\n" + AppConfig.getShopAddress(), smallFont));
            leftCell.addElement(new Paragraph("Phone: " + AppConfig.getShopPhone() + " | " + AppConfig.getShopEmail(), smallFont));
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invP = new Paragraph("SALES INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, accentColor));
            invP.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invP);
            Paragraph invDetails = new Paragraph("No: " + sale.getInvoiceNumber() + "\nDate: " + FormatUtil.formatDateTime(sale.getCreatedAt()) + "\nCashier: " + sale.getCashierName(), smallFont);
            invDetails.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invDetails);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // Customer Info Box
            if ((sale.getCustomerName() != null && !sale.getCustomerName().isBlank()) || 
                (sale.getCustomerPhone() != null && !sale.getCustomerPhone().isBlank())) {
                PdfPTable custTable = new PdfPTable(1);
                custTable.setWidthPercentage(100);
                PdfPCell cCell = new PdfPCell();
                cCell.setBackgroundColor(new Color(248, 250, 252));
                cCell.setPadding(6);
                cCell.setBorderColor(new Color(226, 232, 240));
                cCell.addElement(new Paragraph("Customer: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in") + 
                        " | Tel: " + (sale.getCustomerPhone() != null ? sale.getCustomerPhone() : "N/A"), boldFont));
                custTable.addCell(cCell);
                document.add(custTable);
                document.add(new Paragraph(" "));
            }

            // 2. Items Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{40, 20, 10, 15, 15});

            String[] headers = {"Item Description", "IMEI / Warranty", "Qty", "Price", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                cell.setBackgroundColor(primaryColor);
                cell.setPadding(5);
                cell.setHorizontalAlignment(h.equals("Item Description") || h.equals("IMEI / Warranty") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                table.addCell(cell);
            }

            for (SaleItem item : sale.getItems()) {
                PdfPCell nameCell = new PdfPCell(new Phrase(item.getProductName(), boldFont));
                nameCell.setPadding(4);
                table.addCell(nameCell);

                StringBuilder meta = new StringBuilder();
                if (item.getImei() != null && !item.getImei().isBlank() && !item.getImei().equalsIgnoreCase("N/A")) {
                    meta.append("IMEI: ").append(item.getImei()).append("\n");
                }
                meta.append("Warranty: ").append(item.getWarrantyPeriod() != null ? item.getWarrantyPeriod() : "No Warranty");
                PdfPCell metaCell = new PdfPCell(new Phrase(meta.toString(), smallFont));
                metaCell.setPadding(4);
                table.addCell(metaCell);

                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), bodyFont));
                qtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                qtyCell.setPadding(4);
                table.addCell(qtyCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(FormatUtil.formatAmountWithoutSymbol(item.getUnitPrice()), bodyFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setPadding(4);
                table.addCell(priceCell);

                PdfPCell subCell = new PdfPCell(new Phrase(FormatUtil.formatAmountWithoutSymbol(item.getSubtotal()), boldFont));
                subCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                subCell.setPadding(4);
                table.addCell(subCell);
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // 3. Totals Breakdown
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{65, 35});

            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(Rectangle.NO_BORDER);
            termsCell.addElement(new Paragraph("Warranty Terms & Conditions:", boldFont));
            termsCell.addElement(new Paragraph(AppConfig.getWarrantyTerms(), smallFont));
            totalTable.addCell(termsCell);

            PdfPCell calcCell = new PdfPCell();
            calcCell.setBorder(Rectangle.NO_BORDER);
            
            calcCell.addElement(createSummaryRow("Subtotal:", FormatUtil.formatCurrency(sale.getSubtotal()), bodyFont));
            if (sale.getDiscountAmount() > 0) {
                calcCell.addElement(createSummaryRow("Discount:", "-" + FormatUtil.formatCurrency(sale.getDiscountAmount()), bodyFont));
            }
            if (sale.getTaxAmount() > 0) {
                calcCell.addElement(createSummaryRow("Tax:", FormatUtil.formatCurrency(sale.getTaxAmount()), bodyFont));
            }
            calcCell.addElement(createSummaryRow("Grand Total:", FormatUtil.formatCurrency(sale.getTotalAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor)));
            calcCell.addElement(createSummaryRow("Payment (" + sale.getPaymentMethod() + "):", FormatUtil.formatCurrency(sale.getPaidAmount()), bodyFont));
            if (sale.getChangeAmount() > 0) {
                calcCell.addElement(createSummaryRow("Change Due:", FormatUtil.formatCurrency(sale.getChangeAmount()), bodyFont));
            }
            totalTable.addCell(calcCell);

            document.add(totalTable);

            // Barcode image at bottom
            try {
                BufferedImage bi = barcodeService.generateBarcodeBufferedImage(sale.getInvoiceNumber(), 200, 35);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bi, "png", baos);
                Image barcodeImg = Image.getInstance(baos.toByteArray());
                barcodeImg.setAlignment(Element.ALIGN_CENTER);
                barcodeImg.setSpacingBefore(15);
                document.add(barcodeImg);
            } catch (Exception e) {
                logger.warn("Could not attach barcode image to PDF", e);
            }

            document.close();
            logger.info("PDF Invoice exported to {}", targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Failed to export PDF invoice", e);
            return false;
        }
    }

    private Paragraph createSummaryRow(String label, String value, Font font) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(String.format("%-18s ", label), font));
        p.add(new Chunk(value, font));
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String centerText(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String wrapText(String text, int width) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > width) {
                sb.append(currentLine).append("\n");
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            sb.append(currentLine);
        }
        return sb.toString();
    }
}
