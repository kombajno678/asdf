/*
package client;

import java.util.ArrayList;
import java.util.List;

public class FileEntry {
    private String filename;
    private int hddNo;
    private String path;
    private String owner;
    private List<String> others;
    private long size;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status;

    public FileEntry() {}

    public FileEntry(String filename, String path, long size, String owner) {
        this.filename = filename;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.others = new ArrayList<>();
    }
    public FileEntry(String filename, String path, long size, String owner, String status) {
        this.filename = filename;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.status = status;
        this.others = new ArrayList<>();
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
    public List<String> getOthers() {
        return others;
    }
    public void setOthers(List<String> others) {
        this.others = others;
    }

    public String getOthersString(){
        String ret = "";
        for(String a : others){
            ret += a + ",";
        }
        ret = ret.substring(0, ret.length()-1);
        return ret;
    }

    public String toCsv(){
        if(others.size()> 0)
            return filename + "," + size + "," + owner + "," + getOthersString() + "\n";
        else
            return filename + "," + size + "," + owner + "," + "" + "\n";


    }


    public boolean equals(server.FileEntry f) {
        if(f == null && this != null || f != null && this == null)return false;
        if(
            this.filename.matches(f.getFilename()) &&
            this.hddNo == f.getHddNo() &&
            this.path.matches(f.getPath()) &&
            this.owner.matches(f.getOwner()) &&
            this.others.equals(f.getOthers())
        ){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        server.FileEntry f = (server.FileEntry)obj;
        if(
            this.filename.matches(f.getFilename())// &&
            //this.hddNo == f.getHddNo() &&
            //this.path.matches(f.getPath()) &&
            //this.owner.matches(f.getOwner()) &&
            //this.others.equals(f.getOthers())
        ){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "'" + filename + "'" +
                "," + hddNo +
                ",'" + path + "'" +
                ",'" + owner + "'" +
                "," + others +
                "," + size +
                "}";
    }
}
*/