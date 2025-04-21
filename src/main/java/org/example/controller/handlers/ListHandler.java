package org.example.controller.handlers;


import org.example.Index.FileInfo;
import org.example.Index.Index;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.controller.Controller;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;

public class ListHandler implements CommandHandler {
    private final Controller ctrl;
    public ListHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        TCPSender client = new TCPSender(clientSocket);
        if (ctrl.getDstoreSenders().size() < ctrl.getReplicationFactor()) {
            client.sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        }

        StringBuilder sb = new StringBuilder(Protocol.LIST_TOKEN);
        for (String fn : ctrl.getIndex().getAllFileNames()) {
            FileInfo fi = ctrl.getIndex().getFileInfo(fn);
            if (fi.getFileState() == Index.FileState.STORE_COMPLETE) {
                sb.append(" ").append(fn);
            }
        }
        client.sendOneWay(sb.toString());
    }
}

