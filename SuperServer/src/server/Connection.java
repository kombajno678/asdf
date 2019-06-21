package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * thread of every connection to server on its main port (55555 by default)
 */
public class Connection implements Runnable{
    /**
     * thread
     */
    private Thread t;
    /**
     * socket
     */
    private Socket socket;
    /**
     * reference to ServerThread thread
     */
    private ServerThread server;
    /**
     * list of paths to hdds
     */
    private ArrayList<String> hddPaths;
    /**
     * reference to hddController
     */
    private HddController hddController;

    /**
     * to display message number in console
     */
    private int msgCounter = 0;

    /**
     * socket's input stream
     */
    private InputStream input;
    /**
     * Scanner of socket's input stream
     */
    private Scanner in;
    /**
     * socket's output stream
     */
    public PrintWriter out;
    /**
     * user to separate values in commands from client
     */
    private String s = ":";

    /**
     *
     * @param socket connection socket
     * @param server reference to Server thread
     * @param hddController reference to HddController
     */
    Connection(Socket socket, ServerThread server, HddController hddController){
        this.socket = socket;
        this.server = server;
        hddPaths = server.getHdd();
        this.hddController = hddController;
        //this.start();
        if (t == null) {
            t = new Thread(this, "Connection_"+socket.getPort());
            //t.start();
        }
        printMsg("Connection thread started");
    }

    /**
     * used for testing
     * @param server reference to Server thread
     */
    public Connection(ServerThread server){
        hddPaths = server.getHdd();
        this.server = server;
    }

    @Override
    public void run() {

        printMsg("Has connected to server");

        out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            printMsg("IOException: failed to get output stream from client");
        }
        //-----------------------------------------LISTENER LOOP--------------------------------------------------------

        boolean loop = true;
        // try {
        input = null;
        in = null;
        try{
            input = socket.getInputStream();
            in = new Scanner(input);
        }catch(IOException e){
            System.out.println("couldnt socket.getInputStream");
            loop = false;
        }

        //String fileNameRegex = "[\\w-_()'.]+\\.[A-Za-z0-9]+";
        String fileNameRegex = "[^*&%<>|/\\\\:]+.[A-Za-z0-9]+";
        String fileSizeRegex = "[\\d]+";
        String userRegex = "[\\w-_]+";

        while (loop) {
            if(socket.isClosed()){
                break;
            }
            if(in.hasNextLine()) {
                String temp = in.nextLine();
                //printMsg("temp);
                if(temp.matches("file"+s+fileNameRegex+s+fileSizeRegex+s+userRegex)){
                    receiveFile(temp);
                    break;
                }else
                if(temp.matches("list"+s+userRegex)){
                    sendList(temp);
                }else
                if(temp.matches("getfile"+s+fileNameRegex + s + userRegex + s + userRegex)){
                    //client wants to download a file
                    sendFile(temp);
                    break;
                }else
                if(temp.matches("delete"+s + fileNameRegex + s + userRegex)){
                    //delete file
                    deleteFile(temp);
                }else
                if(temp.matches("share"+s+fileNameRegex+s+userRegex+s+userRegex)){
                    //add new user to others
                    shareFile(temp);
                }else
                if(temp.matches("unshare"+s+fileNameRegex+s+userRegex+s+userRegex)){
                    //add new user to others
                    unshareFile(temp);
                }else
                if(temp.matches("login"+s+userRegex)){
                    //user login
                    userLogin(temp);
                }else
                if(temp.matches("logout"+s+userRegex)){
                    //user logout
                    userLogout(temp);
                    break;
                }else
                if(temp.matches("getusers")){
                    //user wants a list of all users ever
                    //send list of all users to client
                    sendUsersList(temp);
                }else
                if(temp.matches("exit")){
                    break;
                }else {
                    printMsg("no match for : " + temp);
                }
            }
        }


