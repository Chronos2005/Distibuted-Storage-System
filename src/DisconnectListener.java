import java.net.Socket;

public interface DisconnectListener {
    void onDisconnect(Socket s);
}