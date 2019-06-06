package server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ServerThread implements Runnable {
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

    private HddController hddController = null;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    public synchronized void addFilesList(FileEntry f){
        filesList.add(f);
    }
    public synchronized boolean fileExists(String filename){
        if(filesList != null && filesList.size()>0){
            //search for file
            for(FileEntry f : filesList){
                if(f.getFilename().equals(filename)){
                    return true;
                }
            }
            return false;
        }else{
            return false;
        }
    }
    public ArrayList<String> getHdd() {return hdd;}
    public void setHdd(ArrayList<String> hdd) {
        this.hdd = hdd;
    }
    public ArrayList<FileEntry> getUserFilesList(String username) {
        ArrayList<FileEntry> temp = new ArrayList<>();
        for (Iterator<FileEntry> i = filesList.iterator(); i.hasNext(); ) {
            FileEntry file = i.next();
            //check if file is owned by user
            if (file.getOwner().equals(username)) {
                temp.add(file);
                continue;
            } else {
                //check if file is shared to user
                for (String a : file.getOthers()) {
                    if (a.equals(username)) {
                        temp.add(file);
                        continue;
                    }
                }
            }
        }
        return temp;
    }
    public int addDistinct(Collection<FileEntry> f) {
        if (f.isEmpty()) {
            return 0;
        }
        if (filesList.isEmpty()) {
            filesList.addAll(f);
            return f.size();
        }
        int filesAdded = 0;
        //iterate through list of files to add, if duplicate found then don't add
        for(FileEntry fe : f){
            boolean duplicate = false;
            for(FileEntry fs : filesList){
                if(fs.getFilename().equals(fe.getFilename()) && fs.getOwner().equals(fe.getOwner())){
                    duplicate = true;
                    break;
                }
            }
            if(!duplicate){
                filesList.add(fe);
                filesAdded++;
            }
        }
        return filesAdded;
    }
    public int addFromCsv(Collection<FileEntry> f) {
        if (f.isEmpty()) {
            return 0;
        }
        if (filesList.isEmpty()) {
            filesList.addAll(f);
            return f.size();
        }
        int filesAdded = 0;

        //add what is not present in current list of files

        for (FileEntry fnew : f) {
            //check if entry already exists, if no then add, if yes then update info
            //for(FileEntry fglobal : filesList){
            for(Iterator<FileEntry> i = filesList.iterator();i.hasNext();){
                FileEntry fglobal = i.next();
                boolean exists = false;
                System.out.println("checking if " + fnew.getPath() +" == "+ fglobal.getPath());
                if(fnew.getPath().equals(fglobal.getPath())){
                    //entry exists
                    System.out.println("entry exists " + fnew.getPath() +" == "+ fglobal.getPath());
                    fglobal.setOwner(fnew.getOwner());
                    fglobal.setOthers(fnew.getOthers());
                    exists = true;
                    filesAdded += 1;
                    break;
                }
                if(!exists){
                    //filesList.add(i);
                    addFilesList(fglobal);
                    filesAdded += 1;
                }
            }

        }
        return filesAdded;
    }
    public ArrayList<FileEntry> getFilesList() {return filesList;}
    public ArrayList<FileEntry> getFilesListClone() {
        return (ArrayList<FileEntry>) filesList.clone();
    }
    public void setFilesList(ArrayList<FileEntry> filesList) {this.filesList = filesList;}
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

    public synchronized void addToFilesList(FileEntry f){
        filesList.add(f);
    }

    public ServerThread(int ports, int nThreads, String path, ArrayList<String> hdd, HddController hddController) {
        this.port = ports;
        this.nThreads = nThreads;
        this.path = path;
        this.hdd = hdd;
        this.hddController = hddController;
        //not sure if necessary
        filesList = new ArrayList<>();
        users = new ArrayList<>();
        usersOnline = new ArrayList<>();
        //
        /*
        if (t == null) {
            t = new Thread(this);
            t.start();
        }*/
        System.out.println("Server Thread started.");
    }

    private ServerSocket listener;
    private ExecutorService pool;
    @Override
    public void run() {
        pool = null;
        listener = null;
        int maxTries = 10;
        while (maxTries > 0) {
            maxTries -= 1;
            try {
                listener = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            } catch (Exception e) {
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ee) {

                }
            }
        }
        if (maxTries <= 0) return;
        System.out.println("Server is running.");
        while (flag) {
            try {
                pool.execute(new Connection(listener.accept(), this, hddController));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new client connection");
            }
        }
        System.out.println("Server thread end");
    }
    public void start(){
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            listener.close();
        }catch(Exception e){};
        pool.shutdown();
        t.interrupt();
    }

    static class FileListUpdater implements Runnable {
        private Thread t;
        private boolean loop;
        public boolean initialized = false;

        private ArrayList<String> hdd;

        private Controller c;
        private ServerThread s;

        private String csvFileName = "files_list.csv";
        private static final String COMMA_DELIMITER = ",";

        private int waitTime;

        public FileListUpdater(ArrayList<String> hdd, Controller c, ServerThread s, int waitTime) {
            this.hdd = hdd;
            this.c = c;
            this.s = s;
            this.waitTime = waitTime;
            loop = true;
            //start();
        }
        private void printList(ArrayList<FileEntry> list, String name){
            System.out.print(name + "{"+list.size()+"} : ");
            for(FileEntry f : list){
                System.out.print(f.getFilename() + ":" +f.getPath() + ":"+f.getOthers()+", ");
            }
            System.out.println();
        }
        private void init(){
            //------------------------ 0. create hdd folders --------------
            for(String path : hdd){
                new File(path).mkdirs();
            }
            //------------------------ 1. list hdd ------------------------
            ArrayList<FileEntry> fileListHdd = createListHdd();
            //------------------------ 2. list csv ------------------------
            //read entries from csv files
            ArrayList<FileEntry> fileListCsv = createListCsv();
            //------------------------ 3. add csv to global ------------------------
            //add them to global files list
            //s.addFromCsv(fileListCsv);
            s.setFilesList(fileListCsv);
            //------------------------ 4. add (hdd - csv) to global ------------------------
            //hdd - csv
            printList(s.getFilesList(), "fileListCsv");
            printList(fileListHdd, "fileListHdd");
            s.addDistinct(fileListHdd);
            initialized = true;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            init();
            int previousSize = 0;
            while (loop) {

                //todo: check files on hdd
                //delete from global list files that had been deleted
                //

                if(s.getFilesList().size() != previousSize) {
                    System.out.print("Global files list{"+s.getFilesList().size()+"} : ");
                    for(FileEntry f : s.getFilesList()){
                        System.out.print(f.getFilename() + ":"+f.getOwner()+", ");
                    }
                    System.out.println();
                    previousSize = s.getFilesList().size();
                }




                //save global to csv
                writeCsv(s.getHdd(), s.getFilesListClone());

                //update list of online users in gui
                updateUsers();

                //update gui file list
                updateList();


                try {
                    Thread.sleep(waitTime * 1000);
                } catch (InterruptedException e) {

                }
            }
        }

        private ArrayList<FileEntry> createListHdd(){
            ArrayList<FileEntry> filesHdd = new ArrayList<>();
            for(int hddNo = 1; hddNo <= 5; hddNo+=1){
                ArrayList<String> listHdd = listFilesForFolder(new File(hdd.get(hddNo-1)));
                System.out.println("listHdd: " + listHdd);
                for(String fPath : listHdd){
                    if(fPath.contains(csvFileName))continue;//ignore csv file
                    File file = new File(fPath);
                    if (!file.isFile()) {
                        continue;//file doesn't exist, should't happen though
                    }else{
                        int size = (int)file.length();
                        //get ownerfolder name
                        String pattern = Pattern.quote(System.getProperty("file.separator"));
                        String[] split = fPath.split(pattern);
                        //System.out.println("found file: "+split[3]+", owner: "+split[2]);
                        filesHdd.add(new FileEntry(
                                file.getName(),
                                hddNo,
                                file.getPath().replace("\\", "\\\\"),
                                size,
                                split[2]));
                    }
                }
            }
            return filesHdd;
        }

        private ArrayList<FileEntry> createListCsv(){
            ArrayList<FileEntry> fileListCsv = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                //int i = 1;
                ArrayList<FileEntry> temp = readCsv(s.getHdd().get(i - 1), i);
                System.out.println("init> csv[" + i + "]: " + temp);
                if (temp != null) {
                    fileListCsv.addAll(temp);
                }
            }
            return fileListCsv;
        }

        private void updateUsers(){
            ObservableList<String> usersOnlineGui = FXCollections.observableArrayList();
            usersOnlineGui.addAll(s.getUsersOnline());
            c.updateUsersOnline(usersOnlineGui);
        }

        private void updateList() {
            c.updateFiles(s.getFilesList());
        }

        private ArrayList<FileEntry> readCsv(String path, int hddNo) {
            String csvPath = path + File.separator + File.separator+csvFileName;
            File csvFile = new File(csvPath);
            if (!csvFile.isFile()) {
                return null;//csv file doesn't exist
            }
            ArrayList<FileEntry> filesList = new ArrayList<>();
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(csvPath));
            } catch (FileNotFoundException fnf) {
                return null;//couldn't read from file
            }
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(COMMA_DELIMITER);
                    String filename = values[0];
                    int size = Integer.parseInt(values[1]);
                    String owner = values[2];
                    ArrayList<String> others = new ArrayList<>();
                    for(int i = 3; i < values.length; i++){
                        others.add(values[i]);
                    }
                    String filePath = path + File.separator + File.separator+ owner + File.separator + File.separator+filename;
                    // path = localfolder + hdd + owner + file
                    //check if it's not a duplicate
                    FileEntry newEntry = new FileEntry(filename, hddNo, filePath, size, owner, others);
                    boolean notDuplicate = true;
                    for(FileEntry f : filesList) {
                        if (f.getFilename().equals(newEntry.getFilename()) && f.getOwner().equals(newEntry.getOwner())) {
                            notDuplicate = false;
                            break;
                        }
                    }

                    //if(notDuplicate)filesList.add(new FileEntry(filename, hddNo, filePath, size, owner, others));
                    if(notDuplicate){
                        //check if file actually exists
                        if(new File(filePath).exists()) {
                            filesList.add(newEntry);
                        }
                    }
                }
            } catch (IOException ioe) {
            }
            return filesList;
        }

        private void writeCsv(List<String> paths, ArrayList<FileEntry> list) {
            for (int i = 1; i <= 5; i++) {
                //int i = 1;
                String csvPath = paths.get(i - 1) + File.separator +File.separator+ csvFileName;
                File csvFile = new File(csvPath);
                if (!csvFile.isFile()) {
                    //file doesn't exist
                    try {
                        if (csvFile.createNewFile()) {
                            System.out.println(csvFile + " File Created");
                        }
                    } catch (IOException e) {
                        //couldn't create file
                        return;
                    }
                }

                FileWriter csvWriter = null;
                try {
                    csvWriter = new FileWriter(csvPath);
                } catch (IOException e) {
                    return;//cant open file
                }

                for (FileEntry file : list) {
                    if (file.getHddNo() == i) {
                        try {
                            csvWriter.append(file.toCsv());
                        } catch (IOException e) {
                            return;//cant write to file
                        }
                    }
                }

                try {
                    csvWriter.flush();
                    csvWriter.close();
                } catch (IOException e) {
                    return;//whatever now
                }

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
    }

}

