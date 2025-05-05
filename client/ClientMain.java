import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

public class ClientMain {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java ClientMain <cport> <timeout> <operation> [count]");
			System.err.println("  operation: store | list | load | remove");
			System.err.println("  count: number of operations to perform (optional, default=1)");
			return;
		}

		int cport = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		String op = args[2].toLowerCase();
		int count = 1;

		if (args.length >= 4) {
			count = Integer.parseInt(args[3]);
		}

		try {
			Client client = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
			client.connect();

			switch (op) {
				case "store":
					testStore(client, count);
					break;
				case "list":
					testList(client);
					break;
				case "load":
					testLoad(client, count);
					break;
				case "remove":
					testRemove(client, count);
					break;
				case "reload":
					reloadTest(client);
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

	private static void testStore(Client client, int count)
			throws IOException {
		File uploadFolder = new File("to_store");
		if (!uploadFolder.exists()) {
			uploadFolder.mkdirs();
		}

		File[] files = uploadFolder.listFiles();
		if (files == null || files.length == 0) {
			System.out.println("No files found in 'to_store' directory. Creating test files...");
			createTestFiles(uploadFolder, 3); // Create 3 test files
			files = uploadFolder.listFiles();
		}

		System.out.println("==== Performing " + count + " store operations ====");
		int successCount = 0;

		for (int i = 0; i < count; i++) {
			// Get file index with wraparound if count > number of files
			int fileIndex = i % files.length;
			File fileToStore = files[fileIndex];
			String filename = fileToStore.getName();

			// Add index to filename if multiple operations with same file
			String storeFilename = filename;
			if (count > files.length) {
				storeFilename = "copy" + i + "_" + filename;
			}

			try {
				byte[] data = Files.readAllBytes(fileToStore.toPath());
				System.out.println("\nStore operation #" + (i+1) + ": " + storeFilename);
				client.store(storeFilename, data);
				System.out.println("✅ Store completed for: " + storeFilename);
				successCount++;
			} catch (NotEnoughDstoresException e) {
				System.err.println("❌ Not enough Dstores available for: " + storeFilename);
			} catch (FileAlreadyExistsException e) {
				System.err.println("❌ File already exists: " + storeFilename);
			} catch (IOException e) {
				System.err.println("❌ I/O error when storing: " + storeFilename);
				e.printStackTrace();
			}
		}

		System.out.println("\n==== Store operations summary ====");
		System.out.println("Successful: " + successCount + "/" + count);
	}

	private static void testList(Client client)
			throws IOException, NotEnoughDstoresException {
		System.out.println("Listing files...");
		String[] list = client.list();
		System.out.println("Files (" + list.length + "): " + Arrays.toString(list));
	}

	private static void testLoad(Client client, int count) {
		File downloadFolder = new File("downloads");
		downloadFolder.mkdirs();

		try {
			// First get list of files available
			String[] availableFiles = client.list();
			if (availableFiles.length == 0) {
				System.err.println("No files available to load.");
				return;
			}

			System.out.println("==== Performing " + count + " load operations ====");
			int successCount = 0;

			for (int i = 0; i < count; i++) {
				// Get file index with wraparound if count > number of available files
				int fileIndex = i % availableFiles.length;
				String filename = availableFiles[fileIndex];

				System.out.println("\nLoad operation #" + (i+1) + ": " + filename);

				try {
					client.load(filename, downloadFolder);

					File loaded = new File(downloadFolder, filename);
					if (loaded.exists()) {
						System.out.println("✅ Load succeeded: " + loaded.getAbsolutePath());
						successCount++;
					} else {
						System.err.println("❌ Load failed: file not found in downloads.");
					}
				} catch (FileDoesNotExistException e) {
					System.err.println("❌ File does not exist: " + filename);
				} catch (Exception e) {
					System.err.println("❌ Error loading file: " + e.getMessage());
				}
			}

			System.out.println("\n==== Load operations summary ====");
			System.out.println("Successful: " + successCount + "/" + count);
		} catch (NotEnoughDstoresException e) {
			System.err.println("Not enough Dstores available for listing files.");
		} catch (IOException e) {
			System.err.println("I/O error: " + e.getMessage());
		}
	}

	/**
	 * Creates test files with random content in the specified directory
	 *
	 * @param directory Directory to create files in
	 * @param numFiles Number of files to create
	 * @throws IOException If there's an error creating files
	 */
	private static void createTestFiles(File directory, int numFiles) throws IOException {
		Random random = new Random();

		for (int i = 0; i < numFiles; i++) {
			String filename = "testfile_" + (i + 1) + ".dat";
			File file = new File(directory, filename);

			// Create random content between 1KB and 10KB
			int fileSize = 1024 + random.nextInt(9 * 1024);
			byte[] content = new byte[fileSize];
			random.nextBytes(content);

			// Write the file
			try (FileOutputStream fos = new FileOutputStream(file)) {
				fos.write(content);
			}

			System.out.println("Created test file: " + file.getAbsolutePath() + " (" + fileSize + " bytes)");
		}
	}

	private static void testRemove(Client client, int count) throws IOException {
		File  toStore = new File("to_store");
		if (!toStore.exists()) {
			toStore.mkdirs();
		}
		File testFile = new File(toStore, "test.txt");
		client.store(testFile);
		client.remove(testFile.getName());

	}


	private static void reloadTest(Client client) throws IOException {
		File  toStore = new File("to_store");
		if (!toStore.exists()) {
			toStore.mkdirs();
		}
		File testFile = new File(toStore, "test.txt");
		if (!testFile.exists()) {
			testFile.createNewFile();
		}
		client.store(testFile);
		client.wrongLoad("test.txt",5);
	}
}