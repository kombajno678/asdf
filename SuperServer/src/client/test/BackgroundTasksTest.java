package client.test;

import client.BackgroundTasks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.FileEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class BackgroundTasksTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void getFilesHdd(){
        ArrayList<FileEntry> list = new ArrayList<>();
        String localFolder = "test", username = "username", ip = "";
        int port = 0;
        //create test folders
        new File(localFolder).mkdir();
        new File(localFolder + File.separator + username).mkdir();
        //create new files in test folder
        for(int i = 0; i < 5; i++){
            File file = new File(localFolder + File.separator + username+ File.separator + "file"+i);
            try {
                file.createNewFile();
                list.add(new FileEntry(
                        "file"+i,
                        0,
                        file.getPath().replace("\\", "\\\\"),
                        file.length(),
                        username,
                        "local"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BackgroundTasks bg = new BackgroundTasks(localFolder, username, ip, port, null);
        ArrayList<FileEntry> filesHdd = bg.getFilesHdd();
        //delete test files and folders
        for(int i = 0; i < 5; i++){
            File file = new File(localFolder + File.separator + username+ File.separator + "file"+i);
            file.delete();
        }
        new File(localFolder + File.separator + username).delete();
        new File(localFolder).delete();

        assertEquals(list, filesHdd);
    }

    @Test
    public void getDownloadList(){
        String localFolder = "test", username = "username", ip = "";
        int port = 0;
        BackgroundTasks bg = new BackgroundTasks(localFolder, username, ip, port, null);

        //server 5 files
        ArrayList<FileEntry> server = new ArrayList<>();
        server.add(new FileEntry("file01", 1, "file01", 420, "owner1"));
        server.add(new FileEntry("file02", 1, "file02", 420, "owner1"));
        server.add(new FileEntry("file03", 1, "file03", 420, "owner1"));
        server.add(new FileEntry("file05", 1, "file05", 420, "owner1"));
        server.add(new FileEntry("file05", 1, "file05", 420, "owner2"));
        bg.setFilesServer(server);

        //local 3 files
        ArrayList<FileEntry> local = new ArrayList<>();
        local.add(new FileEntry("file01", 1, "file01", 420, "owner1"));
        local.add(new FileEntry("file05", 1, "file05", 420, "owner2"));
        local.add(new FileEntry("file02", 1, "file02", 420, "owner1"));
        bg.setFilesLocal(local);

        //result 2 files
        ArrayList<FileEntry> list = bg.getDownloadList();
        assertEquals(2, list.size());
    }

    @Test
    public void getUploadList(){
        String localFolder = "test", username = "owner1", ip = "";
        int port = 0;
        BackgroundTasks bg = new BackgroundTasks(localFolder, username, ip, port, null);


        //server 5 files
        ArrayList<FileEntry> local = new ArrayList<>();
        local.add(new FileEntry("file01", 1, "file01", 420, "owner1"));
        local.add(new FileEntry("file02", 1, "file02", 420, "owner1"));
        local.add(new FileEntry("file03", 1, "file03", 420, "owner1"));
        local.add(new FileEntry("file05", 1, "file05", 420, "owner1"));
        local.add(new FileEntry("file05", 1, "file05", 420, "owner2"));
        bg.setFilesLocal(local);

        //local 3 files
        ArrayList<FileEntry> server = new ArrayList<>();
        server.add(new FileEntry("file01", 1, "file01", 420, "owner1"));
        server.add(new FileEntry("file02", 1, "file02", 420, "owner1"));
        bg.setFilesServer(server);

        //result 2 files
        ArrayList<FileEntry> list = bg.getUploadList();
        assertEquals(2, list.size());
    }

}