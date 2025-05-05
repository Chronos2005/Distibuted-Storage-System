import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StoreHandler implements CommandHandler {
  private final Controller ctrl;

  public StoreHandler(Controller ctrl) {
    this.ctrl = ctrl;
  }

  @Override

  public void handle(String[] parts, Socket clientSocket) throws IOException {
    if (parts.length != 2) {
      System.err.println("Malformed STORE message: " + Arrays.toString(parts));
      return;
    }

    String[] args = parts[1].split(" ", 2);
    if (args.length != 2) {
      System.err.println("Malformed STORE arguments: " + Arrays.toString(args));
      return;
    }

    String filename = args[0];
    int fileSize;

    try {
      fileSize = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.err.println("Invalid file size in STORE: " + args[1]);
      return;
    }

    // Check for duplicate file
    FileInfo existing = ctrl.getIndex().getFileInfo(filename);
    if (existing != null && existing.getFileState() != Index.FileState.REMOVE_IN_PROGRESS) {
      new TCPSender(clientSocket).sendOneWay(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
      return;
    }

    // Check if enough Dstores are available
    if (ctrl.getDstorePortstoSenders().size() < ctrl.getReplicationFactor()) {
      new TCPSender(clientSocket).sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
      return;
    }

    // Select Dstores and mark file as storing
    ArrayList<Integer> dstores = ctrl.selectLeastLoadedDstores();
    ctrl.getIndex().setFileInfo(filename,
            new FileInfo(Index.FileState.STORE_IN_PROGRESS, fileSize, new ArrayList<>(dstores)));
    System.out.println("Selected Dstores for " + filename + ": " + dstores);

    // Track the pending client and prepare response
    TCPSender clientSender = new TCPSender(clientSocket);
    ctrl.trackPendingStore(filename, clientSender);

    StringBuilder resp = new StringBuilder(Protocol.STORE_TO_TOKEN);
    for (int p : dstores) resp.append(" ").append(p);
    clientSender.sendOneWay(resp.toString());
    ctrl.scheduleStoreTimeout(filename);


  }

}
