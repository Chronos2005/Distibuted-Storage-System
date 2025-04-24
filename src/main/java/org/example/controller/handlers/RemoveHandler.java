package org.example.controller.handlers;

import org.example.controller.Controller;
import org.example.Index.FileInfo;
import org.example.Index.Index;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class RemoveHandler implements CommandHandler {
    private final Controller ctrl;
    public RemoveHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["<REMOVE>", "filename"]
        String filename = parts[1];
        String[] args = parts[1].split(" ");
        if (args.length != 1) {
            return;
        }

        // Check if enough Dstores are available
        if (ctrl.getDstoreSenders().size() < ctrl.getReplicationFactor()) {
            new TCPSender(clientSocket).sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        }
        FileInfo info = ctrl.getIndex().getFileInfo(filename);
        TCPSender client = new TCPSender(clientSocket);

        if (info == null || info.getFileState() != Index.FileState.STORE_COMPLETE) {
            client.sendOneWay(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            return;
        }

        info.setFileState(Index.FileState.REMOVE_IN_PROGRESS);
        ctrl.trackPendingRemove(filename, client);

        List<Integer> dsts = info.getdStorePorts();
        for (int p : dsts) {
            TCPSender ds = ctrl.getDstoreSenders().get(p);
            if (ds != null) {
                ds.sendOneWay(Protocol.REMOVE_TOKEN + " " + filename);
            }
        }
    }
}
