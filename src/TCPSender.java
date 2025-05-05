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
        socket = new Socket(host, port);
        connect();
    }
    public TCPSender( Socket socket) throws IOException {
        this.host= null;
        this.port = -1;
        this.socket = socket;
        connect();
    }

    /**
     * Establishes a persistent TCP connection and configures streams.
     */
    private void connect() throws IOException {


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
   * Streams the raw bytes of the given file over the socket's OutputStream,
   * without sending any header or waiting for an ACK.
   *
   * @param folderPath  full path on disk to the file to send
   * @return          true if the bytes were sent successfully, false on I/O error
   */
  public boolean sendFile(String folderPath, String filename) {
    File file = new File(folderPath, filename);
    if (!file.exists() || !file.isFile()) {
      System.err.println("File not found: " + file.getAbsolutePath());
      return false;
    }

    try (FileInputStream fis = new FileInputStream(file);
        OutputStream out = socket.getOutputStream()) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.flush();
      System.out.println("Sent raw file content: " + filename);
      return true;

    } catch (IOException e) {
      System.err.println("Error sending file '" + filename + "': " + e.getMessage());
      close();
      return false;
    }
  }

    public Socket getSocket() {
        return socket;
    }






}
