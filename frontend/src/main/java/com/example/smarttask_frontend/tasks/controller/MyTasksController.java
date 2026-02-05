package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.comments.CommentCell;
import com.example.smarttask_frontend.comments.CommentService;
import com.example.smarttask_frontend.comments.LiveCommentClient;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.entity.User;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.subtasks.controller.SubtaskController;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.example.smarttask_frontend.entity.Comment;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MyTasksController implements Initializable {

    // --- Shared Table Injectables ---
    @FXML private TableView<Task> sharedTasksTable;
    @FXML private TableColumn<Task, String> sharedTitleColumn;
    @FXML private TableColumn<Task, String> sharedPriorityColumn;
    @FXML private TableColumn<Task, String> sharedCategoryColumn; // NEW
    @FXML private TableColumn<Task, String> sharedDueDateColumn;
    @FXML private TableColumn<Task, String> sharedStatusColumn;
    @FXML private TableColumn<Task, Void>   sharedSubTasksColumn; // NEW

    // --- Main Table Injectables ---
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, String> dueDateColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, Void> subTasksColumn;
    @FXML private TableColumn<Task, Void> shareColumn;

    // --- Other UI ---
    @FXML private ListView<Comment> commentList;
    @FXML private TextField commentField;

    // --- Services & State ---
    private LiveCommentClient ws;
    private Task selectedTask;
    private final TaskService taskService = new TaskService();
    private final CommentService commentService = new CommentService();
    private final UserService userService = new UserService();
    private Map<Long, String> userMap = new HashMap<>();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList("TODO", "IN_PROGRESS", "DONE");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Label emptyLabel = new Label("No comments yet");
        emptyLabel.getStyleClass().add("placeholder-label"); // CSS class for styling

        // 2. Attach it to the ListView
        commentList.setPlaceholder(emptyLabel);
        setupColumnWidths();
        setupMainTableColumns();
        setupSharedTableColumns(); // Updated method

        loadTasks();
        loadSharedTasks();

        // Load Users for Comments
        List<User> users = userService.getUsers();
        for (User u : users) {
            userMap.put(u.getId(), u.getUsername());
        }
        commentList.setCellFactory(list -> new CommentCell(userMap));

        setupSelectionListeners();
        setupWebSocket();
    }

    private void setupColumnWidths() {
        // Main Table Weights
        titleColumn.setPrefWidth(350);
        priorityColumn.setPrefWidth(100);
        categoryColumn.setPrefWidth(120);
        dueDateColumn.setPrefWidth(120);
        statusColumn.setPrefWidth(140);
        subTasksColumn.setPrefWidth(80);
        shareColumn.setPrefWidth(80);

        // Shared Table Weights (Matches Main Table)
        sharedTitleColumn.setPrefWidth(350);
        sharedPriorityColumn.setPrefWidth(100);
        sharedCategoryColumn.setPrefWidth(120);
        sharedDueDateColumn.setPrefWidth(120);
        sharedStatusColumn.setPrefWidth(140);
        sharedSubTasksColumn.setPrefWidth(80);
    }

    // ========================= MAIN TABLE SETUP =========================
    private void setupMainTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        // Category Logic
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"
        ));

        // Use Helper for Status Badge
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        statusColumn.setCellFactory(createStatusCellFactory());

        // Use Helper for Subtask Button
        subTasksColumn.setCellFactory(createSubtaskButtonFactory());

        setupShareButtonColumn(); // Only main tasks are shareable usually
    }

    // ========================= SHARED TABLE SETUP =========================
    private void setupSharedTableColumns() {
        sharedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        sharedPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        sharedDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        // 1. Category (Same logic as main)
        sharedCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"
        ));

        // 2. Status (Now uses the same Fancy Badge Style!)
        sharedStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        sharedStatusColumn.setCellFactory(createStatusCellFactory());

        // 3. Subtasks (Now uses the same Button logic!)
        sharedSubTasksColumn.setCellFactory(createSubtaskButtonFactory());
    }

    // ========================= HELPER FACTORIES (Reuse logic) =========================

    /**
     * Creates the "View" button for subtasks.
     * Reused by both tables.
     */
    private Callback<TableColumn<Task, Void>, TableCell<Task, Void>> createSubtaskButtonFactory() {
        return col -> new TableCell<>() {
            private final Button button = new Button("View");
            {
                button.getStyleClass().addAll("action-button", "subtask-btn");
                button.setOnAction(e -> {
                    // Get task safely from whichever table called this
                    Task task = getTableView().getItems().get(getIndex());
                    openSubtasksWindow(task.getId(), task.getTitle());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        };
    }

    /**
     * Creates the Colored Badge Status Dropdown.
     * Reused by both tables.
     */
    private Callback<TableColumn<Task, String>, TableCell<Task, String>> createStatusCellFactory() {
        return col -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>(statusOptions);
            {
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.getStyleClass().add("status-combo");

                comboBox.setOnAction(e -> {
                    if (getTableView() != null && getIndex() < getTableView().getItems().size()) {
                        Task task = getTableView().getItems().get(getIndex());
                        String newStatus = comboBox.getValue();

                        task.setStatus(newStatus);
                        updateStyle(newStatus);

                        System.out.println("Updating status ID: " + task.getId() + " to " + newStatus);
                        try {
                            taskService.updateTaskStatus(task.getId(), newStatus);
                        } catch (Exception ex) {
                            showError("Failed to update status.");
                            comboBox.setValue(getItem());
                        }
                    }
                });
            }

            private void updateStyle(String status) {
                comboBox.getStyleClass().removeAll("status-todo", "status-inprogress", "status-done");
                if (status != null) {
                    switch (status) {
                        case "TODO": comboBox.getStyleClass().add("status-todo"); break;
                        case "IN_PROGRESS": comboBox.getStyleClass().add("status-inprogress"); break;
                        case "DONE": comboBox.getStyleClass().add("status-done"); break;
                    }
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    updateStyle(item);
                    setGraphic(comboBox);
                }
            }
        };
    }

    // ========================= SHARE BUTTON (Main Table Only) =========================
    private void setupShareButtonColumn() {
        shareColumn.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button("Share");
            {
                button.getStyleClass().addAll("action-button", "share-btn");
                button.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    openShareTaskDialog(task);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    // ========================= DATA & EVENTS =========================
    private void loadTasks() {
        try {
            Long userId = UserSession.getUserId();
            taskTable.setItems(FXCollections.observableArrayList(taskService.getTasksByUser(userId)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSharedTasks() {
        try {
            Long userId = UserSession.getUserId();
            sharedTasksTable.setItems(FXCollections.observableArrayList(taskService.getSharedTasks(userId)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupSelectionListeners() {
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                sharedTasksTable.getSelectionModel().clearSelection();
                selectedTask = n;
                loadComments(n.getId());
            }
        });
        sharedTasksTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                taskTable.getSelectionModel().clearSelection();
                selectedTask = n;
                loadComments(n.getId());
            }
        });
    }

    private void setupWebSocket() {
        ws = new LiveCommentClient(comment -> {
            if (selectedTask != null && comment.getTaskId() == selectedTask.getId()) {
                javafx.application.Platform.runLater(() -> {
                    commentList.getItems().add(comment);
                    commentList.scrollTo(commentList.getItems().size() - 1);
                });
            }
        });
        ws.connect();
    }

    // ... loadComments, sendComment, openSubtasksWindow, openShareTaskDialog, aiGenerate, etc. remain the same ...

    private void loadComments(Long taskId) {
        commentList.getItems().clear();
        commentList.getItems().addAll(commentService.getComments(taskId));
        if(!commentList.getItems().isEmpty()) commentList.scrollTo(commentList.getItems().size() - 1);
    }

    @FXML public void sendComment(ActionEvent e) {
        String text = commentField.getText();
        if (text == null || text.trim().isEmpty() || selectedTask == null) return;
        Comment c = new Comment();
        c.setTaskId(selectedTask.getId());
        c.setUserId(UserSession.getUserId());
        c.setContent(text);
        commentService.addComment(c);
        commentField.clear();
    }

    private void openSubtasksWindow(Long taskId, String taskTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Subtasks.fxml"));
            Parent root = loader.load();
            SubtaskController controller = loader.getController();
            controller.setTaskId(taskId);
            Stage stage = new Stage();
            stage.setTitle("Subtasks - " + taskTitle);
            stage.setScene(new Scene(root, 800, 500));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); showError("Unable to open Subtasks"); }
    }

    private void openShareTaskDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ShareTaskView.fxml"));
            Parent root = loader.load();
            ShareTaskController controller = loader.getController();
            controller.setTaskId(task.getId(), task.getTitle());
            Stage stage = new Stage();
            stage.setTitle("Share Task");
            stage.setScene(new Scene(root, 350, 300));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); showError("Unable to open Share dialog"); }
    }

    @FXML private void createTask() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/CreateTaskView.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Create Task");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { showError("Could not open create task window."); }
    }

    @FXML public void aiGenerate() { System.out.println("AI features coming soon..."); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).show(); }

    @FXML
    public void attachFile() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file");

        // optional filters
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File file = chooser.showOpenDialog(commentField.getScene().getWindow());

        if (file == null) return;

        uploadAttachment(file);
    }

    private void uploadAttachment(File file) {

        if (selectedTask == null) {
            showError("Select a task first");
            return;
        }

        try {
            long userId = UserSession.getUserId();
            String text = commentField.getText(); // optional message

            System.out.println("Uploading: " + file.getName());

            commentService.uploadFile(file, selectedTask.getId(), userId, text);

            commentField.clear();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Upload failed");
        }
    }



}