package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.UserSessionDao;
import com.phoneshop.pos.model.UserSession;
import com.phoneshop.pos.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class CashierActivityView extends VBox {
    private final UserSessionDao sessionDao = new UserSessionDao();

    private final TableView<UserSession> activeTable = new TableView<>();
    private final ObservableList<UserSession> activeList = FXCollections.observableArrayList();

    private final TableView<UserSession> historyTable = new TableView<>();
    private final ObservableList<UserSession> historyList = FXCollections.observableArrayList();

    public CashierActivityView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(18));

        // Header
        HBox topBar = new HBox();
        Label pageTitle = new Label("Cashier Login Activity & Shift Monitoring");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Sessions");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        topBar.getChildren().addAll(pageTitle, refreshBtn);

        // Section 1: Currently Logged In Cashiers
        VBox activeCard = new VBox(10);
        activeCard.getStyleClass().add("card");
        activeCard.setPrefHeight(220);

        Label activeTitle = new Label("🟢 Cashiers Currently Logged In (Live Status)");
        activeTitle.setStyle("-fx-text-fill: #34d399; -fx-font-size: 14px; -fx-font-weight: bold;");

        setupActiveTable();
        VBox.setVgrow(activeTable, Priority.ALWAYS);
        activeCard.getChildren().addAll(activeTitle, activeTable);

        // Section 2: Historical Cashier Login & Shift History
        VBox historyCard = new VBox(10);
        historyCard.getStyleClass().add("card");
        VBox.setVgrow(historyCard, Priority.ALWAYS);

        Label historyTitle = new Label("📋 Shift History & Past Login Logs");
        historyTitle.getStyleClass().add("card-title");

        setupHistoryTable();
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        historyCard.getChildren().addAll(historyTitle, historyTable);

        this.getChildren().addAll(topBar, activeCard, historyCard);
        loadData();
    }

    private void setupActiveTable() {
        activeTable.setItems(activeList);
        activeTable.setPlaceholder(new Label("No cashier is currently logged in."));

        TableColumn<UserSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty("ONLINE"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label("● ACTIVE NOW");
                    badge.getStyleClass().add("badge-success");
                    setGraphic(badge);
                }
            }
        });
        statusCol.setPrefWidth(120);

        TableColumn<UserSession, String> nameCol = new TableColumn<>("Staff Member");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName() + " (@" + d.getValue().getUsername() + ")"));
        nameCol.setPrefWidth(200);

        TableColumn<UserSession, String> loginCol = new TableColumn<>("Logged In At");
        loginCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatDateTime(d.getValue().getLoginTime())));
        loginCol.setPrefWidth(160);

        TableColumn<UserSession, String> durCol = new TableColumn<>("Shift Duration");
        durCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDurationFormatted()));
        durCol.setPrefWidth(120);

        TableColumn<UserSession, String> salesCol = new TableColumn<>("Shift Sales Total");
        salesCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatCurrency(d.getValue().getShiftSalesTotal())));
        salesCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        salesCol.setPrefWidth(140);

        TableColumn<UserSession, String> ordersCol = new TableColumn<>("Orders Count");
        ordersCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getShiftOrdersCount())));
        ordersCol.setStyle("-fx-alignment: CENTER;");
        ordersCol.setPrefWidth(100);

        activeTable.getColumns().addAll(List.of(statusCol, nameCol, loginCol, durCol, salesCol, ordersCol));
    }

    private void setupHistoryTable() {
        historyTable.setItems(historyList);
        historyTable.setPlaceholder(new Label("No shift history records found."));

        TableColumn<UserSession, String> nameCol = new TableColumn<>("Cashier / Staff");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName() + " (@" + d.getValue().getUsername() + ")"));
        nameCol.setPrefWidth(180);

        TableColumn<UserSession, String> loginCol = new TableColumn<>("Login Time");
        loginCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatDateTime(d.getValue().getLoginTime())));
        loginCol.setPrefWidth(150);

        TableColumn<UserSession, String> logoutCol = new TableColumn<>("Logout Time");
        logoutCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLogoutTime() != null ? FormatUtil.formatDateTime(d.getValue().getLogoutTime()) : "Session In Progress"));
        logoutCol.setPrefWidth(160);

        TableColumn<UserSession, String> durCol = new TableColumn<>("Total Time");
        durCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDurationFormatted()));
        durCol.setPrefWidth(100);

        TableColumn<UserSession, String> salesCol = new TableColumn<>("Shift Sales Total");
        salesCol.setCellValueFactory(d -> new SimpleStringProperty(FormatUtil.formatCurrency(d.getValue().getShiftSalesTotal())));
        salesCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
        salesCol.setPrefWidth(130);

        TableColumn<UserSession, String> ordersCol = new TableColumn<>("Orders");
        ordersCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getShiftOrdersCount())));
        ordersCol.setStyle("-fx-alignment: CENTER;");
        ordersCol.setPrefWidth(80);

        TableColumn<UserSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("ACTIVE".equalsIgnoreCase(item) ? "badge-success" : "badge-secondary");
                    badge.setStyle("ACTIVE".equalsIgnoreCase(item) ? "" : "-fx-background-color: #334155; -fx-text-fill: #94a3b8;");
                    setGraphic(badge);
                }
            }
        });
        statusCol.setPrefWidth(110);

        historyTable.getColumns().addAll(List.of(nameCol, loginCol, logoutCol, durCol, salesCol, ordersCol, statusCol));
    }

    public void loadData() {
        List<UserSession> active = sessionDao.findCurrentlyActiveSessions();
        activeList.setAll(active);

        List<UserSession> history = sessionDao.findAllSessions(100);
        historyList.setAll(history);
    }
}
