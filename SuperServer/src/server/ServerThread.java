package server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerThread implements Runnable{
    Thread t;
    int port, nThreads;
    String path;
    boolean flag = true;

    //list of files for every hdd
    ArrayList<FileEntry> hdd1;
    ArrayList<FileEntry> hdd2;
    ArrayList<FileEntry> hdd3;
    ArrayList<FileEntry> hdd4;
    ArrayList<FileEntry> hdd5;

    //list of all users
    ArrayList<String> users;

    //list of online users
    ArrayList<String> usersOnline;

    public ArrayList<FileEntry> getHdd1() {
        return hdd1;
    }

    public void setHdd1(ArrayList<FileEntry> hdd1) {
        this.hdd1 = hdd1;
    }

    public ArrayList<FileEntry> getHdd2() {
        return hdd2;
    }

    public void setHdd2(ArrayList<FileEntry> hdd2) {
        this.hdd2 = hdd2;
    }

    public ArrayList<FileEntry> getHdd3() {
        return hdd3;
    }

    public void setHdd3(ArrayList<FileEntry> hdd3) {
        this.hdd3 = hdd3;
    }

    public ArrayList<FileEntry> getHdd4() {
        return hdd4;
    }

    public void setHdd4(ArrayList<FileEntry> hdd4) {
        this.hdd4 = hdd4;
    }

    public ArrayList<FileEntry> getHdd5() {
        return hdd5;
    }

    public void setHdd5(ArrayList<FileEntry> hdd5) {
        this.hdd5 = hdd5;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }

    public ArrayList<String> getUsersOnline() {
        return usersOnline;
    }

    public void setUsersOnline(ArrayList<String> usersOnline) {
        this.usersOnline = usersOnline;
    }

    public ServerThread(int ports, int nThreads, String path){
        this.port = ports;
        this.nThreads = nThreads;
        this.path = path;
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
        System.out.println("Server Thread started.");
    }
    @Override
    public void run(){
        ExecutorService pool = null;
        ServerSocket listener = null;
        int maxTries = 10;
        while(maxTries > 0){
            maxTries -= 1;
            try{
                listener = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            }catch(Exception e){
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try{
                    Thread.sleep(10000);
                }catch(InterruptedException ee){

                }
            }
        }
        if(maxTries <= 0)return;
        System.out.println("Server is running.");
        while(flag){
            try {
                pool.execute(new Connection(listener.accept(), path));
            }catch(Exception e){
                if(!flag)break;
                System.out.println("Failed to add new client connection");
            }
        }
        System.out.println("Server thread end");
    }
    /*public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }*/
    public void stop(){
        flag = false;
        t.interrupt();
    }
    static class FileListUpdater implements Runnable{
        private Thread t;
        private boolean loop;

        private String hdd1;
        private String hdd2;
        private String hdd3;
        private String hdd4;
        private String hdd5;

        private Controller c;
        private int waitTime;

        public FileListUpdater(String hdd1, String hdd2, String hdd3, String hdd4, String hdd5, Controller c, int waitTime) {
            this.hdd1 = hdd1;
            this.hdd2 = hdd2;
            this.hdd3 = hdd3;
            this.hdd4 = hdd4;
            this.hdd5 = hdd5;
            this.c = c;
            this.waitTime = waitTime;
            loop = true;
            //start();
        }

        @Override
        public void run() {
            while(loop){
                updateList(listFilesForFolder(new File(hdd1)), hdd1, 1);
                updateList(listFilesForFolder(new File(hdd2)), hdd2, 2);
                updateList(listFilesForFolder(new File(hdd3)), hdd3, 3);
                updateList(listFilesForFolder(new File(hdd4)), hdd4, 4);
                updateList(listFilesForFolder(new File(hdd5)), hdd5, 5);

                try{
                    Thread.sleep(waitTime*1000);
                }catch(InterruptedException e){
                    if(!loop)break;
                }
            }
        }
        private void updateList(ArrayList<String> listHdd, String hdd, int hddNo){
            ObservableList<FileEntry> listForGui = FXCollections.observableArrayList();
            for(String a : listHdd){
                File file = new File(hdd + "\\" + a);
                if (!file.exists() || !file.isFile()) continue;
                listForGui.add(new FileEntry(a, file.length(), "owner hdd1", "others hdd1"));
            }
            c.updateFiles(listForGui, hddNo);
        }
        public void start() {
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }
        public void stop(){
            loop = false;
            t.interrupt();
        }
        public ArrayList<String> listFilesForFolder(final File folder) {
            ArrayList<String> list = new ArrayList<String>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        listFilesForFolder(fileEntry);
                    } else {
                        list.add(fileEntry.getName());
                    }
                }
            }catch (NullPointerException e){

            }
            return list;
        }
    }
}

class Connection implements Runnable{
    private Thread t;
    private Socket socket;
    String serverFolderRoot;

    ArrayList<String> hddPaths;

    Connection(Socket s, String path){
        socket = s;
        serverFolderRoot = path;
        //this.start();
        if (t == null) {
            t = new Thread(this);
            //t.start();
        }
        System.out.println(s.getPort() + ">Connection thread started");
    }
    /*public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }*/
    @Override
    public void run() {
        int n = 0;
        System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ "> has connected to server");n++;

