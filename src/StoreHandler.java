import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

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

    // Check if enough Dstores are available
    if (ctrl.getDstorePortstoSenders().size() < ctrl.getReplicationFactor()) {
      new TCPSender(clientSocket)
              .sendOneWay(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
      return;
    }


    ArrayList<Integer> dstores;
       synchronized (ctrl) {
             if (ctrl.getIndex().getFileInfo(filename) != null) {
                   new TCPSender(clientSocket)
                                 .sendOneWay(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
                    return;
                }
              dstores = ctrl.selectLeastLoadedDstores();
             ctrl.getIndex().setFileInfo(
                             filename,
                              new FileInfo(Index.FileState.STORE_IN_PROGRESS,
                                                  fileSize,
                                                  new CopyOnWriteArrayList<>(dstores)));
          }

    System.out.println("Selected Dstores for " + filename + ": " + dstores);

    // Track the pending client and send STORE_TO response
    TCPSender clientSender = new TCPSender(clientSocket);

    ctrl.initStoreTracking(filename, dstores.size(), clientSender);
    StringBuilder resp = new StringBuilder(Protocol.STORE_TO_TOKEN);
    for (int p : dstores) {
      resp.append(" ").append(p);
    }
    clientSender.sendOneWay(resp.toString());
    ctrl.scheduleStoreTimeout(filename);
  }
}
