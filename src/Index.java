import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class Index {
    private ConcurrentHashMap<String, FileInfo> files;

    public Index() {
        files = new ConcurrentHashMap<>();
    }

    public FileInfo getFileInfo(String filename) {
        return files.get(filename);
    }

    public void setFileInfo(String filename, FileInfo fileInfo) {
        files.put(filename, fileInfo);
    }


    public void addFileInfo(String filename, FileInfo fileInfo) {
        files.put(filename, fileInfo);
    }






    /**
     * The state of the File
     */
    public enum FileState {
        STORE_IN_PROGRESS,
        STORE_COMPLETE,
        REMOVE_IN_PROGRESS
    }


    /**
     * Returns a map of Dstore port → number of files
     * that are in STORE_COMPLETE state on that Dstore.
     */
    public Map<Integer,Integer> getFileCountPerDstore() {
        Map<Integer,Integer> counts = new HashMap<>();
        for (FileInfo info : files.values()) {
            // Count both completed and in-progress files
            if (info.getFileState() == FileState.STORE_COMPLETE ||
                    info.getFileState() == FileState.STORE_IN_PROGRESS) {
                for (int port : info.getdStorePorts()) {
                    counts.merge(port, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    public Set<String> getAllFileNames() {
        return files.keySet();
    }


    public void removeFileInfo(String filename) {
        files.remove(filename);
    }

    public Set<Map.Entry<String, FileInfo>> getAllEntries() {
        // Return a copy of the entry set
        return new HashSet<>(files.entrySet());


    }

}



