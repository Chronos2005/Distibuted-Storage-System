
# Distributed File Storage System

This project is a robust, fault-tolerant distributed file storage system . It implements a centralized controller architecture to manage multiple storage nodes (DStores) and handle client requests for storing, loading, listing, and removing files with data replication.

-----

## ✨ Key Features

* **Distributed Storage**: Files are distributed across multiple independent storage nodes (`DStores`).
* **Data Replication**: Each file is replicated on a configurable number (`R`) of `DStores` to ensure **fault tolerance** and high availability.
* **Centralized Control**: A `Controller` node manages the system's state, tracking file locations, `DStore` availability, and orchestrating all operations.
* **Load Balancing**: The `Controller` intelligently selects the least-loaded `DStores` for new file storage operations, ensuring an even distribution of data.
* **Concurrency Management**: The system is designed to handle multiple simultaneous client requests safely, using concurrent data structures and synchronization mechanisms to prevent race conditions.
* **Failure Detection**: The `Controller` gracefully handles `DStore` disconnections, removing them from the active pool and maintaining system integrity. If a client's `LOAD` request fails, it automatically retries with another replica.
* **Comprehensive Test Suite**: Includes a `ClientMain` class with numerous test cases for simple operations, concurrent operations (e.g., loading during a store), and mass concurrency stress tests.

-----

## 🏛️ System Architecture

The system consists of three main components that communicate over TCP/IP using a custom text-based protocol.

1.  **Controller**: The brain of the system.

    * Listens for connections from clients and `DStores`.
    * Maintains an `Index` of all files, their sizes, their locations (which `DStores` hold replicas), and their current state (e.g., `STORE_IN_PROGRESS`, `STORE_COMPLETE`).
    * Handles client requests (`STORE`, `LOAD`, `LIST`, `REMOVE`) by coordinating with the appropriate `DStores`.

2.  **DStore (Data Store)**: A simple storage node.

    * On startup, it joins the system by registering with the `Controller`.
    * Stores file replicas in its local file system.
    * Executes commands from the `Controller` (e.g., "store this file", "remove this file").
    * Communicates directly with the client for the actual file data transfer to offload work from the Controller.

3.  **Client**: The user-facing component.

    * Initiates all operations by sending requests to the `Controller`.
    * For a `STORE` operation, it first contacts the `Controller` to get a list of `DStores`, then sends the file data directly to each of them.
    * For a `LOAD` operation, it gets a `DStore` location from the `Controller` and downloads the file data directly from that `DStore`.

-----

## ⚙️ Detailed Operation Workflows

Here is the step-by-step communication protocol for each of the main operations.

### STORE Operation

1.  **Client → Controller**: Sends `STORE <filename> <filesize>`.
2.  **Controller**:
    * Performs validation (e.g., checks if the file already exists, verifies enough `DStores` are active).
    * Selects `R` least-loaded `DStores` to host the file.
    * Updates its internal index, marking the file as `STORE_IN_PROGRESS`.
3.  **Controller → Client**: Responds with `STORE_TO <port1> <port2> ...` containing the ports of the selected `DStores`.
4.  **Client → DStores**: The client connects to each `DStore` listed, sends a `STORE` command, waits for an `ACK`, and then streams the file data.
5.  **DStores → Controller**: After a `DStore` successfully saves the file to its disk, it sends a `STORE_ACK <filename>` confirmation to the Controller.
6.  **Controller → Client**: Once the Controller has received acknowledgements from all `R` DStores, it updates the file's status to `STORE_COMPLETE` and sends a final `STORE_COMPLETE` message to the client.

### LOAD Operation

1.  **Client → Controller**: Sends `LOAD <filename>`.
2.  **Controller**:
    * Looks up the file in its index. If it doesn't exist or isn't fully stored, it returns an error.
    * Selects one of the `DStores` that holds a replica of the file.
3.  **Controller → Client**: Responds with `LOAD_FROM <port> <filesize>`, directing the client to the chosen `DStore`.
4.  **Client → DStore**: The client connects to the specified `DStore`, sends a `LOAD_DATA <filename>` request, and prepares to receive the file.
5.  **DStore → Client**: The `DStore` reads the file from its disk and streams the data directly to the client.
6.  **(Fault Tolerance)** If the connection to the `DStore` fails, the client sends a `RELOAD <filename>` request to the Controller, which responds with the port of a *different* `DStore` replica to try next.

### LIST Operation

1.  **Client → Controller**: Sends `LIST`.
2.  **Controller**:
    * Scans its index for all files currently marked as `STORE_COMPLETE`.
