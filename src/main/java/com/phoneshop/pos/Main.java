package com.phoneshop.pos;

import com.phoneshop.pos.config.AppConfig;
import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.service.AuthService;
import com.phoneshop.pos.ui.LoginView;
import com.phoneshop.pos.ui.MainLayout;
import com.phoneshop.pos.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Stage primaryStage;
    private static Scene currentScene;

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
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

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

    private static void setAppRoot(Parent root, double defaultWidth, double defaultHeight) {
        if (currentScene == null) {
            currentScene = new Scene(root, defaultWidth, defaultHeight);
            try {
                currentScene.getStylesheets().add(Main.class.getResource("/styles/app.css").toExternalForm());
            } catch (Exception ignored) {}
            ThemeManager.registerScene(currentScene);
            primaryStage.setScene(currentScene);
            primaryStage.centerOnScreen();
        } else {
            currentScene.setRoot(root);
            ThemeManager.applyCurrentTheme(currentScene);
        }
    }

    public static void showLogin() {
        LoginView loginView = new LoginView();
        setAppRoot(loginView, 1180, 740);
    }

    public static void showMainApp() {
        MainLayout mainLayout = new MainLayout();
        setAppRoot(mainLayout, 1280, 800);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
