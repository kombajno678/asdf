package com.company;

//import com.company.myUtils;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;



import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;

public class client extends Application{
    Stage window;
    Button button;
    Label label;
    String ip = "127.0.0.1";
    int port = 55555;
    public final static String username = "adamko";
    public final static String localFolder = "local\\"+username;

    public static void main(String[] args){
        //ClientThread client = new ClientThread(args);
        //client.start();
        launch(args);

    }
    @Override
    public void start(Stage primaryStage) throws Exception {

        ClientThread.UpdateFilesFromServerClass updater = new ClientThread.UpdateFilesFromServerClass(
                ip, port,60, localFolder, username);
        updater.start();
        //ExecutorService poolUploadFiles = Executors.newFixedThreadPool(10);
        ExecutorService poolUploadFiles = Executors.newCachedThreadPool();

        window = primaryStage;
        window.setTitle("Super Project - Client");

        //Form
        TextField input = new TextField();
        input.setPromptText("file path");

        button = new Button("Send file");
        button.setOnAction(e->{
            String filename = input.getText();
            System.out.println("GUI: sending file : " + filename);
            poolUploadFiles.execute(
                    new ClientThread.FileSenderClass(ip, port, filename)
            );
            //ClientThread.FileSenderClass fileSender = new ClientThread.FileSenderClass(ip, port, filename);
            //fileSender.start();
        });

        window.setOnCloseRequest(e->{
            updater.stop();
            System.out.println("exit main");
        });

        //Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20, 20, 20, 20));
        layout.getChildren().addAll(button, input);

        Scene scene = new Scene(layout, 300, 250);
        window.setScene(scene);
        window.show();



    }

}
class ClientThread implements Runnable{
    Thread t;
    public final static String username = "adamko";
    public final static String localFolder = "local\\"+username;
    public static String ip = "";
    final static int port = 55555;
    String[] args;
    public ClientThread(String[] args){
        this.args = args;
    }
    @Override
    public void run(){
        if (args.length < 1) {
            ip = "127.0.0.1";
        }else {
            ip = args[0];
        }

        Socket socketCommunication = null;
        do{
            try{
                socketCommunication = new Socket(ip, port);
            }catch(Exception e){
                System.out.println("Can't connect to server. Trying again in 1s");
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException ie){
                    //System.out.println("InterruptedException: thread interrupted while sleeping");
                }
            }
        }while(socketCommunication == null);
        //--------------------------------------------------------------------------------------------------------------
        // is connected

        //start update files thread
        UpdateFilesFromServerClass updater = new UpdateFilesFromServerClass(
                ip, port,
                60, localFolder, username);
        updater.start();

