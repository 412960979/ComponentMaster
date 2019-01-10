package com.wn.gradle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    /**
     * 通配符匹配
     *
     * @param pattern 通配符模式
     * @param str     待匹配的字符串
     * @return 匹配成功则返回true，否则返回false
     */
    public static boolean wildcardMatch(String pattern, String str) {
        int patternLength = pattern.length();
        int strLength = str.length();
        int strIndex = 0;
        char ch;
        for (int patternIndex = 0; patternIndex < patternLength; patternIndex++) {
            ch = pattern.charAt(patternIndex);
            if (ch == '*') {
                //通配符星号*表示可以匹配任意多个字符
                while (strIndex < strLength) {
                    if (wildcardMatch(pattern.substring(patternIndex + 1),
                            str.substring(strIndex))) {
                        return true;
                    }
                    strIndex++;
                }
            } else if (ch == '?') {
                //通配符问号?表示匹配任意一个字符
                strIndex++;
                if (strIndex > strLength) {
                    //表示str中已经没有字符匹配?了。
                    return false;
                }
            } else {
                if ((strIndex >= strLength) || (ch != str.charAt(strIndex))) {
                    return false;
                }
                strIndex++;
            }
        }
        return (strIndex == strLength);
    }


    public static List<File> recursiveListFiles(@Nonnull File dir) {
        return recursiveListFiles(dir, null);
    }

    public static List<File> recursiveListFiles(@Nonnull File dir
            , @Nullable FilenameFilter filenameFilter) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("need dir");
        }

        return recursiveListFiles(dir, filenameFilter, new ArrayList<>());
    }

    public static String getRelativePath(File relativeFile, File srcFile) {
        if (relativeFile.isFile()) {
            relativeFile = relativeFile.getParentFile();
        }

        String p1 = relativeFile.getAbsolutePath();
        String p2 = srcFile.getAbsolutePath();

        if (!p2.startsWith(p1)) {
            throw new RuntimeException("getRelativePath");
        }

        return p2.substring(p1.length() + 1);
    }

    private static List<File> recursiveListFiles(@Nonnull File dir
            , @Nullable FilenameFilter filenameFilter
            , @Nonnull List<File> files) {
        String[] list = dir.list();

        assert list != null;
        for (String f : list) {
            if (f == null) continue;
            File file = new File(dir, f);
            if (!file.exists()) continue;
            if (file.isDirectory()) {
                recursiveListFiles(file, filenameFilter, files);
            } else {
                if (filenameFilter == null || filenameFilter.accept(file, f)) {
                    files.add(file);
                }
            }
        }

        return files;
    }


    public static String getSystemTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }
}
