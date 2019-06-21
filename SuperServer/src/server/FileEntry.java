package server;

import java.io.Serializable;
import java.util.*;

/**
 * contains information about file,
 * is used in lists of files
 */
public class FileEntry implements Serializable {
    /**
     * full name of file, example "text_file.txt"
     */
    private String filename = "";
    /**
     * number of hdd on which file is on server
     */
    private int hddNo = 0;
    /**
     * local path file (will be different on server and client)
     * for example: "hdd1\adamko\file.txt"
     */
    private String path = "";//local folder + owner name + file name
    /**
     * username of file owner
     */
    private String owner = "";
    /**
     * list of users that can download this file (that can't delete or share this file)
     */
    private ArrayList<String> others = new ArrayList<>();
    /**
     * size of file in bytes
     */
    private long size = 0;

    public String getSizeString() {
        if(size < 1024)
            return size+" B";
        if(size < 1048576.0)
            return String.format("%.02f KB", size/1024.0);
        if(size < 1073741824.0)
            return String.format("%.02f MB", size/1048576.0);
        else
            return String.format("%.02f GB", size/1073741824.0);
    }


    /**
     * used to display size in gui
     */
    private String sizeString;
    /**
     * where file is located, used in client
     * default value: "server"
     * for example: "server", "local", "local + server"
     */
    private String status = "server";

    public FileEntry() {}

    public FileEntry(String filename, int hddNo, String path, long size, String owner) {
        this.filename = filename;
        this.hddNo = hddNo;
        this.path = path;
        this.size = size;
        this.owner = owner;
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

    /**
     * used to write file entry to csv
     * @return string to be saved in csv file
     */
    String toCsv(){
        if(others.size()> 0)
            return filename + "," + size + "," + owner + "," + getOthersString() + "\n";
        else
            return filename + "," + size + "," + owner + "," + "" + "\n";
    }

    /**
     * adds new String to Others
     * @param userToShare user to be added to Others
     */
    void share(String userToShare){
        //add userToShare to others
        //check if userToShare already exists in others. don't add then
        for(String u : this.others){
            if(u.equals(userToShare)){
                return;
            }
        }
        this.others.add(userToShare);
    }

    /**
     * removes String from Others
     * @param userToUnshare user to be removes from Others
     */
    void unshare(String userToUnshare){
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
            this.size == f.size &&
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
