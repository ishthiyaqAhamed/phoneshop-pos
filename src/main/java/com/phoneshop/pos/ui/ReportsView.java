package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.ReportDao;
import com.phoneshop.pos.model.DashboardSummary;
import com.phoneshop.pos.util.FormatUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.Map;

public class ReportsView extends ScrollPane {
    private final ReportDao reportDao = new ReportDao();

    private final Label todayRevLabel = new Label("Rs. 0.00");
    private final Label todayProfitLabel = new Label("Rs. 0.00");
    private final Label todayOrdersLabel = new Label("0 Orders");

    private final Label monthRevLabel = new Label("Rs. 0.00");
    private final Label monthProfitLabel = new Label("Rs. 0.00");
    private final Label monthOrdersLabel = new Label("0 Orders");

    private final VBox topSellingContainer = new VBox(8);
    private final VBox paymentMethodsContainer = new VBox(8);

    public ReportsView() {
        this.getStyleClass().add("content-area");
        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: #0f172a; -fx-background: #0f172a;");

        VBox content = new VBox(18);
        content.setPadding(new Insets(20));

        // Header
        HBox topBar = new HBox();
        Label pageTitle = new Label("Financial Reports & Sales Analytics");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Metrics");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());

        topBar.getChildren().addAll(pageTitle, refreshBtn);

        // 4 KPI Summary Cards
        HBox kpiBox = new HBox(14);
        kpiBox.setAlignment(Pos.CENTER);

        VBox cardTodayRev = createKpiCard("TODAY'S REVENUE", todayRevLabel, todayOrdersLabel, "stat-card-blue");
        VBox cardTodayProfit = createKpiCard("TODAY'S GROSS PROFIT", todayProfitLabel, new Label("Margin based on item cost"), "stat-card-green");
        VBox cardMonthRev = createKpiCard("MONTH REVENUE", monthRevLabel, monthOrdersLabel, "stat-card-cyan");
        VBox cardMonthProfit = createKpiCard("MONTH GROSS PROFIT", monthProfitLabel, new Label("Net margin estimated"), "stat-card-amber");

        HBox.setHgrow(cardTodayRev, Priority.ALWAYS);
        HBox.setHgrow(cardTodayProfit, Priority.ALWAYS);
        HBox.setHgrow(cardMonthRev, Priority.ALWAYS);
        HBox.setHgrow(cardMonthProfit, Priority.ALWAYS);
        kpiBox.getChildren().addAll(cardTodayRev, cardTodayProfit, cardMonthRev, cardMonthProfit);

        // Analytics Row (Top Selling & Payment Methods)
        HBox analyticsRow = new HBox(16);

        // Card 1: Top Selling Phones & Accessories
        VBox topSellingCard = new VBox(12);
        topSellingCard.getStyleClass().add("card");
        HBox.setHgrow(topSellingCard, Priority.ALWAYS);

        Label topSellingTitle = new Label("🏆 Top Selling Products & Phones");
        topSellingTitle.getStyleClass().add("card-title");
        topSellingCard.getChildren().addAll(topSellingTitle, topSellingContainer);

        // Card 2: Payment Methods Breakdown
        VBox paymentCard = new VBox(12);
        paymentCard.getStyleClass().add("card");
        HBox.setHgrow(paymentCard, Priority.ALWAYS);

        Label paymentTitle = new Label("💳 Revenue by Payment Method");
        paymentTitle.getStyleClass().add("card-title");
        paymentCard.getChildren().addAll(paymentTitle, paymentMethodsContainer);

        analyticsRow.getChildren().addAll(topSellingCard, paymentCard);

        content.getChildren().addAll(topBar, kpiBox, analyticsRow);
        this.setContent(content);

        loadData();
    }

    private VBox createKpiCard(String title, Label valLabel, Label subLabel, String styleClass) {
        VBox card = new VBox(4);
        card.getStyleClass().add(styleClass);
        Label t = new Label(title);
        t.getStyleClass().add("stat-label");
        valLabel.getStyleClass().add("stat-number");
        subLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
        card.getChildren().addAll(t, valLabel, subLabel);
        return card;
    }

    public void loadData() {
        DashboardSummary summary = reportDao.getDashboardSummary();

        todayRevLabel.setText(FormatUtil.formatCurrency(summary.getTodayRevenue()));
        todayProfitLabel.setText(FormatUtil.formatCurrency(summary.getTodayProfit()));
        todayOrdersLabel.setText(summary.getTodayOrdersCount() + " Orders Completed");

        monthRevLabel.setText(FormatUtil.formatCurrency(summary.getMonthRevenue()));
        monthProfitLabel.setText(FormatUtil.formatCurrency(summary.getMonthProfit()));
        monthOrdersLabel.setText(summary.getMonthOrdersCount() + " Orders Completed");

        // Populate Top Selling
        topSellingContainer.getChildren().clear();
        Map<String, Integer> topSelling = summary.getTopSellingProducts();
        if (topSelling.isEmpty()) {
            Label empty = new Label("No sales data recorded yet.");
            empty.setStyle("-fx-text-fill: #64748b;");
            topSellingContainer.getChildren().add(empty);
        } else {
            int rank = 1;
            for (Map.Entry<String, Integer> entry : topSelling.entrySet()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #1e293b; -fx-padding: 8 12; -fx-background-radius: 6;");

                Label rLabel = new Label("#" + (rank++));
                rLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-min-width: 28px;");

                Label nameLabel = new Label(entry.getKey());
                nameLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: 500;");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Label qtyBadge = new Label(entry.getValue() + " sold");
                qtyBadge.getStyleClass().add("badge-success");

                row.getChildren().addAll(rLabel, nameLabel, qtyBadge);
                topSellingContainer.getChildren().add(row);
            }
        }

        // Populate Payment Methods
        paymentMethodsContainer.getChildren().clear();
        Map<String, Double> paymentMap = reportDao.getSalesByPaymentMethod();
        if (paymentMap.isEmpty()) {
            Label empty = new Label("No payment transactions recorded yet.");
            empty.setStyle("-fx-text-fill: #64748b;");
            paymentMethodsContainer.getChildren().add(empty);
        } else {
            for (Map.Entry<String, Double> entry : paymentMap.entrySet()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #1e293b; -fx-padding: 8 12; -fx-background-radius: 6;");

                Label methodLabel = new Label(entry.getKey());
                methodLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");
                HBox.setHgrow(methodLabel, Priority.ALWAYS);

                Label amountLabel = new Label(FormatUtil.formatCurrency(entry.getValue()));
                amountLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");

                row.getChildren().addAll(methodLabel, amountLabel);
                paymentMethodsContainer.getChildren().add(row);
            }
        }
    }
}
