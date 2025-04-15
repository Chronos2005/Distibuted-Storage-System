package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPSender {

    private final String host;
    private final int port;


    /**
     *
     * @param host The host
     * @param port The port number
     */
    public TCPSender(String host, int port) {
        this.host = host;
        this.port = port;
    }




    /**
     * Sends a message to the specified host and port.
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(message);
            System.out.println("Message sent: " + message);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }



}
