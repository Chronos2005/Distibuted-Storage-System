package org.example.Networking;

import org.example.Intefaces.MessageHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP server that listens for incoming connections,
 * handles both simple text messages and file transfers.
 * For messages: prints them to stdout.
 * For file transfers: expects header "FILE filename filesize",
 * reads exactly filesize bytes via readNBytes(), saves file, and sends back an ACK.
 */
public class TCPReceiver {
    private final int port;
    private final MessageHandler handler;
    private ServerSocket serverSocket;

    public TCPReceiver(int port, MessageHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("TCPReceiver listening on port " + port);

        Thread listener = new Thread(() -> {
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());

                }
            }
        });
        listener.start();
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                handler.handle(line, socket);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

}

