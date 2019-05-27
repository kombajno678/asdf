package client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
//import java.util.concurrent.ThreadLocalRandom;

import server.FileEntry;

class ClientThread {
    public ClientThread(){}

    static class BackgroundTasks implements Runnable{
        private Thread t;
        private boolean loop;
        private ArrayList<FileEntry> filesLocal = new ArrayList<>();
        private ArrayList<FileEntry> filesServer = new ArrayList<>();
        private String localFolder, username;
        private Controller c;
        private Socket socket;
        private PrintWriter out;
        private String ip;
        private int port;
        private int waitTime = 5;
        public BackgroundTasks(String localFolder, String username, String ip, int port, Controller c) {
            this.localFolder = localFolder;
            this.username = username;
            this.ip = ip;
            this.port = port;
            this.c = c;
            loop = true;
            if(t == null){
                this.t = new Thread(this);
                t.start();
            }
        }

        @Override
        public void run() {
            System.out.println("BG thread srtared");
            if(socket == null)createSocket();
            if(login()){
                c.printText(" connected and logged in as: " + username);
            }
            int n = 2;
            while (loop) {
                n--;
                if(socket == null)createSocket();
                //-------------------------------------------read files from hdd
                ArrayList<FileEntry> tempLocal = getFilesHdd();

                //-------------------------------------------update ListLocal
                //todo: if file is deleted from folder, its still in fileslocal list
                //remove from temp entries that are already in filesLocal (temp - filesLocal)
                if(filesLocal.size() == 0){//if filesLocal is empty just add all from temp
                    filesLocal.addAll(tempLocal);
                }else {
                    for (Iterator<FileEntry> i = tempLocal.iterator(); i.hasNext(); ) {
                        FileEntry fe = i.next();
                        for (FileEntry fold : filesLocal) {
                            if (fe.getFilename().matches(fold.getFilename())) {
                                i.remove();
                            }
                        }
                    }
                    //add what is left in temp to filesLocal
                    if(tempLocal.size() > 0)
                        filesLocal.addAll(tempLocal);
                }


                //------------------------------------------get list of files from server

                ArrayList<FileEntry> tempServer = new ArrayList<>();
                if(socket != null) {
                    tempServer = getFilesServer();
                    //update filesServer
                    filesServer = tempServer;
                }



                //update owners n others in local lost
                for(int i_local = 0; i_local < filesLocal.size(); i_local++){
                    String name = filesLocal.get(i_local).getFilename();
                    for(FileEntry fs: filesServer){
                        if(fs.getFilename().matches(name)){
                            //found local file on filesServer, now update owner and stuff
                            filesLocal.get(i_local).setOwner(fs.getOwner());
                            filesLocal.get(i_local).setOthers(fs.getOthers());
                            filesLocal.get(i_local).setHddNo(fs.getHddNo());
                            filesLocal.get(i_local).setStatus("local+server");
                            break;
                        }

                    }

                }


                //create list for gui
                ArrayList<FileEntry> listForGui = new ArrayList<>();
                listForGui.addAll(filesLocal);
                for(FileEntry fs : filesServer){
                    boolean both = false;
                    for(FileEntry fg : listForGui){
                        if(fg.getFilename().matches(fs.getFilename())){
                            //is both local and server
                            fs.setStatus("local+server");
                            both = true;
                            break;
                        }
                    }
                    if(!both)listForGui.add(fs);
                }
                //listForGui.addAll(filesServer);

                //send list to gui
                c.updateFiles(listForGui);

                //create list of files to download/upload
                ArrayList<FileEntry> downloadList = getDownloadList();
                ArrayList<FileEntry> uploadList = getUploadList();

                printList(filesLocal, "filesLocal");
                printList(filesServer, "filesServer");
                printList(listForGui, "listForGui");

                if(n <= 0){
                    n = 5;
                    if(downloadList.size() > 0){
                        printList(downloadList, "downloadList");
                        download(downloadList);
                    }
                    if(uploadList.size() > 0){
                        printList(uploadList, "uploadList");
                        upload(uploadList);
                    }
                }
                System.out.println("-------------------------------------------------- "+n);
                try{
                    Thread.sleep(waitTime * 1000);
                }catch(InterruptedException e){}

            }
            logout();
            closeSocket();
            System.out.println("BG thread ended");
        }
        public void stop(){
            loop = false;
            t.interrupt();
        }
        private void printList(ArrayList<FileEntry> list, String name){
            System.out.print(name + "{"+list.size()+"} : ");
            for(FileEntry f : list){
                System.out.print(f.getFilename() + ":" +f.getStatus() + ":"+f.getOthers()+", ");
            }
            System.out.println();
        }
        private void createSocket(){
            //do {
                //if(!loop)break;
                try {
                    socket = new Socket(ip, port);
                } catch (Exception e) {
                    //if(!loop)break;
                    System.out.println("BG> Can't create socket.");
                    //c.printText("CheckForNewLocalFiles> Can't connect to server. Trying again in 10s");

                }
            //} while (socket == null);
            if(socket != null){
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException e) {
                    System.out.println("BG> Failed to get output stream from server");
                }
            }
        }
        private void closeSocket(){
            if(socket != null){
                out.println("exit");
                try{
                    socket.close();
                }catch(Exception e){

                }
            }
        }
        private boolean login(){
            if(socket == null)return false;
            out.println("login "+username);
            return true;
        }
        private boolean logout(){
            if(socket == null)return false;
            out.println("logout "+username);
            return true;
        }
        private ArrayList<FileEntry> getFilesHdd(){
            File folder = new File(localFolder);
            ArrayList<FileEntry> list = new ArrayList<>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    //if (fileEntry.isDirectory()) {
                    //   listFilesForFolder(fileEntry);
                    //} else {
                        String filename = fileEntry.getName();
                        String path = localFolder + File.separator + File.separator + filename;
                        String owner = "";//don't know who is the owner of the file yet
                        String status = "local";
                        long size = fileEntry.length();
                        list.add(new FileEntry(filename, path, size, owner, status));
                    //}
                }
            }catch (NullPointerException e){

            }
            return list;
        }
        private ArrayList<FileEntry> getFilesServer(){
            ArrayList<FileEntry> list = new ArrayList<>();
            if(socket == null)
                return list;
            out.println("list " + username);
            try {
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                try {
                    Object object = objectInput.readObject();
                    list = (ArrayList<FileEntry>) object;
                } catch (ClassNotFoundException e) {
                    //e.printStackTrace();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            return list;
        }
        private ArrayList<FileEntry> getDownloadList(){
            ArrayList<FileEntry> list = (ArrayList<FileEntry>)filesServer.clone();
            for(Iterator<FileEntry> i = list.iterator();i.hasNext();){
                FileEntry f = i.next();
                for(FileEntry fs : filesLocal){
                    if(fs.getFilename().matches(f.getFilename())){
                        //same filename
                        i.remove();
                        break;
                    }
                }
            }
            return list;
        }
        private ArrayList<FileEntry> getUploadList(){
            ArrayList<FileEntry> list = (ArrayList<FileEntry>)filesLocal.clone();
            for(Iterator<FileEntry> i = list.iterator();i.hasNext();){
                FileEntry f = i.next();
                for(FileEntry fs : filesServer){
                    if(fs.getFilename().matches(f.getFilename())){
                        //same filename
                        i.remove();
                        break;
                    }
                }
            }
            return list;
        }
        public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException ex) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        private int download(ArrayList<FileEntry> list){
            if(socket == null)return -1;
            int n = list.size();
            System.out.println("download> files to download:"+list);
            //create thread pool
            ExecutorService poolDownload = Executors.newFixedThreadPool(n);
            System.out.println("download> Downloading "+n+" files ...");
            //c.printText("Updater> Downloading new files ...");
            Iterator<FileEntry> i = list.iterator();
            //List<Callable<Object>> todo = new ArrayList<>(n);
            while (i.hasNext()) {
                FileEntry f = i.next();
                poolDownload.execute(new ClientThread.FileDownloadClass(c, ip, port, username, f.getFilename(), localFolder));
                f.setStatus("local+server");//probs wont work :(
                try{
                    t.sleep(100);
                }catch(InterruptedException e){

                }
            }
            System.out.println("download> Waiting for "+n+" files to download ... ");
            awaitTerminationAfterShutdown(poolDownload);
            System.out.println("download> Downloaded "+n+" files!");
            return n;
        }
        private int upload(ArrayList<FileEntry> list){
            if(socket == null)return -1;
            int n = list.size();
            System.out.println("upload> files to upload:"+list);
            //create thread pool
            ExecutorService poolUpload = Executors.newFixedThreadPool(n);
            System.out.println("upload> uploading "+n+" files ...");
            //c.printText("Updater> Downloading new files ...");
            Iterator<FileEntry> i = list.iterator();
            List<Callable<Object>> todo = new ArrayList<>(n);
            while (i.hasNext()) {
                FileEntry f = i.next();
                poolUpload.execute(new ClientThread.FileUploadClass(c, ip, port, f.getFilename(), localFolder, username));
                f.setStatus("local+server");//probs wont work :(
                try{
                    t.sleep(100);
                }catch(InterruptedException e){

                }
            }
            System.out.println("upload> Waiting for "+n+" files to upload ... ");
            awaitTerminationAfterShutdown(poolUpload);
            System.out.println("upload> uploaded "+n+" files!");
            return n;
        }

    }


    static class FileDownloadClass implements Runnable{
        private Thread t;
        private String filename;
        private String ip;
        private int port;
        private String destination, username;
        private Controller c;
        FileDownloadClass(Controller c, String ip, int port, String username, String fn, String dest) {
            this.ip = ip;
            this.port = port;
            filename = fn;
            destination = dest;
            this.username = username;
            this.c = c;
            //this.start();
            if (t == null) {
                t = new Thread (this);
                //t.start ();
            }
        }
        public void run(){
            System.out.println(t.getId() + "\\" +filename + " downloader thread start");
            Socket socketFile = null;
            do{
                try{
                    socketFile = new Socket(ip, port);
                }catch(Exception e){
                    System.out.println(t.getId() + "\\" +filename + " Can't connect to server. Trying again in 1s");
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException ie){
                    }
                }
            }while(socketFile == null);

            PrintWriter out = null;
            Scanner in = null;
            try{
                out = new PrintWriter(socketFile.getOutputStream(), true);
                in = new Scanner(socketFile.getInputStream());
            }catch (IOException e){
                System.out.println("IOException: failed to get output stream from server");
            }
            //File f = new File(destination+"\\"+filename);

            out.println("getfile " + filename + " " + username);
            int size = Integer.parseInt(in.nextLine());

            System.out.println(t.getId() + "\\" +filename+ " Downloading "+size+"B to "+destination+" ... ");
            c.printText("Downloading "+filename+" ("+size+"B) ... ");
            try {
                DataInputStream dis = new DataInputStream(socketFile.getInputStream());
                FileOutputStream fos = new FileOutputStream(destination+"\\"+filename);
                byte[] buffer = new byte[1024*64];
                int filesize = size;
                int read = 0;
                int totalRead = 0;
                int remaining = filesize;
                while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0 && remaining > 0) {
                    totalRead += read;
                    remaining -= read;
                    //System.out.println("read " + totalRead + " bytes.");
                    fos.write(buffer, 0, read);
                }
                System.out.println(filename+ " read " + totalRead + "B of "+ size);
                out.println("received " + filename);
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 10000));
                }catch(Exception e){}

                if(socketFile.isClosed()){
                    System.out.println(filename + " socket closed");
                }else{
                    fos.close();
                    dis.close();
                }
            }catch(Exception e){
                System.out.println("Exception in FileDownloaderClass : " + e.getMessage());
            }
            try{
                socketFile.close();
            }catch(IOException e){
                System.out.println("socketFile: failed to close");
            }
            c.printText("file \""+ filename +"\" downloaded");
            System.out.println("file \""+ filename +"\" downloaded");

        }
        /*public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }*/
    }
    static class FileUploadClass implements Runnable{
        public Thread t;
        String filename, localFolder, username;
        String ip;
        int port;
        Controller c;
        FileUploadClass(Controller c, String i, int p, String fn, String localFolder, String username) {
            ip = i;
            port = p;
            filename = fn;
            this.c = c;
            this.localFolder = localFolder;
            this.username = username;
            //this.start();
            if (t == null) {
                t = new Thread (this);
                //t.start ();
            }
        }
        public void run(){
            System.out.println(t.getId() + "\\" +filename + " sender thread start");
            Socket socketFile = null;
            do{
                try{
                    socketFile = new Socket(ip, port);
                }catch(Exception e){
                    System.out.println("socketFile : Can't connect to server. Trying again in 1s");
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException ie){}
                }
            }while(socketFile == null);
            PrintWriter out = null;
            Scanner in = null;
            try{
                out = new PrintWriter(socketFile.getOutputStream(), true);
                in = new Scanner(socketFile.getInputStream());
            }catch (IOException e){
                System.out.println(t.getId() + "\\" +"IOException: failed to get output stream from server");
            }
            File f = new File(localFolder + "\\" +filename);
            int fsize = (int)f.length();

            //out.close();
            c.printText("Sending file: " + filename +" ("+ fsize + "B) ...");
            System.out.println("file " + filename + " " + fsize + " " + username);
            out.println("file " + filename + " " + fsize + " " + username);
            DataOutputStream dos;
            FileInputStream fis;
            try {
                dos = new DataOutputStream(socketFile.getOutputStream());
                fis = new FileInputStream(localFolder + "\\" +filename);

                byte[] buffer = new byte[1024*64];
                while (fis.read(buffer) > 0) {
                    dos.write(buffer);
                }
                try {
                    Thread.sleep(10000);
                }catch(Exception e){}

                System.out.println(t.getId() + "\\" +filename + " waiting for confirmation from server ... ");
                //if(in.hasNextLine()){
                //out.println("sent");
                System.out.println(t.getId() + "\\" +filename + " server: "+in.nextLine());
                if(socketFile.isClosed()){
                    System.out.println(filename + " socket closed");
                }else{
                    fis.close();
                    dos.close();
                }

                c.printText("File "+ filename +" sent to server!");
                c.setFileStatus(filename, true);

            }catch(Exception e){
                System.out.println(t.getId() + "\\" +"Exception in FileSenderClass : " + e.getMessage());
            }
            //
            try{
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 10000));
            }catch(InterruptedException e){}
            try{
                socketFile.close();
            }catch(IOException e){
                System.out.println(t.getId() + "\\" +"socketFile: failed to close");
            }
            System.out.println(t.getId() + "\\" +filename + " sender thread stop");
        }
        /*public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }*/
    }
}
/*
    static class CheckForNewLocalFiles implements Runnable{
        private Thread t;
        boolean loop = true;
        int waitTime;
        Socket socket;
        String localFolder;
        String ip;
        String username;
        int port;
        Controller c;

        public CheckForNewLocalFiles(Controller controller,String ip, int port, int waitTimeSeconds, String localFolder, String username) {
            this.ip = ip;
            this.port = port;
            socket = null;
            waitTime = waitTimeSeconds * 1000;
            this.localFolder = localFolder;
            this.username = username;
            c = controller;
            this.start();
        }

        public void run() {
            loop = true;
            do {
                if(!loop)break;
                try {
                    socket = new Socket(ip, port);
                } catch (Exception e) {
                    if(!loop)break;
                    System.out.println("CheckForNewLocalFiles> Can't connect to server. Trying again in 10s");
                    c.printText("CheckForNewLocalFiles> Can't connect to server. Trying again in 10s");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        if(!loop)break;
                    }
                }
            } while (socket == null);
            PrintWriter out = null;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Updater> IOException: failed to get output stream from server");
            }


            while(loop){

                //System.out.println("Checker> Checking for new local files ... ");
                //c.printText("Checker> Checking for new local files ... ");
                ArrayList<FileEntry> listFilesLocal = listFilesForFolder(new File(localFolder));
                //------------------------------------------------------------------------------ update new files on gui
                ObservableList<FileEntry> listForGui = FXCollections.observableArrayList();
                out.println("list " + username);
                ArrayList<FileEntry> listFilesOnServer;
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                    try {
                        Object object = objectInput.readObject();
                        listFilesOnServer = (ArrayList<FileEntry>) object;
                    } catch (ClassNotFoundException e) {
                        continue;
                        //e.printStackTrace();
                    }
                } catch (IOException e) {
                    continue;
                    //e.printStackTrace();
                }

                //add files to gui list
                for(FileEntry f : listFilesLocal){
                    if(listFilesOnServer.contains(f)){
                        f.setStatus("on server");
                    }else
                        f.setStatus("local");

                    listForGui.add(f);
                }

                try{
                    c.updateFiles(listForGui);
                    Thread.sleep(waitTime);
                }catch(Exception e){
                    System.out.println("Checker> " + e.getMessage());
                }

            }
            out.println("exit");
            try{
                socket.close();
            }catch(Exception e){

            }
            System.out.println("Checker> Thread closed");
        }
        public void start() {
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }

        public void stop() {
            loop = false;
            t.interrupt();
        }

        public ArrayList<FileEntry> listFilesForFolder(final File folder) {
            ArrayList<FileEntry> list = new ArrayList<>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        listFilesForFolder(fileEntry);
                    } else {
                        String filename = fileEntry.getName();
                        String path = localFolder + File.separator + File.separator + filename;
                        String owner = username;
                        long size = fileEntry.length();
                        list.add(new FileEntry(filename, path, size, owner));
                    }
                }
            }catch (NullPointerException e){

            }
            return list;
        }
    }
    static class UpdateFilesFromServerClass implements Runnable {
        private Thread t;
        boolean loop;
        int waitTime;
        Socket socket;
        String localFolder;
        String ip;
        String username;
        int port;
        Controller c;
        UpdateFilesFromServerClass(Controller controller,String ip, int port, int waitTimeSeconds, String localFolder, String username) {
            this.ip = ip;
            this.port = port;
            socket = null;
            waitTime = waitTimeSeconds * 1000;
            this.localFolder = localFolder;
            this.username = username;
            c = controller;
            this.start();
        }

        public void run() {
            loop = true;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if(!loop)return;
                System.out.println("Updater> thread interrupted while sleeping");
            }
            do {
                if(!loop)return;
                try {
                    socket = new Socket(ip, port);
                } catch (Exception e) {
                    if(!loop)return;
                    System.out.println("Updater> Can't connect to server. Trying again in 10s");
                    c.printText("Updater> Can't connect to server. Trying again in 10s");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {}
                }
            } while (socket == null);


            PrintWriter out = null;
            //---------------------------------------------------------------initial sleep

            while (loop) {

                //------------------------------------------ask server if any new files for me
                System.out.println("Updater> Asking server if there are any new files ... ");
                //c.printText("Updater> Asking server if there are any new files ... ");

                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException e) {
                    System.out.println("Updater> IOException: failed to get output stream from server");
                    continue;
                    //c.printText("Updater> IOException: failed to get output stream from server");
                }
                out.println("list " + username);
                ArrayList<FileEntry> listFilesOnServer;
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                    try {
                        Object object = objectInput.readObject();
                        listFilesOnServer = (ArrayList<FileEntry>) object;
                    } catch (ClassNotFoundException e) {
                        System.out.println("Updater> ClassNotFoundException");
                        continue;
                        //e.printStackTrace();
                    }
                } catch (IOException e) {
                    System.out.println("Updater> IOException");
                    continue;
                    //e.printStackTrace();
                }
                System.out.println("Updater> listFilesOnServer:"+listFilesOnServer);

                ArrayList<FileEntry> listFilesLocal = listFilesForFolder(new File(localFolder));
                System.out.println("Updater> listFilesLocal:"+listFilesLocal);
                ArrayList<FileEntry> listFilesOnServerCopy = (ArrayList<FileEntry>)listFilesOnServer.clone();

                listFilesOnServer.removeAll(listFilesLocal);
                //if so, ------------------------------------------------------download new files

                int numberOfFilesToDownload = listFilesOnServer.size();
                if (numberOfFilesToDownload > 0) {
                    System.out.println("Updater> files to download:"+listFilesOnServer);
                    //create thread pool
                    ExecutorService poolDownload = Executors.newFixedThreadPool(numberOfFilesToDownload);
                    System.out.println("Updater> Downloading "+numberOfFilesToDownload+" files ...");
                    //c.printText("Updater> Downloading new files ...");
                    Iterator<FileEntry> i = listFilesOnServer.iterator();
                    List<Callable<Object>> todo = new ArrayList<>(numberOfFilesToDownload);
                    while (i.hasNext()) {
                        try {
                            poolDownload.execute(new ClientThread.FileDownloaderClass(c, ip, port, i.next().getFilename(), localFolder));
                            t.sleep(100);
                        } catch (Exception e) {
                            System.out.println("Failed to add new connection");
                            continue;
                           // c.printText("Updater> Failed to add new connection");
                        }
                    }
                    System.out.println("Updater> Waiting for "+numberOfFilesToDownload+" files to download ... ");
                    try {
                        //poolUpload.awaitTermination(600, TimeUnit.SECONDS);
                        List<Future<Object>> answers = poolDownload.invokeAll(todo);
                    } catch (InterruptedException e) {

                    }
                    System.out.println("Updater> Downloaded "+numberOfFilesToDownload+" files!");

                }
                //if so, ------------------------------------------------------upload new files
                listFilesLocal.removeAll(listFilesOnServerCopy);
                int numberOfFilesTpUpload = listFilesLocal.size();
                if (numberOfFilesTpUpload > 0) {
                    System.out.println("Updater> files to upload:"+listFilesLocal);
                    //create thread pool?
                    ExecutorService poolUpload = Executors.newFixedThreadPool(numberOfFilesTpUpload);
                    System.out.println("Updater> Uploading "+numberOfFilesTpUpload+" files ...");
                    //c.printText("Updater> Uploading new files ...");
                    Iterator<FileEntry> i = listFilesLocal.iterator();
                    List<Callable<Object>> todo = new ArrayList<Callable<Object>>(numberOfFilesTpUpload);
                    while (i.hasNext()) {
                        try {
                            poolUpload.execute(new ClientThread.FileSenderClass(c, ip, port, i.next().getFilename(), localFolder, username));
                            t.sleep(100);
                        } catch (Exception e) {
                            System.out.println("Failed to add new connection");
                            continue;
                            //c.printText("Updater> Failed to add new connection");
                        }
                    }
                    System.out.println("Updater> Waiting for "+numberOfFilesTpUpload+" files to upload");

                    try {
                        //poolUpload.awaitTermination(600, TimeUnit.SECONDS);
                        List<Future<Object>> answers = poolUpload.invokeAll(todo);
                    } catch (InterruptedException e) {

                    }
                    System.out.println("Updater> Uploaded "+numberOfFilesTpUpload+" files!");
                }
                //---------------------------------------------------------------go to sleep
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    System.out.println("Updater> thread interrupted while sleeping");
                    //c.printText("Updater> thread interrupted while sleeping");
                    continue;
                }

            }
            System.out.println("Updater> Thread stopped");
            //c.printText("Updater> Thread stopped");
            out.println("exit");
            try{
                socket.close();
            }catch(Exception e){

            }
        }

        public void start() {
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }

        public void stop() {
            loop = false;
            t.interrupt();
        }

        public ArrayList<FileEntry> listFilesForFolder(final File folder) {
            ArrayList<FileEntry> list = new ArrayList<>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        listFilesForFolder(fileEntry);
                    } else {
                        String filename = fileEntry.getName();
                        String path = localFolder + File.separator + File.separator + filename;
                        String owner = username;
                        long size = fileEntry.length();
                        list.add(new FileEntry(filename, path, size, owner));
                    }
                }
            }catch (NullPointerException e){

            }
            return list;
        }
    }


    */