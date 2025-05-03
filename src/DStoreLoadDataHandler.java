import java.io.IOException;
import java.net.Socket;

public class DStoreLoadDataHandler implements CommandHandler {
    private final String fileFolder;

    public DStoreLoadDataHandler(String fileFolder) {
        this.fileFolder = fileFolder;
    }

    @Override
    public void handle(String[] parts, Socket clientSocket) throws IOException {
        // parts = ["LOAD_DATA", "filename"]
        String filename = parts[1];
        TCPSender sender = new TCPSender(clientSocket);
        if (!sender.sendFile(fileFolder, filename)) {
            System.err.println("Failed to send file: " + filename);
        }
    }
}
