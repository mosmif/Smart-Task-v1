package com.example.smarttask_frontend.comments;

import com.example.smarttask_frontend.entity.Comment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class LiveCommentClient implements WebSocket.Listener {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CommentListener listener;

    private WebSocket webSocket; // ✅ store connection

    public interface CommentListener {
        void onComment(Comment comment);
    }

    public LiveCommentClient(CommentListener listener) {
        this.listener = listener;
        this.mapper.registerModule(new JavaTimeModule());
    }

    // ================= CONNECT =================
    public void connect() {

        HttpClient client = HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:8080/comments"),
                        this
                )
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    ws.request(1);
                });
    }

    // ================= CLOSE =================
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "Client closing"
            );
            System.out.println("WebSocket closed");
        }
    }

    // ================= LISTENER =================
    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WebSocket Connected!");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence data,
                                     boolean last) {

        try {
            Comment c =
                    mapper.readValue(data.toString(), Comment.class);

            Platform.runLater(() -> listener.onComment(c));

        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }
}
