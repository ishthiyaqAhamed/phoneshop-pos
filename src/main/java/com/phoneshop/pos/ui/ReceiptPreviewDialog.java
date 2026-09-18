package com.phoneshop.pos.ui;

import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.service.ReceiptService;
import com.phoneshop.pos.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        setTitle("Receipt / Invoice Preview - " + sale.getInvoiceNumber());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPrefSize(480, 620);

        Label titleLabel = new Label("Sales Invoice & Receipt");
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");

        TextArea receiptArea = new TextArea();
        receiptArea.setEditable(false);
        receiptArea.setWrapText(true);
        receiptArea.setText(receiptService.generateThermalReceiptText(sale));
        receiptArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-background-color: #1e293b;");
        VBox.setVgrow(receiptArea, Priority.ALWAYS);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button exportPdfBtn = new Button("📄 Export PDF Invoice");
        exportPdfBtn.getStyleClass().add("btn-accent");
        exportPdfBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PDF Invoice");
            fileChooser.setInitialFileName("Invoice_" + sale.getInvoiceNumber() + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));
            File file = fileChooser.showSaveDialog(this);
            if (file != null) {
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

        actionBox.getChildren().addAll(exportPdfBtn, closeBtn);
        root.getChildren().addAll(titleLabel, receiptArea, actionBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        setScene(scene);
    }
}
