package com.example.smarttask_frontend.auth.controller;

import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField email;   // ✅ email, not username

    @FXML
    private PasswordField password;

    @FXML
    private Button loginButton;

    private final UserService userService = new UserService();

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
