package com.example.smarttask_frontend.auth.controller;

import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.entity.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField email;   // ✅ email, not username

    @FXML
    private PasswordField password;

    @FXML
    private Button loginButton;

    private final UserService userService = new UserService();


    @FXML private TextField regName;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML
    public void initialize() {

        loginForm.setVisible(true);
        loginForm.setManaged(true);

        registerForm.setVisible(false);
        registerForm.setManaged(false);
    }

    @FXML
    private void switchToRegister() {

        loginForm.setVisible(false);
        loginForm.setManaged(false);

        registerForm.setVisible(true);
        registerForm.setManaged(true);
    }

    @FXML
    private void switchToLogin() {

        registerForm.setVisible(false);
        registerForm.setManaged(false);

        loginForm.setVisible(true);
        loginForm.setManaged(true);
    }

    @FXML
    private void register() {

        String name = regName.getText();
        String mail = regEmail.getText();
        String pass = regPassword.getText();

        if (name.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        new Thread(() -> {

            boolean success =
                    userService.register(name, mail, pass);

            Platform.runLater(() -> {

                if (success) {

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Account created successfully! Please sign in.");
                    alert.showAndWait();

                    regName.clear();
                    regEmail.clear();
                    regPassword.clear();

                    switchToLogin();

                } else {
                    showError("Registration failed. Email might already be in use.");
                }

            });

        }).start();
    }

    @FXML
    private void login() {

        User user = userService.login(
                email.getText(),
                password.getText()
        );

        if (user == null) {
            showError("Invalid email or password");
            return;
        }

        try {
            UserSession.setUser(user);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/DashboardView.fxml")
            );

            Scene scene = new Scene(loader.load(), 1200, 700);

            // ✅ Get current stage from button
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Smart Task Manager");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

}