        Scanner in = new Scanner(System.in);
        PrintWriter out = null;
        try{
            out = new PrintWriter(socketCommunication.getOutputStream(), true);
        }catch (IOException e){
            System.out.println("IOException: failed to get output stream from server");
        }
        boolean loop = true;
        while(loop) {
            if (in.hasNextLine()) {
                String temp = in.nextLine();

                if (temp.matches("sendfile [\\w_()]+\\.[A-Za-z0-9]{3}")) {
                    String[] a = temp.split(" ");
                    //System.out.println("a0: " + a[0]);
                    //System.out.println("a1: " + a[1]);
                    String filename = a[1];

                    System.out.println("sending file : " + filename);
                    FileSenderClass fileSender = new FileSenderClass(ip, port, filename);
                    fileSender.start();

                }
                if (temp.matches("exit")) {
                    updater.stop();
                    out.println(temp);
                    //loop = false;
                    break;
                }
            }
        }
    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    static class UpdateFilesFromServerClass implements Runnable{
        private Thread t;
        boolean loop;
        int waitTime;
        Socket socket;
        String localFolder;
        String ip;
        String username;
        int port;
        UpdateFilesFromServerClass(String ip, int port, int waitTimeSeconds, String localFolder, String username) {
            this.ip = ip;
            this.port = port;
            socket = null;
            waitTime = waitTimeSeconds*1000;
            this.localFolder = localFolder;
            this.username = username;
            this.start();
        }
        public void run(){
            do{
                try{
                    socket = new Socket(ip, port);
                }catch(Exception e){
                    System.out.println("Updater> Can't connect to server. Trying again in 10s");
                    try{
                        Thread.sleep(10000);
                    }catch(InterruptedException ie){
                    }
                }
            }while(socket == null);

            loop = true;
            while(loop){
                //------------------------------------------ask server if any new files for me
                System.out.println("Updater> Asking server if there are any new files ... ");
                PrintWriter out = null;
                try{
                    out = new PrintWriter(socket.getOutputStream(), true);
                }catch (IOException e){
                    System.out.println("Updater> IOException: failed to get output stream from server");
                }
                out.println("list " + username);
                ArrayList<String> listFilesOnServer;
                listFilesOnServer = new ArrayList<String>();
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
                    try {
                        Object object = objectInput.readObject();
                        listFilesOnServer =  (ArrayList<String>) object;
                        System.out.println(listFilesOnServer);
                    } catch (ClassNotFoundException e) {
                        System.out.println("The title list has not come from the server");
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    System.out.println("The socket for reading the object has problem");
                    e.printStackTrace();
                }



                ArrayList<String> listFilesLocal;
                listFilesLocal = listFilesForFolder(new File(localFolder));
                listFilesOnServer.removeAll(listFilesLocal);
                //if so, ------------------------------------------------------download those files
                int numberOfFilesToDownload = listFilesOnServer.size();
                if(numberOfFilesToDownload > 0){
                    //create thread pool?
                    ExecutorService pool = Executors.newFixedThreadPool(numberOfFilesToDownload);
                    System.out.println("Updater> Downloading new files ...");
                    Iterator<String> i = listFilesOnServer.iterator();
                    while(i.hasNext()){
                        try {
                            pool.execute(new FileDownloaderClass(ip, port, i.next(), localFolder));

                        }catch(Exception e){
                            System.out.println("Failed to add new connection");
                        }
                    }
                    try{
                        pool.awaitTermination(5, TimeUnit.SECONDS);
                    }catch(InterruptedException e){

                    }

                }
                //---------------------------------------------------------------go to sleep
                try {
                    Thread.sleep(waitTime);
                }catch(InterruptedException e){
                    System.out.println("Updater> thread interrupted while sleeping");
                }
            }
            System.out.println("Updater> Thread stopped");
        }
        public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }
        public void stop(){
            loop = false;
        }
        public static ArrayList<String> listFilesForFolder(final File folder) {
            ArrayList<String> list = new ArrayList<String>();
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry);
                } else {
                    //System.out.println(fileEntry.getName());
                    list.add(fileEntry.getName());
                }
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
        FileDownloaderClass(String ip, int port, String fn, String dest) {
            this.ip = ip;
            this.port = port;
            filename = fn;
            destination = dest;
            //this.start();
        }
        public void run(){
            Socket socketFile = null;
            do{
                try{
                    socketFile = new Socket(ip, port);
                }catch(Exception e){
                    System.out.println("socketFile : Can't connect to server. Trying again in 1s");
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
            //File f = new File(filename);
            out.println("getfile " + filename);
            int size = -1;
            if(in.hasNextLine()){
                size = Integer.parseInt(in.nextLine());
            }
            System.out.println("downloading "+filename+"("+size+"B) to "+destination);
            try {
                DataInputStream dis = new DataInputStream(socketFile.getInputStream());
                FileOutputStream fos = new FileOutputStream(destination+"\\"+filename);
                byte[] buffer = new byte[size];// was 4096
                int filesize = size;
                int read = 0;
                int totalRead = 0;
                int remaining = filesize;
                while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                    totalRead += read;
                    remaining -= read;
                    System.out.println(filename+" read " + totalRead + " bytes.");
                    fos.write(buffer, 0, read);
                }
                fos.close();
                dis.close();
            }catch(Exception e){
                System.out.println("Exception in FileDownloaderClass : " + e.getMessage());
            }
            System.out.println("file \""+ filename +"\" downloaded");
            //out.println("exit");
            try{
                socketFile.close();
            }catch(IOException e){
                System.out.println("socketFile: failed to close");
            }

        }
        public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }
    }
    static class FileSenderClass implements Runnable{
        public Thread t;
        String filename;
        String ip;
        int port;
        FileSenderClass(String i, int p, String fn) {
            ip = i;
            port = p;
            filename = fn;
            //this.start();
        }
        public void run(){
            Socket socketFile = null;
            do{
                try{
                    socketFile = new Socket(ip, port);
                }catch(Exception e){
                    System.out.println("socketFile : Can't connect to server. Trying again in 1s");
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException ie){
                    }
                }
            }while(socketFile == null);
            PrintWriter out = null;
            try{
                out = new PrintWriter(socketFile.getOutputStream(), true);
            }catch (IOException e){
                System.out.println("IOException: failed to get output stream from server");
            }
            File f = new File(filename);
            out.println("file " + filename + " " + (int)f.length());
            try {
                DataOutputStream dos = new DataOutputStream(socketFile.getOutputStream());
                FileInputStream fis = new FileInputStream(filename);

                byte[] buffer = new byte[(int)f.length()];
                while (fis.read(buffer) > 0) {
                    dos.write(buffer);
                }
                fis.close();
                dos.close();
                socketFile.close();

            }catch(Exception e){
                System.out.println("Exception in FileSenderClass : " + e.getMessage());
            }
            System.out.println("file \""+ filename +"\" sent");
            out.println("exit");
            try{
                socketFile.close();
            }catch(IOException e){
                System.out.println("socketFile: failed to close");
            }

        }
        public void start(){
            if (t == null) {
                t = new Thread (this);
                t.start ();
            }
        }
    }
}