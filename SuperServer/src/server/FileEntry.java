package server;

import java.io.Serializable;
import java.util.*;

public class FileEntry implements Serializable {
    private String filename;
    private int hddNo;
    private String path;//local folder + owner name + file name
    private String owner;
    private ArrayList<String> others;
    private long size;
    private String status = "server";

    public FileEntry() {}
    //constructors for client
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
    //constructors for server
    public FileEntry(String filename, int hddNo, String path, long size, String owner) {
        this.filename = filename;
        this.hddNo = hddNo;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.others = new ArrayList<>();
    }
    public FileEntry(String filename, int hddNo, String path, long size, String owner, String status) {
        this.filename = filename;
        this.hddNo = hddNo;
        this.path = path;
        this.size = size;
        this.owner = owner;
        this.status = status;
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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

    public void share(String userToShare){
        //add userToShare to others
        //check if userToShare already exists in others. don't add then
        for(String u : this.others){
            if(u.equals(userToShare)){
                return;
            }
        }
        this.others.add(userToShare);
    }
    public void unshare(String userToUnshare){
        //delete userToUnshare from others
        System.out.println("deleting "+userToUnshare+" from: "+others);
        for(Iterator<String> i = this.others.iterator();i.hasNext();){
            String u = i.next();
            if(u.equals(userToUnshare)){
                i.remove();
                break;
            }
        }
        System.out.println("after : "+others);
    }


    public boolean equals2(FileEntry f) {
        if(f == null && this != null || f != null && this == null)return false;
        if(
            this.filename.matches(f.getFilename()) &&
            this.others.equals(f.getOthers()) &&
            this.status.equals(f.getStatus())
        ){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        FileEntry f = (FileEntry)obj;
        if(this == null && obj != null){
            return false;
        }
        if(this != null && obj == null){
            return false;
        }
        if(
            this.filename.matches(f.getFilename()) &&
            this.hddNo == f.getHddNo() &&
            this.owner.matches(f.getOwner()) &&
            this.others.size() == f.getOthers().size()
        ){
            //check others
            Collections.sort(this.others);
            Collections.sort(f.getOthers());
            for(int i = 0; i < this.others.size();i++){
                if(!this.others.get(i).equals(f.getOthers().get(i))){
                    return false;
                }
            }
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