class HddController{

    private List<Integer> hddNumberOfOperations = Arrays.asList(0, 0, 0, 0, 0);
    private Controller gui;
/*
    public synchronized List<Integer> getOperations(){
        return hddNumberOfOperations;
    }*/

    public HddController(Controller c) {
        gui = c;
    }

    public synchronized int addOperation(){
        int hddToReturn;
        //find hdd with min operations
        int minOperations = Collections.min(hddNumberOfOperations);
        for(hddToReturn = 0; hddToReturn < hddNumberOfOperations.size(); hddToReturn++){
            if(hddNumberOfOperations.get(hddToReturn) == minOperations){
                hddNumberOfOperations.set(hddToReturn,hddNumberOfOperations.get(hddToReturn) + 1);
                gui.updateOperations(hddNumberOfOperations);
                printStatus();
                return hddToReturn;
            }
        }
        return -1;
    }

    public synchronized void addOperation(int hddNo) {
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) + 1);
        gui.updateOperations(hddNumberOfOperations);
        printStatus();
    }
    public synchronized void endOperation(int hddNo){
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) - 1);
        gui.updateOperations(hddNumberOfOperations);
        printStatus();
    }
    private void updateGui(){
        //invoke update method on controller
    }
    private void printStatus(){
        System.out.print("HDD STATUS:");
        for(int i = 0; i < hddNumberOfOperations.size(); i++)
            System.out.print(" ["+(i+1)+"]:"+hddNumberOfOperations.get(i));
        System.out.println();
    }
}

