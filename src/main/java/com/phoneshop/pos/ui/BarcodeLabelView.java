package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.ProductDao;
import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.service.BarcodeService;
import com.phoneshop.pos.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.List;

public class BarcodeLabelView extends VBox {
    private final ProductDao productDao = new ProductDao();
    private final BarcodeService barcodeService = new BarcodeService();
    private final ComboBox<Product> productCombo = new ComboBox<>();
    private final ImageView barcodePreview = new ImageView();
    private final Spinner<Integer> copiesSpinner = new Spinner<>(1, 100, 1);
    private final TextField customCodeField = new TextField();
    private final TextField customNameField = new TextField();
    private final TextField customPriceField = new TextField();
    private final TextField customWarrantyField = new TextField();

    public BarcodeLabelView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(20));

        Label pageTitle = new Label("Barcode & Price Tag Label Generator");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox split = new HBox(20);

        // Left Config Card
        VBox configCard = new VBox(14);
        configCard.getStyleClass().add("card");
        configCard.setPrefWidth(420);

        Label secTitle = new Label("Select Product or Custom Tag");
        secTitle.getStyleClass().add("card-title");

        productCombo.setMaxWidth(Double.MAX_VALUE);
        productCombo.setPromptText("Choose product from inventory...");
        productCombo.setOnAction(e -> {
            Product selected = productCombo.getValue();
            if (selected != null) {
                customCodeField.setText(selected.getBarcode());
                customNameField.setText(selected.getFullDisplayName());
                customPriceField.setText(String.valueOf(selected.getSellingPrice()));
                customWarrantyField.setText(selected.getWarrantyPeriod());
                updatePreview();
            }
        });

        Label customTitle = new Label("Label Content");
        customTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 11px;");

        customCodeField.setPromptText("Barcode String (Code 128)");
        customNameField.setPromptText("Product Name & Specs");
        customPriceField.setPromptText("Price (Rs.)");
        customWarrantyField.setPromptText("Warranty (e.g. 1 Year Official)");

        customCodeField.textProperty().addListener((obs, o, n) -> updatePreview());
        customNameField.textProperty().addListener((obs, o, n) -> updatePreview());
        customPriceField.textProperty().addListener((obs, o, n) -> updatePreview());
        customWarrantyField.textProperty().addListener((obs, o, n) -> updatePreview());

        HBox copiesRow = new HBox(10);
        copiesRow.setAlignment(Pos.CENTER_LEFT);
        Label copLabel = new Label("Number of Copies:");
        copLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        copiesSpinner.setPrefWidth(90);
        copiesRow.getChildren().addAll(copLabel, copiesSpinner);

        HBox actionBtns = new HBox(10);
        Button printBtn = new Button("🖨 Print Barcodes");
        printBtn.getStyleClass().add("btn-primary");
        printBtn.setOnAction(e -> handlePrint());

        Button saveBtn = new Button("💾 Save PNG");
        saveBtn.getStyleClass().add("btn-accent");
        saveBtn.setOnAction(e -> handleSavePng());

        actionBtns.getChildren().addAll(printBtn, saveBtn);

        configCard.getChildren().addAll(secTitle, productCombo, customTitle,
                createFieldBox("Barcode / SKU:", customCodeField),
                createFieldBox("Product Title:", customNameField),
                createFieldBox("Price (Rs.):", customPriceField),
                createFieldBox("Warranty Period:", customWarrantyField),
                copiesRow, actionBtns);

        // Right Preview Card
        VBox previewCard = new VBox(16);
        previewCard.getStyleClass().add("card");
        previewCard.setAlignment(Pos.CENTER);
        HBox.setHgrow(previewCard, Priority.ALWAYS);

        Label prevTitle = new Label("Live Tag Preview (50mm x 35mm)");
        prevTitle.getStyleClass().add("card-title");

        int tagWidth = 340;
        int tagHeight = 150;
        barcodePreview.setFitWidth(tagWidth);
        barcodePreview.setFitHeight(tagHeight);
        barcodePreview.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 3);");

        previewCard.getChildren().addAll(prevTitle, barcodePreview);

        split.getChildren().addAll(configCard, previewCard);
        this.getChildren().addAll(pageTitle, split);

        loadProducts();
    }

    private VBox createFieldBox(String labelText, TextField field) {
        VBox box = new VBox(4);
        Label l = new Label(labelText);
        l.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold;");
        box.getChildren().addAll(l, field);
        return box;
    }

    public void loadProducts() {
        List<Product> list = productDao.findAll();
        productCombo.setItems(FXCollections.observableArrayList(list));
        if (!list.isEmpty()) {
            productCombo.setValue(list.get(0));
        }
    }

    private Product buildCurrentProduct() {
        Product p = new Product();
        p.setBarcode(customCodeField.getText().isBlank() ? "195949012345" : customCodeField.getText().trim());
        p.setName(customNameField.getText().isBlank() ? "Sample Phone" : customNameField.getText().trim());
        try {
            p.setSellingPrice(Double.parseDouble(customPriceField.getText().trim()));
        } catch (Exception e) {
            p.setSellingPrice(0.0);
        }
        p.setWarrantyPeriod(customWarrantyField.getText().isBlank() ? "1 Year Warranty" : customWarrantyField.getText().trim());
        return p;
    }

    private void updatePreview() {
        Product p = buildCurrentProduct();
        barcodePreview.setImage(barcodeService.generateProductPriceTagFxImage(p, 340, 150));
    }

    private void handlePrint() {
        Product p = buildCurrentProduct();
        int copies = copiesSpinner.getValue();
        try {
            BufferedImage bi = barcodeService.generateProductPriceTagLabel(p, 340, 150);
            PrinterJob job = PrinterJob.getPrinterJob();
            if (job.printDialog()) {
                DialogUtil.showInfo("Printing", "Printing " + copies + " label(s) for " + p.getName());
            }
        } catch (Exception ex) {
            DialogUtil.showError("Print Error", ex.getMessage());
        }
    }

    private void handleSavePng() {
        Product p = buildCurrentProduct();
        try {
            BufferedImage bi = barcodeService.generateProductPriceTagLabel(p, 340, 150);
            File f = new File("Label_" + p.getBarcode() + ".png");
            ImageIO.write(bi, "png", f);
            DialogUtil.showInfo("Saved", "Barcode tag image saved to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            DialogUtil.showError("Error", ex.getMessage());
        }
    }
}
