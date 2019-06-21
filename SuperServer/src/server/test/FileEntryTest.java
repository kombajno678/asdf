package server.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.FileEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FileEntryTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void equals() {

        assertNotEquals(new FileEntry(), null);

        assertEquals(new FileEntry(), new FileEntry());

        FileEntry f1 = new FileEntry("file01", 1, "path", 420, "user01");

        assertNotEquals(f1, null);

        assertEquals(f1, f1);

        FileEntry f2 = new FileEntry("file01", 1, "path", 420, "user01");

        assertEquals(f1, f2);

        String l[] = new String[]{"user1", "user2"};
        ArrayList<String> others = new ArrayList<>(Arrays.asList(l));
        FileEntry f3 = new FileEntry("file01", 1, "path", 420, "user01", others);

        assertNotEquals(f1, f3);

        l= new String[]{"user1", "user3"};
        others = new ArrayList<>(Arrays.asList(l));
        FileEntry f4 = new FileEntry("file01", 1, "path", 420, "user01", others);

        assertNotEquals(f3, f4);


    }
}