package org.example.Networking;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * TCP client that maintains a persistent connection to the server,
 * supports sending text messages (with or without expecting a response) and files,
 * and detects connection loss.
 */
public class TCPSender {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * @param host The server host
     * @param port The server port
     * @throws IOException if connection fails
     */
    public TCPSender(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    /**
     * Establishes a persistent TCP connection and configures streams.
     */
    private void connect() throws IOException {
        socket = new Socket(host, port);
        // Set a read timeout to detect unresponsive server
        socket.setSoTimeout(5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Close the underlying socket and streams.
     */
    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Sends a text message over the persistent connection,
     * and waits for a response (ACK) from the peer.
     * @param message The message to send
     * @return the peer response, or null if no response or disconnected
     */
    public String sendMessage(String message) {
        return sendMessage(message, true);
    }

    /**
     * Sends a text message over the persistent connection,
     * optionally waiting for a response.
     * @param message The message to send
     * @param expectResponse if true, waits for and returns the response; otherwise returns null immediately
     * @return the peer response if expectResponse=true (or null on error), or null if expectResponse=false
     */
    public String sendMessage(String message, boolean expectResponse) {
        try {
            out.println(message);
            System.out.println("Sent: " + message);
            if (!expectResponse) {
                return null;
            }
            String response = in.readLine();
            if (response == null) {
                throw new IOException("Server closed connection");
            }
            return response;
        } catch (SocketTimeoutException e) {
            System.err.println("Read timed out: server unresponsive");
        } catch (IOException e) {
            System.err.println("Connection lost while sending message: " + e.getMessage());
            close();
        }
        return null;
    }

    /**
     * For fire-and-forget messages that never return any ACK.
     * @param message The message to send
     */
    public void sendOneWay(String message) {
        sendMessage(message, false);
    }

    /**
     * Sends a file over the persistent connection.
     * Format: send a header line "FILE filename filesize" then raw bytes using write().
     * @param filePath Path to the file to send
     * @return true if sent successfully, false on failure or disconnection
     */
    public boolean sendFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream dataOut = socket.getOutputStream()) {

            long fileSize = file.length();
            // Send header
            out.println("FILE " + file.getName() + " " + fileSize);
            System.out.println("Sent header for file: " + file.getName());

            // Send file content using write()
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, read);
            }
            dataOut.flush();
            System.out.println("File transfer complete: " + file.getName());

            // Wait for ACK
            String ack = in.readLine();
            if (ack == null) {
                throw new IOException("Server closed connection after file transfer");
            }
            System.out.println("Received ACK: " + ack);
            return true;

        } catch (SocketTimeoutException e) {
            System.err.println("File send timed out: server unresponsive");
        } catch (IOException e) {
            System.err.println("Connection lost during file send: " + e.getMessage());
            close();
        }
        return false;
    }



}
