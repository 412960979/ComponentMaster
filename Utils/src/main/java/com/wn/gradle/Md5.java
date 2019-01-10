package com.wn.gradle;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Md5 {
    public static String getMd5Str(String text) {
        StringBuilder hashtext = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes());
            BigInteger bigInt = new BigInteger(1, digest);
            hashtext = new StringBuilder(bigInt.toString(16));
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return hashtext.toString();
    }
}
