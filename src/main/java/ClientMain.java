
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class ClientMain {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java ClientMain <cport> <timeout> <operation>");
			System.err.println("  operation: store | list");
			return;
		}

		int cport = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		String op = args[2].toLowerCase();

		try {
			Client client = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
			client.connect();

			switch (op) {
				case "store":
					testStore(client);
					break;
				case "list":
					testList(client);
					break;
				default:
					System.err.println("Unknown operation: " + op);
			}

			client.disconnect();
		} catch (IOException e) {
			System.err.println("I/O error: " + e.getMessage());
			e.printStackTrace();
		}
    }

	/**
	 * Test storing the first file in 'to_store' directory.
	 */
	private static void testStore(Client client) throws IOException, NotEnoughDstoresException, FileAlreadyExistsException {
		File uploadFolder = new File("to_store");
		File[] files = uploadFolder.listFiles();
		if (files == null || files.length == 0) {
			System.err.println("No files in 'to_store' to store.");
			return;
		}
		File fileToStore = files[0];
		String filename = fileToStore.getName();
		byte[] data = Files.readAllBytes(fileToStore.toPath());
		System.out.println("Storing file: " + filename + " (" + data.length + " bytes)");
		client.store(filename, data);
		System.out.println("Store completed.");
	}

	/**
	 * Test listing files from the Controller.
	 */
	private static void testList(Client client) throws IOException, NotEnoughDstoresException {
		System.out.println("Requesting file list...");
		String[] list = client.list();
		System.out.println("Files stored (" + list.length + "): " + Arrays.toString(list));
	}
}
