package kz.bitlab.websocket.middle01socket.handler;

import kz.bitlab.websocket.middle01socket.session.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MyWebSocketHandler extends AbstractWebSocketHandler {

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
        }else{
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

    private void sendMessageToAll(String message){
        for(UserSession userSession : sessions.values()){
            try{
                userSession.getSession().sendMessage(new TextMessage(message));
            }catch (IOException e){
                System.err.println("Error on sending WebSocket: " + e.getMessage());
            }
        }
    }
}
