package kz.bitlab.websocket.middle01socket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.bitlab.websocket.middle01socket.dto.ChatCustomMessage;
import kz.bitlab.websocket.middle01socket.service.ChatMessageService;
import kz.bitlab.websocket.middle01socket.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends AbstractWebSocketHandler {

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userNameToSessionIdMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userName = null;
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1 && "username".equals(keyValue[0])) {
                    userName = keyValue[1];
                    break;
                }
            }
        }

        if (userName != null) {
            sessions.put(session.getId(), new UserSession(session, userName));
            userNameToSessionIdMap.put(userName, session.getId());
        } else {
            session.sendMessage(new TextMessage("Error: User name is required"));
            session.close(new CloseStatus(4001, "User name is required"));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        ChatCustomMessage parsedMessage = parseMessage(payload);
        if (parsedMessage != null) {
            handleChatMessage(session, parsedMessage);
        }
    }

    private ChatCustomMessage parseMessage(String payload) {
        try {
            return objectMapper.readValue(payload, ChatCustomMessage.class);
        } catch (JsonProcessingException e) {
            log.info("Error on parsing JSON message: " + e.getMessage());
        }
        return null;
    }

    private void handleChatMessage(WebSocketSession session, ChatCustomMessage message) {
        String sender = sessions.get(session.getId()).getUsername();
        String receiver = message.getReceiver();
        chatMessageService.saveMessage(sender, receiver, message.getContent());

        String sessionId = userNameToSessionIdMap.get(receiver);
        if (sessionId != null) {
            UserSession userSession = sessions.get(sessionId);
            if (userSession != null) {
                try {
                    userSession.getSession().sendMessage(new TextMessage(message.getContent()));
                } catch (IOException e) {
                    System.err.println("Error on sending WebSocket message: " + e.getMessage());
                }
            }
        }
    }
}
