package org.example.dstore.handlers;

import org.example.Networking.TCPSender;
import org.example.handlers.CommandHandler;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class DStoreListHandler implements CommandHandler {
    private TCPSender csender;
    private String folder;
    public DStoreListHandler(String fileFolder , TCPSender controllerSender) {
        this.csender = controllerSender;
        this.folder = fileFolder;

    }
    @Override
    public void handle(String[] command, Socket socket) throws IOException {
        File folderFile = new File(folder);
        File[] files = folderFile.listFiles();

        if (files != null) {
            StringBuilder sb = new StringBuilder("LIST");
            for (File file : files) {
                if (file.isFile()) {
                    sb.append(" ").append(file.getName());
                }
            }
            String listCommand = sb.toString();
            csender.sendOneWay(listCommand);  // <-- Send back to controller
        } else {
            csender.sendOneWay("LIST");  // Send empty LIST if folder invalid
        }
    }

}
