package com.phoneshop.pos.ui;

import com.phoneshop.pos.Main;
import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class LoginView extends StackPane {
    private final AuthService authService = new AuthService();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button loginBtn = new Button("Sign In to POS");

    public LoginView() {
        this.getStyleClass().add("content-area");

        VBox loginCard = new VBox(16);
        loginCard.getStyleClass().add("card");
        loginCard.setMaxWidth(420);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(32, 28, 32, 28));

        // Header Logo / Icon
        StackPane iconCircle = new StackPane();
        Circle circle = new Circle(28, Color.web("#3b82f6"));
        Label iconLabel = new Label("📱");
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconCircle.getChildren().addAll(circle, iconLabel);

        Label titleLabel = new Label(AppConfig.getShopName());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Point of Sale & Inventory Terminal");
        subtitleLabel.getStyleClass().add("card-subtitle");

        VBox headerBox = new VBox(6, iconCircle, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Form Fields
        VBox formBox = new VBox(12);
        formBox.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("Username / Staff ID");
        userLabel.getStyleClass().add("stat-label");
        usernameField.setPromptText("e.g. admin or cashier1");
        usernameField.setPrefHeight(38);

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("stat-label");
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(38);

        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(42);
        loginBtn.setOnAction(e -> handleLogin());

        // Enter key trigger
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());

        formBox.getChildren().addAll(userLabel, usernameField, passLabel, passwordField, errorLabel, loginBtn);

        // Quick login demo buttons for easy switching between Admin and 2 Cashiers
        VBox demoBox = new VBox(8);
        demoBox.getStyleClass().add("card");
        demoBox.setStyle("-fx-padding: 12;");
        
        Label demoTitle = new Label("Quick Access Accounts:");
        demoTitle.getStyleClass().add("stat-label");

        HBox demoButtons = new HBox(8);
        demoButtons.setAlignment(Pos.CENTER);

        Button adminQuick = createQuickBtn("Admin", "admin", "admin123");
        Button cashier1Quick = createQuickBtn("Cashier 1", "cashier1", "cashier123");
        Button cashier2Quick = createQuickBtn("Cashier 2", "cashier2", "cashier123");

        demoButtons.getChildren().addAll(adminQuick, cashier1Quick, cashier2Quick);
        demoBox.getChildren().addAll(demoTitle, demoButtons);

        loginCard.getChildren().addAll(headerBox, formBox, demoBox);
        this.getChildren().add(loginCard);
        StackPane.setAlignment(loginCard, Pos.CENTER);
    }

    private Button createQuickBtn(String label, String user, String pass) {
        Button b = new Button(label);
        b.getStyleClass().add("btn-secondary");
        b.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        b.setOnAction(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            handleLogin();
        });
        return b;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            errorLabel.setText("Please enter username and password.");
            errorLabel.setVisible(true);
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        boolean success = authService.login(username, password);
        if (success) {
            errorLabel.setVisible(false);
            Main.showMainApp();
        } else {
            errorLabel.setText("Invalid username or password.");
            errorLabel.setVisible(true);
            loginBtn.setDisable(false);
            loginBtn.setText("Sign In to POS");
        }
    }
}
