package com.phoneshop.pos.ui;

import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.service.ReceiptService;
import com.phoneshop.pos.util.DialogUtil;
import com.phoneshop.pos.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class ReceiptPreviewDialog extends Stage {
    private final ReceiptService receiptService = new ReceiptService();

    public ReceiptPreviewDialog(Sale sale) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Sales Invoice & Receipt — " + (sale != null ? sale.getInvoiceNumber() : "Invoice"));

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");
        root.setPrefSize(500, 680);
        root.setMinSize(440, 560);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Sales Invoice & Receipt Slip");
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label invBadge = new Label(sale != null ? sale.getInvoiceNumber() : "");
        invBadge.getStyleClass().add("badge");
        invBadge.getStyleClass().add("badge-info");

        headerBox.getChildren().addAll(titleLabel, invBadge);

        // Realistic Thermal Receipt Paper Container
        VBox paperCard = new VBox(8);
        paperCard.getStyleClass().add("receipt-paper-box");
        VBox.setVgrow(paperCard, Priority.ALWAYS);

        String receiptText = sale != null ? receiptService.generateThermalReceiptText(sale) : "No invoice data available.";

        TextArea receiptArea = new TextArea();
        receiptArea.setEditable(false);
        receiptArea.setWrapText(false);
        receiptArea.setText(receiptText);
        receiptArea.getStyleClass().add("thermal-receipt-text");
        receiptArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-text-fill: #0f172a; -fx-control-inner-background: #ffffff; -fx-background-color: #ffffff;");
        VBox.setVgrow(receiptArea, Priority.ALWAYS);

        paperCard.getChildren().add(receiptArea);

        // Action Buttons Bar
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button printThermalBtn = new Button("🖨 Print Thermal Receipt");
        printThermalBtn.getStyleClass().add("btn-primary");
        printThermalBtn.setOnAction(e -> {
            try {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(this)) {
                    boolean success = job.printPage(paperCard);
                    if (success) {
                        job.endJob();
                        DialogUtil.showInfo("Print Success", "Receipt sent to printer successfully.");
                    }
                }
            } catch (Exception ex) {
                DialogUtil.showError("Print Error", "Could not complete printing: " + ex.getMessage());
            }
        });

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.getStyleClass().add("btn-accent");
        exportPdfBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PDF Invoice");
            fileChooser.setInitialFileName("Invoice_" + (sale != null ? sale.getInvoiceNumber() : "bill") + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));
            File file = fileChooser.showSaveDialog(this);
            if (file != null && sale != null) {
                boolean ok = receiptService.exportPdfInvoice(sale, file);
                if (ok) {
                    DialogUtil.showInfo("Invoice Exported", "Invoice PDF saved successfully to:\n" + file.getAbsolutePath());
                } else {
                    DialogUtil.showError("Export Error", "Failed to generate PDF invoice.");
                }
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> close());

        actionBox.getChildren().addAll(printThermalBtn, exportPdfBtn, closeBtn);
        root.getChildren().addAll(headerBox, paperCard, actionBox);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        } catch (Exception ignored) {}
        ThemeManager.applyCurrentTheme(scene);
        setScene(scene);
    }
}
