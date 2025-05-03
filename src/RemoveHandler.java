import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class RemoveHandler implements CommandHandler {
    private final Controller ctrl;
    public RemoveHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["<REMOVE>", "filename"]
        String filename = parts[1];
        FileInfo info = ctrl.getIndex().getFileInfo(filename);
        TCPSender client = new TCPSender(clientSocket);

        if (info == null || info.getFileState() != Index.FileState.STORE_COMPLETE) {
            client.sendOneWay(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            return;
        }

        info.setFileState(Index.FileState.REMOVE_IN_PROGRESS);
        ctrl.trackPendingRemove(filename, client);

        List<Integer> dsts = info.getdStorePorts();
        for (int p : dsts) {
            TCPSender ds = ctrl.getDstorePortstoSenders().get(p);
            if (ds != null) {
                ds.sendOneWay(Protocol.REMOVE_TOKEN + " " + filename);
            }
        }
    }
}
