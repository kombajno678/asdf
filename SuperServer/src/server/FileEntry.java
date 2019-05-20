package server;

import java.util.ArrayList;

public class FileEntry {
    private String filename;
    private int hddNo;
    private String path;
    private String owner;
    private ArrayList<String> others;
    private long size;

    public FileEntry() {}

    public FileEntry(String filename, int hddNo, String path, long size, String owner) {
        this.filename = filename;
        this.hddNo = hddNo;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.others = new ArrayList<>();
    }
    public FileEntry(String filename, int hddNo, String path, long size, String owner, ArrayList<String> others) {
        this.filename = filename;
        this.hddNo = hddNo;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.others = others;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getHddNo() { return hddNo; }
    public void setHddNo(int hddNo) { this.hddNo = hddNo; }
    public long getSize() {
        return size;
    }
    public void setSize(long size) { this.size = size; }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    public ArrayList<String> getOthers() {
        return others;
    }
    public void setOthers(ArrayList<String> others) {
        this.others = others;
    }
}
