package server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * main thread of server
 */
class ServerThread implements Runnable {
    private Thread t;
    private int port, nThreads;
    //private String path;
    private boolean flag = true;
    /**
     * list of hdd paths
     */
    private ArrayList<String> hdd;
    /**
     * list of files for every hdd
     */

    private ArrayList<FileEntry> filesList;
    /**
     * list of all users
     */

    //private ArrayList<String> users;
    /**
     * list of online users
     */

    private ArrayList<String> usersOnline;
    private HddController hddController;
    protected Controller c;

    ArrayList<String> getHdd() {return hdd;}
    ArrayList<FileEntry> getUserFilesList(String username) {
        ArrayList<FileEntry> temp = new ArrayList<>();
        for (Iterator<FileEntry> i = filesList.iterator(); i.hasNext(); ) {
            FileEntry file = i.next();
            //check if file is owned by user
            if (file.getOwner().equals(username)) {
                temp.add(file);
                //continue;
            } else {
                //check if file is shared to user
                for (String a : file.getOthers()) {
                    if (a.equals(username)) {
                        temp.add(file);
                        //continue;
                    }
                }
            }
        }
        return temp;
    }

    /**
     * add files from f to global files list, skips duplicates
     * @param f files to add to global files list
     */
    private void addDistinct(Collection<FileEntry> f) {
        if (f.isEmpty()) {
            return;
        }
        if (filesList.isEmpty()) {
            filesList.addAll(f);
            return;
        }
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
            }
        }
        return;
    }
    ArrayList<FileEntry> getFilesList() {return filesList;}
    private ArrayList<FileEntry> getFilesListClone() {
        return (ArrayList<FileEntry>) filesList.clone();
    }
    private void setFilesList(ArrayList<FileEntry> filesList) {this.filesList = filesList;}
    ArrayList<String> getUsersOnline() {
        return usersOnline;
    }
    synchronized void addToFilesList(FileEntry f){
        filesList.add(f);
    }

    /**
     *
     * @param port server's port
     * @param nThreads maximum numver of connection threads
     * @param path path where hard drives are
     * @param hdd list of hdd paths
     * @param hddController reference to hddcontroller,holds information about number of operations
     * @param c reference to gui controller
     */
    ServerThread(int port, int nThreads, String path, ArrayList<String> hdd, HddController hddController, Controller c) {
        this.port = port;
        this.nThreads = nThreads;
        //this.path = path;
        this.hdd = hdd;
        this.hddController = hddController;
        this.c = c;
        filesList = new ArrayList<>();
        //users = new ArrayList<>();
        usersOnline = new ArrayList<>();

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

    /**
     * starts thread
     */
    public void start(){
        if (t == null) {
            t = new Thread (this, "ServerThread");
            t.start ();
        }
    }

    /**
     * tries to stop thread
     */
    public void stop() {
        flag = false;

        try{
            pool.shutdown();
            listener.close();
        }catch(Exception e){
            //e.printStackTrace();
        }
        t.interrupt();
    }

    /**
     * creates folder for hdds
     * creates list of all files on hdds
     * reads files from csvs
     * creates global list of files
     * does periodically:
     * saves this global list to csvs
     * updates list of online users in gui
     * updates gui file list
     */
    static class FileListUpdater implements Runnable {
        private Thread t;
        private boolean loop;
        boolean initialized = false;
        private ArrayList<String> hdd;
        private Controller c;
        private ServerThread s;
        private String csvFileName = "files_list.csv";
        private static final String COMMA_DELIMITER = ",";
        private int waitTime;

        /**
         * @param hdd list of hdd paths
         * @param c reference to gyu controller
         * @param s reference to main server thread
         * @param waitTime how often to do periodical tasks
         */
        FileListUpdater(ArrayList<String> hdd, Controller c, ServerThread s, int waitTime) {
            this.hdd = hdd;
            this.c = c;
            this.s = s;
            this.waitTime = waitTime;
            loop = true;
            //start();
        }

        /**
         * pronts list
         * @param list list to prints
         * @param name name of list
         */
        private void printList(ArrayList<FileEntry> list, String name){
            System.out.print(name + "{"+list.size()+"} : ");
            for(FileEntry f : list){
                System.out.print(f.getFilename() + ":" +f.getPath() + ":"+f.getOthers()+", ");
            }
            System.out.println();
        }

        /**
         * creates hdd folders
         * creates list of files from hdds
         * creates list of files from csvs
         * creates global files list
         */
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
            } catch (InterruptedException e) {}
            init();
            int previousSize = 0;
            while (loop) {
                //maybe todo: check files on hdd
                //
                if(s.getFilesList().size() != previousSize) {
                    System.out.print("Global files list{"+s.getFilesList().size()+"} : ");
                    for(FileEntry f : s.getFilesList()){
                        System.out.print(f);
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
                } catch (InterruptedException e) {}
            }
        }

        /**
         * creates list of all files on hdds
         * @return list of files on hdds
         */
        private ArrayList<FileEntry> createListHdd(){
            ArrayList<FileEntry> filesHdd = new ArrayList<>();
            for(int hddNo = 1; hddNo <= 5; hddNo+=1){
                ArrayList<String> listHdd = listFilesForFolder(new File(hdd.get(hddNo-1)));
                System.out.println("listHdd: " + listHdd);
                for(String fPath : listHdd){
                    if(fPath.contains(csvFileName))continue;//ignore csv file
                    File file = new File(fPath);
                    if (!file.isFile()) {
                        //continue;//file doesn't exist, should't happen though
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

        /**
         * reads entries from csv files
         * @return list of files from csvs
         */
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
        /**
         * send list of users to gui
         */
        private void updateUsers(){
            ObservableList<String> usersOnlineGui = FXCollections.observableArrayList();
            usersOnlineGui.addAll(s.getUsersOnline());
            c.updateUsersOnline(usersOnlineGui);
        }

        /**
         * send list of files to gui
         */
        private void updateList() {
            c.updateFiles(s.getFilesList());
        }

        /**
         * read entries from csv file
         * ignores duplicates
         * ignores entries to files that don't exist
         * @param path path to csv file
         * @param hddNo  number of hdd those files are from
         * @return list of files read from csv file
         */
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

        /**
         * writes files from list to all csv files
         * @param paths paths to csv files
         * @param list list of files to write
         */
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

        /**
         * starts thread
         */
        public void start() {
            if (t == null) {
                t = new Thread(this, "FileListUpdater_Thread");
                t.start();
            }
        }

        /**
         * tries to stop thread
         */
        public void stop() {
            loop = false;
            t.interrupt();
        }

        /**
         * lists all files in given folder and subfolder
         * @param folder folder from where to list files
         * @return list of all files in folder and its subfolders
         */
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