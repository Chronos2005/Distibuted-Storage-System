package org.example.Index;

import java.io.IOException;
import java.util.ArrayList;

public class FileInfo {

    private Index.FileState fileState;
    private int fileSize;
    private ArrayList<Integer> dStorePorts = new ArrayList<>();
    public FileInfo(Index.FileState fileState, int fileSize, ArrayList<Integer> dStorePorts){
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
    public ArrayList<Integer> getdStorePorts() {
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
