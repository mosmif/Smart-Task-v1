package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Comment;
import model.LocalDateTimeAdapter;
import service.CommentService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/comments/*")
public class CommentController extends HttpServlet {

    private final CommentService service = new CommentService();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    new LocalDateTimeAdapter())
            .create();

    // GET history
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        long taskId = Long.parseLong(req.getPathInfo().substring(1));

        List<Comment> comments = service.getByTask(taskId);

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(comments));
    }

    // ✅ POST new comment
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Comment c = gson.fromJson(req.getReader(), Comment.class);

        service.addComment(c);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("{\"status\":\"ok\"}");
    }
}

