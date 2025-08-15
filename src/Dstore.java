

import java.io.IOException;
import java.net.Socket;

public class Dstore implements DisconnectListener {

    private final TCPReceiver receiver;
    private final TCPSender   controllerSender;
    private final DstoreHandlerFactory handlerFactory;
    private final int port;
    private final int timeout;
    private final int cPort;

    public Dstore(int port, int controllerPort, int timeout, String fileFolder)
            throws IOException {
        this.port = port;

        // 1) Open persistent channel back to Controller
        this.controllerSender = new TCPSender("localhost", controllerPort);

        // 2) Build the handler factory
        this.handlerFactory = new DstoreHandlerFactory(fileFolder, controllerSender, timeout);

        // 3) Listen for incoming connections on Dstore port
        this.receiver = new TCPReceiver(port, this::dispatch, this,timeout);
        receiver.attach(controllerSender.getSocket());
        this.timeout = timeout;
        this.cPort = controllerPort;

        System.out.printf("DStore[%d] → Controller:%d, folder=%s%n",
                port, controllerPort, fileFolder);
    }

    public void start() throws IOException {
        receiver.start();
        // Send the JOIN after we’re listening
        controllerSender.sendOneWay(Protocol.JOIN_TOKEN + " " + port);
    }

    private void dispatch(String line, Socket socket) {
        try {
            String[] parts = line.split(" ", 2);
            String cmd     = parts[0];

            CommandHandler handler = handlerFactory.get(cmd);
            if (handler != null) {
                handler.handle(parts, socket);
            } else {
                System.err.println("Unknown DStore cmd: " + cmd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: java DStore <port> <controllerPort> <timeout> <fileFolder>");
            return;
        }
        int port          = Integer.parseInt(args[0]);
        int cport         = Integer.parseInt(args[1]);
        int timeout       = Integer.parseInt(args[2]);
        String fileFolder = args[3];

        new Dstore(port, cport, timeout, fileFolder).start();
    }

    @Override
    public void onDisconnect(Socket s) {
        if (s.getPort()==cPort) {
            System.err.println("Disconnected from Controller: null");

        }


    }
}
