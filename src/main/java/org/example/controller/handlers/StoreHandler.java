package org.example.controller.handlers;


import org.example.Index.FileInfo;
import org.example.Index.Index;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.controller.Controller;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class StoreHandler implements CommandHandler {
    private final Controller ctrl;
    public StoreHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["<STORE>", "filename filesize"]
        String[] args = parts[1].split(" ");
        String filename = args[0];
        int fileSize    = Integer.parseInt(args[1]);

        // pick least-loaded dstores
        ArrayList<Integer> dsts = ctrl.selectLeastLoadedDstores();

        // mark in-progress
        ctrl.getIndex().setFileInfo(
                filename,
                new FileInfo(Index.FileState.STORE_IN_PROGRESS, fileSize,dsts )
        );

        // send STORE_TO back to client
        TCPSender client = new TCPSender(clientSocket);
        ctrl.trackPendingStore(filename, client);

        StringBuilder resp = new StringBuilder(Protocol.STORE_TO_TOKEN);
        for (int p : dsts) resp.append(" ").append(p);
        client.sendOneWay(resp.toString());
    }
}
