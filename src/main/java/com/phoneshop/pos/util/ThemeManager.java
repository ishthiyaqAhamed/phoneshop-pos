package com.phoneshop.pos.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    public enum Theme {
        DARK("🌙 Dark Mode", "theme-dark"),
        LIGHT("☀️ Light Mode", "theme-light");

        private final String displayName;
        private final String styleClass;

        Theme(String displayName, String styleClass) {
            this.displayName = displayName;
            this.styleClass = styleClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    private static Theme currentTheme = Theme.DARK;
    private static final List<Scene> registeredScenes = new ArrayList<>();

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public static void toggleTheme() {
        setTheme(currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        for (Scene scene : registeredScenes) {
            applyCurrentTheme(scene);
        }
    }

    public static void registerScene(Scene scene) {
        if (scene != null && !registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyCurrentTheme(scene);
        }
    }

    public static void applyCurrentTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        scene.getRoot().getStyleClass().removeAll(Theme.DARK.getStyleClass(), Theme.LIGHT.getStyleClass());
        scene.getRoot().getStyleClass().add(currentTheme.getStyleClass());
    }

    public static void applyThemeToStage(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            registerScene(stage.getScene());
        }
    }
}
