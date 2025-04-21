package org.example;

import org.example.Networking.TCPReceiver;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class DStore {

    private int port;
    private int controllerPort;
    private int timeout;
    private String fileFolder;
    private TCPReceiver receiver;
    private TCPSender controllerSender;

    /**
     *
     * @param port The port number the DStore is listening on
     * @param controllerPort The controllers port number
     * @param timeout the time to wait when waiting for a response
     * @param fileFolder A Path to the folder a where the files will be stored
     */
    public DStore(int port, int controllerPort, int timeout, String fileFolder) throws IOException {
        this.port = port;
        this.controllerPort = controllerPort;
        this.timeout = timeout;
        this.fileFolder = fileFolder;
        this.receiver = new TCPReceiver(port,this::handleMessage);  // Listening on DStore's port
        this.controllerSender = new TCPSender("localhost", controllerPort);  // Communicating with Controller
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java DStore <port> <controllerPort> <timeout> <fileFolder>");

        }

        try {
            int port = Integer.parseInt(args[0]);
            int controllerPort = Integer.parseInt(args[1]);
            int timeout = Integer.parseInt(args[2]);
            String fileFolder = args[3];

            DStore dstore = new DStore(port, controllerPort, timeout, fileFolder);
            System.out.println("DStore started with: port=" + port +
                    ", controllerPort=" + controllerPort +
                    ", timeout=" + timeout +
                    ", fileFolder=" + fileFolder);


            dstore.receiver.start();
            dstore.Join();


        } catch (NumberFormatException | IOException e) {
            System.out.println(e);
        }
    }

    /**
     * The JOIN operation so the DStore can join the storage system.
     */
    public void Join() throws IOException {
        controllerSender.sendOneWay("JOIN " + port);
    }

    public void handleMessage(String message , Socket socket) throws IOException {
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case Protocol.STORE_TOKEN:
                handleStore(parts, socket);
                break;

            case Protocol.REMOVE_TOKEN:

                break;


            case Protocol.REBALANCE_TOKEN:

                break;

            case Protocol.LOAD_DATA_TOKEN:
                handleLoad(parts, socket);
                break;

            default:
                System.err.println("Dstore received unknown command: " + command);
        }

    }

    private void handleStore(String[] parts, Socket clientSocket) throws IOException {
        String filename = parts[1];
        int fileSize = Integer.parseInt(parts[2]);

        // 1. Send ACK using TCPSender
        TCPSender clientSender = new TCPSender(clientSocket);
        clientSender.sendOneWay(Protocol.ACK_TOKEN);

        // 2. Read file content
        InputStream in = clientSocket.getInputStream();
        byte[] data = in.readNBytes(fileSize);

        // 3. Save to disk
        try (FileOutputStream fos = new FileOutputStream(fileFolder + File.separator + filename)) {
            fos.write(data);
        }

        // 4. Notify Controller via persistent TCPSender
        controllerSender.sendOneWay(Protocol.STORE_ACK_TOKEN + " " + filename);
    }

    private void handleLoad(String[] message , Socket socket) throws IOException {
        TCPSender clientSender = new TCPSender(socket);
        clientSender.sendFile(fileFolder,message[1]);
    }







}
