package com.example.smarttask_frontend.comments;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.entity.Comment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CommentService {

    private static final String BASE_URL =
            AppConfig.get("backend.base-url") + "comments";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // GET comment history
    // =========================
    public List<Comment> getComments(long taskId) {

        try {
            String url = BASE_URL + "/" + taskId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<Comment>>() {}
                );
            }

            throw new RuntimeException("Failed to load comments");

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // =========================
    // POST new comment
    // =========================
    public void addComment(Comment comment) {

        try {
            String json = objectMapper.writeValueAsString(comment);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Failed to add comment");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(File file, long taskId, long userId, String text) throws Exception {

        String boundary = "----JavaBoundary";
        var body = new ArrayList<byte[]>();

        // taskId
        body.add(("--" + boundary + "\r\n").getBytes());
        body.add(("Content-Disposition: form-data; name=\"taskId\"\r\n\r\n" + taskId + "\r\n").getBytes());

        // userId
        body.add(("--" + boundary + "\r\n").getBytes());
        body.add(("Content-Disposition: form-data; name=\"userId\"\r\n\r\n" + userId + "\r\n").getBytes());

        // comment text
        body.add(("--" + boundary + "\r\n").getBytes());
        body.add(("Content-Disposition: form-data; name=\"content\"\r\n\r\n" + text + "\r\n").getBytes());

        // file
        body.add(("--" + boundary + "\r\n").getBytes());
        body.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        body.add(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
        body.add(Files.readAllBytes(file.toPath()));
        body.add(("\r\n--" + boundary + "--").getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(body))
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Upload failed: " + response.body());
        }
    }





}
