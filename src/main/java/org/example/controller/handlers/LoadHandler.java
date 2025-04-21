package org.example.controller.handlers;

import org.example.controller.Controller;
import org.example.Index.FileInfo;
import org.example.Index.Index;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;

public class LoadHandler implements CommandHandler {
    private final Controller ctrl;
    public LoadHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["<LOAD>", "filename"]
        String filename = parts[1];
        FileInfo info = ctrl.getIndex().getFileInfo(filename);

        TCPSender client = new TCPSender(clientSocket);
        if (info == null || info.getFileState() != Index.FileState.STORE_COMPLETE) {
            client.sendOneWay(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            return;
        }

        int dport = info.getdStorePorts().get(0);
        String resp = Protocol.LOAD_FROM_TOKEN + " " + dport + " " + info.getFileSize();
        client.sendOneWay(resp);
    }
}
