package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.CustomerDao;
import com.phoneshop.pos.dao.ProductDao;
import com.phoneshop.pos.dao.SaleDao;
import com.phoneshop.pos.model.*;
import com.phoneshop.pos.util.AppSession;
import com.phoneshop.pos.util.DialogUtil;
import com.phoneshop.pos.util.FormatUtil;
import com.phoneshop.pos.util.ThemeManager;
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

public class PosTerminalView extends BorderPane {
    private final ProductDao productDao = new ProductDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final SaleDao saleDao = new SaleDao();

    // UI Components - Catalog
    private final TextField barcodeScanField = new TextField();
    private final TextField searchField = new TextField();
    private final TableView<Product> productTable = new TableView<>();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private String selectedCategory = "ALL";

    // UI Components - Cart
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private final TableView<SaleItem> cartTable = new TableView<>();
    private final Label subtotalLabel = new Label("Rs. 0.00");
    private final Label discountLabel = new Label("Rs. 0.00");
    private final Label grandTotalLabel = new Label("Rs. 0.00");
    private double overallDiscount = 0.0;

    // Customer
    private final TextField customerPhoneField = new TextField();
    private final TextField customerNameField = new TextField();
    private Customer selectedCustomer = null;

    public PosTerminalView() {
        this.getStyleClass().add("content-area");
        this.setPadding(new Insets(16));

        // Left Section: Product Search & Grid
        VBox catalogSection = createCatalogSection();

        // Right Section: Cart & Checkout
        VBox cartSection = createCartSection();

        // Layout Split
        HBox mainSplit = new HBox(16, catalogSection, cartSection);
        HBox.setHgrow(catalogSection, Priority.ALWAYS);
        catalogSection.setPrefWidth(680);
        cartSection.setPrefWidth(480);
        cartSection.setMinWidth(420);

        this.setCenter(mainSplit);

        // Load initial data
        refreshProducts();
    }

    private VBox createCatalogSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");

        // Top Scanner Bar
        HBox scannerBox = new HBox(10);
        scannerBox.setAlignment(Pos.CENTER_LEFT);

        Label scanIcon = new Label("🔍");
        scanIcon.setStyle("-fx-font-size: 16px;");

        barcodeScanField.setPromptText("Scan Barcode / IMEI or press Enter (F1)...");
        barcodeScanField.getStyleClass().add("search-input");
        HBox.setHgrow(barcodeScanField, Priority.ALWAYS);
        barcodeScanField.setOnAction(e -> handleBarcodeScan());

        searchField.setPromptText("Search by model/brand...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterProducts());

        scannerBox.getChildren().addAll(scanIcon, barcodeScanField, searchField);

        // Category Filter Chips
        HBox chipsBox = new HBox(8);
        chipsBox.setAlignment(Pos.CENTER_LEFT);
        String[] categories = {"ALL", "SMARTPHONES", "TABLETS", "ACCESSORIES", "SPARE_PARTS"};
        for (String cat : categories) {
            Button chip = new Button(cat.replace("_", " "));
            chip.getStyleClass().add("filter-chip");
            if (cat.equals("ALL")) chip.getStyleClass().add("active");
            chip.setOnAction(e -> {
                chipsBox.getChildren().forEach(n -> n.getStyleClass().remove("active"));
                chip.getStyleClass().add("active");
                selectedCategory = cat;
                filterProducts();
            });
            chipsBox.getChildren().add(chip);
        }

        // Product Catalog Table
        setupProductTable();
        VBox.setVgrow(productTable, Priority.ALWAYS);

        box.getChildren().addAll(scannerBox, chipsBox, productTable);
        return box;
    }

