package com.wn.gradle;

public class TextUtils {
    private TextUtils(){}

    public static String firstUpperCase(String text) {
        if (text.length() > 1) {
            return String.valueOf(text.charAt(0)).toUpperCase() + text.substring(1, text.length());
        } else {
            return text.toUpperCase();
        }
    }

    public static boolean isEmpty(String text) {
        return text == null || "".equals(text);
    }


}
