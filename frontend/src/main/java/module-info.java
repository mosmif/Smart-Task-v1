module com.example.smarttask_frontend {

    // ===== JavaFX =====
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // ===== HTTP & JSON =====
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // ===== Optional =====
    requires com.calendarfx.view;
    requires java.desktop;
    requires com.fasterxml.jackson.core;
    requires net.bytebuddy;

    // ===== JavaFX Controllers (FXML reflection) =====
    opens com.example.smarttask_frontend.auth.controller to javafx.fxml;
    opens com.example.smarttask_frontend.tasks.controller to javafx.fxml;

    opens com.example.smarttask_frontend.dashboard to javafx.fxml;
    opens com.example.smarttask_frontend.calendar to javafx.fxml;
    opens com.example.smarttask_frontend.subtasks.controller to javafx.fxml;

    // ===== ENTITIES =====
    opens com.example.smarttask_frontend.entity
            to javafx.base, com.fasterxml.jackson.databind;

    // ===== DTOs =====
    opens com.example.smarttask_frontend.dto
            to com.fasterxml.jackson.databind;

    // ===== EXPORTS =====
    exports com.example.smarttask_frontend;
    exports com.example.smarttask_frontend.subtasks.controller;
    exports com.example.smarttask_frontend.subtasks.service;
}
