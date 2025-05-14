import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class RemoveAckHandler implements CommandHandler {
    private final Controller ctrl;
    public RemoveAckHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket socket) throws IOException {
        String filename = parts[1];

        synchronized(ctrl.getIndex()) {
            // 1) re‑read under lock to see a consistent state
            FileInfo info = ctrl.getIndex().getFileInfo(filename);
            if (info == null || info.getFileState() != Index.FileState.REMOVE_IN_PROGRESS) {
                return;
            }
        }
        CountDownLatch latch = ctrl.getPendingRemoveLatches(filename);
        if (latch != null) {
            latch.countDown();
            long remaining = latch.getCount();
            System.out.printf("✔ Remove_ACK %s (remaining=%d)%n",
                    filename, remaining);

            if (remaining == 0) {
                ctrl.onRemoveSuccess(filename);
            }
        }


    }
}
