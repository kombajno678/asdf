package server;
public class FileEntry {
    private String filename;
    private String owner;
    private String others;
    private long size;

    public FileEntry() {
    }

    public FileEntry(String filename, long size, String owner, String others) {
        this.filename = filename;
        this.size = size;
        this.owner = owner;
        this.others = others;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

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

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }
}
