package server.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import server.Connection;
import server.FileEntry;
import server.ServerThread;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

import static org.junit.Assert.*;

public class ConnectionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void shareFile(){
        String filename = "file01";
        String owner = "user1";
        String other = "user2";
        String temp = "share:"+filename+":"+owner+":"+other;
        ArrayList<FileEntry> list = new ArrayList<>();
        list.add(new FileEntry(filename, 1, "path", 420, owner));
        list.add(new FileEntry(filename+"2", 1, "path", 420, owner));
        ServerThread s = new ServerThread();
        s.setFilesList(list);
        assertEquals(s.getFilesList().get(0).getOthers().size(), 0);
        Connection c = new Connection(s);
        c.shareFile(temp);
        assertEquals(s.getFilesList().get(0).getOthers().size(), 1);
        c.shareFile(temp);
        assertEquals(s.getFilesList().get(0).getOthers().size(), 1);
        c.shareFile("share:"+filename+":"+owner+":"+other+"1");
        assertEquals(s.getFilesList().get(0).getOthers().size(), 2);
    }

    @Test
    public void unshareFile(){
        ArrayList<FileEntry> list = new ArrayList<>();
        ArrayList<String> others = new ArrayList<>();
        others.add("user2");
        others.add("user3");
        others.add("user4");
        others.add("user5");
        others.add("user6");
        list.add(new FileEntry("file1", 1, "path", 420, "user1",others ));
        list.add(new FileEntry("file2", 1, "path2", 420, "user2" ));
        ServerThread s = new ServerThread();
        s.setFilesList(list);
        assertEquals(s.getFilesList().get(0).getOthers().size(), 5);
        Connection c = new Connection(s);
        c.unshareFile("unshare:file1:user1:user2");
        assertEquals(s.getFilesList().get(0).getOthers().size(), 4);
        c.unshareFile("unshare:file1:user1:user2");
        assertEquals(s.getFilesList().get(0).getOthers().size(), 4);
        c.unshareFile("unshare:file1:user1:user6");
        assertEquals(s.getFilesList().get(0).getOthers().size(), 3);
        c.unshareFile("unshare:file1:user1:user4");
        assertEquals(s.getFilesList().get(0).getOthers().size(), 2);
    }

    @Test
    public void userLogin(){
        String username = "test";
        String username2 = "test2";
        String rootFolder = "test_folder";
        ServerThread s = new ServerThread();
        ArrayList<String> hdd = new ArrayList<>();
        hdd.add(rootFolder+ File.separator+File.separator+"hdd1");
        hdd.add(rootFolder+ File.separator+File.separator+"hdd2");
        s.setHdd(hdd);
        Connection c = new Connection(s);
        c.userLogin("login:"+username);
        assertEquals(s.getUsersOnline().size(), 1);
        c.userLogin("login:"+username);
        assertEquals(s.getUsersOnline().size(), 1);
        c.userLogin("login:"+username2);
        assertEquals(s.getUsersOnline().size(), 2);
        //check if folders have been created
        File root = new File(rootFolder);
        assertTrue(root.exists());
        File hdd1 = new File(rootFolder+ File.separator+File.separator+"hdd1");
        assertTrue(hdd1.exists());
        File hdd1_test = new File(rootFolder+ File.separator+File.separator+"hdd1"+File.separator+File.separator+username);
        assertTrue(hdd1_test.exists());
        File hdd1_test2 = new File(rootFolder+ File.separator+File.separator+"hdd1"+File.separator+File.separator+username2);
        assertTrue(hdd1_test2.exists());
        File hdd2 = new File(rootFolder+ File.separator+File.separator+"hdd2");
        assertTrue(hdd2.exists());
        File hdd2_test = new File(rootFolder+ File.separator+File.separator+"hdd2"+File.separator+File.separator+username);
        assertTrue(hdd2_test.exists());
        File hdd2_test2 = new File(rootFolder+ File.separator+File.separator+"hdd2"+File.separator+File.separator+username2);
        assertTrue(hdd2_test2.exists());
        deleteDirectory(root);
    }

    @Test
    public void userLogout(){
        String username = "test";
        ServerThread s = new ServerThread();
        ArrayList<String> usersLoggedIn = new ArrayList<>();
        usersLoggedIn.add("user1");
        usersLoggedIn.add("user2");
        usersLoggedIn.add("user3");
        usersLoggedIn.add("user4");
        s.setUsersOnline(usersLoggedIn);
        Connection c = new Connection(s);
        assertEquals(s.getUsersOnline().size(), 4);
        c.userLogout("logout:user5");
        assertEquals(s.getUsersOnline().size(), 4);
        c.userLogout("logout:user5");
        assertEquals(s.getUsersOnline().size(), 4);
        c.userLogout("logout:user4");
        assertEquals(s.getUsersOnline().size(), 3);
        c.userLogout("logout:user3");
        assertEquals(s.getUsersOnline().size(), 2);
        c.userLogout("logout:user2");
        assertEquals(s.getUsersOnline().size(), 1);
        c.userLogout("logout:user1");
        assertEquals(s.getUsersOnline().size(), 0);
        c.userLogout("logout:user1");
        assertEquals(s.getUsersOnline().size(), 0);

    }

    @Test
    public void deleteFile() throws IOException{
        String filename = "file";
        String owner = "user";
        String root = "test_folder";
        String path = root + File.separator + File.separator + owner + File.separator + File.separator + filename;

        FileEntry f = new FileEntry(filename, 1, path, 420, owner);

        String temp = "delete:"+filename+":"+owner;
        //create folders and file to be deleted
        File rootFolder = new File(root);
        if(!rootFolder.exists())rootFolder.mkdirs();
        assertTrue(rootFolder.exists());

        File userFolder = new File(root+File.separator+File.separator+owner);
        if(!userFolder.exists())userFolder.mkdirs();
        assertTrue(userFolder.exists());

        File file = new File(path);
        file.createNewFile();
        assertTrue(file.exists());

        ServerThread s = new ServerThread();
        s.getFilesList().add(f);
        assertEquals(s.getFilesList().size(), 1);

        Connection c = new Connection(s);

        assertFalse(c.deleteFile("delete:"+filename+"123"+":"+owner));
        assertFalse(c.deleteFile("delete:"+filename+":"+owner+"123"));
        assertTrue(c.deleteFile("delete:"+filename+":"+owner));

        deleteDirectory(rootFolder);
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}