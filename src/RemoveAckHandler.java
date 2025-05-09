import java.io.IOException;
import java.net.Socket;

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

            // 2) increment and test the ACK count atomically
            int count = ctrl.incrementRemoveAck(filename);
            int needed = info.getdStorePorts().size();
            if (count < needed) {
                return;
            }

            // 3) only when we have enough ACKs do we remove the entry
            ctrl.getIndex().removeFileInfo(filename);
        }

        // 4) notify client outside the lock
        TCPSender client = ctrl.completeRemove(filename);
        if (client != null) {
            client.sendOneWay(Protocol.REMOVE_COMPLETE_TOKEN);
        }
    }
}
