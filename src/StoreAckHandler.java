import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

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

        CountDownLatch latch = ctrl.getPendingLatches(filename);
        if (latch != null) {
            latch.countDown();  // decrement one replica’s ACK
            System.out.printf("✔ STORE_ACK %s (remaining=%d)%n",
                    filename, latch.getCount());



        }
    }
}