class Connection implements Runnable{
    private Thread t;
    private Socket socket;
    private ServerThread server;

    private String fileNameRegex = "[\\w-_()'.]+\\.[A-Za-z0-9]+";
    private String fileSizeRegex = "[\\d]+";
    private String userRegex = "[\\w-_]+";

    private ArrayList<String> hddPaths;
    private HddController hddController;

    private int msgCounter = 0;

    Connection(Socket socket, ServerThread server, HddController hddController){
        this.socket = socket;
        this.server = server;
        hddPaths = server.getHdd();
        this.hddController = hddController;
        //this.start();
        if (t == null) {
            t = new Thread(this);
            //t.start();
        }
        printMsg("Connection thread started");
    }

    @Override
    public void run() {

        printMsg("Has connected to server");

        PrintWriter out = null;
        try{
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e){
            printMsg("IOException: failed to get output stream from client");
        }
        //-----------------------------------------LISTENER LOOP--------------------------------------------------------

        boolean loop = true;
       // try {
            InputStream input = null;
            Scanner in = null;
            try{
                input = socket.getInputStream();
                in = new Scanner(input);
            }catch(IOException e){
                System.out.println("couldnt socket.getInputStream");
                loop = false;
            }

            while (loop) {
                if(socket.isClosed()){
                    break;
                }
                if(in.hasNextLine()) {
                    String temp = in.nextLine();
                    //printMsg("temp);
                    if(temp.matches("file "+fileNameRegex+" "+fileSizeRegex+" "+userRegex)){
                        //------------------------------------------------------------------user sends file
                        String[] a = temp.split(" ");
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
                            break;
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

                        hddController.endOperation(hddNo-1);

                        break;
                    }else
                    if(temp.matches("list "+userRegex)){
                        //----------------------------------------------------------------- user wants list of his files
                        String[] a = temp.split(" ");
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
                    }else
                    if(temp.matches("getfile "+fileNameRegex + " " + userRegex + " " + userRegex)){
                        //client wants to download a file

                        String[] a = temp.split(" ");
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
                            hddController.endOperation(fileToSend.getHddNo()-1);
                        }else{
                            //didn't find file user wants
                            out.println("-1");
                        }


                        break;
                    }else
                    if(temp.matches("delete "+fileNameRegex)){
                        //delete file
                        //delete entry in global file list
                        //delete file from hdd
                    }else
                    if(temp.matches("share "+fileNameRegex+" "+userRegex+" "+userRegex)){
                        //add new user to others
                        String[] a = temp.split(" ");
                        String filename = a[1];
                        String owner = a[2];
                        String userToShare = a[3];
                        //file specified file
                        for(int i = 0 ; i < server.getFilesList().size();i++){
                            FileEntry f = server.getFilesList().get(i);
                            if(f.getFilename().equals(filename) && f.getOwner().equals(owner)){
                                f.share(userToShare);
                                server.getFilesList().set(i, f);
                                break;
                            }
                        }
                        printMsg(temp);
                    }else
                    if(temp.matches("unshare "+fileNameRegex+" "+userRegex+" "+userRegex)){
                        //add new user to others
                        String[] a = temp.split(" ");
                        String filename = a[1];
                        String owner = a[2];
                        String userToUnshare = a[3];
                        //file specified file
                        for(int i = 0 ; i < server.getFilesList().size();i++){
                            FileEntry f = server.getFilesList().get(i);
                            if(f.getFilename().equals(filename) && f.getOwner().equals(owner)){
                                f.unshare(userToUnshare);
                                server.getFilesList().set(i, f);
                                break;
                            }
                        }
                        printMsg(temp);
                    }else
                    if(temp.matches("login "+userRegex)){
                        //user login
                        String[] a = temp.split(" ");
                        boolean logFlag = true;
                        for(Iterator<String> i = server.getUsersOnline().iterator();i.hasNext();){
                            if(i.next().equals(a[1])){
                                //user is already logged in
                                logFlag = false;
                                break;
                            }
                        }
                        if(logFlag)
                            server.getUsersOnline().add(a[1]);

                        //create folders for user on every drive
                        for(String hdd : hddPaths){
                            new File(hdd + File.separator + File.separator + a[1]).mkdirs();
                        }
                        printMsg("User " + a[1] + " has logged in");
                    }else
                    if(temp.matches("logout "+userRegex)){
                        //user logout
                        String[] a = temp.split(" ");
                        for(Iterator<String> i = server.getUsersOnline().iterator();i.hasNext();){
                            if(i.next().equals(a[1])){
                                i.remove();
                                break;
                            }
                        }
                        printMsg("User " + a[1] + " has logged out");
                        break;
                    }else
                    if(temp.matches("getusers")){
                        //user wants a list of all users ever
                        //send list of all users to client
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

                    }else
                    if(temp.matches("exit")){
                        //loop = false;
                        break;
                    }else {
                        printMsg("no match for : " + temp);
                    }
                }
            }
        //}catch(Exception e){
        //    System.out.println("Listener> Exception occured: " + e.getMessage());
        //}
        //--------------------------------------------------------------------------------------------------------------

        //socket.close();
        //while(true);

        try{
            socket.close();
        }catch(IOException e){
            printMsg("exception while closing socket: "+e.getMessage());
        }finally{
            printMsg("CLOSED ");
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
    private void printMsg(String msg){
        System.out.println(socket.getPort() +File.separator+ t.getId() + File.separator+msgCounter++ +"> "+msg);
        //msgCounter++;
    }
}
