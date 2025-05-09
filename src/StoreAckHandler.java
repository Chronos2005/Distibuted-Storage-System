import java.io.IOException;
import java.net.Socket;

public class StoreAckHandler implements CommandHandler {
    private final Controller ctrl;
    public StoreAckHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket dstSocket) throws IOException {
        // parts = ["<STORE_ACK>", "filename"]
        String filename = parts[1];

        Integer dport = ctrl.getSocketToPort().get(dstSocket);
        if (dport == null) {
            System.err.println("STORE_ACK from unknown socket");
            return;
        }

        // Atomically update ack count and file state under index lock
        synchronized (ctrl.getIndex()) {
            FileInfo info = ctrl.getIndex().getFileInfo(filename);
            if (info == null) {
                System.err.println("STORE_ACK for unknown file: " + filename);
                return;
            }

            int count = ctrl.incrementStoreAck(filename);
            System.out.printf("✔ STORE_ACK %s (%d/%d)%n",
                    filename, count, ctrl.getReplicationFactor());

            if (count >= ctrl.getReplicationFactor()) {
                // complete
                info.setFileState(Index.FileState.STORE_COMPLETE);
                TCPSender client = ctrl.completeStore(filename);
                if (client != null) {
                    client.sendOneWay(Protocol.STORE_COMPLETE_TOKEN);
                    System.out.println("→ " + Protocol.STORE_COMPLETE_TOKEN);
                }
            }
        }
    }
}
