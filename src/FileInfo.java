import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileInfo {

    private Index.FileState fileState;
    private int fileSize;
    private CopyOnWriteArrayList<Integer> dStorePorts;
    public FileInfo(Index.FileState fileState, int fileSize, CopyOnWriteArrayList<Integer> dStorePorts){
        this.fileState = fileState;
        this.fileSize = fileSize;
        this.dStorePorts = dStorePorts;


    }

    public Index.FileState getFileState() {
        return fileState;
    }
    public int getFileSize() {
        return fileSize;
    }
    public CopyOnWriteArrayList<Integer> getdStorePorts() {
        return dStorePorts;
    }

    public void addDStorePorts(int port) {
        dStorePorts.add(port);
    }

    public void setFileState(Index.FileState fileState) {
        this.fileState = fileState;
    }

    public void removeDstorePorts(int port) {
        dStorePorts.remove(port);

    }





}
