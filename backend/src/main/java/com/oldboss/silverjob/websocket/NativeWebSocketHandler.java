package com.oldboss.silverjob.websocket;

import com.oldboss.silverjob.service.AuthService;
import com.oldboss.silverjob.model.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NativeWebSocketHandler extends TextWebSocketHandler implements HandshakeInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userIdSessions = new ConcurrentHashMap<>();

    public NativeWebSocketHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler handler, Map<String, Object> attributes) throws Exception {
        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = (query != null && query.startsWith("token=")) ? query.substring(6) : null;

        if (token == null) {
            log.warn("WebSocket: no token in query");
            return false;
        }

        try {
            CurrentUser user = authService.getCurrentUserByToken(token);
            if (user == null) {
                log.warn("WebSocket: invalid token");
                return false;
            }
            attributes.put("userId", user.getUserId());
            log.info("WebSocket handshake: userId={}", user.getUserId());
            return true;
        } catch (Exception e) {
            log.error("WebSocket auth error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                  WebSocketHandler handler, Exception exception) {
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId == null) {
            log.warn("WebSocket: no userId in session, closing");
            session.close();
            return;
        }

        String sessionId = session.getId();
        userSessions.put(sessionId, session);
        userIdSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("WebSocket connected: userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = ((TextMessage) message).getPayload();
        String sessionId = session.getId();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            if ("subscribe".equals(type)) {
                String destination = (String) data.get("destination");
                sessionSubscriptions.put(sessionId, destination);
                log.info("WebSocket subscribed: sessionId={}, destination={}", sessionId, destination);
            }
        } catch (Exception e) {
            log.error("WebSocket message error: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        userSessions.remove(sessionId);
        sessionSubscriptions.remove(sessionId);
        
        for (Map.Entry<Long, Set<String>> entry : userIdSessions.entrySet()) {
            entry.getValue().remove(sessionId);
            if (entry.getValue().isEmpty()) {
                userIdSessions.remove(entry.getKey());
            }
        }
        
        log.info("WebSocket closed: sessionId={}, status={}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) throws Exception {
        log.error("WebSocket error: sessionId={}, error={}", session.getId(), error.getMessage());
    }

    public void sendMessageToUser(Long userId, String destination, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("sendMessageToUser: userId={}, destination={}", userId, destination);
            
            Set<String> sessionIds = userIdSessions.get(userId);
            if (sessionIds == null || sessionIds.isEmpty()) {
                log.info("No active sessions for userId={}", userId);
                return;
            }
            
            for (String sid : sessionIds) {
                WebSocketSession session = userSessions.get(sid);
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonPayload));
                    log.info("Sent message to session {}", sid);
                }
            }
        } catch (Exception e) {
            log.error("Push error: {}", e.getMessage());
        }
    }
}