package client;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
//import java.util.concurrent.ThreadLocalRandom;

import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import server.FileEntry;

class ClientThread {
    public ClientThread(){}

    static class BackgroundTasks implements Runnable{
        private Thread t;
        private boolean loop;
        private ArrayList<FileEntry> filesLocal = new ArrayList<>();
        private ArrayList<FileEntry> filesServer = new ArrayList<>();
        private ArrayList<FileEntry> listForGui = new ArrayList<>();
        private String localFolder, username;
        private Controller c;
        private Socket socket;
        private PrintWriter out;
        private Scanner in;
        private String ip;
        private int port;
        private int waitTime = 5;
        private boolean loggedIn = false;

        BackgroundTasks(String localFolder, String username, String ip, int port, Controller c) {
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
            loggedIn = login();
            if(loggedIn){
                c.printText("Connected and logged in as: " + username);
            }else{
                c.printText("Trying to connect to server ... ");
            }
            new File(localFolder + File.separator + File.separator + username).mkdirs();
            int n = 2;
            while (loop) {
                n--;
                if(socket == null)createSocket();
                if(socket == null || socket.isClosed()){
                    c.printText("Can't connect to server");
                }else{
                    if(!loggedIn){
                        loggedIn = login();
                        if(loggedIn){
                            c.printText("Connected and logged in as: " + username);
                        }else{
                            c.printText("Trying to connect to server ... ");
                        }
                    }
                }

                //-------------------------------------------read files from hdd
                filesLocal = getFilesHdd();
                //------------------------------------------get list of files from server
                if(socket != null) {
                    filesServer = getFilesServer();
                }
                ArrayList<FileEntry> filesServerCopy = (ArrayList<FileEntry>)filesServer.clone();

                //update owners n others in local list

                for(int i_local = 0; i_local < filesLocal.size(); i_local++){
                    String name = filesLocal.get(i_local).getFilename();
                    String owner = filesLocal.get(i_local).getOwner();
                    for(Iterator<FileEntry> i_server = filesServerCopy.iterator(); i_server.hasNext();){
                        FileEntry fs = i_server.next();
                        if(fs.getFilename().equals(name) && fs.getOwner().equals(owner)){
                            //found local file on filesServerCopy, now update owner and stuff



                            filesLocal.get(i_local).setOthers(fs.getOthers());
                            filesLocal.get(i_local).setHddNo(fs.getHddNo());
                            filesLocal.get(i_local).setStatus("local + server");
                            //delete entry from filesServerCopy
                            i_server.remove();
                            break;
                        }
                    }
                }


                //create list for gui
                listForGui.clear();
                listForGui.addAll(filesLocal);
                //add files that are only on server
                listForGui.addAll(filesServerCopy);
                //send list to gui
                c.updateFiles(listForGui);

                //filesServer = filesServerCopy;

                //create list of files to download/upload
                ArrayList<FileEntry> downloadList = getDownloadList();
                ArrayList<FileEntry> uploadList = getUploadList();

                printList(filesLocal, "filesLocal");
                printList(filesServer, "filesServer");
                printList(listForGui, "listForGui");

                if(n <= 0){
                    c.setTextLeft("Syncing with server ... downloading ");
                    n = 5;
                    if(downloadList.size() > 0 && socket != null){
                        printList(downloadList, "downloadList");
                        download(downloadList);
                    }
                    c.setTextLeft("Syncing with server ... uploading ");
                    if(uploadList.size() > 0  && socket != null){
                        printList(uploadList, "uploadList");
                        upload(uploadList);
                    }
                }
                System.out.println("-------------------------- sync with server in "+n*waitTime+"s --------------------");
                c.setTextLeft("Next sync with server in "+n*waitTime+"s");
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
        public ArrayList<String> getUsersFromServer(){
            ArrayList<String> list = null;
            try {
                out.println("getusers");
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                try {
                    Object object = objectInput.readObject();
                    list = (ArrayList<String>) object;
                } catch (ClassNotFoundException e) {
                    //e.printStackTrace();
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            return list;
        }
        public void unshare(FileEntry f){
            System.out.println("file to unshare : "+f);
            if(f.getOwner().equals(username)){
                //open new window and ask from whom unshare this file
                List<String> choices = f.getOthers();
                if(choices.size() > 0){
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(null, choices);
                    dialog.setTitle("Unshare file");
                    dialog.setHeaderText("Unshare file "+f.getFilename()+" from:");
                    dialog.setContentText("Choose user:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(letter -> {
                        System.out.println("Your choice: " + letter);
                        out.println("unshare "+f.getFilename() +" "+f.getOwner()+" "+letter);
                        //c.updateFilesForce(listForGui);
                    });
                }else{
                    infoDialog("SuperClient - info", "There's nobody from whom you can unshare this file.");
                }

            }else{
                infoDialog("SuperClient - info", "You can't share somebody else's file");
            }
        }
        public void share(FileEntry f){
            System.out.println("file to share : "+f);
            if(f.getOwner().equals(username)){
                //open new window and ask to whom share this file
                List<String> choices = getUsersFromServer();
                if(choices == null){
                    //cant get users from server
                    return;
                }
                choices.removeAll(f.getOthers());
                choices.remove(username);
                if(choices.size() > 0){
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(null, choices);
                    dialog.setTitle("Share file");
                    dialog.setHeaderText("Share file "+f.getFilename()+" to:");
                    dialog.setContentText("Choose user:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(letter -> {
                        System.out.println("Your choice: " + letter);
                        out.println("share "+f.getFilename() +" "+f.getOwner()+" "+letter);
                    });
                }else{
                    infoDialog("SuperClient - info", "There's nobody to share this file to.");
                }

            }else{
                infoDialog("SuperClient - info", "You can't share somebody else's file");
            }
        }
        public void delete(FileEntry f){
            //send delete command to server
            out.println("delete "+f.getFilename()+" "+f.getOwner());
            //get msg from server, if it deleted file
            String msg = "";
            if(in.hasNextLine()) {
                msg = in.nextLine();
            }
            if(msg.equals("deleted")){
                //delete file from hdd
                if(new File(f.getPath()).delete()){
                    //System.out.println("deleted successfully: "+f.getFilename());
                    c.printText("File: \""+f.getFilename()+"\" has been deleted");
                }else {
                    System.out.println("didn't delete: " + f.getFilename());
                    c.printText("Error: File: \""+f.getFilename()+"\" has not been deleted (could't delete file from local folder)");
                }
            }else{
                //server couldn't delete file
                c.printText("Error: File: \""+f.getFilename()+"\" has not been deleted (server could't delete file)");
            }
        }

        private void printList(ArrayList<FileEntry> list, String name){
            System.out.print(name + "{"+list.size()+"} : ");
            for(FileEntry f : list){
                System.out.print(f.getFilename() + ":" +f.getOwner() + ":"+f.getOthers()+"; ");
            }
            System.out.println();
        }
        private void createSocket(){
            try {
                socket = new Socket(ip, port);
            } catch (Exception e) {
                System.out.println("BG> Can't create socket.");
            }
            if(socket != null){
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new Scanner(socket.getInputStream());
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
            try{
                out.println("login "+username);
                return true;
            }catch(Exception e){
                return false;
            }
        }
        private void logout(){
            if(socket != null)out.println("logout "+username);
        }
        private ArrayList<FileEntry> getFilesHdd(){
            ArrayList<FileEntry> filesHdd = new ArrayList<>();

            ArrayList<String> listHdd = listFilesForFolder(new File(localFolder));
            System.out.println("listHdd: " + listHdd);
            for(String fPath : listHdd){
                //if(fPath.contains(csvFileName))continue;//ignore csv file
                File file = new File(fPath);
                if (!file.isFile()) {
                    continue;//file doesn't exist, should't happen though
                }else{
                    int size = (int)file.length();
                    //get owner/folder name
                    String pattern = Pattern.quote(System.getProperty("file.separator"));
                    String[] split = fPath.split(pattern);
                    //System.out.println("found file: "+split[split.length-1]+", owner: "+split[split.length-2]);
                    filesHdd.add(new FileEntry(
                            file.getName(),
                            0,
                            file.getPath().replace("\\", "\\\\"),
                            size,
                            split[split.length-2],
                            "local"));
                }
            }
            return filesHdd;
        }
        private ArrayList<String> listFilesForFolder(final File folder) {
            ArrayList<String> list = new ArrayList<String>();
            try {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        list.addAll(listFilesForFolder(fileEntry));
                    } else {
                        list.add(fileEntry.getPath());
                    }
                }
            } catch (NullPointerException e) {

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
                    if(fs.getFilename().equals(f.getFilename()) && fs.getOwner().equals(f.getOwner())){
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
            //files local - files on server
            //also skip files that you're not the owner of
            for(Iterator<FileEntry> i = list.iterator();i.hasNext();){
                FileEntry f = i.next();
                if(!f.getOwner().equals(username)){
                    //am not owner, no need to upload somebody's files
                    i.remove();
                    continue;
                }
                for(FileEntry fs : filesServer){
                    if(fs.getFilename().equals(f.getFilename()) && fs.getOwner().equals(f.getOwner())){
                        //same filename
                        i.remove();
                        break;
                    }
                }
            }
            return list;
        }
        private void awaitTerminationAfterShutdown(ExecutorService threadPool) {
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
            System.out.println("download> Files to download:"+list);
            //create thread pool
            ExecutorService poolDownload = Executors.newFixedThreadPool(n);
            System.out.println("download> Downloading "+n+" files ...");
            c.addTextLeft(n+"files ");
            Iterator<FileEntry> i = list.iterator();
            while (i.hasNext()) {
                FileEntry f = i.next();
                poolDownload.execute(new ClientThread.FileDownloadClass(c, ip, port, username, f, localFolder));
                try{
                    t.sleep(100);
                }catch(InterruptedException e){}
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
            System.out.println("upload> uploading "+n+" file(s) ...");
            c.addTextLeft(n+"files ");
            Iterator<FileEntry> i = list.iterator();
            while (i.hasNext()) {
                FileEntry f = i.next();
                poolUpload.execute(new ClientThread.FileUploadClass(this, c, ip, port, f, localFolder, username));
                try{
                    t.sleep(100);
                }catch(InterruptedException e){}
            }
            System.out.println("upload> Waiting for "+n+" files to upload ... ");
            awaitTerminationAfterShutdown(poolUpload);
            System.out.println("upload> uploaded "+n+" file(s)!");
            return n;
        }

        private void infoDialog(String title, String text){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(text);
            alert.showAndWait();
        }

    }


    static class FileDownloadClass implements Runnable{
        private Thread t;
        private FileEntry file;
        private String ip;
        private int port;
        private String localFolder, username;
        private Controller c;
        FileDownloadClass(Controller c, String ip, int port, String username, FileEntry file, String localFolder) {
            this.ip = ip;
            this.port = port;
            this.file = file;
            this.localFolder = localFolder;
            this.username = username;
            this.c = c;
            //this.start();
            if (t == null) {
                t = new Thread (this);
                //t.start ();
            }
        }
        public void run(){
            String filename = file.getFilename();
            String owner = file.getOwner();
            String destination = localFolder +File.separator + File.separator+ owner;
            //reate folder
            new File(destination).mkdirs();


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
            //ask server for this file
            out.println("getfile " + filename + " " + owner + " " + username);
            int filesize = Integer.parseInt(in.nextLine());
            if(filesize < 0){
                //server said that this client cant download this file
                System.out.println(t.getId() + "\\" +filename+ " Can't download "+filesize+"B to "+destination+"");
            }else{
                System.out.println(t.getId() + "\\" +filename+ " Downloading "+filesize+"B to "+destination+" ... ");
                c.printText("Downloading "+filename+" ("+filesize+"B) ... ");
                try {
                    DataInputStream dis = new DataInputStream(socketFile.getInputStream());
                    FileOutputStream fos = new FileOutputStream(destination+File.separator + File.separator+filename);
                    byte[] buffer = new byte[1024*64];
                    //int filesize = size;
                    int read = 0;
                    int totalRead = 0;
                    int remaining = filesize;
                    while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0 && remaining > 0) {
                        totalRead += read;
                        remaining -= read;
                        //System.out.println("read " + totalRead + " bytes.");
                        fos.write(buffer, 0, read);
                    }
                    System.out.println(filename+ " read " + totalRead + "B of "+ filesize);
                    //sleep for random time
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 10000));
                    }catch(Exception e){}
                    out.println("received " + filename);



                    if(socketFile.isClosed()){
                        System.out.println(filename + " socket closed");
                    }else{
                        fos.close();
                        dis.close();
                    }
                }catch(Exception e){
                    System.out.println("Exception in FileDownloaderClass : " + e.getMessage());
                }
                c.printText("file \""+ filename +"\" downloaded");
                System.out.println("file \""+ filename +"\" downloaded");
            }

            try{
                socketFile.close();
            }catch(IOException e){
                System.out.println("socketFile: failed to close");
            }


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
        FileEntry file;
        String localFolder, username;
        String ip;
        int port;
        Controller c;
        BackgroundTasks bg;
        FileUploadClass(BackgroundTasks bg, Controller c, String i, int p, FileEntry file, String localFolder, String username) {
            ip = i;
            port = p;
            this.file = file;
            this.c = c;
            this.localFolder = localFolder;
            this.username = username;
            this.bg = bg;
            //this.start();
            if (t == null) {
                t = new Thread (this);
                //t.start ();
            }
        }
        public void run(){
            String filename = file.getFilename();
            //String owner = file.getOwner();
            //String destination = localFolder +File.separator + File.separator+ owner;

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

            long fsize = file.getSize();


            c.printText("Uploading file: " + filename +" ("+ fsize + "B) ...");
            System.out.println("file " + filename + " " + fsize + " " + username);
            out.println("file " + filename + " " + fsize + " " + username);
            DataOutputStream dos;
            FileInputStream fis;
            try {
                dos = new DataOutputStream(socketFile.getOutputStream());
                fis = new FileInputStream(file.getPath());
                //sleep for random time
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 10000));
                }catch(Exception e){}
                byte[] buffer = new byte[1024*64];
                while (fis.read(buffer) > 0) {
                    dos.write(buffer);
                }
                /*
                try {
                    Thread.sleep(10000);
                }catch(Exception e){}*/

                System.out.println(t.getId() + "\\" +filename + " waiting for confirmation from server ... ");
                //if(in.hasNextLine()){
                //out.println("sent");

                String serverMsg = in.nextLine();
                if(serverMsg.matches("nope")){
                    //client tried to send file that is already on server
                }else{
                    System.out.println(t.getId() + "\\" +filename + " server: "+serverMsg);
                    //c.printText("File "+ filename +" uploaded to server!");
                }
                if(socketFile.isClosed()){
                    System.out.println(filename + " socket closed");
                }else{
                    fis.close();
                    dos.close();
                }
            }catch(Exception e){
                System.out.println(t.getId() + "\\" +"Exception in FileSenderClass : " + e.getMessage());
                c.printText("File "+ filename +" couldn't be uploaded to server :(");
            }
            //sleep for random time
            try{
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 10000));
            }catch(InterruptedException e){}
            c.printText("File "+ filename +" uploaded to server!");
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
