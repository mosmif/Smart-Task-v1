package service;

import dao.NotificationDao;
import model.Notification;

import java.util.List;
import java.util.Optional;


import com.google.gson.Gson;

import controller.NotificationWebSocket;

public class NotificationService {

    private final NotificationDao notificationDao;

    public NotificationService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public List<Notification> getUserNotifications(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return notificationDao.findByUser(userId);
    }

    public Notification getNotification(Long id, Long userId) {
        if (id == null || userId == null) {
            return null;
        }
        Optional<Notification> notification = notificationDao.findById(id, userId);
        return notification.orElse(null);
    }

    public Notification create(Long userId, String message) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notificationDao.save(notification);

        try {
            NotificationWebSocket.broadcastNotification("hello");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notification;
    }

    public boolean markAsRead(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }
        return notificationDao.markAsRead(id, userId);
    }
    
    public int countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationDao.countUnread(userId);
    }
}