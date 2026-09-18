package com.phoneshop.pos.ui;

import com.phoneshop.pos.dao.UserDao;
import com.phoneshop.pos.model.Role;
import com.phoneshop.pos.model.User;
import com.phoneshop.pos.util.DialogUtil;
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
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserManagementView extends VBox {
    private final UserDao userDao = new UserDao();
    private final TableView<User> table = new TableView<>();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    public UserManagementView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(16);
        this.setPadding(new Insets(18));

        // Header
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Staff & Cashier User Accounts");
        pageTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(pageTitle, Priority.ALWAYS);

        Button addStaffBtn = new Button("＋ Add New Staff Account");
        addStaffBtn.getStyleClass().add("btn-primary");
        addStaffBtn.setOnAction(e -> openAddUserDialog());

        topBar.getChildren().addAll(pageTitle, addStaffBtn);

        // Table
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        this.getChildren().addAll(topBar, table);
        loadUsers();
    }

    private void setupTable() {
        table.setItems(userList);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        idCol.setPrefWidth(60);

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        userCol.setPrefWidth(140);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        nameCol.setPrefWidth(200);

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name()));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("ADMIN".equalsIgnoreCase(item) ? "badge-info" : "badge-success");
                    setGraphic(badge);
                }
            }
        });
        roleCol.setPrefWidth(120);

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Disabled"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Active".equalsIgnoreCase(item) ? "badge-success" : "badge-danger");
                    setGraphic(badge);
                }
            }
        });
        statusCol.setPrefWidth(100);

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button resetBtn = new Button("🔑 Reset Password");
            private final Button toggleBtn = new Button("Toggle Status");
            private final HBox box = new HBox(6, resetBtn, toggleBtn);

            {
                resetBtn.getStyleClass().add("btn-secondary");
                resetBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                resetBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openResetPasswordDialog(u);
                });

                toggleBtn.getStyleClass().add("btn-secondary");
                toggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                toggleBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    userDao.updateStatus(u.getId(), !u.isActive());
                    loadUsers();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actionCol.setPrefWidth(220);

        table.getColumns().addAll(List.of(idCol, userCol, nameCol, roleCol, statusCol, actionCol));
    }

    public void loadUsers() {
        List<User> list = userDao.findAll();
        userList.setAll(list);
    }

    private void openAddUserDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Create Staff Account");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPrefSize(380, 420);

        Label title = new Label("Add New Staff / Cashier");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");

        TextField usernameF = new TextField();
        usernameF.setPromptText("Username (e.g. cashier3)");

        TextField fullNameF = new TextField();
        fullNameF.setPromptText("Staff Full Name");

        PasswordField passF = new PasswordField();
        passF.setPromptText("Initial Password");

        ComboBox<Role> roleCombo = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleCombo.setValue(Role.CASHIER);
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Create Account");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String u = usernameF.getText() != null ? usernameF.getText().trim() : "";
            String name = fullNameF.getText() != null ? fullNameF.getText().trim() : "";
            String p = passF.getText() != null ? passF.getText() : "";

            if (u.isBlank() || name.isBlank() || p.isBlank()) {
                DialogUtil.showWarning("Missing Fields", "Please complete all fields.");
                return;
            }

            User newUser = new User();
            newUser.setUsername(u);
            newUser.setFullName(name);
            newUser.setPasswordHash(BCrypt.hashpw(p, BCrypt.gensalt(10)));
            newUser.setRole(roleCombo.getValue());
            newUser.setActive(true);

            boolean ok = userDao.create(newUser);
            if (ok) {
                dialog.close();
                loadUsers();
                DialogUtil.showInfo("User Created", "Staff account for " + name + " created successfully.");
            } else {
                DialogUtil.showError("Error", "Could not create user. Username might already exist.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(title,
                new Label("Username:"), usernameF,
                new Label("Full Name:"), fullNameF,
                new Label("Password:"), passF,
                new Label("Role:"), roleCombo,
                actions);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openResetPasswordDialog(User user) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Reset Password - " + user.getUsername());

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPrefSize(340, 260);

        Label title = new Label("Reset Password for @" + user.getUsername());
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold;");

        PasswordField newPassF = new PasswordField();
        newPassF.setPromptText("Enter new password");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Update Password");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String p = newPassF.getText() != null ? newPassF.getText() : "";
            if (p.isBlank()) {
                DialogUtil.showWarning("Empty Password", "Please type a new password.");
                return;
            }
            String hash = BCrypt.hashpw(p, BCrypt.gensalt(10));
            boolean ok = userDao.updatePassword(user.getId(), hash);
            if (ok) {
                dialog.close();
                DialogUtil.showInfo("Password Reset", "Password for @" + user.getUsername() + " was updated.");
            } else {
                DialogUtil.showError("Error", "Failed to update password.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(title, new Label("New Password:"), newPassF, actions);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