        try{
            socket.close();
        }catch(IOException e){
            printMsg("exception while closing socket: "+e.getMessage());
        }finally{
            printMsg("CLOSED ");
        }
    }

    /**
     * prints message on system console
     * before actual message also prints socket's port, thread id and number of message
     * @param msg message to print on console
     */
    private void printMsg(String msg){
        System.out.println(socket.getPort() +File.separator+ t.getId() + File.separator+msgCounter++ +"> "+msg);
    }

    /**
     * receive single file from client
     * @param temp client's comamnd, contains file name, file size, file owner (client's username)
     */
    private void receiveFile(String temp){
        //------------------------------------------------------------------user sends file
        String[] a = temp.split(s);
        String filename = a[1];
        int filesize = Integer.parseInt(a[2]);
        String username = a[3];
        //check if this file already exists
        boolean fileExists = false;
        String msg = "";
        for(FileEntry fe : server.getFilesList()){
            if(fe.getFilename().equals(filename) && fe.getOwner().equals(username)){
                //file is already on server
                msg = fe.getFilename() +"="+ filename +" and "+ fe.getOwner() +"="+ username;
                fileExists = true;
            }
        }
        //if so, then dont let user upload it it
        if(fileExists){
            printMsg("nope, bc: "+msg);
            out.println("nope");
            return;
        }
        //int hddNo = 1;//hardcoded for now
        int hddNo = hddController.addOperation()+1;
        String path = hddPaths.get(hddNo-1)+File.separator+File.separator+username+File.separator+File.separator+filename;
        //add entry to global file list
        //server.getFilesList().add(new FileEntry(filename, hddNo, path, filesize, username));
        server.addToFilesList(new FileEntry(filename, hddNo, path, filesize, username));
        printMsg("receiving file : " + filename + " from: " + username);
        try {
            //printMsg(filename + " client: "+in.nextLine());
            DataInputStream dis = new DataInputStream(input);
            FileOutputStream fos = new FileOutputStream(path);
            byte[] buffer = new byte[1024*64];
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0 && remaining > 0) {
                totalRead += read;
                remaining -= read;
                //printMsg(filename + " remaining " + remaining);
                fos.write(buffer, 0, read);
            }
            printMsg(filename + " read " + totalRead + " of " + filesize);

            out.println("received " + filename);

            try {
                Thread.sleep(1000);
            }catch(Exception e){}
            fos.close();
            dis.close();
        }catch(Exception e){
            System.out.println("Exception in FileReceiverClass : " + e.getMessage());
        }
        printMsg("file "+ filename +" saved");
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 10000));
        }catch(Exception e){}
        hddController.endOperation(hddNo-1);
    }

    /**
     * send to client list of files that user has rights to
     * @param temp client's command, contains client's username
     */
    private void sendList(String temp){
        //----------------------------------------------------------------- user wants list of his files
        String[] a = temp.split(s);
        String username = a[1];
        //get list of files user has rights to
        ArrayList<FileEntry> userFilesList = server.getUserFilesList(username);
        //printMsg("userFilesList["+username+"]{"+userFilesList.size()+"} : " + userFilesList);
        ObjectOutputStream objectOutput = null;
        try {
            objectOutput = new ObjectOutputStream(socket.getOutputStream());
        }catch(IOException e){
            printMsg("list socket.getOutputStream: " + e.getMessage() + " " + e.getCause());
        }
        try{
            objectOutput.writeObject(userFilesList);
        } catch (IOException e) {
            printMsg("list objectOutput.writeObject: " + e.getMessage() + " " + e.getCause());
        }
    }

    /**
     * send single file to client
     * @param temp client's command, contains file name, file owner, client's username
     */
    private void sendFile(String temp){
        String[] a = temp.split(s);
        String filename = a[1];
        String owner = a[2];
        String username = a[3];

        //find file that user wants
        FileEntry fileToSend = null;
        for(FileEntry f : server.getFilesList()){
            if(f.getFilename().equals(filename) && f.getOwner().equals(owner)){
                fileToSend = f;
                break;
            }
        }
        if(fileToSend != null){
            //check if user has right to file
            boolean canSend = false;
            if(!username.equals(fileToSend.getOwner())){
                //check if file is shared to user
                for(String shared : fileToSend.getOthers()){
                    if(shared.equals(username)){
                        canSend = true;
                        break;
                    }
                }
            }else{
                canSend = true;
            }
            if(canSend){
                String filePath = fileToSend.getPath();
                long size = fileToSend.getSize();
                out.println(size);
                printMsg("sending "+fileToSend.getFilename()+":"+fileToSend.getOwner()+" to"+username+" ...");
                //sending
                //tell hdd controller that is new operation on this hdd
                hddController.addOperation(fileToSend.getHddNo()-1);
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
                        Thread.sleep(100);//1000
                    }catch(Exception e){}
                    printMsg("waiting for confirmation from client ... ");
                    printMsg("client: "+in.nextLine());
                    if(socket.isClosed()){
                        System.out.println(filename + " socket closed");
                    }else{
                        fis.close();
                        dos.close();
                    }

                }catch(Exception e){
                    printMsg("Exception while sending file : " + e.getMessage());
                }
                printMsg("sent "+filename + " to client");
            }else{
                // tell user he cant download file
                out.println(-1);
            }
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 10000));
            }catch(Exception e){}
            hddController.endOperation(fileToSend.getHddNo()-1);
        }else{
            //didn't find file user wants
            out.println("-1");
        }
    }

    /**
     * shares file
     * adds new username to list of users that have right to file
     * @param temp client's command, contains file name, file owner, name of user to whom file will be shared
     */
    public void shareFile(String temp){
        String[] a = temp.split(s);
        String filename = a[1];
        String owner = a[2];
        String userToShare = a[3];
        //file specified file
        for(int i = 0 ; i < server.getFilesList().size();i++){
            FileEntry f = server.getFilesList().get(i);
            if(f.getFilename().equals(filename) && f.getOwner().equals(owner)){
                f.share(userToShare);
                server.getFilesList().set(i, f);
                if(server.c != null)server.c.updateFilesForce(server.getFilesList());
                break;
            }
        }
        if(socket != null && t != null)printMsg(temp);


    }

    /**
     * unshares file
     * removes username from list of users that have right to file
     * @param temp client's command, contains file name, file owner, name of user that won't have right to file
     */
    public void unshareFile(String temp){
        String[] a = temp.split(s);
        String filename = a[1];
        String owner = a[2];
        String userToUnshare = a[3];
        //file specified file
        for(int i = 0 ; i < server.getFilesList().size();i++){
            FileEntry f = server.getFilesList().get(i);
            if(f.getFilename().equals(filename) && f.getOwner().equals(owner)){
                f.unshare(userToUnshare);
                server.getFilesList().set(i, f);
                if(server.c != null)server.c.updateFilesForce(server.getFilesList());
                break;
            }
        }
        if(socket != null && t != null)printMsg(temp);

    }

    /**
     * logs in user
     * adds username to online users list
     * @param temp client's command, contains client's username
     */
    public void userLogin(String temp){
        String[] a = temp.split(s);
        boolean logFlag = true;
        for(Iterator<String> i = server.getUsersOnline().iterator(); i.hasNext();){
            if(i.next().equals(a[1])){
                //user is already logged in
                logFlag = false;
                break;
                //return;
            }
        }
        if(logFlag){
            server.getUsersOnline().add(a[1]);
            //create folders for user on every drive
            for(String hdd : hddPaths){
                new File(hdd + File.separator + File.separator + a[1]).mkdirs();
            }
            server.updateUsers();
            if(socket != null && t != null)printMsg("User " + a[1] + " has logged in");
        }




    }

    /**
     * logs out user
     * removes username from online users list
     * @param temp client's command, contains client's username
     */
    public void userLogout(String temp){
        String[] a = temp.split(s);
        for(Iterator<String> i = server.getUsersOnline().iterator();i.hasNext();){
            if(i.next().equals(a[1])){
                i.remove();
                break;
            }
        }
        server.updateUsers();
        if(socket != null && t != null)printMsg("User " + a[1] + " has logged out");
    }

    /**
     * send list of all users to client
     * @param temp client's command
     */
    private void sendUsersList(String temp){
        ArrayList<String> users = new ArrayList<>();
        for(Iterator<FileEntry> i = server.getFilesList().iterator(); i.hasNext();){
            FileEntry x = i.next();
            boolean exists = false;
            for(String u : users){
                if(u.equals(x.getOwner())){
                    exists = true;
                    break;
                }
            }
            if(!exists)users.add(x.getOwner());
        }
        //sending object
        ObjectOutputStream objectOutput = null;
        try {
            objectOutput = new ObjectOutputStream(socket.getOutputStream());
        }catch(IOException e){
            printMsg("list socket.getOutputStream: " + e.getMessage() + " " + e.getCause());
        }
        try{
            objectOutput.writeObject(users);
        } catch (IOException e) {
            printMsg("list objectOutput.writeObject: " + e.getMessage() + " " + e.getCause());
        }
    }

    /**
     * deletes file from global files list and from hdd
     * @param temp client's command, contains file name, file owner
     * @return if file was deleted successfully
     */
    public boolean deleteFile(String temp){
        String[] a = temp.split(s);
        String fileName = a[1];
        String fileOwner = a[2];
        //find this file
        for(int i = 0 ; i < server.getFilesList().size();i++){
            FileEntry f = server.getFilesList().get(i);
            if(f.getFilename().equals(fileName) && f.getOwner().equals(fileOwner)){
                //found file
                FileEntry fileToDelete = f;
                //delete entry from global files list
                server.getFilesList().remove(i);
                //delete file from hdd
                if(new File(fileToDelete.getPath()).delete()){
                    //deleted successfully
                    System.out.println("deleted successfully: "+fileToDelete.getFilename());
                    if(out != null)out.println("deleted");
                    return true;
                }else{
                    //didn't delete
                    System.out.println("didn't delete: "+fileToDelete.getFilename());
                    if(out != null)out.println("not_deleted");
                    return false;
                }
            }
        }
        return false;
    }
}
