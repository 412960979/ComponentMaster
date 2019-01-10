package com.wn.gradle.util;

import com.wn.gradle.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;


public class ZipUtilsCompat {


    public static boolean findEntryName(File zipFile, String... names) {
        if (names.length == 0) throw new RuntimeException("names need count > 0");

        List<String> ns = Arrays.asList(names);

        Enumeration entries;
        try {
            entries = ZipUtils.getEntriesEnumeration(zipFile);
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            try {
                String entryName = ZipUtils.getEntryName(entry);
                if (ns.contains(entryName)) {
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();

                return false;
            }
        }


        return false;
    }
}
