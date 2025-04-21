package org.example.handlers;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface MessageHandler {
    void handle(String message, Socket socket) throws IOException;
}
