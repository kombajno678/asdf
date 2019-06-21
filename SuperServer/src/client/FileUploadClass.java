package client;

import server.FileEntry;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * uploads one file
 */
class FileUploadClass implements Runnable{
    /**
     * thread
     */
    public Thread t;
    /**
     * file to upload
     */
    private FileEntry file;
    /**
     * path to local folder
     */
    private String localFolder;
    /**
     * login
     */
    private String username;
    /**
     * server's ip address
     */
    private String ip;
    /**
     * server's port
     */
    private int port;
    /**
     * reference to gui controller
     */
    private Controller c;
    /**
     * reference to BackgroundTasks thread
     */
    private BackgroundTasks bg;

    /**
     * @param bg reference to BackgroundTasks thread
     * @param c reference to gui controller
     * @param ip server's ip address
     * @param p server's port
     * @param file file to upload
     * @param localFolder path to local folder
     * @param username login
     */
    FileUploadClass(BackgroundTasks bg, Controller c, String ip, int p, FileEntry file, String localFolder, String username) {
        this.ip = ip;
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
        String s = ":";
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
            try{
                socketFile.close();
            }catch(IOException ex){}
            return;
        }
        long fsize = file.getSize();
        c.printText("Uploading file: " + filename +" ("+ fsize + "B) ...");
        System.out.println("file" + s+filename + s + fsize + s + username);

        out.println("file" + s + filename + s + fsize + s + username);
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
            System.out.println(t.getId() + "\\" +filename + " waiting for confirmation from server ... ");
            String serverMsg = in.nextLine();
            if(serverMsg.matches("nope")){
                //client tried to send file that is already on server
            }else{
                System.out.println(t.getId() + "\\" +filename + " server: "+serverMsg);
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

}