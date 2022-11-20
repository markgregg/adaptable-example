package com.demo.app.controlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@ClientEndpoint
@Component
public class BinaryClient {
    private Session session;
    private ByteBuffer byteBuffer;
    private Consumer<ByteBuffer> messageHandler;

    public void connect() throws URISyntaxException, DeploymentException, IOException {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        webSocketContainer.connectToServer(this, new URI("ws://localhost:9085/bin"));
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
    public void onMessage(ByteBuffer message, boolean islast, Session session) {
        if ( byteBuffer == null ) {
            if( islast ) {
                if (messageHandler != null) {
                    messageHandler.accept(message);
                }
                return;
            }
            byteBuffer = message;
        }
        ByteBuffer newBuffer = ByteBuffer.allocate(byteBuffer.capacity() + message.capacity());
        newBuffer.put(byteBuffer);
        newBuffer.put(message);
        newBuffer.rewind();
        byteBuffer = newBuffer;
        if( islast ) {
            if (messageHandler != null) {
                messageHandler.accept(byteBuffer);
            }
            byteBuffer = null;
        }
    }

    public void addMessageHandler(Consumer<ByteBuffer> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public <T> void sendMessage(ByteBuffer message) throws JsonProcessingException {
        session.getAsyncRemote().sendBinary(message);
    }
}
