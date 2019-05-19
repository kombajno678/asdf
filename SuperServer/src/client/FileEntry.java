package client;
public class FileEntry {
    private String filename;
    private String sharedTo;
    private String status;

    public FileEntry() {
    }
    public FileEntry(String filename, String sharedTo, String status) {
        this.filename = filename;
        this.sharedTo = sharedTo;
        this.status = status;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSharedTo() {
        return sharedTo;
    }

    public void setSharedTo(String sharedTo) {
        this.sharedTo = sharedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
