// File: src/main/java/org/example/controller/handlers/ControllerHandlerFactory.java
package org.example.controller.handlers;

import org.example.controller.Controller;
import org.example.Protocol.Protocol;
import org.example.handlers.CommandHandler;

import java.util.HashMap;
import java.util.Map;

public class ControllerHandlerFactory {
    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public ControllerHandlerFactory(Controller ctrl) {
        handlers.put(Protocol.JOIN_TOKEN,             new JoinHandler(ctrl));
        handlers.put(Protocol.STORE_TOKEN,            new StoreHandler(ctrl));
        handlers.put(Protocol.STORE_ACK_TOKEN,        new StoreAckHandler(ctrl));
        handlers.put(Protocol.LOAD_TOKEN,             new LoadHandler(ctrl));
        handlers.put(Protocol.REMOVE_TOKEN,           new RemoveHandler(ctrl));
        handlers.put(Protocol.REMOVE_ACK_TOKEN,       new RemoveAckHandler(ctrl));
        handlers.put(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN,
               new RemoveAckHandler(ctrl));
        handlers.put(Protocol.LIST_TOKEN,             new ListHandler(ctrl));
//        handlers.put(Protocol.REBALANCE_COMPLETE_TOKEN,
//                new RebalanceCompleteHandler(ctrl));

        handlers.put(Protocol.RELOAD_TOKEN, new ReloadHandler(ctrl));
    }

    public CommandHandler get(String command) {
        return handlers.get(command);
    }
}