        PrintWriter out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ ">IOException: failed to get output stream from client");n++;
        }
        //-----------------------------------------LISTENER LOOP--------------------------------------------------------

        boolean loop = true;
        try {
            InputStream input = socket.getInputStream();
            Scanner in = new Scanner(input);
            while (loop) {
                if(socket.isClosed()){
                    break;
                }
                if(in.hasNextLine()) {
                    String temp = in.nextLine();
                    System.out.println(socket.getPort() +"\\"+ t.getId() +"\\"+n+ "> " + temp);n++;
                    if(temp.matches("file [\\w-_()']+\\.[A-Za-z0-9]{3} [\\d]+")){
                        //------------------------------------------------------------------user sends file
                        String[] a = temp.split(" ");
                        String filename = a[1];
                        int filesize = Integer.parseInt(a[2]);
                        System.out.println(socket.getPort() +"\\"+ t.getId() +"\\"+n+">receiving file : " + filename);n++;
                        //System.out.println(socket.getPort() + ">waiting for file transfer");
                        try {
                            //System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+"> " + filename + " client: "+in.nextLine());
                            DataInputStream dis = new DataInputStream(input);
                            FileOutputStream fos = new FileOutputStream(serverFolderRoot +"\\hdd1\\"+filename);
                            byte[] buffer = new byte[1024*64];
                            int read = 0;
                            int totalRead = 0;
                            int remaining = filesize;
                            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0 && remaining > 0) {
                                totalRead += read;
                                remaining -= read;
                                //System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+"> " + filename + " remaining " + remaining);n++;
                                fos.write(buffer, 0, read);
                            }
                            System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+"> " + filename + " read " + totalRead + " of " + filesize);n++;
                            out.println("received " + filename);

                            try {
                                Thread.sleep(1000);
                            }catch(Exception e){}
                            fos.close();
                            dis.close();
                        }catch(Exception e){
                            System.out.println("Exception in FileReceiverClass : " + e.getMessage());
                        }
                        System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ "> file "+ filename +" saved");n++;

                        break;
                    }
                    if(temp.matches("list [\\w]+")){
                        //---------------------------------------------------------------------user wants new files
                        String[] a = temp.split(" ");
                        //System.out.println("a0: " + a[0]);
                        //System.out.println("a1: " + a[1]);//username
                        //create list of user files
                        final File folder = new File(serverFolderRoot);
                        ArrayList<String> files;
                        files = listFilesForFolder(folder);
                        try {
                            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                            objectOutput.writeObject(files);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //out.println(files.toString());
                        //System.out.println(files.toString());
                        //System.out.println(socket.getPort() + "> sent list of files for " + a[1]);
                        //break;
                    }
                    if(temp.matches("getfile [\\w-_()']+\\.[A-Za-z0-9]{3}")){
                        String[] a = temp.split(" ");
                        //System.out.println("a0: " + a[0]);
                        //System.out.println("a1: " + a[1]);//filename
                        String filename = a[1];
                        String filePath = serverFolderRoot+"\\"+filename;
                        File f = new File(filePath);
                        int size = (int)f.length();
                        out.println(size);
                        System.out.println(socket.getPort() +"\\"+ t.getId() + "> sending "+filename+"("+size+" B) ...");
                        DataOutputStream dos;
                        FileInputStream fis;
                        try {
                            dos = new DataOutputStream(socket.getOutputStream());
                            fis = new FileInputStream(filePath);

                            byte[] buffer = new byte[1024*64];
                            while (fis.read(buffer) > 0) {
                                dos.write(buffer);
                            }
                            try {
                                Thread.sleep(1000);
                            }catch(Exception e){}
                            System.out.println(t.getId() + "\\" +filename + " waiting for confirmation from client ... ");
                            System.out.println(t.getId() + "\\" +filename + " client: "+in.nextLine());
                            if(socket.isClosed()){
                                System.out.println(filename + " socket closed");
                            }else{
                                fis.close();
                                dos.close();
                            }

                        }catch(Exception e){
                            System.out.println(socket.getPort() +"\\"+ t.getId() +  "> Exception while sending file : " + e.getMessage());
                        }
                        System.out.println(socket.getPort() +"\\"+ t.getId() + "> sent "+filename + " to client");
                        break;
                    }
                    if(temp.matches("exit")){
                        //loop = false;
                        break;
                    }
                }
            }
        }catch(Exception e){
            System.out.println("Listener> Exception occured: " + e.getMessage());
        }
        //--------------------------------------------------------------------------------------------------------------

        //socket.close();
        //while(true);

        try{
            socket.close();
        }catch(IOException e){
            System.out.println(socket.getPort() +"\\"+ t.getId() + "> exception while closing socket: "+e.getMessage());
        }finally{
            System.out.println(socket.getPort() +"\\"+ t.getId() + "> CLOSED ");
        }
    }
    public static ArrayList<String> listFilesForFolder(final File folder) {
        ArrayList<String> list = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                list.add(fileEntry.getName());
            }
        }
        return list;
    }
}
