package org.example.handlers;

import org.example.controller.Controller;

import java.io.IOException;
import java.net.Socket;

public interface CommandHandler {
    void handle(String[] command , Socket socket ) throws IOException;
}
