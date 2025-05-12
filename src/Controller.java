

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Controller {
    private final int                 replicationFactor;
    private final TCPReceiver         receiver;
    private final Index index = new Index();
    private final Map<Integer,TCPSender> dstorePortstoSenders = new ConcurrentHashMap<>();
    private final Map<Socket,Integer>      socketToDstorePort= new ConcurrentHashMap<>();
    private final Map<String,TCPSender>    pendingClients    = new ConcurrentHashMap<>();
    private final Map<String,Integer>      pendingAcks       = new ConcurrentHashMap<>();
    private final Map<String,TCPSender>    pendingRemoveClients = new ConcurrentHashMap<>();
    private final Map<String,Integer>      pendingRemoveAcks    = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch>    pendingLatches = new ConcurrentHashMap<>();
    private final AtomicInteger rrCounter = new AtomicInteger();

    private final ControllerHandlerFactory factory;
    private final int timeout;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // map from clientSocket+filename → list of candidate Dstore ports
    private final Map<String,List<Integer>> loadCandidates = new ConcurrentHashMap<>();

    public Controller(int cport, int R, int timeout, int rebalancePeriod) throws IOException {
        this.replicationFactor = R;
        this.receiver          = new TCPReceiver(cport, this::dispatch);
        this.factory           = new ControllerHandlerFactory(this);
        this.timeout          = timeout;
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
    public Map<Integer,TCPSender> getDstorePortstoSenders() { return dstorePortstoSenders; }
    public Map<Socket,Integer> getSocketToPort() { return socketToDstorePort; }

    public void addDstore(int port, TCPSender sender) {
        dstorePortstoSenders.put(port, sender);
        System.out.println("Dstore added: " + port);
    }
    public void mapSocketToPort(Socket s,int port) {
        socketToDstorePort.put(s, port);
    }

    public synchronized ArrayList<Integer> selectLeastLoadedDstores() {
        var counts = index.getFileCountPerDstore();
        var ports  = new ArrayList<>(dstorePortstoSenders.keySet());
        // 1st key = current load, 2nd key = rotated index → fair tie-break
        int base = rrCounter.getAndIncrement();
        ports.sort(Comparator
                .comparingInt((Integer p) -> counts.getOrDefault(p, 0))
                .thenComparingInt(p -> (p + base) % ports.size()));
        if (ports.size() < replicationFactor)
            throw new IllegalStateException("Not enough Dstores");
        return new ArrayList<>(ports.subList(0, replicationFactor));
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

    public void scheduleStoreTimeout(String filename, TCPSender clientSender) throws InterruptedException {
       CountDownLatch latch = new CountDownLatch(replicationFactor);

        pendingLatches.put(filename, latch);
        pendingClients.put(filename, clientSender);

        scheduler.execute(() -> {
            try {
                boolean success = latch.await(timeout, TimeUnit.MILLISECONDS);
                if (success)  onStoreSuccess(filename);
                else          onStoreTimeout(filename);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });



    }


    public void trackLoadRequest(String filename, Socket client, List<Integer> ports) {
        loadCandidates.put(client.getRemoteSocketAddress() + "|" + filename, new ArrayList<>(ports));
    }
    public int nextLoadPort(String filename, Socket client) {
        String key = client.getRemoteSocketAddress() + "|" + filename;
        List<Integer> ports = loadCandidates.getOrDefault(key, List.of());
        if (ports.isEmpty()) return -1;
        // rotate list: drop the one just tried
        int tried = ports.remove(0);
        System.out.println("The remaining ports are: " + ports);
        if (ports.isEmpty()) return -1;
        return ports.get(0);
    }
    public void clearLoadRequest(String filename, Socket client) {
        loadCandidates.remove(client.getRemoteSocketAddress() + "|" + filename);
    }


    public void handleRebalance() {
        System.out.println("Starting rebalance...");

        Map<Integer, List<String>> dstoreFiles = new HashMap<>();

        // 1. Ask all Dstores for LIST
        for (Map.Entry<Integer, TCPSender> entry : dstorePortstoSenders.entrySet()) {
            int dstorePort = entry.getKey();
            TCPSender sender = entry.getValue();

            sender.sendOneWay(Protocol.LIST_TOKEN);

        }

    }

    public void onStoreSuccess(String filename) {
        pendingLatches.remove(filename);
        TCPSender client = pendingClients.remove(filename);

        synchronized (index) {
            index.getFileInfo(filename).setFileState(Index.FileState.STORE_COMPLETE);
        }
        if (client != null) client.sendOneWay(Protocol.STORE_COMPLETE_TOKEN);
        System.out.println("→ STORE_COMPLETE for " + filename);
    }

    public void onStoreTimeout(String filename) {
        CountDownLatch latch = pendingLatches.remove(filename);
        if (latch != null && latch.getCount() > 0) {
            System.err.println("⚠ STORE failed due to timeout for file: " + filename);
            index.removeFileInfo(filename);
            TCPSender client = pendingClients.remove(filename);

        }
    }


    public CountDownLatch getPendingLatches(String filename) {
        return pendingLatches.get(filename);

    }


}
