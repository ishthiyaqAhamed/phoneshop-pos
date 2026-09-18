package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.ProductDao;
import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.util.AppSession;
import com.phoneshop.pos.util.DialogUtil;
import com.phoneshop.pos.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Random;

public class ProductManagementView extends VBox {
    private final ProductDao productDao = new ProductDao();
    private final TableView<Product> table = new TableView<>();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();

    public ProductManagementView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(18));

        // Top Action Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Product Inventory & Barcodes");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        searchField.setPromptText("Search by name, brand, barcode, or IMEI...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterProducts());

        categoryFilter.setItems(FXCollections.observableArrayList("ALL", "SMARTPHONES", "TABLETS", "ACCESSORIES", "SPARE_PARTS"));
        categoryFilter.setValue("ALL");
        categoryFilter.setPrefWidth(140);
        categoryFilter.setOnAction(e -> filterProducts());

        Button addProductBtn = new Button("＋ Add Product");
        addProductBtn.getStyleClass().add("btn-primary");
        addProductBtn.setOnAction(e -> openProductFormDialog(null));

        topBar.getChildren().addAll(pageTitle, searchField, categoryFilter, addProductBtn);

        // Products Table
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        this.getChildren().addAll(topBar, table);
        refreshData();
    }

    private void setupTable() {
        table.setItems(productList);
        table.setPlaceholder(new Label("No products found. Click '＋ Add Product' to create one."));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode / SKU");
        barcodeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBarcode()));
        barcodeCol.setPrefWidth(120);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name & Specs");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullDisplayName()));
        nameCol.setPrefWidth(220);

        TableColumn<Product, String> imeiCol = new TableColumn<>("IMEI / Serial");
        imeiCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getImei() != null ? d.getValue().getImei() : "N/A"));
        imeiCol.setPrefWidth(130);

        TableColumn<Product, String> brandCol = new TableColumn<>("Brand / Cat");
        brandCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBrand() + " • " + d.getValue().getCategory()));
        brandCol.setPrefWidth(120);

        TableColumn<Product, String> warrantyCol = new TableColumn<>("Warranty Period");
        warrantyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWarrantyPeriod()));
        warrantyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge-warranty");
                    setGraphic(badge);
                }
            }
        });
        warrantyCol.setPrefWidth(130);

        TableColumn<Product, String> stockCol = new TableColumn<>("Stock Qty");
        stockCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getStockQuantity())));
        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    Label badge = new Label();
                    if (p.getStockQuantity() <= 0) {
                        badge.setText("Out of Stock");
                        badge.getStyleClass().add("badge-danger");
                    } else if (p.isLowStock()) {
                        badge.setText(p.getStockQuantity() + " (Low)");
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.setText(p.getStockQuantity() + " units");
                        badge.getStyleClass().add("badge-success");
                    }
                    badge.getStyleClass().add("badge");
                    setGraphic(badge);
                }
            }
        });
        stockCol.setPrefWidth(100);

        TableColumn<Product, String> priceCol = new TableColumn<>("Selling Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatCurrency(d.getValue().getSellingPrice())));
        priceCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        priceCol.setPrefWidth(110);

        TableColumn<Product, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button printBtn = new Button("🏷 Barcode");
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(6, printBtn, editBtn);

            {
                printBtn.getStyleClass().add("btn-accent");
                printBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                printBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    BarcodePrintDialog dialog = new BarcodePrintDialog(p);
                    dialog.show();
                });

                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    openProductFormDialog(p);
                });

                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                deleteBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (DialogUtil.showConfirmation("Delete Product", "Are you sure?", "Delete product '" + p.getName() + "'?")) {
                        productDao.delete(p.getId());
                        refreshData();
                    }
                });

                if (AppSession.getInstance().isAdmin()) {
                    box.getChildren().add(deleteBtn);
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actionCol.setPrefWidth(210);

        table.getColumns().addAll(List.of(barcodeCol, nameCol, imeiCol, brandCol, warrantyCol, stockCol, priceCol, actionCol));
    }

    public void refreshData() {
        filterProducts();
    }

    private void filterProducts() {
        String kw = searchField.getText();
        String cat = categoryFilter.getValue();
        List<Product> list = productDao.search(kw, cat);
        productList.setAll(list);
    }

    private void openProductFormDialog(Product productToEdit) {
        boolean isEdit = productToEdit != null;
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(isEdit ? "Edit Product - " + productToEdit.getName() : "Add New Phone Shop Product");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPrefSize(540, 680);

        Label header = new Label(isEdit ? "Edit Product Information" : "Add New Product to Inventory");
        header.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        // Fields
        TextField barcodeField = new TextField();
        Button genBarcodeBtn = new Button("⚡ Generate");
        genBarcodeBtn.getStyleClass().add("btn-secondary");
        genBarcodeBtn.setOnAction(e -> {
            long rand = 100000000000L + (long)(new Random().nextDouble() * 899999999999L);
            barcodeField.setText(String.valueOf(rand));
        });
        HBox barcodeRow = new HBox(8, barcodeField, genBarcodeBtn);
        HBox.setHgrow(barcodeField, Priority.ALWAYS);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. iPhone 15 Pro, Samsung S24, 65W GaN Charger");

        TextField brandField = new TextField();
        brandField.setPromptText("e.g. Apple, Samsung, Xiaomi, Anker, Baseus");

        ComboBox<String> catCombo = new ComboBox<>(FXCollections.observableArrayList("SMARTPHONES", "TABLETS", "ACCESSORIES", "SPARE_PARTS", "SERVICES"));
        catCombo.setValue("SMARTPHONES");
        catCombo.setMaxWidth(Double.MAX_VALUE);

        TextField imeiField = new TextField();
        imeiField.setPromptText("15-digit IMEI or Serial (for phones/parts)");

        TextField storageRamField = new TextField();
        storageRamField.setPromptText("e.g. 256GB / 8GB RAM");

        TextField colorField = new TextField();
        colorField.setPromptText("e.g. Natural Titanium, Midnight Black");

        ComboBox<String> conditionCombo = new ComboBox<>(FXCollections.observableArrayList("BRAND_NEW", "PRE_OWNED", "REFURBISHED"));
        conditionCombo.setValue("BRAND_NEW");
        conditionCombo.setMaxWidth(Double.MAX_VALUE);

        TextField costPriceField = new TextField("0.00");
        TextField sellingPriceField = new TextField("0.00");
        TextField stockQtyField = new TextField("1");
        TextField minStockField = new TextField("3");

        ComboBox<String> warrantyCombo = new ComboBox<>(FXCollections.observableArrayList(
                "1 Year Official Apple Care",
                "1 Year Company Warranty",
                "2 Years Company Warranty",
                "6 Months Shop Warranty",
                "3 Months Shop Warranty",
                "1 Month Testing Warranty",
                "No Warranty"
        ));
        warrantyCombo.setEditable(true);
        warrantyCombo.setValue("1 Year Company Warranty");
        warrantyCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea descField = new TextArea();
        descField.setPromptText("Optional item details, TRCSL approval status, or box contents");
        descField.setPrefRowCount(3);

        // Pre-fill if edit
        if (isEdit) {
            barcodeField.setText(productToEdit.getBarcode());
            nameField.setText(productToEdit.getName());
            brandField.setText(productToEdit.getBrand());
            catCombo.setValue(productToEdit.getCategory());
            imeiField.setText(productToEdit.getImei());
            storageRamField.setText(productToEdit.getStorageRam());
            colorField.setText(productToEdit.getColor());
            conditionCombo.setValue(productToEdit.getCondition());
            costPriceField.setText(String.valueOf(productToEdit.getCostPrice()));
            sellingPriceField.setText(String.valueOf(productToEdit.getSellingPrice()));
            stockQtyField.setText(String.valueOf(productToEdit.getStockQuantity()));
            minStockField.setText(String.valueOf(productToEdit.getMinStockLevel()));
            warrantyCombo.setValue(productToEdit.getWarrantyPeriod());
            descField.setText(productToEdit.getDescription());
        } else {
            long rand = 100000000000L + (long)(new Random().nextDouble() * 899999999999L);
            barcodeField.setText(String.valueOf(rand));
        }

        // Layout Grid
        int r = 0;
        grid.add(createLabel("Barcode / SKU *:"), 0, r); grid.add(barcodeRow, 1, r++);
        grid.add(createLabel("Product Name *:"), 0, r); grid.add(nameField, 1, r++);
        grid.add(createLabel("Brand *:"), 0, r); grid.add(brandField, 1, r++);
        grid.add(createLabel("Category *:"), 0, r); grid.add(catCombo, 1, r++);
        grid.add(createLabel("IMEI / Serial:"), 0, r); grid.add(imeiField, 1, r++);
        grid.add(createLabel("Storage / RAM:"), 0, r); grid.add(storageRamField, 1, r++);
        grid.add(createLabel("Color:"), 0, r); grid.add(colorField, 1, r++);
        grid.add(createLabel("Condition:"), 0, r); grid.add(conditionCombo, 1, r++);
        grid.add(createLabel("Buying Cost Price (Rs.):"), 0, r); grid.add(costPriceField, 1, r++);
        grid.add(createLabel("Selling Price (Rs.) *:"), 0, r); grid.add(sellingPriceField, 1, r++);
        grid.add(createLabel("Stock Quantity *:"), 0, r); grid.add(stockQtyField, 1, r++);
        grid.add(createLabel("Low Stock Alert Level:"), 0, r); grid.add(minStockField, 1, r++);
        grid.add(createLabel("Warranty Period *:"), 0, r); grid.add(warrantyCombo, 1, r++);
        grid.add(createLabel("Description / Notes:"), 0, r); grid.add(descField, 1, r++);

        scroll.setContent(grid);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button(isEdit ? "Save Changes" : "Create Product");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                String barcode = barcodeField.getText() != null ? barcodeField.getText().trim() : "";
                String name = nameField.getText() != null ? nameField.getText().trim() : "";
                String brand = brandField.getText() != null ? brandField.getText().trim() : "";
                String category = catCombo.getValue() != null ? catCombo.getValue() : "SMARTPHONES";
                String imei = imeiField.getText() != null ? imeiField.getText().trim() : "";
                String storageRam = storageRamField.getText() != null ? storageRamField.getText().trim() : "";
                String color = colorField.getText() != null ? colorField.getText().trim() : "";
                String condition = conditionCombo.getValue() != null ? conditionCombo.getValue() : "BRAND_NEW";
                String costPriceStr = costPriceField.getText() != null ? costPriceField.getText().trim() : "0.00";
                String sellingPriceStr = sellingPriceField.getText() != null ? sellingPriceField.getText().trim() : "0.00";
                String stockQtyStr = stockQtyField.getText() != null ? stockQtyField.getText().trim() : "0";
                String minStockStr = minStockField.getText() != null ? minStockField.getText().trim() : "3";
                
                double costPrice = Double.parseDouble(costPriceStr.isBlank() ? "0.00" : costPriceStr);
                double sellingPrice = Double.parseDouble(sellingPriceStr.isBlank() ? "0.00" : sellingPriceStr);
                int stockQty = Integer.parseInt(stockQtyStr.isBlank() ? "0" : stockQtyStr);
                int minStock = Integer.parseInt(minStockStr.isBlank() ? "3" : minStockStr);
                String warranty = warrantyCombo.getValue();
                String desc = descField.getText() != null ? descField.getText().trim() : "";

                if (barcode.isBlank() || name.isBlank() || brand.isBlank()) {
                    DialogUtil.showWarning("Required Fields", "Please fill in Barcode, Name, Brand, and Selling Price.");
                    return;
                }

                Product p = isEdit ? productToEdit : new Product();
                p.setBarcode(barcode);
                p.setName(name);
                p.setBrand(brand);
                p.setCategory(category);
                p.setImei(imei.isBlank() ? "N/A" : imei);
                p.setStorageRam(storageRam);
                p.setColor(color);
                p.setCondition(condition);
                p.setCostPrice(costPrice);
                p.setSellingPrice(sellingPrice);
                p.setStockQuantity(stockQty);
                p.setMinStockLevel(minStock);
                p.setWarrantyPeriod(warranty == null || warranty.isBlank() ? "No Warranty" : warranty);
                p.setDescription(desc);

                boolean success;
                if (isEdit) {
                    success = productDao.update(p);
                } else {
                    success = productDao.create(p);
                }

                if (success) {
                    dialogStage.close();
                    refreshData();
                    DialogUtil.showInfo("Success", "Product saved successfully: " + p.getName());
                } else {
                    DialogUtil.showError("Save Error", "Failed to save product. Ensure barcode is unique.");
                }

            } catch (NumberFormatException ex) {
                DialogUtil.showError("Invalid Number", "Please check cost price, selling price, and quantity numbers.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialogStage.close());

        actionBox.getChildren().addAll(saveBtn, cancelBtn);
        root.getChildren().addAll(header, scroll, actionBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold; -fx-font-size: 12px;");
        return l;
    }
}
