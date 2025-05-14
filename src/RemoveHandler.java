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

        // Ensure correct args
        String[] args = parts[1].split(" ");
        if (args.length != 1) return;

        // Check enough Dstores
        if (ctrl.getDstorePortstoSenders().size() < ctrl.getReplicationFactor()) {
            new TCPSender(clientSocket)
                    .sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        }

        // Atomically check file exists and mark remove in progress
        synchronized (ctrl.getIndex()) {
            FileInfo info = ctrl.getIndex().getFileInfo(filename);
            if (info == null || info.getFileState() != Index.FileState.STORE_COMPLETE) {
                new TCPSender(clientSocket)
                        .sendOneWay(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
                return;
            }
            info.setFileState(Index.FileState.REMOVE_IN_PROGRESS);
        }

        // Track pending acks and send REMOVE to dstores
        TCPSender client = new TCPSender(clientSocket);


        List<Integer> dsts;
        synchronized (ctrl.getIndex()) {
            dsts = List.copyOf(ctrl.getIndex().getFileInfo(filename).getdStorePorts());
        }
        System.out.println("Removing from: " + dsts);
        for (int p : dsts) {
            TCPSender ds = ctrl.getDstorePortstoSenders().get(p);
            if (ds != null) {
                ds.sendOneWay(Protocol.REMOVE_TOKEN + " " + filename);
            }
        }

        ctrl.scheduleRemoveTimeout(filename, client);
    }

}
