package org.example.Intefaces;

import java.net.Socket;

@FunctionalInterface
public interface Handler {
    void handle(String message, Socket socket);
}
