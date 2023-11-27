package kz.bitlab.websocket.middle01socket.service;

import kz.bitlab.websocket.middle01socket.model.ChatMessage;
import kz.bitlab.websocket.middle01socket.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public void saveMessage(String sender, String receiver, String content){
        log.info("Trying to save message from {} to {}", sender, receiver);
        ChatMessage message = new ChatMessage();
        message.setReceiver(receiver);
        message.setSender(sender);
        message.setContent(content);
        message.setSentTime(LocalDateTime.now());
        chatMessageRepository.save(message);
    }
}
