package socket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/comments")
public class CommentSocket {

    // all connected clients
    private static final Set<Session> clients =
            ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        System.out.println("Client connected: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        System.out.println("Client disconnected: " + session.getId());
    }

    // 🔥 broadcast to everyone
    public static void broadcast(String msg) {
        clients.forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(msg);
            }
        });
    }
}
