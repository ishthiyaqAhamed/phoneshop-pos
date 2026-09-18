package com.phoneshop.pos.ui;

import com.phoneshop.pos.Main;
import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.model.User;
import com.phoneshop.pos.service.AuthService;
import com.phoneshop.pos.util.AppSession;
import com.phoneshop.pos.util.FormatUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class MainLayout extends BorderPane {
    private final AuthService authService = new AuthService();
    private final VBox sidebarNav = new VBox(6);
    private final StackPane contentPane = new StackPane();

    // Views
    private PosTerminalView posTerminalView;
    private ProductManagementView productManagementView;
    private BarcodeLabelView barcodeLabelView;
    private SalesHistoryView salesHistoryView;
    private InventoryDashboardView inventoryDashboardView;
    private ReportsView reportsView;
    private CashierActivityView cashierActivityView;
    private UserManagementView userManagementView;

    private Button activeNavBtn = null;

    public MainLayout() {
        this.setStyle("-fx-background-color: #0f172a;");

        // 1. Top Bar
        this.setTop(createTopBar());

        // 2. Left Sidebar Navigation
        this.setLeft(createSidebar());

        // 3. Center Content Pane
        contentPane.setStyle("-fx-background-color: #0f172a;");
        this.setCenter(contentPane);

        // Initial default view
        showPosTerminal();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label brandTitle = new Label(AppConfig.getShopName() + " — " + AppConfig.getShopBranch());
        brandTitle.getStyleClass().add("top-bar-title");
        HBox.setHgrow(brandTitle, Priority.ALWAYS);

        // Real-time Clock
        Label clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            clockLabel.setText("🕒 " + FormatUtil.formatDateTime(LocalDateTime.now()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // User Badge
        User current = AppSession.getInstance().getCurrentUser();
        HBox userBadge = new HBox(8);
        userBadge.getStyleClass().add("user-badge");
        userBadge.setAlignment(Pos.CENTER);

        Label userNameLabel = new Label(current != null ? current.getFullName() : "Guest");
        userNameLabel.getStyleClass().add("user-badge-name");

        Label rolePill = new Label(current != null ? current.getRole().name() : "NONE");
        rolePill.getStyleClass().add("badge");
        rolePill.getStyleClass().add(AppSession.getInstance().isAdmin() ? "badge-info" : "badge-success");

        userBadge.getChildren().addAll(userNameLabel, rolePill);

        // Logout Button
        Button logoutBtn = new Button("🚪 Sign Out");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            Main.showLogin();
        });

        topBar.getChildren().addAll(brandTitle, clockLabel, userBadge, logoutBtn);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");

        // Header Title
        VBox titleBox = new VBox(2);
        titleBox.setPadding(new Insets(0, 0, 12, 4));
        Label logo = new Label("📱 POS SYSTEM");
        logo.getStyleClass().add("sidebar-brand-title");
        Label sub = new Label("PHONE SHOP TERMINAL");
        sub.getStyleClass().add("sidebar-brand-subtitle");
        titleBox.getChildren().addAll(logo, sub);

        sidebar.getChildren().addAll(titleBox, sidebarNav);

        buildNavigationItems();
        return sidebar;
    }

    private void buildNavigationItems() {
        sidebarNav.getChildren().clear();

        boolean isAdmin = AppSession.getInstance().isAdmin();

        // Section: Cashier & Sales Operations
        addNavSectionLabel("MAIN OPERATIONS");

        Button posBtn = createNavButton("🛒  POS Terminal", this::showPosTerminal);
        Button productsBtn = createNavButton("📱  Product Catalog", this::showProducts);
        Button barcodesBtn = createNavButton("🏷  Print Barcodes", this::showBarcodeLabels);

        sidebarNav.getChildren().addAll(posBtn, productsBtn, barcodesBtn);

        // Admin Only Sections
        if (isAdmin) {
            addNavSectionLabel("ADMINISTRATION & REPORTS");

            Button salesBtn = createNavButton("📋  Sales History", this::showSalesHistory);
            Button stockBtn = createNavButton("📊  Stock Dashboard", this::showInventoryDashboard);
            Button reportsBtn = createNavButton("📈  Profit & Analytics", this::showReports);
            Button cashierBtn = createNavButton("👥  Cashier Monitoring", this::showCashierActivity);
            Button usersBtn = createNavButton("⚙️  User Accounts", this::showUserManagement);

            sidebarNav.getChildren().addAll(salesBtn, stockBtn, reportsBtn, cashierBtn, usersBtn);
        }

        activeNavBtn = posBtn;
        posBtn.getStyleClass().add("active");
    }

    private void addNavSectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("nav-section-label");
        sidebarNav.getChildren().add(l);
    }

    private Button createNavButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            if (activeNavBtn != null) {
                activeNavBtn.getStyleClass().remove("active");
            }
            b.getStyleClass().add("active");
            activeNavBtn = b;
            action.run();
        });
        return b;
    }

    private void setContent(Node node) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(node);
    }

    public void showPosTerminal() {
        if (posTerminalView == null) {
            posTerminalView = new PosTerminalView();
        } else {
            posTerminalView.refreshProducts();
        }
        setContent(posTerminalView);
    }

    public void showProducts() {
        if (productManagementView == null) {
            productManagementView = new ProductManagementView();
        } else {
            productManagementView.refreshData();
        }
        setContent(productManagementView);
    }

    public void showBarcodeLabels() {
        if (barcodeLabelView == null) {
            barcodeLabelView = new BarcodeLabelView();
        } else {
            barcodeLabelView.loadProducts();
        }
        setContent(barcodeLabelView);
    }

    public void showSalesHistory() {
        if (salesHistoryView == null) {
            salesHistoryView = new SalesHistoryView();
        } else {
            salesHistoryView.loadSales();
        }
        setContent(salesHistoryView);
    }

    public void showInventoryDashboard() {
        if (inventoryDashboardView == null) {
            inventoryDashboardView = new InventoryDashboardView();
        } else {
            inventoryDashboardView.loadData();
        }
        setContent(inventoryDashboardView);
    }

    public void showReports() {
        if (reportsView == null) {
            reportsView = new ReportsView();
        } else {
            reportsView.loadData();
        }
        setContent(reportsView);
    }

    public void showCashierActivity() {
        if (cashierActivityView == null) {
            cashierActivityView = new CashierActivityView();
        } else {
            cashierActivityView.loadData();
        }
        setContent(cashierActivityView);
    }

    public void showUserManagement() {
        if (userManagementView == null) {
            userManagementView = new UserManagementView();
        } else {
            userManagementView.loadUsers();
        }
        setContent(userManagementView);
    }
}
