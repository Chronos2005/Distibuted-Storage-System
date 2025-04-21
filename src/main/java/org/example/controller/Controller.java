package org.example.controller;

import org.example.Index.Index;
import org.example.Networking.TCPReceiver;
import org.example.Networking.TCPSender;
import org.example.Protocol.Protocol;
import org.example.controller.handlers.ControllerHandlerFactory;
import org.example.handlers.CommandHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Controller {
    private final int                 replicationFactor;
    private final TCPReceiver         receiver;
    private final Index index = new Index();
    private final Map<Integer,TCPSender>   dstoreSenders    = new ConcurrentHashMap<>();
    private final Map<Socket,Integer>      socketToDstorePort= new ConcurrentHashMap<>();
    private final Map<String,TCPSender>    pendingClients    = new ConcurrentHashMap<>();
    private final Map<String,Integer>      pendingAcks       = new ConcurrentHashMap<>();
    private final Map<String,TCPSender>    pendingRemoveClients = new ConcurrentHashMap<>();
    private final Map<String,Integer>      pendingRemoveAcks    = new ConcurrentHashMap<>();

    private final ControllerHandlerFactory factory;

    public Controller(int cport, int R, int timeout, int rebalancePeriod) throws IOException {
        this.replicationFactor = R;
        this.receiver          = new TCPReceiver(cport, this::dispatch);
        this.factory           = new ControllerHandlerFactory(this);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java ControllerMain <cport> <R> <timeout> <rebalance_period>");
            return;
        }

        int cport           = Integer.parseInt(args[0]);
        int R               = Integer.parseInt(args[1]);
        int timeout         = Integer.parseInt(args[2]);
        int rebalancePeriod = Integer.parseInt(args[3]);

        Controller controller = new Controller(cport, R, timeout, rebalancePeriod);
        controller.start();
    }

    public void start() throws IOException {
        receiver.start();
    }

    private void dispatch(String line, Socket socket) throws IOException {
        String[] parts = line.split(" ", 2);
        CommandHandler h = factory.get(parts[0]);
        if (h != null) h.handle(parts, socket);
        else System.err.println("Unknown: " + parts[0]);
    }

    // ─── helpers for handlers ─────────────────────────────────────────────────────

    public Index getIndex() { return index; }
    public int getReplicationFactor() { return replicationFactor; }
    public Map<Integer,TCPSender> getDstoreSenders() { return dstoreSenders; }
    public Map<Socket,Integer> getSocketToPort() { return socketToDstorePort; }

    public void addDstore(int port, TCPSender sender) {
        dstoreSenders.put(port, sender);
        System.out.println("Dstore added: " + port);
    }
    public void mapSocketToPort(Socket s,int port) {
        socketToDstorePort.put(s, port);
    }

    public ArrayList<Integer> selectLeastLoadedDstores() {
        var counts = index.getFileCountPerDstore();
        var ports = new ArrayList<>(dstoreSenders.keySet());
        ports.sort(Comparator.comparingInt(p -> counts.getOrDefault(p, 0)));
        if (ports.size() < replicationFactor) {
            throw new IllegalStateException("Not enough Dstores");
        }
        return new ArrayList<>(ports.subList(0, replicationFactor));
    }

    public void trackPendingStore(String filename, TCPSender client) {
        pendingClients.put(filename, client);
        pendingAcks.put(filename, 0);
    }
    public int incrementStoreAck(String filename) {
        return pendingAcks.merge(filename,1,Integer::sum);
    }
    public TCPSender completeStore(String filename) {
        pendingAcks.remove(filename);
        return pendingClients.remove(filename);
    }

    public void trackPendingRemove(String filename, TCPSender client) {
        pendingRemoveClients.put(filename, client);
        pendingRemoveAcks.put(filename, 0);
    }
    public int incrementRemoveAck(String filename) {
        return pendingRemoveAcks.merge(filename,1,Integer::sum);
    }
    public TCPSender completeRemove(String filename) {
        pendingRemoveAcks.remove(filename);
        return pendingRemoveClients.remove(filename);
    }
}