    private void setupProductTable() {
        productTable.setItems(productList);
        productTable.setPlaceholder(new Label("No products match search criteria."));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product & Specs");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullDisplayName()));
        nameCol.setPrefWidth(220);

        TableColumn<Product, String> brandCol = new TableColumn<>("Brand / Cat");
        brandCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBrand() + " • " + data.getValue().getCategory()));
        brandCol.setPrefWidth(120);

        TableColumn<Product, String> warrantyCol = new TableColumn<>("Warranty");
        warrantyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWarrantyPeriod()));
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
        warrantyCol.setPrefWidth(110);

        TableColumn<Product, String> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStockQuantity())));
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
                        badge.setText(p.getStockQuantity() + " left");
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.setText(p.getStockQuantity() + " in stock");
                        badge.getStyleClass().add("badge-success");
                    }
                    badge.getStyleClass().add("badge");
                    setGraphic(badge);
                }
            }
        });
        stockCol.setPrefWidth(90);

        TableColumn<Product, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(FormatUtil.formatCurrency(data.getValue().getSellingPrice())));
        priceCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        priceCol.setPrefWidth(100);

        TableColumn<Product, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button addBtn = new Button("＋ Add");
            {
                addBtn.getStyleClass().add("btn-primary");
                addBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                addBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    addToCart(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    addBtn.setDisable(p.getStockQuantity() <= 0);
                    setGraphic(addBtn);
                }
            }
        });
        actionCol.setPrefWidth(70);

        productTable.getColumns().addAll(List.of(nameCol, brandCol, warrantyCol, stockCol, priceCol, actionCol));

        // Double click to add to cart
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    addToCart(row.getItem());
                }
            });
            return row;
        });
    }

    private VBox createCartSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");

        // Header
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Current Sale Cart");
        title.getStyleClass().add("card-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button clearCartBtn = new Button("Clear Cart");
        clearCartBtn.getStyleClass().add("btn-secondary");
        clearCartBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        clearCartBtn.setOnAction(e -> {
            if (!cartItems.isEmpty() && DialogUtil.showConfirmation("Clear Cart", "Are you sure?", "Remove all items from current cart?")) {
                cartItems.clear();
                recalculateTotals();
            }
        });
        headerBox.getChildren().addAll(title, clearCartBtn);

        // Customer Info Card
        VBox customerBox = new VBox(6);
        customerBox.getStyleClass().add("card");
        customerBox.setStyle("-fx-padding: 10;");
        
        Label custTitle = new Label("Customer Info (Warranty & Receipt)");
        custTitle.getStyleClass().add("card-subtitle");

        HBox custInputs = new HBox(8);
        customerPhoneField.setPromptText("Phone (e.g. 0771234567)");
        customerPhoneField.setPrefWidth(160);
        customerPhoneField.setOnAction(e -> lookupCustomerByPhone());

        customerNameField.setPromptText("Customer Name");
        HBox.setHgrow(customerNameField, Priority.ALWAYS);

        custInputs.getChildren().addAll(customerPhoneField, customerNameField);
        customerBox.getChildren().addAll(custTitle, custInputs);

        // Cart Table
        setupCartTable();
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        // Totals & Checkout Box
        VBox checkoutBox = new VBox(8);
        checkoutBox.getStyleClass().add("card");
        checkoutBox.setStyle("-fx-padding: 14;");

        HBox subtotalRow = createSummaryLine("Subtotal:", subtotalLabel, false);
        HBox discountRow = createSummaryLine("Discount:", discountLabel, false);
        HBox grandTotalRow = createSummaryLine("Grand Total:", grandTotalLabel, true);
        grandTotalLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 20px; -fx-font-weight: bold;");

        Button checkoutBtn = new Button("💳 Complete Checkout (F4)");
        checkoutBtn.getStyleClass().add("btn-checkout");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> openPaymentDialog());

        checkoutBox.getChildren().addAll(subtotalRow, discountRow, grandTotalRow, checkoutBtn);

        box.getChildren().addAll(headerBox, customerBox, cartTable, checkoutBox);
        return box;
    }

    private void setupCartTable() {
        cartTable.setItems(cartItems);
        cartTable.setPlaceholder(new Label("Cart is empty. Scan barcode or click ＋ Add."));

        TableColumn<SaleItem, String> itemCol = new TableColumn<>("Item & Warranty");
        itemCol.setCellValueFactory(data -> {
            SaleItem item = data.getValue();
            String desc = item.getProductName();
            if (item.getWarrantyPeriod() != null && !item.getWarrantyPeriod().isBlank()) {
                desc += "\nWarranty: " + item.getWarrantyPeriod();
            }
            return new SimpleStringProperty(desc);
        });
        itemCol.setPrefWidth(170);

        TableColumn<SaleItem, Void> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellFactory(col -> new TableCell<>() {
            private final Button minusBtn = new Button("-");
            private final Label qtyLabel = new Label("1");
            private final Button plusBtn = new Button("+");
            private final HBox box = new HBox(4, minusBtn, qtyLabel, plusBtn);

            {
                box.setAlignment(Pos.CENTER);
                minusBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
                plusBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
                minusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        cartTable.refresh();
                        recalculateTotals();
                    } else {
                        cartItems.remove(item);
                        recalculateTotals();
                    }
                });
                plusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    Product p = productDao.findById(item.getProductId());
                    if (p != null && item.getQuantity() >= p.getStockQuantity()) {
                        DialogUtil.showWarning("Stock Limit", "Cannot add more. Available in stock: " + p.getStockQuantity());
                        return;
                    }
                    item.setQuantity(item.getQuantity() + 1);
                    cartTable.refresh();
                    recalculateTotals();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SaleItem si = getTableView().getItems().get(getIndex());
                    qtyLabel.setText(String.valueOf(si.getQuantity()));
                    setGraphic(box);
                }
            }
        });
        qtyCol.setPrefWidth(90);

        TableColumn<SaleItem, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(FormatUtil.formatCurrency(data.getValue().getSubtotal())));
        totalCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        totalCol.setPrefWidth(100);

        TableColumn<SaleItem, Void> delCol = new TableColumn<>("");
        delCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("✕");
            {
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
                delBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    recalculateTotals();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });
        delCol.setPrefWidth(40);

        cartTable.getColumns().addAll(List.of(itemCol, qtyCol, totalCol, delCol));
    }

    private HBox createSummaryLine(String title, Label valLabel, boolean isBold) {
        HBox line = new HBox();
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: " + (isBold ? "15px" : "13px") + "; -fx-font-weight: " + (isBold ? "bold" : "normal") + ";");
        HBox.setHgrow(t, Priority.ALWAYS);
        valLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: " + (isBold ? "18px" : "13px") + "; -fx-font-weight: " + (isBold ? "bold" : "500") + ";");
        line.getChildren().addAll(t, valLabel);
        return line;
    }

    public void handleBarcodeScan() {
        String code = barcodeScanField.getText();
        if (code == null || code.isBlank()) return;

        Product p = productDao.findByBarcode(code);
        if (p != null) {
            addToCart(p);
            barcodeScanField.clear();
        } else {
            DialogUtil.showWarning("Not Found", "No product found matching barcode or IMEI: " + code);
        }
    }

    private void addToCart(Product p) {
        if (p.getStockQuantity() <= 0) {
            DialogUtil.showWarning("Out of Stock", "Product '" + p.getName() + "' is out of stock.");
            return;
        }

        // Check if already in cart
        for (SaleItem item : cartItems) {
            if (item.getProductId() == p.getId()) {
                if (item.getQuantity() >= p.getStockQuantity()) {
                    DialogUtil.showWarning("Stock Limit", "Available stock is only " + p.getStockQuantity() + " unit(s).");
                    return;
                }
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                recalculateTotals();
                return;
            }
        }

        // New item
        SaleItem item = new SaleItem(p, 1);
        cartItems.add(item);
        recalculateTotals();
    }

    private void recalculateTotals() {
        double subtotal = 0.0;
        for (SaleItem item : cartItems) {
            subtotal += item.getSubtotal();
        }
        double grandTotal = Math.max(0.0, subtotal - overallDiscount);

        subtotalLabel.setText(FormatUtil.formatCurrency(subtotal));
        discountLabel.setText(overallDiscount > 0 ? ("-" + FormatUtil.formatCurrency(overallDiscount)) : "Rs. 0.00");
        grandTotalLabel.setText(FormatUtil.formatCurrency(grandTotal));
    }

    private void lookupCustomerByPhone() {
        String phone = customerPhoneField.getText();
        if (phone != null && !phone.isBlank()) {
            Customer c = customerDao.findByPhone(phone);
            if (c != null) {
                selectedCustomer = c;
                customerNameField.setText(c.getName());
            }
        }
    }

    private void openPaymentDialog() {
        if (cartItems.isEmpty()) {
            DialogUtil.showWarning("Cart Empty", "Please add at least one product to the cart.");
            return;
        }

        double calcSubtotal = 0.0;
        for (SaleItem item : cartItems) {
            calcSubtotal += item.getSubtotal();
        }
        final double finalSubtotal = calcSubtotal;
        final double finalDiscount = overallDiscount;
        final double finalGrandTotal = Math.max(0.0, calcSubtotal - finalDiscount);

        Stage payStage = new Stage();
        payStage.initModality(Modality.APPLICATION_MODAL);
        payStage.setTitle("Complete Payment");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("content-area");
        root.setPrefSize(420, 520);

        Label header = new Label("Checkout Payment");
        header.getStyleClass().add("card-title");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Amount Due Banner
        VBox dueCard = new VBox(4);
        dueCard.getStyleClass().add("card");
        dueCard.setStyle("-fx-padding: 14; -fx-alignment: CENTER;");
        Label dueTitle = new Label("Total Amount Due");
        dueTitle.getStyleClass().add("stat-label");
        Label dueAmount = new Label(FormatUtil.formatCurrency(finalGrandTotal));
        dueAmount.setStyle("-fx-text-fill: #10b981; -fx-font-size: 26px; -fx-font-weight: bold;");
        dueCard.getChildren().addAll(dueTitle, dueAmount);

        // Payment Method Combo
        Label methodLabel = new Label("Payment Method");
        methodLabel.getStyleClass().add("stat-label");
        ComboBox<PaymentMethod> methodCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setValue(PaymentMethod.CASH);
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        // Amount Paid Input
        Label paidLabel = new Label("Amount Received (Cash Tendered)");
        paidLabel.getStyleClass().add("stat-label");
        TextField paidField = new TextField(String.valueOf(finalGrandTotal));
        paidField.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Change Due Label
        HBox changeRow = new HBox();
        Label cLabel = new Label("Change Due:");
        cLabel.getStyleClass().add("stat-label");
        cLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(cLabel, Priority.ALWAYS);
        Label changeVal = new Label("Rs. 0.00");
        changeVal.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-font-weight: bold;");
        changeRow.getChildren().addAll(cLabel, changeVal);

        paidField.textProperty().addListener((obs, oldV, newV) -> {
            try {
                double paid = Double.parseDouble(newV.trim());
                double change = Math.max(0.0, paid - finalGrandTotal);
                changeVal.setText(FormatUtil.formatCurrency(change));
            } catch (Exception e) {
                changeVal.setText("Rs. 0.00");
            }
        });

        // Quick Cash Buttons
        HBox quickCash = new HBox(8);
        quickCash.setAlignment(Pos.CENTER);
        double[] quickAmounts = {finalGrandTotal, Math.ceil(finalGrandTotal / 1000.0) * 1000.0, Math.ceil(finalGrandTotal / 5000.0) * 5000.0};
        for (double amt : quickAmounts) {
            if (amt <= 0) continue;
            Button qb = new Button(FormatUtil.formatCurrency(amt));
            qb.getStyleClass().add("btn-secondary");
            qb.setStyle("-fx-font-size: 11px;");
            qb.setOnAction(e -> paidField.setText(String.valueOf((int) amt)));
            quickCash.getChildren().add(qb);
        }

        // Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button payAndPrintBtn = new Button("✔ Pay & Generate Invoice");
        payAndPrintBtn.getStyleClass().add("btn-success");
        payAndPrintBtn.setPrefHeight(40);
        payAndPrintBtn.setOnAction(e -> {
            try {
                double paid = Double.parseDouble(paidField.getText().trim());
                if (paid < finalGrandTotal && methodCombo.getValue() == PaymentMethod.CASH) {
                    DialogUtil.showWarning("Insufficient Amount", "Paid amount is less than total due.");
                    return;
                }

                // Process Sale
                Sale sale = new Sale();
                sale.setInvoiceNumber(saleDao.generateNextInvoiceNumber());
                sale.setUserId(AppSession.getInstance().getCurrentUser().getId());
                sale.setCashierName(AppSession.getInstance().getCurrentUser().getFullName());

                // Customer info
                String custPhone = customerPhoneField.getText();
                String custName = customerNameField.getText();
                if (custPhone != null && !custPhone.isBlank()) {
                    Customer c = customerDao.getOrCreate(custName, custPhone, null, null);
                    sale.setCustomerId(c.getId());
                    sale.setCustomerName(c.getName());
                    sale.setCustomerPhone(c.getPhone());
                } else {
                    sale.setCustomerName(custName == null || custName.isBlank() ? "Walk-in Customer" : custName.trim());
                }

                sale.setSubtotal(finalSubtotal);
                sale.setDiscountAmount(finalDiscount);
                sale.setTaxAmount(0.0);
                sale.setTotalAmount(finalGrandTotal);
                sale.setPaidAmount(paid);
                sale.setChangeAmount(Math.max(0.0, paid - finalGrandTotal));
                sale.setPaymentMethod(methodCombo.getValue().name());
                sale.setPaymentStatus("PAID");

                for (SaleItem item : cartItems) {
                    sale.addItem(item);
                }

                int sessionId = AppSession.getInstance().getCurrentSessionId();
                saleDao.processSale(sale, sessionId);

                payStage.close();
                cartItems.clear();
                customerPhoneField.clear();
                customerNameField.clear();
                recalculateTotals();
                refreshProducts();

                // Open receipt preview modal
                ReceiptPreviewDialog receiptDialog = new ReceiptPreviewDialog(sale);
                receiptDialog.show();

            } catch (NumberFormatException ex) {
                DialogUtil.showError("Invalid Input", "Please enter a valid paid amount number.");
            } catch (Exception ex) {
                DialogUtil.showError("Checkout Failed", ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> payStage.close());

        actionBox.getChildren().addAll(payAndPrintBtn, cancelBtn);

        root.getChildren().addAll(header, dueCard, methodLabel, methodCombo, paidLabel, paidField, quickCash, changeRow, actionBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ThemeManager.applyCurrentTheme(scene);
        payStage.setScene(scene);
        payStage.showAndWait();
    }

    public void refreshProducts() {
        filterProducts();
    }

    private void filterProducts() {
        String keyword = searchField.getText();
        List<Product> products = productDao.search(keyword, selectedCategory);
        productList.setAll(products);
    }
}
