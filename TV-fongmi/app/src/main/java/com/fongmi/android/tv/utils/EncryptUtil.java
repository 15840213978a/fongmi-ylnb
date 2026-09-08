package com.fongmi.android.tv.utils;

import android.util.Base64;

public class EncryptUtil {

    private static final String KEY = "ylys_2027";

    public static String decrypt(String encrypted) {
        byte[] decoded = Base64.decode(encrypted, Base64.DEFAULT);
        return xor(new String(decoded), KEY);
    }

    public static String encrypt(String plain) {
        String encrypted = xor(plain, KEY);
        return Base64.encodeToString(encrypted.getBytes(), Base64.DEFAULT);
    }

    private static String xor(String text, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append((char) (text.charAt(i) ^ key.charAt(i % key.length())));
        }
        return result.toString();
    }
}