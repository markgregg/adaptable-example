package com.demo.app.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

@ClientEndpoint
@Component
public class WebClient {
    private Session session;
    private StringBuilder textBuffer;
    private Consumer<String> messageHandler;

    public void connect() throws URISyntaxException, DeploymentException, IOException {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        webSocketContainer.connectToServer(this, new URI("ws://localhost:9085/socket"));
    }

    /***
     *
     */
    public void close() throws IOException {
        session.close();
        session = null;
    }

    @OnOpen
    public void onOpen(Session openingSession) {
        session = openingSession;
    }

    @OnClose
    public void onClose(Session closingSession, CloseReason reason) {
        session = null;
    }

    @OnMessage
    public void onMessage(String message, boolean islast, Session session) {
        if (textBuffer == null) {
            textBuffer = new StringBuilder(message);
        } else {
            textBuffer.append(message);
        }
        if (islast) {
            if( messageHandler != null ) {
                messageHandler.accept(textBuffer.toString());
            }
            textBuffer = null;
        }
    }

    public void addMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public <T> void sendMessage(String message) throws JsonProcessingException {
        session.getAsyncRemote().sendText(message);
    }
}
