package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.ProductDao;
import com.phoneshop.pos.dao.ReportDao;
import com.phoneshop.pos.model.DashboardSummary;
import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.util.DialogUtil;
import com.phoneshop.pos.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class InventoryDashboardView extends VBox {
    private final ReportDao reportDao = new ReportDao();
    private final ProductDao productDao = new ProductDao();

    private final Label totalRetailValLabel = new Label("Rs. 0.00");
    private final Label totalCostValLabel = new Label("Rs. 0.00");
    private final Label totalItemsLabel = new Label("0");
    private final Label alertItemsLabel = new Label("0");

    private final TableView<Product> lowStockTable = new TableView<>();
    private final ObservableList<Product> lowStockList = FXCollections.observableArrayList();

    public InventoryDashboardView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(18));

        // Top Header
        HBox topBar = new HBox();
        Label pageTitle = new Label("Stock Valuation & Inventory Dashboard");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        topBar.getChildren().addAll(pageTitle, refreshBtn);

        // 4 Stat Cards
        HBox cardsBox = new HBox(14);
        cardsBox.setAlignment(Pos.CENTER);

        VBox card1 = createStatCard("TOTAL INVENTORY VALUE", totalRetailValLabel, "stat-card-blue");
        VBox card2 = createStatCard("TOTAL INVENTORY COST", totalCostValLabel, "stat-card-green");
        VBox card3 = createStatCard("TOTAL PRODUCT SKUS", totalItemsLabel, "stat-card-cyan");
        VBox card4 = createStatCard("LOW / OUT OF STOCK ALERTS", alertItemsLabel, "stat-card-amber");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);
        cardsBox.getChildren().addAll(card1, card2, card3, card4);

        // Low Stock Warning Section
        VBox warningCard = new VBox(10);
        warningCard.getStyleClass().add("card");
        VBox.setVgrow(warningCard, Priority.ALWAYS);

        Label warningTitle = new Label("⚠️ Items Requiring Restock (At or Below Min Threshold)");
        warningTitle.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px; -fx-font-weight: bold;");

        setupLowStockTable();
        VBox.setVgrow(lowStockTable, Priority.ALWAYS);

        warningCard.getChildren().addAll(warningTitle, lowStockTable);

        this.getChildren().addAll(topBar, cardsBox, warningCard);
        loadData();
    }

    private VBox createStatCard(String title, Label valLabel, String styleClass) {
        VBox card = new VBox(6);
        card.getStyleClass().add(styleClass);
        Label t = new Label(title);
        t.getStyleClass().add("stat-label");
        valLabel.getStyleClass().add("stat-number");
        card.getChildren().addAll(t, valLabel);
        return card;
    }

    private void setupLowStockTable() {
        lowStockTable.setItems(lowStockList);
        lowStockTable.setPlaceholder(new Label("All stock levels are optimal! No low stock alerts."));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode / SKU");
        barcodeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBarcode()));
        barcodeCol.setPrefWidth(120);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product & Specs");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullDisplayName()));
        nameCol.setPrefWidth(220);

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        catCol.setPrefWidth(120);

        TableColumn<Product, String> currentStockCol = new TableColumn<>("Current Stock");
        currentStockCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getStockQuantity())));
        currentStockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    Label badge = new Label(p.getStockQuantity() <= 0 ? "0 (Out of Stock)" : (p.getStockQuantity() + " left"));
                    badge.getStyleClass().add(p.getStockQuantity() <= 0 ? "badge-danger" : "badge-warning");
                    setGraphic(badge);
                }
            }
        });
        currentStockCol.setPrefWidth(120);

        TableColumn<Product, String> minLevelCol = new TableColumn<>("Min Alert Level");
        minLevelCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMinStockLevel())));
        minLevelCol.setPrefWidth(100);

        TableColumn<Product, Void> restockCol = new TableColumn<>("Quick Restock Action");
        restockCol.setCellFactory(col -> new TableCell<>() {
            private final Button add5Btn = new Button("＋5 Units");
            private final Button add10Btn = new Button("＋10 Units");
            private final HBox box = new HBox(6, add5Btn, add10Btn);

            {
                add5Btn.getStyleClass().add("btn-primary");
                add5Btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                add5Btn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    productDao.updateStock(p.getId(), 5);
                    loadData();
                });

                add10Btn.getStyleClass().add("btn-accent");
                add10Btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                add10Btn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    productDao.updateStock(p.getId(), 10);
                    loadData();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        restockCol.setPrefWidth(180);

        lowStockTable.getColumns().addAll(List.of(barcodeCol, nameCol, catCol, currentStockCol, minLevelCol, restockCol));
    }

    public void loadData() {
        DashboardSummary summary = reportDao.getDashboardSummary();
        totalRetailValLabel.setText(FormatUtil.formatCurrency(summary.getInventoryTotalValue()));
        totalCostValLabel.setText(FormatUtil.formatCurrency(summary.getInventoryTotalCost()));
        totalItemsLabel.setText(String.valueOf(summary.getTotalProductsCount()));
        alertItemsLabel.setText((summary.getLowStockCount() + summary.getOutOfStockCount()) + " items");

        List<Product> lowStock = productDao.findLowStockProducts();
        lowStockList.setAll(lowStock);
    }
}
