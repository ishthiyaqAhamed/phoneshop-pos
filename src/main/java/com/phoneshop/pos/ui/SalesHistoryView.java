package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.SaleDao;
import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

public class SalesHistoryView extends VBox {
    private final SaleDao saleDao = new SaleDao();
    private final TableView<Sale> table = new TableView<>();
    private final ObservableList<Sale> salesList = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();
    private final ComboBox<String> dateFilter = new ComboBox<>();

    public SalesHistoryView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(18));

        // Header
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Sales Invoices & Transaction History");
        pageTitle.getStyleClass().add("card-title");
        pageTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        searchField.setPromptText("Search invoice #, customer, or cashier...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, o, n) -> loadSales());

        dateFilter.setItems(FXCollections.observableArrayList("Today", "Last 7 Days", "This Month", "All Time"));
        dateFilter.setValue("This Month");
        dateFilter.setPrefWidth(130);
        dateFilter.setOnAction(e -> loadSales());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadSales());

        topBar.getChildren().addAll(pageTitle, searchField, dateFilter, refreshBtn);

        // Sales Table
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        this.getChildren().addAll(topBar, table);
        loadSales();
    }

    private void setupTable() {
        table.setItems(salesList);
        table.setPlaceholder(new Label("No sales transactions found."));

        TableColumn<Sale, String> invCol = new TableColumn<>("Invoice #");
        invCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceNumber()));
        invCol.setPrefWidth(160);

        TableColumn<Sale, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatDateTime(d.getValue().getCreatedAt())));
        dateCol.setPrefWidth(150);

        TableColumn<Sale, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCashierName()));
        cashierCol.setPrefWidth(120);

        TableColumn<Sale, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(d -> {
            Sale s = d.getValue();
            String name = s.getCustomerName() != null ? s.getCustomerName() : "Walk-in";
            if (s.getCustomerPhone() != null && !s.getCustomerPhone().isBlank()) {
                name += " (" + s.getCustomerPhone() + ")";
            }
            return new SimpleStringProperty(name);
        });
        custCol.setPrefWidth(180);

        TableColumn<Sale, String> methodCol = new TableColumn<>("Payment");
        methodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentMethod()));
        methodCol.setPrefWidth(100);

        TableColumn<Sale, String> totalCol = new TableColumn<>("Total Amount");
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatCurrency(d.getValue().getTotalAmount())));
        totalCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        totalCol.setPrefWidth(120);

        TableColumn<Sale, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("📄 View Receipt");
            {
                viewBtn.getStyleClass().add("btn-accent");
                viewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                viewBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    Sale fullSale = saleDao.findById(sale.getId());
                    if (fullSale != null) {
                        ReceiptPreviewDialog dialog = new ReceiptPreviewDialog(fullSale);
                        dialog.show();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });
        actionCol.setPrefWidth(120);

        table.getColumns().addAll(List.of(invCol, dateCol, cashierCol, custCol, methodCol, totalCol, actionCol));

        table.setRowFactory(tv -> {
            TableRow<Sale> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Sale fullSale = saleDao.findById(row.getItem().getId());
                    if (fullSale != null) {
                        ReceiptPreviewDialog dialog = new ReceiptPreviewDialog(fullSale);
                        dialog.show();
                    }
                }
            });
            return row;
        });
    }

    public void loadSales() {
        LocalDate start = null;
        LocalDate end = LocalDate.now();
        String filter = dateFilter.getValue();

        if ("Today".equalsIgnoreCase(filter)) {
            start = LocalDate.now();
        } else if ("Last 7 Days".equalsIgnoreCase(filter)) {
            start = LocalDate.now().minusDays(7);
        } else if ("This Month".equalsIgnoreCase(filter)) {
            start = LocalDate.now().withDayOfMonth(1);
        }

        String kw = searchField.getText();
        List<Sale> list = saleDao.findRecentSales(100, kw, start, end);
        salesList.setAll(list);
    }
}
