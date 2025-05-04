import java.io.IOException;
import java.net.Socket;

public class LoadHandler implements CommandHandler {
    private final Controller ctrl;
    public LoadHandler(Controller ctrl) { this.ctrl = ctrl; }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["<LOAD>", "filename"]
        String[] args = parts[1].split(" ");
        if (args.length != 1) {
            return;
        }

        // Check if enough Dstores are available
        if (ctrl.getDstorePortstoSenders().size() < ctrl.getReplicationFactor()) {
            new TCPSender(clientSocket).sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        }

        String filename = parts[1];
        FileInfo info = ctrl.getIndex().getFileInfo(filename);

        TCPSender client = new TCPSender(clientSocket);
        if (info == null || info.getFileState() != Index.FileState.STORE_COMPLETE) {
            client.sendOneWay(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            return;
        }



        int dport = info.getdStorePorts().getFirst();
        String resp = Protocol.LOAD_FROM_TOKEN + " " + dport + " " + info.getFileSize();
        client.sendOneWay(resp);
    }
}
