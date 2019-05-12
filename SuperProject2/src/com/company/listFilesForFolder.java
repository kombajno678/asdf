package com.company;

import java.io.File;
import java.util.ArrayList;

public class listFilesForFolder {
    public static ArrayList<String> main(final File folder) {
        ArrayList<String> list = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                main(fileEntry);
            } else {
                list.add(fileEntry.getName());
            }
        }
        return list;
    }
}
