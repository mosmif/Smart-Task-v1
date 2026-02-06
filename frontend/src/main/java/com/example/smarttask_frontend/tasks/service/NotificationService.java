package com.example.smarttask_frontend.tasks.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.entity.Notification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import com.google.gson.Gson;
import javafx.application.Platform;

public class NotificationService {

    private static final String BASE_URL;
    private WebSocket webSocket;
    private final Gson gson = new Gson();

    private NotificationListener listener;

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    public void setNotificationListener(NotificationListener listener) {
        this.listener = listener;
    }

    public void connectWebSocket() {
        try {
            String wsUrl = AppConfig.get("backend.base-url")
                    .replace("http", "ws") + "ws/notifications";

            webSocket = java.net.http.HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

                            // --- FIX STARTS HERE ---
                            // We received "hello". Don't parse it.
                            // Just treat it as a signal to refresh.
                            System.out.println("[WS] Signal received: " + data);

                            if (listener != null) {
                                // Pass null because we are reloading from DB anyway
                                Platform.runLater(() -> listener.onNotificationReceived(null));
                            }
                            // --- FIX ENDS HERE ---

                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }
                    }).join();

            System.out.println("[NotificationService] Connected to WebSocket: " + wsUrl);
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to connect WebSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static {
        String baseUrl = AppConfig.get("backend.base-url");
        if (baseUrl.endsWith("/")) {
            BASE_URL = baseUrl + "notifications/";
        } else {
            BASE_URL = baseUrl + "/notifications/";
        }
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ✅ Get notifications by user - matches backend GET
    // /notifications/user/{userId}
    public List<Notification> getNotificationsByUser(Long userId) {
        try {
            String url = BASE_URL + "user/" + userId;
            System.out.println("[NotificationService] GET: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[NotificationService] Response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                List<Notification> notifications = objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<Notification>>() {
                        });
                System.out.println("[NotificationService] Loaded " + notifications.size() + " notifications");
                return notifications;
            } else if (response.statusCode() == 404) {
                System.out.println("[NotificationService] No notifications found for user: " + userId);
                return List.of();
            }

            throw new RuntimeException("Failed to load notifications, status: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("[NotificationService] Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    // ✅ Mark notification as read - matches backend PUT
    // /notifications/{id}/user/{userId}/read
    public boolean markAsRead(Long notificationId, Long userId) {
        try {
            String url = BASE_URL + notificationId + "/user/" + userId + "/read";
            System.out.println("[NotificationService] PUT: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[NotificationService] Mark as read response: " + response.statusCode());

            return response.statusCode() == 204 || response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("[NotificationService] Error marking as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Create notification - matches backend POST /notifications/user/{userId}
    public boolean createNotification(Long userId, String message) {
        try {
            String url = BASE_URL + "user/" + userId;
            System.out.println("[NotificationService] POST: " + url);

            // Create notification object
            Notification notification = new Notification();
            notification.setMessage(message);

            String jsonBody = objectMapper.writeValueAsString(notification);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[NotificationService] Create notification response: " + response.statusCode());

            if (response.statusCode() == 201 || response.statusCode() == 200) {

                // Parse the created notification returned by backend
                Notification createdNotification = objectMapper.readValue(response.body(), Notification.class);
                // ✅ Push it to local WebSocket listener immediately
                if (webSocket != null && listener != null) {
                    System.out.println("attempt to send msg");
                    Platform.runLater(() -> listener.onNotificationReceived(createdNotification));
                }

                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[NotificationService] Error creating notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Get unread count - matches backend GET
    // /notifications/user/{userId}/unread-count
    public int getUnreadCount(Long userId) {
        try {
            String url = BASE_URL + "user/" + userId + "/unread-count";
            System.out.println("[NotificationService] GET: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[NotificationService] Unread count response: " + response.statusCode());

            if (response.statusCode() == 200) {
                return Integer.parseInt(response.body().trim());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("[NotificationService] Error getting unread count: " + e.getMessage());
            return 0;
        }
    }
}