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
    private DisconnectListener Listener;
    private int timeout;

    public TCPReceiver(int port, MessageHandler handler , DisconnectListener listener,int timeout) {
        this.port = port;
        this.handler = handler;
        this.Listener = listener;
        this.timeout = timeout;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("TCPReceiver listening on port " + port);

        Thread listener = new Thread(() -> {
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(timeout);
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
                System.out.println("Received: " + line);
                handler.handle(line, socket);
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            try {
                Listener.onDisconnect(socket);
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public void attach(Socket s) {
        new Thread(() -> handleClient(s)).start();
    }

}

