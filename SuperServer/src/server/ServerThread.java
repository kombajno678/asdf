package server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerThread implements Runnable{
    private Thread t;
    private int port, nThreads;
    private String path;
    private boolean flag = true;

    //list of hdd paths
    private ArrayList<String> hdd;

    //list of files for every hdd
    private ArrayList<FileEntry> filesList;

    //list of all users
    private ArrayList<String> users;

    //list of online users
    private ArrayList<String> usersOnline;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public ArrayList<String> getHdd() { return hdd; }
    public void setHdd(ArrayList<String> hdd) { this.hdd = hdd; }
    public ArrayList<FileEntry> getUserFilesList(String username){
        ArrayList<FileEntry> temp = new ArrayList<>();
        for(Iterator<FileEntry> i = filesList.iterator(); i.hasNext();){
            FileEntry file = i.next();
            //check if file is owned by user
            if(file.getOwner().matches(username)){
                temp.add(file);
                continue;
            }else{
                //check if file is shared to user
                for (String a:file.getOthers()) {
                    if(a.matches(username)){
                        temp.add(file);
                        continue;
                    }
                }
            }
        }
        return temp;
    }
    public int addDistinct(Collection<FileEntry> f){
        if(f.isEmpty()){
            return 0;
        }
        if(filesList.isEmpty()){
            filesList.addAll(f);
            return f.size();
        }
        int filesAdded = 0;
        //remove what is not in new list of files
        //for(FileEntry fold : filesList)
        for(Iterator<FileEntry> iterator = filesList.iterator(); iterator.hasNext();){
            FileEntry fold = iterator.next();
            boolean fileFound = false;
            FileEntry del = null;
            for(FileEntry fnew : f){
                if(fnew.getPath().equals(fold.getPath())){
                    fileFound = true;
                    del = fnew;
                    filesAdded -= 1;
                    break;
                }
            }
            if(!fileFound){
                //filesList.remove(fold);
                iterator.remove();

            }else{
                f.remove(del);
            }

        }
        //add what is not present in current list of files
        if(f.size() > 0)
            for(FileEntry fnew : f){
                filesList.add(fnew);
                filesAdded += 1;
            }
        return filesAdded;
    }
    public ArrayList<FileEntry> getFilesList() { return filesList; }
    public void setFilesList(ArrayList<FileEntry> filesList) { this.filesList = filesList; }
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

    public ServerThread(int ports, int nThreads, String path, ArrayList<String> hdd){
        this.port = ports;
        this.nThreads = nThreads;
        this.path = path;
        this.hdd = hdd;

        //not sure if necessary
        filesList = new ArrayList<>();
        users = new ArrayList<>();
        usersOnline = new ArrayList<>();
        //

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
                pool.execute(new Connection(listener.accept(), this));
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

        ArrayList<String> hdd;

        private Controller c;
        private ServerThread s;
        private int waitTime;

        public FileListUpdater(ArrayList<String> hdd, Controller c, ServerThread s, int waitTime) {
            this.hdd = hdd;
            this.c = c;
            this.s = s;
            this.waitTime = waitTime;
            loop = true;
            //start();
        }

        @Override
        public void run() {
            while(loop){
                ArrayList<FileEntry> listNew = new ArrayList<>();
                for(int i = 1; i<= 5; i+=1) {
                    listNew.addAll(updateList(i));
                }
                //System.out.println("New files list : \n" + listNew.size());
                s.addDistinct(listNew);

                //--------------------------------------------------------------------update list of online users in gui
                ObservableList<String> usersOnlineGui = FXCollections.observableArrayList();
                usersOnlineGui.addAll(s.getUsersOnline());
                c.updateUsersOnline(usersOnlineGui);

                //System.out.println("Global files list : \n" + s.filesList.size());
                try{
                    Thread.sleep(waitTime*1000);
                }catch(InterruptedException e){}
            }
        }
        private Collection<FileEntry> updateList(int hddNo){
            ArrayList<String> listHdd = listFilesForFolder(new File(hdd.get(hddNo-1)));
            ObservableList<FileEntry> listForGui = FXCollections.observableArrayList();

            for(String a : listHdd){
                //if file is already on global list, skip



                File file = new File(hdd.get(hddNo-1) + "\\" + a);
                if (!file.exists() || !file.isFile()) continue;
                listForGui.add(
                        new FileEntry(
                                a,
                                hddNo,
                                s.path + "\\" + hdd.get(hddNo-1) + "\\" + a, file.length(),
                                "SERVER")
                );
            }
            c.updateFiles(listForGui, hddNo);
            return listForGui;
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
    private ServerThread server;

    ArrayList<String> hddPaths;

    Connection(Socket socket, ServerThread server){
        this.socket = socket;
        this.server = server;
        hddPaths = server.getHdd();
        //this.start();
        if (t == null) {
            t = new Thread(this);
            //t.start();
        }
        System.out.println(socket.getPort() + "> Connection thread started");
    }

    @Override
    public void run() {
        int n = 0;
        System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ "> Has connected to server");n++;

        PrintWriter out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ "> IOException: failed to get output stream from client");n++;
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
                    //System.out.println(socket.getPort() +"\\"+ t.getId() +"\\"+n+ "> " + temp);n++;
                    if(temp.matches("file [\\w-_()']+\\.[A-Za-z0-9]{3} [\\d]+ [\\w-_]+")){
                        //------------------------------------------------------------------user sends file
                        int hddNo = 0;//hardcoded for now
                        String[] a = temp.split(" ");
                        String filename = a[1];
                        int filesize = Integer.parseInt(a[2]);
                        String username = a[3];
                        String path = hddPaths.get(hddNo)+"\\"+filename;

                        //add entry to global file list
                        server.getFilesList().add(new FileEntry(filename, hddNo, path, filesize, username));

                        System.out.println(socket.getPort() +"\\"+ t.getId() +"\\"+n+">receiving file : " + filename + " from: " + username);n++;
                        try {
                            //System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+"> " + filename + " client: "+in.nextLine());
                            DataInputStream dis = new DataInputStream(input);
                            FileOutputStream fos = new FileOutputStream(path);
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
                    }else
                    if(temp.matches("list [\\w-_]+")){
                        //----------------------------------------------------------------- user wants list of his files
                        String[] a = temp.split(" ");
                        String username = a[1];
                        //get list of files user has rights to
                        ArrayList<FileEntry> files = server.getUserFilesList(username);
                        try {
                            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                            objectOutput.writeObject(files);
                        } catch (Exception e) {
                            System.out.println(socket.getPort() +"\\"+ t.getId() + "\\"+n+ ">list");n++;
                        }
                    }else
                    if(temp.matches("getfile [\\w-_()']+\\.[A-Za-z0-9]{3}")){
                        String[] a = temp.split(" ");
                        //System.out.println("a0: " + a[0]);
                        //System.out.println("a1: " + a[1]);//filename
                        String filename = a[1];
                        String filePath = hddPaths.get(0)+"\\"+filename;
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
                    }else
                    if(temp.matches("login [\\w-_]+")){
                        //user login
                        String[] a = temp.split(" ");
                        boolean logFlag = true;
                        for(Iterator<String> i = server.getUsersOnline().iterator();i.hasNext();){
                            if(i.next().matches(a[1])){
                                //user is already logged in
                                logFlag = false;
                                break;
                            }
                        }
                        if(logFlag)
                            server.getUsersOnline().add(a[1]);

                        System.out.println("User " + a[1] + " has logged in");
                    }else
                    if(temp.matches("logout [\\w-_]+")){
                        //user logout
                        String[] a = temp.split(" ");
                        for(Iterator<String> i = server.getUsersOnline().iterator();i.hasNext();){
                            if(i.next().matches(a[1])){
                                server.getUsersOnline().remove(i);
                                break;
                            }
                        }
                        System.out.println("User " + a[1] + " has logged out");
                    }else
                        /*
                    if(temp.matches("share [\\w-_()']+\\.[A-Za-z0-9]{3} [\\w-_]+")){
                        String[] a = temp.split(" ");
                        String filename = a[1];
                        String username = a[2];
                        for(Iterator<FileEntry> i = server.getFilesList().iterator(); i.hasNext();){
                            FileEntry x = i.next();
                            if(x.getFilename().matches(filename)){
                                x.getOthers().add(username);
                            }
                        }
                    }else
                        */
                    if(temp.matches("exit")){
                        //loop = false;
                        break;
                    }else {
                        System.out.println(socket.getPort() +"\\"+ t.getId() + " <no match> ");
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
