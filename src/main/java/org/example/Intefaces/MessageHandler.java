package org.example.Intefaces;

import java.net.Socket;

@FunctionalInterface
public interface MessageHandler {
    void handle(String message, Socket socket);
}
