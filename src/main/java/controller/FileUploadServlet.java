package controller;

import dao.AttachementDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import model.Comment;
import service.CommentService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet("/comments/upload")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,   // 1MB memory threshold
        maxFileSize = 50 * 1024 * 1024,    // 50MB per file
        maxRequestSize = 60 * 1024 * 1024  // 60MB total request
)
public class FileUploadServlet extends HttpServlet {

    private static final String UPLOAD_DIR =
            System.getProperty("user.home") + File.separator + "smarttask_uploads";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        resp.setContentType("application/json");

        try {

            // =========================
            // 1. Validate inputs
            // =========================
            String taskParam = req.getParameter("taskId");
            String userParam = req.getParameter("userId");
            String text = req.getParameter("content");

            if (taskParam == null || userParam == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Missing parameters\"}");
                return;
            }

            long taskId = Long.parseLong(taskParam);
            long userId = Long.parseLong(userParam);

            Part filePart = req.getPart("file");

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"No file uploaded\"}");
                return;
            }

            // =========================
            // 2. Create upload folder
            // =========================
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            // =========================
            // 3. Safe filename
            // =========================
            String originalName = Paths.get(filePart.getSubmittedFileName())
                    .getFileName()
                    .toString();

            String safeName =
                    System.currentTimeMillis() + "_" +
                            originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

            Path file = uploadPath.resolve(safeName);

            // =========================
            // 4. Save file to disk
            // =========================
            filePart.write(file.toString());

            // =========================
            // 5. Save attachment record
            // =========================
            AttachementDao attachmentDao = new AttachementDao();
            attachmentDao.saveAttachment(taskId, safeName, file.toString());

            // =========================
            // 6. Create comment
            // =========================
            Comment c = new Comment();
            c.setTaskId(taskId);
            c.setUserId(userId);
            c.setContent(text);
            c.setFileName(safeName);
            c.setFilePath(file.toString());

            new CommentService().addComment(c);

            // =========================
            // 7. Success response
            // =========================
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"status\":\"ok\"}");

        } catch (Exception e) {

            e.printStackTrace();

            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Upload failed\"}");
        }
    }
}
