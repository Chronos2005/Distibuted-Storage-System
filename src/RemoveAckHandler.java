import java.io.IOException;
import java.net.Socket;

public class RemoveAckHandler implements CommandHandler {
    private final Controller ctrl;
    public RemoveAckHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket socket) throws IOException {
        // parts = ["<REMOVE_ACK>", "filename"] (or ERROR_FILE_DOES_NOT_EXIST)
        String filename = parts[1];
        FileInfo info = ctrl.getIndex().getFileInfo(filename);
        if (info == null || info.getFileState() != Index.FileState.REMOVE_IN_PROGRESS) {
            return;
        }

        int count = ctrl.incrementRemoveAck(filename);
        int needed = info.getdStorePorts().size();
        if (count >= needed) {
            ctrl.getIndex().removeFileInfo(filename);
            TCPSender client = ctrl.completeRemove(filename);
            if (client != null) {
                client.sendOneWay(Protocol.REMOVE_COMPLETE_TOKEN);
            }
        }
    }
}
