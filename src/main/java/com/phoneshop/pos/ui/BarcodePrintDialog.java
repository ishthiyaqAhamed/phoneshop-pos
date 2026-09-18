package com.phoneshop.pos.ui;

import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.service.BarcodeService;
import com.phoneshop.pos.util.DialogUtil;
import com.phoneshop.pos.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;

public class BarcodePrintDialog extends Stage {
    private final BarcodeService barcodeService = new BarcodeService();

    public BarcodePrintDialog(Product product) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Barcode Label Tag - " + product.getName());

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("content-area");
        root.setPrefSize(420, 460);
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Product Barcode & Price Tag");
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Barcode Preview Image
        int labelWidth = 320;
        int labelHeight = 140;
        ImageView previewImage = new ImageView(barcodeService.generateProductPriceTagFxImage(product, labelWidth, labelHeight));
        previewImage.setFitWidth(labelWidth);
        previewImage.setFitHeight(labelHeight);
        previewImage.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 2);");

        HBox countBox = new HBox(12);
        countBox.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of Labels:");
        countLabel.getStyleClass().add("stat-label");
        Spinner<Integer> labelSpinner = new Spinner<>(1, 100, 1);
        labelSpinner.setPrefWidth(90);
        countBox.getChildren().addAll(countLabel, labelSpinner);

        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER);

        Button printBtn = new Button("🖨 Print Labels");
        printBtn.getStyleClass().add("btn-primary");
        printBtn.setOnAction(e -> {
            int copies = labelSpinner.getValue();
            try {
                BufferedImage bi = barcodeService.generateProductPriceTagLabel(product, labelWidth, labelHeight);
                PrinterJob job = PrinterJob.getPrinterJob();
                if (job.printDialog()) {
                    DialogUtil.showInfo("Print Sent", "Sent " + copies + " barcode label(s) for '" + product.getName() + "' to printer.");
                    close();
                }
            } catch (Exception ex) {
                DialogUtil.showError("Print Error", "Could not complete print job: " + ex.getMessage());
            }
        });

        Button saveImageBtn = new Button("💾 Save Image");
        saveImageBtn.getStyleClass().add("btn-accent");
        saveImageBtn.setOnAction(e -> {
            try {
                BufferedImage bi = barcodeService.generateProductPriceTagLabel(product, labelWidth, labelHeight);
                File out = new File("barcode_" + product.getBarcode() + ".png");
                ImageIO.write(bi, "png", out);
                DialogUtil.showInfo("Saved", "Label tag saved to:\n" + out.getAbsolutePath());
            } catch (Exception ex) {
                DialogUtil.showError("Error", "Could not save image: " + ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> close());

        actionBox.getChildren().addAll(printBtn, saveImageBtn, closeBtn);
        root.getChildren().addAll(titleLabel, previewImage, countBox, actionBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ThemeManager.applyCurrentTheme(scene);
        setScene(scene);
    }
}
