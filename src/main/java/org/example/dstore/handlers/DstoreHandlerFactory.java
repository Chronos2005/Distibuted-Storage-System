package org.example.dstore.handlers;

import org.example.Networking.TCPSender;
import org.example.dstore.DStore;
import org.example.handlers.CommandHandler;
import org.example.Protocol.Protocol;

import java.util.HashMap;
import java.util.Map;

public class DstoreHandlerFactory {
    private final Map<String, CommandHandler> handlers = new HashMap<>();



    public DstoreHandlerFactory(String fileFolder, TCPSender controllerSender ) {
        handlers.put(Protocol.STORE_TOKEN,      new StoreHandler(fileFolder, controllerSender));
        handlers.put(Protocol.LOAD_DATA_TOKEN,  new LoadDataHandler(fileFolder));
        handlers.put(Protocol.REMOVE_TOKEN,     new RemoveHandler(fileFolder, controllerSender));
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