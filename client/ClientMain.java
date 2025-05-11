import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Assumes Client, Logger, NotEnoughDstoresException, FileDoesNotExistException, FileAlreadyExistsException are in the classpath
public class ClientMain {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java ClientMain <cport> <timeout> <operation> [count]");
			System.err.println("  operation: store | list | load | remove | reload | " +
					"concurrentStore | concurrentLoadDuringStore | concurrentRemoveDuringStore | concurrentListDuringStore | " +
					"concurrentStoreDuringRemove | concurrentLoadDuringRemove | concurrentRemoveDuringRemove | concurrentListDuringRemove");
			System.err.println("  count: number of operations to perform (optional, default=1)");
			return;
		}

		int cport = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		String op = args[2].toLowerCase();
		int count = (args.length >= 4) ? Integer.parseInt(args[3]) : 1;

		try {
			switch (op) {
				case "store":
				case "list":
				case "load":
				case "remove":
				case "reload": {
					Client client = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
					client.connect();
					runSimpleOp(client, op, count);
					client.disconnect();
					break;
				}
				case "concurrentstore":
					testConcurrentStore(cport, timeout);
					break;
				case "concurrentloadduringstore":
					testConcurrentLoadDuringStore(cport, timeout);
					break;
				case "concurrentremoveduringstore":
					testConcurrentRemoveDuringStore(cport, timeout);
					break;
				case "concurrentlistduringstore":
					testConcurrentListDuringStore(cport, timeout);
					break;
				case "concurrentstoreduringremove":
					testConcurrentStoreDuringRemove(cport, timeout);
					break;
				case "concurrentloadduringremove":
					testConcurrentLoadDuringRemove(cport, timeout);
					break;
				case "concurrentremoveduringremove":
					testConcurrentRemoveDuringRemove(cport, timeout);
					break;
				case "concurrentlistduringremove":
					testConcurrentListDuringRemove(cport, timeout);
					break;
				default:
					System.err.println("Unknown operation: " + op);
			}
		} catch (IOException e) {
			System.err.println("I/O error: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void runSimpleOp(Client client, String op, int count)
			throws IOException, NotEnoughDstoresException, FileDoesNotExistException {
		switch (op) {
			case "store":   testStore(client, count); break;
			case "list":    testList(client);        break;
			case "load":    testLoad(client, count); break;
			case "remove":  testRemove(client);      break;
			case "reload":  reloadTest(client);      break;
		}
	}

	private static void testStore(Client client, int count) throws IOException {
		File dir = new File("to_store");
		if (!dir.exists()) dir.mkdirs();
		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			createTestFiles(dir, 3);
			files = dir.listFiles();
		}
		System.out.println("==== Performing " + count + " store operations ====");
		int success = 0;
		for (int i = 0; i < count; i++) {
			File f = files[i % files.length];
			String name = (count > files.length) ? "copy"+i+"_"+f.getName() : f.getName();
			try {
				byte[] data = Files.readAllBytes(f.toPath());
				System.out.println("Store #"+(i+1)+": "+name);
				client.store(name, data);
				System.out.println("Stored " + name);
				success++;
			} catch (NotEnoughDstoresException e) {
				System.err.println("ERROR: Not enough dstores for " + name);
			} catch (FileAlreadyExistsException e) {
				System.err.println("ERROR: File already exists " + name);
			}
		}
		System.out.println(success + "/" + count + " stores succeeded");
	}

	private static void testList(Client client)
			throws IOException, NotEnoughDstoresException {
		String[] list = client.list();
		System.out.println("Files: " + Arrays.toString(list));
	}

	private static void testLoad(Client client, int count)
			throws IOException, NotEnoughDstoresException, FileDoesNotExistException {
		File dl = new File("downloads");
		dl.mkdirs();
		String[] files = client.list();
		if (files.length == 0) { System.err.println("No files to load"); return; }
		int success = 0;
		for (int i = 0; i < count; i++) {
			String name = files[i % files.length];
			client.load(name, dl);
			File out = new File(dl, name);
			if (out.exists()) { System.out.println("Loaded "+name); success++; }
		}
		System.out.println(success+"/"+count+" loads succeeded");
	}

	private static void testRemove(Client client)
			throws IOException, NotEnoughDstoresException {
		String target = "removeTest.txt";
		client.remove(target);
		System.out.println("Remove issued for " + target);
	}

	private static void reloadTest(Client client) throws IOException {
		File dir = new File("to_store"); if (!dir.exists()) dir.mkdirs();
		File f = new File(dir, "reloadTest.txt");
		if (!f.exists()) f.createNewFile();
		client.store(f.getName(), Files.readAllBytes(f.toPath()));
		client.wrongLoad(f.getName(), 5);
	}

	private static void createTestFiles(File dir, int n) throws IOException {
		Random rnd = new Random();
		for (int i = 1; i <= n; i++) {
			File f = new File(dir, "testfile_"+i+".dat");
			byte[] b = new byte[1024 + rnd.nextInt(8192)]; rnd.nextBytes(b);
			try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(b); }
			System.out.println("Created "+f.getName());
		}
	}

	// ---------- CONCURRENT TESTS ----------

	private static void testConcurrentStore(int cport, int timeout)
			throws IOException, InterruptedException {
		System.out.println("Concurrent STORE vs STORE");
		runConcurrent(cport, timeout,
				c -> c.store("concurrent.txt", new byte[512]),
				c -> c.store("concurrent.txt", new byte[512]));
	}

	private static void testConcurrentLoadDuringStore(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent LOAD vs STORE");

		runConcurrent(cport, timeout,
				c -> c.store("x.dat", new byte[2048]),
				c -> c.load( "x.dat", new File("downloads")));
	}

	private static void testConcurrentRemoveDuringStore(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent REMOVE vs STORE");
		runConcurrent(cport, timeout,
				c -> c.store(  "y.dat", new byte[2048]),
				c -> c.remove( "y.dat"));
	}

	private static void testConcurrentListDuringStore(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent LIST vs STORE");
		runConcurrent(cport, timeout,
				c -> c.store( "z.dat", new byte[2048]),
				c -> System.out.println("List: "+Arrays.toString(c.list())));
	}

	private static void testConcurrentStoreDuringRemove(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent STORE vs REMOVE");
		preStore(cport, timeout, "a.dat", 1024);
		runConcurrent(cport, timeout,
				c -> c.remove("a.dat"),
				c -> c.store("a.dat", new byte[2048]));
	}

	private static void testConcurrentLoadDuringRemove(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException, FileDoesNotExistException {
		System.out.println("Concurrent LOAD vs REMOVE");
		preStore(cport, timeout, "b.dat", 1024);
		runConcurrent(cport, timeout,
				c -> c.remove("b.dat"),
				c -> c.load( "b.dat", new File("downloads")));
	}

	private static void testConcurrentRemoveDuringRemove(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent REMOVE vs REMOVE");
		preStore(cport, timeout, "c.dat", 1024);
		runConcurrent(cport, timeout,
				c -> c.remove("c.dat"),
				c -> c.remove("c.dat"));
	}

	private static void testConcurrentListDuringRemove(int cport, int timeout)
			throws IOException, InterruptedException, NotEnoughDstoresException {
		System.out.println("Concurrent LIST vs REMOVE");
		preStore(cport, timeout, "d.dat", 1024);
		runConcurrent(cport, timeout,
				c -> c.remove("d.dat"),
				c -> System.out.println("List: "+Arrays.toString(c.list())));
	}

	private static void preStore(int cport, int timeout, String name, int size)
			throws IOException, NotEnoughDstoresException {
		Client c = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
		c.connect();
		c.store(name, new byte[size]);
		c.disconnect();
	}

	@FunctionalInterface
	private interface ClientOp { void apply(Client c) throws Exception; }

	private static void runConcurrent(int cport, int timeout, ClientOp op1, ClientOp op2)
			throws IOException, InterruptedException {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		exec.execute(() -> {
			try {
				Client c = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
				c.connect(); start.await(); op1.apply(c); c.disconnect();
			} catch (Exception e) { System.err.println(e.getMessage()); }
		});
		exec.execute(() -> {
			try {
				Client c = new Client(cport, timeout, Logger.LoggingType.ON_FILE_AND_TERMINAL);
				c.connect(); start.await(); op2.apply(c); c.disconnect();
			} catch (Exception e) { System.err.println(e.getMessage()); }
		});
		start.countDown(); exec.shutdown();
	}
}
