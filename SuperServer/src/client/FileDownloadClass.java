package client;

import server.FileEntry;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * downloads one file
 */
class FileDownloadClass implements Runnable{
    private Thread t;
    private FileEntry file;
    private String ip;
    private int port;
    private String localFolder, username;
    private Controller c;

    /**
     *
     * @param c reference to gui controller
     * @param ip server's ip address
     * @param port server's port
     * @param username login
     * @param file file to download
     * @param localFolder path to local folder
     */
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
        String destination = localFolder + File.separator + File.separator+ owner;
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
}
