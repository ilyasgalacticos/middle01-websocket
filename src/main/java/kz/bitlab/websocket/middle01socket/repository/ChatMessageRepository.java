package kz.bitlab.websocket.middle01socket.repository;

import kz.bitlab.websocket.middle01socket.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
