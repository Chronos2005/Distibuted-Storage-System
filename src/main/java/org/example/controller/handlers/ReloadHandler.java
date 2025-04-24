package org.example.controller.handlers;

import org.example.controller.Controller;
import org.example.Index.FileInfo;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;

public class ReloadHandler implements CommandHandler {
    private final Controller ctrl;
    public ReloadHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["RELOAD","filename"]
        String filename = parts[1];
        int nextPort = ctrl.nextLoadPort(filename, clientSocket);
        if (nextPort < 0) {
            // no more replicas to try
            new TCPSender(clientSocket)
                    .sendOneWay(Protocol.ERROR_LOAD_TOKEN);
            ctrl.clearLoadRequest(filename, clientSocket);
        } else {
            // send LOAD_FROM nextPort filesize
            FileInfo info = ctrl.getIndex().getFileInfo(filename);
            String msg = Protocol.LOAD_FROM_TOKEN + " " + nextPort + " " + info.getFileSize();
            new TCPSender(clientSocket).sendOneWay(msg);
        }
    }
}
