package org.example;

public class Controller {

    private int cport;
    private int replicationFactor;
    private int timeout;
    private int rebalancePeriod;
    private TCPSender sender;
    private TCPReceiver receiver;


    /**
     * Creates a controller object
     * @param cport The port the controller in listening on
     * @param R The replication Factor
     * @param timeout The time to wait while waiting for a response in milliseconds
     * @param rebalancePeriod The time in seconds till the next rebalance operation
     */
    public Controller(int cport, int R, int timeout, int rebalancePeriod) {
        this.cport = cport;
        this.replicationFactor = R;
        this.timeout = timeout;
        this.rebalancePeriod = rebalancePeriod;

        this.receiver = new TCPReceiver(cport);
        this.sender = new TCPSender("localhost", cport);

    }


    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java Controller <cport> <R> <timeout> <rebalance_period>");
            return;
        }

        int cport = Integer.parseInt(args[0]);
        int R = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int rebalancePeriod = Integer.parseInt(args[3]);

        Controller controller = new Controller(cport, R, timeout, rebalancePeriod);
        controller.start();

    }

    public void start() {

        receiver.start();
        sender.sendMessage("Hello, DStore!");
    }







}
