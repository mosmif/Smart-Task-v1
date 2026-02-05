package com.example.smarttask_frontend.comments;

import com.example.smarttask_frontend.entity.Comment;
import com.example.smarttask_frontend.session.UserSession;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

public class CommentCell extends ListCell<Comment> {

    private final Map<Long, String> userMap;

    public CommentCell(Map<Long, String> userMap) {
        this.userMap = userMap;
    }

    @Override
    protected void updateItem(Comment comment, boolean empty) {
        super.updateItem(comment, empty);

        if (empty || comment == null) {
            setGraphic(null);
            return;
        }

        boolean isMine = comment.getUserId() == UserSession.getUserId();

        String username =
                userMap.getOrDefault(comment.getUserId(), "Unknown");

        Label name = new Label(username);
        name.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

        VBox bubble = new VBox(5); // spacing inside message
        bubble.getChildren().add(name);

        // ------------------------
        // TEXT MESSAGE
        // ------------------------
        if (comment.getContent() != null && !comment.getContent().isBlank()) {

            Label text = new Label(comment.getContent());
            text.setWrapText(true);
            text.setMaxWidth(250);

            text.setStyle(
                    isMine
                            ? "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 12;"
                            : "-fx-background-color: #e0e0e0; -fx-padding: 8; -fx-background-radius: 12;"
            );

            bubble.getChildren().add(text);
        }

        // ------------------------
        // ATTACHMENT
        // ------------------------
        if (comment.getFileName() != null) {

            Button download = new Button("📎 " + comment.getFileName());
            download.setStyle("-fx-font-size: 11;");

            download.setOnAction(e -> {
                System.out.println("Download file: " + comment.getFilePath());
                // TODO: open/download file
            });

            bubble.getChildren().add(download);
        }

        HBox container = new HBox(bubble);

        container.setAlignment(
                isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT
        );

        setGraphic(container);
    }
}
