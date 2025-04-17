package org.example;

import org.example.Networking.TCPReceiver;
import org.example.Networking.TCPSender;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Controller {

    private int cport;
    private int replicationFactor;
    private int timeout;
    private int rebalancePeriod;
    private TCPSender sender;
    private TCPReceiver receiver;


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
        this.receiver = new TCPReceiver(cport);
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
     * Starts the the reciever and does any necessary initialisation so the controller works properly
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
        switch (message) {
            case "STORE": handleStore(); break;
            case "LOAD": handleLoad(); break;
            case "REMOVE": handleRemove(); break;
            case "LIST": handleList(); break;
            case "RELOAD": handleReload(); break;
            case "JOIN": handleJoin(); break;
            case "STORE_ACK": handleStoreAck(); break;
            case "REMOVE_ACK": handleRemoveAck(); break;
            case "ERROR_FILE_DOES_NOT_EXIST": handleRemoveAck(); break; // same effect
            case "REBALANCE_COMPLETE": handleRebalanceComplete(); break;
            default: System.err.println("Unknown command: " + message);
        }
    }

    private void handleStore() {

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

    private void handleJoin() {

    }







}
