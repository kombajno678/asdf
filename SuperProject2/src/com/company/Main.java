package com.company;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileOutputStream;


public class Main extends Application {

    Stage window;
    Button button;

    public static void main(String[] args) {

        //launch(args);
        try{
            ServerSocket listener = new ServerSocket(55555);
            ExecutorService pool = Executors.newFixedThreadPool(100);
            System.out.println("server is running ...");
            while(true){
                try {
                    pool.execute(new Connection(listener.accept()));
                }catch(IOException e){
                    System.out.println("Failed to add new client connection");
                }
            }
        }catch(Exception e){
            System.out.println("Failed to create server socket " + e.getMessage());
            return;
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;
        window.setTitle("thenewboston - JavaFX");
        button = new Button("Click me");

        StackPane layout = new StackPane();
        layout.getChildren().add(button);
        Scene scene = new Scene(layout, 300, 250);

        window.setScene(scene);
        window.show();
    }
}
class Connection implements Runnable{
    private Socket socket;
    final static String serverFolderRoot = "server";
    Connection(Socket s){
        socket = s;
    }
    @Override
    public void run() {
        System.out.println(socket.getPort() + "> has connected to server");
        //ListenerClass listener = new ListenerClass(socket);
        //listener.start();
        PrintWriter out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            System.out.println(socket.getPort() + ">IOException: failed to get output stream from client");
        }
        //-----------------------------------------LISTENER LOOP--------------------------------------------------------

        boolean loop = true;
        try {
            Scanner in = new Scanner(socket.getInputStream());
            while (loop) {
                if(in.hasNextLine()) {
                    String temp = in.nextLine();
                    System.out.println(socket.getPort() + "> " + temp);
                    if(temp.matches("file [\\w_()]+\\.[A-Za-z0-9]{3} [\\d]+")){
                        //------------------------------------------------------------------user sends file
                        //launch file receiver thread
                        String[] a = temp.split(" ");
                        //System.out.println("a0: " + a[0]);
                        //System.out.println("a1: " + a[1]);
                        //System.out.println("a2: " + a[2]);
                        String filename = a[1];
                        int fileSize = Integer.parseInt(a[2]);
                        System.out.println("recieving file : " + filename);
                        String folder = "received\\";
                        FileReceiverClass fileReceiver = new FileReceiverClass(socket, folder+filename, fileSize);
                        System.out.println(socket.getPort() + ">waiting fpr file transfer");
                        fileReceiver.start();
                        try{
                            fileReceiver.t.join();
                        }catch(InterruptedException e){
                            System.out.println(socket.getPort() + ">InterruptedException");
                        }
                        System.out.println(socket.getPort() + ">file \""+ filename +"\" saved");
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
                        System.out.println(socket.getPort() + "> sent list of files for " + a[1]);
                        //break;
                    }
                    if(temp.matches("getfile [\\w_()]+\\.[A-Za-z0-9]{3}")){
                        String[] a = temp.split(" ");
                        //System.out.println("a0: " + a[0]);
                        //System.out.println("a1: " + a[1]);//filename
                        String filename = a[1];
                        filename = serverFolderRoot+"\\"+filename;
                        File f = new File(filename);
                        out.println((int)f.length());
                        System.out.println(socket.getPort()+"> sending "+filename+"("+(int)f.length()+") ...");
                        try {
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            FileInputStream fis = new FileInputStream(filename);

                            byte[] buffer = new byte[(int)f.length()];
                            while (fis.read(buffer) > 0) {
                                dos.write(buffer);
                            }
                            fis.close();
                            dos.close();
                            socket.close();

                        }catch(Exception e){
                            System.out.println(socket.getPort() + "> Exception while sending file : " + e.getMessage());
                        }
                        System.out.println(socket.getPort()+"> sent "+filename);
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
            System.out.println(socket.getPort() +"> exception while closing socket: "+e.getMessage());
        }finally{
            System.out.println(socket.getPort() +"> CLOSED ");
        }
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
class FileReceiverClass implements Runnable{
    Thread t;
    Socket socket;
    String filename;
    int size;
    FileReceiverClass(Socket s, String fn, int sz) {
        socket = s;
        filename = fn;
        size = sz;
        this.start();
    }
    public void run(){
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(filename);
            byte[] buffer = new byte[4096];
            int filesize = size;
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                totalRead += read;
                remaining -= read;
                System.out.println("read " + totalRead + " bytes.");
                fos.write(buffer, 0, read);
            }

            fos.close();
            dis.close();
        }catch(Exception e){
            System.out.println("Exception in FileReceiverClass : " + e.getMessage());
        }


    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
}
