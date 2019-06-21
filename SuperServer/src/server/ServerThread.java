package server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * main thread of server
 */
public class ServerThread implements Runnable {
    /**
     * thread
     */
    private Thread t;
    /**
     * server's port
     */
    private int port;
    /**
     * maximum number of connections on this port
     */
    private int nThreads;
    /**
     * used in main thread loop
     */
    private boolean flag = true;
    /**
     * list of hdd paths
     */
    private ArrayList<String> hdd;
    /**
     * list of files for every hdd
     */
    private ArrayList<FileEntry> filesList;
    /**
     * list of online users
     */
    private ArrayList<String> usersOnline;
    /**
     * reference to hddController
     */
    private HddController hddController;
    /**
     * reference to gui controller
     */
    protected Controller c;
    /**
     * server's listener socket
     */
    private ServerSocket listener;
    /**
     * thread pool of all connections to server
     */
    private ExecutorService pool;

    public void setHdd(ArrayList<String> hdd) {
        this.hdd = hdd;
    }

    ArrayList<String> getHdd() {return hdd;}

    /**
     * returns list of all files owned by or shared to user
     * @param username username
     * @return list of user's files
     */
    ArrayList<FileEntry> getUserFilesList(String username) {
        ArrayList<FileEntry> temp = new ArrayList<>();
        for (Iterator<FileEntry> i = filesList.iterator(); i.hasNext(); ) {
            FileEntry file = i.next();
            //check if file is owned by user
            if (file.getOwner().equals(username)) {
                temp.add(file);
                //continue;
            } else {
                //check if file is shared to user
                for (String a : file.getOthers()) {
                    if (a.equals(username)) {
                        temp.add(file);
                    }
                }
            }
        }
        return temp;
    }

    /**
     * add files from f to global files list, skips duplicates
     * @param f files to add to global files list
     */
    void addDistinct(Collection<FileEntry> f) {
        if (f.isEmpty()) {
            return;
        }
        if (filesList.isEmpty()) {
            filesList.addAll(f);
            return;
        }
        //iterate through list of files to add, if duplicate found then don't add
        for(FileEntry fe : f){
            boolean duplicate = false;
            for(FileEntry fs : filesList){
                if(fs.getFilename().equals(fe.getFilename()) && fs.getOwner().equals(fe.getOwner())){
                    duplicate = true;
                    break;
                }
            }
            if(!duplicate){
                filesList.add(fe);
            }
        }
        return;
    }
    public ArrayList<FileEntry> getFilesList() {return filesList;}
    ArrayList<FileEntry> getFilesListClone() {
        return (ArrayList<FileEntry>) filesList.clone();
    }
    public void setFilesList(ArrayList<FileEntry> filesList) {this.filesList = filesList;}
    public ArrayList<String> getUsersOnline() {
        return usersOnline;
    }
    public void setUsersOnline(ArrayList<String> usersOnline) { this.usersOnline = usersOnline; }
    synchronized void addToFilesList(FileEntry f){
        filesList.add(f);
    }

    /**
     * ServerThread constructor
     * @param port server's port
     * @param nThreads maximum numver of connection threads
     * @param path path where hard drives are
     * @param hdd list of hdd paths
     * @param hddController reference to hddcontroller,holds information about number of operations
     * @param c reference to gui controller
     */
    ServerThread(int port, int nThreads, String path, ArrayList<String> hdd, HddController hddController, Controller c) {
        this.port = port;
        this.nThreads = nThreads;
        this.hdd = hdd;
        this.hddController = hddController;
        this.c = c;
        filesList = new ArrayList<>();
        usersOnline = new ArrayList<>();
        System.out.println("Server Thread started.");
    }

    /**
     * constructor used for testing
     */
    public ServerThread(){
        usersOnline = new ArrayList<>();
        filesList = new ArrayList<>();
    }


    @Override
    public void run() {
        pool = null;
        listener = null;
        int maxTries = 10;
        while (maxTries > 0) {
            maxTries -= 1;
            try {
                listener = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            } catch (Exception e) {
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ee) {

                }
            }
        }
        if (maxTries <= 0) return;
        System.out.println("Server is running.");
        while (flag) {
            try {
                pool.execute(new Connection(listener.accept(), this, hddController));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new client connection");
            }
        }
        System.out.println("Server thread end");
    }

    /**
     * starts thread
     */
    public void start(){
        if (t == null) {
            t = new Thread (this, "ServerThread");
            t.start ();
        }
    }

    /**
     * tries to stop thread
     */
    public void stop() {
        flag = false;

        try{
            pool.shutdown();
            listener.close();
        }catch(Exception e){
            //e.printStackTrace();
        }
        t.interrupt();
    }
    /**
     * send list of users to gui
     */
    void updateUsers(){
        ObservableList<String> usersOnlineGui = FXCollections.observableArrayList();
        usersOnlineGui.addAll(getUsersOnline());
        c.updateUsersOnline(usersOnlineGui);
    }


}