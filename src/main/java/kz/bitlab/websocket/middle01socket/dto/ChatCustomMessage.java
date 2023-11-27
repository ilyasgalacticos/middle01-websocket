package kz.bitlab.websocket.middle01socket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatCustomMessage {

    private String content;
    private String receiver;

}