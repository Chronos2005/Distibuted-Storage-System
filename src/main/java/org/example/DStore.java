package org.example;

import org.example.Networking.TCPReceiver;
import org.example.Networking.TCPSender;

import java.io.IOException;
import java.net.Socket;

public class DStore {

    private int port;
    private int controllerPort;
    private int timeout;
    private String fileFolder;
    private TCPReceiver receiver;
    private TCPSender sender;

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
        this.sender = new TCPSender("localhost", controllerPort);  // Communicating with Controller
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
        sender.sendOneWay("JOIN " + port);
    }

    public void handleMessage(String message , Socket socket) throws IOException {
    }






}
