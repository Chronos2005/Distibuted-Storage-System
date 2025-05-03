import java.io.IOException;
import java.net.Socket;

public interface CommandHandler {
    void handle(String[] command , Socket socket ) throws IOException;
}
