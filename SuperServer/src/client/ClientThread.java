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

class ClientThread {
    public ClientThread(){}
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
                if(!loop)return;
                try {
                    socket = new Socket(ip, port);
                } catch (Exception e) {
                    if(!loop)return;
                    System.out.println("CheckForNewLocalFiles> Can't connect to server. Trying again in 10s");
                    c.printText("CheckForNewLocalFiles> Can't connect to server. Trying again in 10s");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        if(!loop)return;
                    }
                }
            } while (socket == null);
            PrintWriter out = null;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                if(!loop)return;
                System.out.println("Updater> IOException: failed to get output stream from server");
            }


            while(loop){

                //System.out.println("Checker> Checking for new local files ... ");
                //c.printText("Checker> Checking for new local files ... ");
                ArrayList<String> listFilesLocal = listFilesForFolder(new File(localFolder));
                //--------------------------------------------------------------------------------update new files on gui
                ObservableList<FileEntry> listForGui = FXCollections.observableArrayList();
                out.println("list " + username);
                ArrayList<String> listFilesOnServer;
                listFilesOnServer = new ArrayList<String>();
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                    try {
                        Object object = objectInput.readObject();
                        listFilesOnServer = (ArrayList<String>) object;
                    } catch (ClassNotFoundException e) {
                        continue;
                        //e.printStackTrace();
                    }
                } catch (IOException e) {
                    continue;
                    //e.printStackTrace();
                }

                for(String a : listFilesLocal){
                    if(listFilesOnServer.contains(a)){
                        listForGui.add(new FileEntry(a, username, "on server"));
                    }else
                    listForGui.add(new FileEntry(a, username, "local"));
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
            /*try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("Updater> thread interrupted while sleeping");
                c.printText("Updater> thread interrupted while sleeping");

            }*/
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
                ArrayList<String> listFilesOnServer;
                listFilesOnServer = new ArrayList<String>();
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                    try {
                        Object object = objectInput.readObject();
                        listFilesOnServer = (ArrayList<String>) object;
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

                ArrayList<String> listFilesLocal = listFilesForFolder(new File(localFolder));
                System.out.println("Updater> listFilesLocal:"+listFilesLocal);
                ArrayList<String> listFilesOnServerCopy = (ArrayList<String>)listFilesOnServer.clone();

                listFilesOnServer.removeAll(listFilesLocal);
                //if so, ------------------------------------------------------download new files

                int numberOfFilesToDownload = listFilesOnServer.size();
                if (numberOfFilesToDownload > 0) {
                    System.out.println("Updater> files to download:"+listFilesOnServer);
                    //create thread pool?
                    ExecutorService poolDownload = Executors.newFixedThreadPool(numberOfFilesToDownload);
                    System.out.println("Updater> Downloading "+numberOfFilesToDownload+" files ...");
                    //c.printText("Updater> Downloading new files ...");
                    Iterator<String> i = listFilesOnServer.iterator();
                    List<Callable<Object>> todo = new ArrayList<Callable<Object>>(numberOfFilesToDownload);
                    while (i.hasNext()) {
                        try {
                            poolDownload.execute(new ClientThread.FileDownloaderClass(c, ip, port, i.next(), localFolder));
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
                    } catch (InterruptedException e) {}
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
                    Iterator<String> i = listFilesLocal.iterator();
                    List<Callable<Object>> todo = new ArrayList<Callable<Object>>(numberOfFilesTpUpload);
                    while (i.hasNext()) {
                        try {
                            poolUpload.execute(new ClientThread.FileSenderClass(c, ip, port, i.next(), localFolder));
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
                    } catch (InterruptedException e) {}
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

        public ArrayList<String> listFilesForFolder(final File folder) {
            ArrayList<String> list = new ArrayList<String>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        listFilesForFolder(fileEntry);
                    } else {
                        //System.out.println(fileEntry.getName());
                        list.add(fileEntry.getName());
                    }
                }
            }catch (NullPointerException e){

            }
            return list;
        }
    }

    static class FileDownloaderClass implements Runnable{
        private Thread t;
        String filename;
        String ip;
        int port;
        String destination;
        Controller c;
        FileDownloaderClass(Controller c, String ip, int port, String fn, String dest) {
            this.ip = ip;
            this.port = port;
            filename = fn;
            destination = dest;
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

            out.println("getfile " + filename);
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
                    Thread.sleep(1000);
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
            System.out.println("file \""+ filename +"\" downloaded");
            c.printText("file \""+ filename +"\" downloaded");
        }
        /*public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }*/
    }
    static class FileSenderClass implements Runnable{
        public Thread t;
        String filename, localFolder;
        String ip;
        int port;
        Controller c;
        FileSenderClass(Controller c, String i, int p, String fn, String localFolder) {
            ip = i;
            port = p;
            filename = fn;
            this.c = c;
            this.localFolder = localFolder;
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
            out.println("file " + filename + " " + fsize);
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
                    Thread.sleep(1000);
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
