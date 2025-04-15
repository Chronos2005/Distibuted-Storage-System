package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPReceiver {

    private final int port;

    private volatile boolean running = true;


    /**
     * Creates a TCP Receiver with a specified port number
     * @param port The port number to listen on.
     */
    public TCPReceiver(int port) {
        this.port = port;
    }

    /**
     * Creates a listener which listens on a port.
     */
    public void start() {
        Thread listenerThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("TCPReceiver listening on port " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                    // Handle each connection on its own thread
                    new Thread(() -> handleClient(clientSocket)).start();
                }

            } catch (IOException e) {
                if (running) {
                    System.err.println("TCPReceiver error: " + e.getMessage());
                } else {
                    System.out.println("TCPReceiver stopped.");
                }
            }
        });

        listenerThread.start();
    }


    /**
     * Handles the message recieved.
     * @param socket The socket we are listening on.
     * @return The message
     */
    public String handleClient(Socket socket) {
        String message = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            // Read one message (one line) from the client.
            message = in.readLine();
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("Error handling client " + socket.getInetAddress() + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore exception on close.
            }
        }
        return message;
    }


    /**
     * Stops the listener
     */
    public void stop() {
        running = false;

    }


}
