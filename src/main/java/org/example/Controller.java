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

    // Track pending client requests waiting for STORE_COMPLETE
    private final Map<String, TCPSender> pendingClients = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingAcks = new ConcurrentHashMap<>();


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
        System.out.println("Message received: " + message);
        switch (command) {
            case "STORE": handleStore(parts,socket); break;
            case "LOAD": handleLoad(); break;
            case "REMOVE": handleRemove(); break;
            case "LIST": handleList(socket); break;
            case "RELOAD": handleReload(); break;
            case "JOIN": handleJoin(message , socket); break;
            case "STORE_ACK": handleStoreAck(message); break;
            case "REMOVE_ACK": handleRemoveAck(); break;
            case "ERROR_FILE_DOES_NOT_EXIST": handleRemoveAck(); break; // same effect
            case "REBALANCE_COMPLETE": handleRebalanceComplete(); break;
            default: System.err.println("Unknown command: " + command);
        }
    }

    private void handleStore(String[] parts, Socket clientSocket) throws IOException {
        String filename = parts[1];
        int fileSize = Integer.parseInt(parts[2]);

        // Select R least-loaded dstores
        List<Integer> selected = selectLeastLoadedDstores(replicationFactor);

        // Update index: store in-progress

        FileInfo info = new FileInfo(Index.FileState.STORE_IN_PROGRESS, fileSize, new ArrayList<>(selected));
        index.setFileInfo(filename, info);

        // Send STORE_TO to client
        TCPSender clientSender = new TCPSender(clientSocket);
        StringBuilder resp = new StringBuilder(Protocol.STORE_TO_TOKEN);
        for (int p : selected) resp.append(" ").append(p);
        clientSender.sendOneWay(resp.toString());

        // Track pending client and ack count
        pendingClients.put(filename, clientSender);
        pendingAcks.put(filename, 0);
    }

    private void handleStoreAck(String message) {
        // 1) Parse the filename from the incoming message
        //    message looks like "STORE_ACK filename"
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            System.err.println("Malformed STORE_ACK: " + message);
            return;
        }
        String filename = parts[1];

        // 2) Look up the FileInfo; guard against it being null
        FileInfo info = index.getFileInfo(filename);
        if (info == null) {
            System.err.println("STORE_ACK for unknown file: " + filename);
            return;
        }

        // 3) Increment and check ack‐count
        int count = pendingAcks.merge(filename, 1, Integer::sum);
        if (count >= replicationFactor) {
            // a) Mark store complete
            info.setFileState(Index.FileState.STORE_COMPLETE);

            // b) Reply to the waiting client
            TCPSender clientSender = pendingClients.remove(filename);
            pendingAcks.remove(filename);
            if (clientSender != null) {
                clientSender.sendOneWay(Protocol.STORE_COMPLETE_TOKEN);
                System.out.println("Sent STORE_COMPLETE for " + filename);
            } else {
                System.err.println("No pending client for " + filename);
            }
        } else {
            System.out.println("Received STORE_ACK " + filename + " (" + count + "/" + replicationFactor + ")");
        }
    }

    private void handleLoad() {

    }

    private void handleRemove() {

    }
    private void handleList(Socket socket) throws IOException {
        // Wrap the client socket so we can use sendOneWay(...)
        TCPSender clientSender = new TCPSender(socket);

        // 1) Not enough Dstores?
        if (dstoreSenders.size() < replicationFactor) {
            clientSender.sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        }

        // 2) Build the LIST response
        StringBuilder resp = new StringBuilder(Protocol.LIST_TOKEN);
        // You need a helper in your Index to retrieve all filenames:
        //   public Set<String> getAllFileNames() { return files.keySet(); }
        for (String filename : index.getAllFileNames()) {
            FileInfo info = index.getFileInfo(filename);
            if (info.getFileState() == Index.FileState.STORE_COMPLETE) {
                resp.append(" ").append(filename);
            }
        }

        // 3) Send it back to the client
        clientSender.sendOneWay(resp.toString());



    }
    private void handleReload() {

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