3.  **Controller → Client**: Responds with `LIST <file1> <file2> ...`, a space-separated list of all available files.

### REMOVE Operation

1.  **Client → Controller**: Sends `REMOVE <filename>`.
2.  **Controller**:
    * Looks up the file in its index and identifies all `DStores` that store its replicas.
    * Updates the file's status to `REMOVE_IN_PROGRESS` to prevent new `LOAD` requests.
3.  **Controller → DStores**: Sends a `REMOVE <filename>` command to every `DStore` holding a replica.
4.  **DStores → Controller**: Each `DStore` deletes the file from its disk and sends a `REMOVE_ACK <filename>` confirmation back to the Controller.
5.  **Controller → Client**: Once the Controller has received acknowledgements from all relevant `DStores`, it removes the file entry from its index and sends a final `REMOVE_COMPLETE` message to the client.

-----

## 🛠️ Technical Stack & Design

* **Language**: **Java (JDK 21)**
* **Networking**: Core **Java Sockets (TCP)** for all inter-component communication.
* **Concurrency**: Extensive use of the **Java Concurrency API** (`java.util.concurrent`).
    * `ConcurrentHashMap` for thread-safe state management in the Controller's `Index`.
    * `CountDownLatch` to efficiently track acknowledgements from multiple DStores during `STORE` and `REMOVE` operations.
    * A thread-per-connection model to handle multiple simultaneous clients and `DStores`.
* **Design Patterns**:
    * **Command Pattern**: The `CommandHandler` interface and its various implementations (`StoreHandler`, `LoadHandler`, etc.) decouple the networking layer from the application logic, making the system modular and extensible.
    * **Factory Pattern**: `ControllerHandlerFactory` and `DstoreHandlerFactory` are used to instantiate the correct handler for incoming protocol messages.
* **Build Tool**: **Apache Maven** for dependency management.

-----

## 🚀 How to Run

### Prerequisites

* Java JDK 21
* Apache Maven

### Easy Start with Script

The provided `start.sh` script automates the entire process: compilation, setup, execution of a test case, and cleanup.

```bash
# Give execute permissions to the script
chmod +x start.sh

# Run the script
./start.sh
```

This will:

1.  Compile all Java source files into the `bin/` directory.
2.  Start the `Controller` and 3 `DStore` instances in the background.
3.  Run a client test (`concurrentremoveduringremove` by default, editable in the script).
4.  Kill all background processes upon completion.
5.  Log files for each component are generated in the `logs/` directory.

### Manual Execution

You can also run each component manually.

1.  **Compile the Code:**

    ```bash
    # Compile server-side code
    javac src/*.java -d bin
    # Compile the client (requires client.jar in classpath)
    javac -cp client/client.jar client/ClientMain.java -d bin
    ```

2.  **Start the Controller:**

    ```bash
    java -cp bin Controller <cport> <R> <timeout> <rebalance_period>
    # Example:
    java -cp bin Controller 12345 2 3000 10000
    ```

3.  **Start the DStores (in separate terminals):**

    ```bash
    java -cp bin Dstore <port> <controller_port> <timeout> <file_folder>
    # Example for DStore 1:
    java -cp bin Dstore 2000 12345 3000 DStore1
    # Example for DStore 2:
    java -cp bin Dstore 2001 12345 3000 DStore2
    ```

4.  **Run a Client Operation:**

    ```bash
    java -cp client/client.jar:bin ClientMain <cport> <timeout> <operation> [count]
    # Example: Store 5 files
    java -cp client/client.jar:bin ClientMain 12345 3000 store 5
    # Example: List files
    java -cp client/client.jar:bin ClientMain 12345 3000 list
    ```

-----

## 🧠 Key Learnings & Future Improvements

This project was an excellent exercise in practical distributed systems engineering, focusing on concurrency, fault tolerance, and protocol design.

### Potential Improvements

* **Dynamic Rebalancing**: Implement a mechanism to automatically redistribute files when a new DStore joins the system or when the data distribution becomes skewed.
* **Controller High Availability**: The current Controller is a single point of failure. This could be mitigated by implementing a primary-backup replication for the Controller or by using a consensus algorithm like **Raft** to create a distributed cluster of controllers.
* **Enhanced Security**: Implement TLS/SSL to encrypt data in transit and add an authentication layer for clients and DStores.
* **Stream-Based Transfers**: For very large files, modify the system to stream data instead of loading the entire file into memory before sending.