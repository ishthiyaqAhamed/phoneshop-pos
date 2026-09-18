package com.phoneshop.pos;

import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.service.AuthService;
import com.phoneshop.pos.ui.LoginView;
import com.phoneshop.pos.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        try {
            // Initialize Database Connection Pool & Schema
            DatabaseConfig.initialize();
        } catch (Exception e) {
            logger.error("Failed to connect or initialize database: {}", e.getMessage(), e);
        }

        stage.setTitle(AppConfig.getShopName() + " — Point of Sale & Inventory");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);

        // Graceful cleanup on window close
        stage.setOnCloseRequest(e -> {
            try {
                new AuthService().logout();
                DatabaseConfig.close();
            } catch (Exception ex) {
                logger.warn("Error during application shutdown: {}", ex.getMessage());
            }
        });

        showLogin();
        stage.show();
    }

    public static void showLogin() {
        LoginView loginView = new LoginView();
        Scene scene = new Scene(loginView, 1180, 760);
        try {
            scene.getStylesheets().add(Main.class.getResource("/styles/app.css").toExternalForm());
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void showMainApp() {
        MainLayout mainLayout = new MainLayout();
        Scene scene = new Scene(mainLayout, 1280, 820);
        try {
            scene.getStylesheets().add(Main.class.getResource("/styles/app.css").toExternalForm());
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
