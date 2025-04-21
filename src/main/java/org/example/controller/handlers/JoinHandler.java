package org.example.controller.handlers;

import org.example.controller.Controller;
import org.example.Networking.TCPSender;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;

public class JoinHandler implements CommandHandler {
    private final Controller ctrl;
    public JoinHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket socket) throws IOException {
        // parts = ["<JOIN>", "<dstorePort>"]
        int dport = Integer.parseInt(parts[1]);
        // Open persistent sender back to the Dstore's listening port:
        TCPSender sender = new TCPSender("localhost", dport);
        ctrl.addDstore(dport, sender);
        // Map the incoming socket → the dstore port for later ACKs:
        ctrl.mapSocketToPort(socket, dport);
    }
}
