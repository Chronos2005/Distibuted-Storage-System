import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class DStoreRemoveHandler implements CommandHandler {
    private final String fileFolder;
    private final TCPSender controllerSender;

    public DStoreRemoveHandler(String fileFolder, TCPSender controllerSender) {
        this.fileFolder       = fileFolder;
        this.controllerSender = controllerSender;
    }

    @Override
    public void handle(String[] parts, Socket socket) throws IOException {
        // parts = ["REMOVE", "filename"]
        String filename = parts[1];
        File f = new File(fileFolder, filename);

        if (f.exists() && f.delete()) {
            System.out.println("Removed: " + filename);
            controllerSender.sendOneWay(Protocol.REMOVE_ACK_TOKEN + " " + filename);
        } else {
            System.out.println("Remove failed/not found: " + filename);
            controllerSender.sendOneWay(
                    Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN + " " + filename);
        }
    }
}
