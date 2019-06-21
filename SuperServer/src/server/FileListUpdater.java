package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <pre>
 * creates folder for hdds
 * creates list of all files on hdds
 * reads files from csvs
 * creates global list of files
 * does periodically:
 * saves this global list to csvs
 * updates list of online users in gui
 * updates gui file list
 * </pre>
 */
public class FileListUpdater implements Runnable {
    /**
     * thread
     */
    private Thread t;
    /**
     * used in main thread loop
     */
    private boolean loop;
    /**
     * tells if thread is initialized
     */
    boolean initialized = false;
    /**
     * list of hdd paths
     */
    private ArrayList<String> hdd;
    /**
     * reference to gui controller
     */
    private Controller c;
    /**
     * reference to main server thread
     */
    private ServerThread s;
    /**
     * name of csv file which is located on every hdd
     */
    private String csvFileName = "files_list.csv";
    /**
     * character that separates values in csv files
     */
    private static final String COMMA_DELIMITER = ",";
    /**
     * how long to sleep before next iteration of main loop (in seconds)
     */
    private int waitTime;

    /**
     * @param hdd list of hdd paths
     * @param c reference to gui controller
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
     * prints list
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
     * <pre>
     * creates hdd folders
     * creates list of files from hdds
     * creates list of files from csvs
     * creates global files list
     * </pre>
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
        init();
        int previousListSize = 0;
        while (loop) {
            //maybe todo: check files on hdd
            //print files list of size changed
            if(s.getFilesList().size() != previousListSize) {
                System.out.print("Global files list{"+s.getFilesList().size()+"} : ");
                for(FileEntry f : s.getFilesList()){
                    System.out.print(f);
                }
                System.out.println();
                previousListSize = s.getFilesList().size();
            }
            //save global to csv
            writeCsv(s.getHdd(), s.getFilesListClone());
            //update gui file list
            updateList();
            try {
                Thread.sleep(waitTime * 1000);
            } catch (InterruptedException e) {}
        }
        //save global to csv before exiting
        writeCsv(s.getHdd(), s.getFilesListClone());
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
     * send list of files to gui
     */
    private void updateList() {
        c.updateFiles(s.getFilesList());
    }

    /**
     * <pre>
     * read entries from csv file
     * ignores duplicates
     * ignores entries to files that don't exist
     * </pre>
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
