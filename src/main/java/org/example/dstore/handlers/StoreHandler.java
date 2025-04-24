package org.example.dstore.handlers;

import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.handlers.CommandHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class StoreHandler implements CommandHandler {
    private final String fileFolder;
    private final TCPSender controllerSender;

    public StoreHandler(String fileFolder, TCPSender controllerSender) {
        this.fileFolder        = fileFolder;
        this.controllerSender  = controllerSender;
    }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
    // parts = ["STORE", "filename filesize"]

      String[] args = parts[1].split(" ");
      String filename = args[0];
      int filesize = Integer.parseInt(args[1]);

      // 1) ACK to client
      new TCPSender(clientSocket).sendOneWay(Protocol.ACK_TOKEN);

      // 2) Read file content
      InputStream in = clientSocket.getInputStream();
      byte[] data = in.readNBytes(filesize);

      // 3) Save to disk
      File outFile = new File(fileFolder, filename);
      try (FileOutputStream fos = new FileOutputStream(outFile)) {
        fos.write(data);
      }
      System.out.println("Stored: " + filename);

      // 4) Notify Controller
      controllerSender.sendOneWay(Protocol.STORE_ACK_TOKEN + " " + filename);


    }
}
