package service;

import dao.CommentDao;
import model.Comment;
import socket.CommentSocket;
import util.JsonUtil;

import java.util.List;

public class CommentService {

    private final CommentDao commentDao = new CommentDao();

    public void addComment(Comment c) {
        commentDao.save(c);
        CommentSocket.broadcast(JsonUtil.toJson(c));
    }

    public List<Comment> getByTask(long taskId) {
        return commentDao.findByTaskId(taskId);
    }
}
