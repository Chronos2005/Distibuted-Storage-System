package org.example;

import org.example.Index.FileInfo;
import org.example.Index.Index;
import org.example.Networking.TCPReceiver;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Controller {

    private int cport;
    private int replicationFactor;
    private int timeout;
    private int rebalancePeriod;
    private TCPSender sender;
    private TCPReceiver receiver;
    private Map<Integer, TCPSender> dstoreSenders;
    private Index index;
    private Protocol protocol;
    private ConcurrentHashMap<Integer, Integer>  dStoreFileCount;


    /**
     * Creates a controller object
     * @param cport The port the controller in listening on
     * @param R The replication Factor (The number of time each file is replicated).
     * @param timeout The time to wait while waiting for an ACK in milliseconds
     * @param rebalancePeriod The time in seconds till the next rebalance operation
     */
    public Controller(int cport, int R, int timeout, int rebalancePeriod) throws IOException {
        this.cport = cport;
        this.replicationFactor = R;
        this.timeout = timeout;
        this.rebalancePeriod = rebalancePeriod;
        this.receiver = new TCPReceiver(cport, this::handleMessage);
        dstoreSenders= new ConcurrentHashMap<>();
        index = new Index();
    }


    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: java Controller <cport> <R> <timeout> <rebalance_period>");
            return;
        }

        int cport = Integer.parseInt(args[0]);
        int R = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int rebalancePeriod = Integer.parseInt(args[3]);

        Controller controller = new Controller(cport, R, timeout, rebalancePeriod);
        controller.start();

    }

    /**
     * Starts  the reciever and does any necessary initialisation so the controller works properly
     * @throws IOException
     */
    public void start() throws IOException {
        receiver.start();

    }

    /**
     * Handles any Incoming messages
     * @param message The message recieved
     * @param socket
     * @throws IOException
     */
    public void handleMessage(String message , Socket socket) throws IOException {
        String[] parts = message.split(" ");
        String command = parts[0];
        switch (command) {
            case "STORE": handleStore(message,socket); break;
            case "LOAD": handleLoad(); break;
            case "REMOVE": handleRemove(); break;
            case "LIST": handleList(); break;
            case "RELOAD": handleReload(); break;
            case "JOIN": handleJoin(message , socket); break;
            case "STORE_ACK": handleStoreAck(); break;
            case "REMOVE_ACK": handleRemoveAck(); break;
            case "ERROR_FILE_DOES_NOT_EXIST": handleRemoveAck(); break; // same effect
            case "REBALANCE_COMPLETE": handleRebalanceComplete(); break;
            default: System.err.println("Unknown command: " + command);
        }
    }

    private void handleStore(String message , Socket socket) throws IOException {
        String[] parts = message.split(" ");
        String fileName = parts[1];
        int port = Integer.parseInt(parts[2]);
        ArrayList<Integer> dstorePorts = new ArrayList<>();
        FileInfo fileInfo = new FileInfo(Index.FileState.STORE_IN_PROGRESS, port,dstorePorts);
        index.addFileInfo(fileName, fileInfo);
        List<Integer> portList = selectLeastLoadedDstores(replicationFactor);
        StringBuilder messageBuilder = new StringBuilder(Protocol.STORE_TO_TOKEN);
        for (int portNumbers : portList) {
            messageBuilder.append(" ").append(portNumbers);
        }

        String clientResponse = messageBuilder.toString();
        TCPSender clientSender = new TCPSender(socket);
        clientSender.sendMessage(clientResponse);





    }

    private void handleLoad() {

    }

    private void handleRemove() {

    }
    private void handleList(String... args) {

    }
    private void handleReload() {

    }

    private void handleStoreAck() {

    }

    private void handleRemoveAck() {

    }
    private void handleRebalanceComplete() {

    }

    private void handleJoin(String message,Socket socket) throws IOException {
        String[] parts = message.split(" ");
        int port = Integer.parseInt(parts[1]);
        TCPSender sender = new TCPSender(socket);
        dstoreSenders.put(port,sender);
        System.out.println("Added Socket to system: " + port);


    }

    private List<Integer> selectLeastLoadedDstores(int R) {
        Map<Integer,Integer> counts = index.getFileCountPerDstore();

        List<Integer> ports = new ArrayList<>(dstoreSenders.keySet());
        if (ports.size() < R) {
            throw new IllegalStateException("Not enough Dstores: have "
                    + ports.size() + ", need " + R);
        }

        ports.sort(Comparator.comparingInt(p -> counts.getOrDefault(p, 0)));

        return ports.subList(0, R);
    }








}
