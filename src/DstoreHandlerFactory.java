import java.util.HashMap;
import java.util.Map;

public class DstoreHandlerFactory {
    private final Map<String, CommandHandler> handlers = new HashMap<>();



    public DstoreHandlerFactory(String fileFolder, TCPSender controllerSender , int timeout ) {
        handlers.put(Protocol.STORE_TOKEN,      new DStoreStoreHandler(fileFolder, controllerSender, timeout ));
        handlers.put(Protocol.LOAD_DATA_TOKEN,  new DStoreLoadDataHandler(fileFolder ));
        handlers.put(Protocol.REMOVE_TOKEN,     new DStoreRemoveHandler(fileFolder, controllerSender));
        // If you add rebalance later, wire it here:
        // handlers.put(Protocol.REBALANCE_TOKEN,        new RebalanceHandler(...));
        // handlers.put(Protocol.REBALANCE_STORE_TOKEN,  new RebalanceStoreHandler(...));
        // handlers.put(Protocol.REBALANCE_COMPLETE_TOKEN,new RebalanceCompleteHandler(...));
    }

    /** Return null if no handler for that command */
    public CommandHandler get(String command) {
        return handlers.get(command);
    }
}