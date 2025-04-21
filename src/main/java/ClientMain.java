import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;



public class ClientMain {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java ClientMain <cport> <timeout> <operation>");
			System.err.println("  operation: store | list | load | remove");
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
				case "load":
					testLoad(client);
					break;
				case "remove":
					testRemove(client);
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

	private static void testStore(Client client)
			throws IOException, NotEnoughDstoresException, FileAlreadyExistsException {
		File uploadFolder = new File("to_store");
		File[] files = uploadFolder.listFiles();
		if (files == null || files.length == 0) {
			System.err.println("No files in 'to_store' to store.");
			return;
		}
		File fileToStore = files[0];
		String filename = fileToStore.getName();
		byte[] data = Files.readAllBytes(fileToStore.toPath());
		System.out.println("Storing: " + filename);
		client.store(filename, data);
		System.out.println("Store completed.");
	}

	private static void testList(Client client)
			throws IOException, NotEnoughDstoresException {
		System.out.println("Listing files...");
		String[] list = client.list();
		System.out.println("Files (" + list.length + "): " + Arrays.toString(list));
	}

	private static void testLoad(Client client) {
		try {
			File uploadFolder = new File("to_store");
			File[] files = uploadFolder.listFiles();
			if (files == null || files.length == 0) {
				System.err.println("No files to store for load test.");
				return;
			}

			String filename = files[0].getName();
			byte[] data = Files.readAllBytes(files[0].toPath());
			System.out.println("Storing for load test: " + filename);
			client.store(filename, data);

			File downloadFolder = new File("downloads");
			downloadFolder.mkdirs();

			System.out.println("Loading: " + filename);
			client.load(filename, downloadFolder);

			File loaded = new File(downloadFolder, filename);
			if (loaded.exists()) {
				System.out.println("✅ Load succeeded: " + loaded.getAbsolutePath());
			} else {
				System.err.println("❌ Load failed: file not found in downloads.");
			}

		} catch (IOException e) {
			System.err.println("Load test error: " + e.getMessage());
		}
	}

	private static void testRemove(Client client) {
		try {
			File uploadFolder = new File("to_store");
			File[] files = uploadFolder.listFiles();
			if (files == null || files.length == 0) {
				System.err.println("No files to store for remove test.");
				return;
			}

			File fileToStore = files[0];
			String filename = fileToStore.getName();
			byte[] data = Files.readAllBytes(fileToStore.toPath());

			// Step 1: Store the file
			System.out.println("Storing for remove test: " + filename);
			client.store(filename, data);
			System.out.println("Store completed.");

			// Step 2: Remove the file
			System.out.println("Removing: " + filename);
			client.remove(filename);
			System.out.println("Remove completed.");

			// Step 3: Check via list()
			String[] list = client.list();
			boolean stillExists = Arrays.asList(list).contains(filename);

			if (stillExists) {
				System.err.println("❌ Remove failed: " + filename + " still appears in file list.");
			} else {
				System.out.println("✅ Remove successful: " + filename + " no longer in file list.");
			}

		} catch (NotEnoughDstoresException e) {
			System.err.println("Not enough Dstores available.");
		} catch (FileAlreadyExistsException e) {
			System.err.println("Unexpected: file already exists.");
		} catch (FileDoesNotExistException e) {
			System.err.println("File missing during remove: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O error during remove test: " + e.getMessage());
		}
	}
}
