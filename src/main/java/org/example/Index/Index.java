package org.example.Index;

import java.util.HashMap;


public class Index {
    private HashMap<String, FileInfo> files;

    public Index() {
        files = new HashMap<>();
    }

    public FileInfo getFileInfo(String filename) {
        return files.get(filename);
    }

    public void setFileInfo(String filename, FileInfo fileInfo) {
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


}
