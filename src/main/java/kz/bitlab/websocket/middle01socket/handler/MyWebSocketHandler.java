package kz.bitlab.websocket.middle01socket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.bitlab.websocket.middle01socket.dto.CustomMessage;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

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
            sendMessageToAll("User " + userName + " joined!");
        } else {
            session.sendMessage(new TextMessage("Error : Username is required!"));
            session.close(new CloseStatus(4001, "Username is required"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String request = sessions.get(session.getId()).getUsername() + " : " + message.getPayload();
        log.info("Server received : {}", request);
        sendMessageToAll(request);

        //String response = "Response from server to POSTMAN (" + request + ")";
        //log.info("Server sends : {}", response);
        //session.sendMessage(new TextMessage(response));
    }

    private void sendMessageToAll(String message) {
        for (UserSession userSession : sessions.values()) {
            try {
                userSession.getSession().sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println("Error on sending WebSocket: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        String payload = message.getPayload().toString();
        CustomMessage parsedMessage = parseMessage(payload);
        if (parsedMessage != null) {
            switch (parsedMessage.getType()) {
                case "CHAT" -> handleChatMessage(session, parsedMessage);
                case "COMMAND" -> handleCommand(session, parsedMessage);
            }
        }


    }

    private CustomMessage parseMessage(String payload) {
        try {
            return objectMapper.readValue(payload, CustomMessage.class);
        } catch (JsonProcessingException e) {
            log.info("Error on parsing message: {}", e.getMessage());
            return null;
        }
    }

    private void handleChatMessage(WebSocketSession session, CustomMessage message) {
        String username = sessions.get(session.getId()).getUsername();
        sendMessageToAll(username + " : " + message.getContent());
    }

    private void handleCommand(WebSocketSession session, CustomMessage message) {
        switch (message.getContent()) {
            case "disconnect" -> {
                try {
                    sessions.remove(session.getId());
                    session.close();
                } catch (IOException e) {
                    System.err.println("Error on closing WebSocket session" + e.getMessage());
                }
            }
            case "listUsers" -> {
                String activeUsers = getActiveUsers();
                sendMessage(session, "Active users: " + activeUsers);
            }
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try{
            session.sendMessage(new TextMessage(message));
        }catch (IOException e){
            System.err.println("Error on sending WebSocket session" + e.getMessage());
        }
    }

    private String getActiveUsers() {
        return sessions.values().stream()
                .map(UserSession::getUsername)
                .collect(Collectors.joining(", "));
    }
}
