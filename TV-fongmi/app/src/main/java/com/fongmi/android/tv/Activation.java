package com.fongmi.android.tv;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Prefers;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Activation {

    private static final String PREF_DEVICE_ID = "activation_device_id";
    private static final String PREF_CODE = "activation_code";
    private static final String PREF_TIME = "activation_time";
    private static final String SECRET = "ylys_neibu_jz_2024_secret_key";

    private static final char[] TIMESTAMP_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] HMAC_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*".toCharArray();

    public static void notifyActivated() {
        EventBus.getDefault().post(new ActivationEvent());
    }

    public static String getDeviceId() {
        String id = Prefers.getString(PREF_DEVICE_ID);
        if (TextUtils.isEmpty(id)) {
            id = generateDeviceId();
            Prefers.put(PREF_DEVICE_ID, id);
        }
        return id;
    }

    private static String generateDeviceId() {
        String androidId = Util.getAndroidId();
        String serial = Util.getSerial();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return sha256(androidId + serial + uuid).substring(0, 32).toUpperCase();
    }

    public static boolean isActivated() {
        String storedCode = Prefers.getString(PREF_CODE);
        String storedDevice = Prefers.getString(PREF_DEVICE_ID);
        if (TextUtils.isEmpty(storedCode) || TextUtils.isEmpty(storedDevice)) return false;
        String currentDevice = getDeviceId();
        if (!storedDevice.equals(currentDevice)) return false;
        return validateCode(storedCode);
    }

    public static boolean activate(String code) {
        if (!validateCode(code)) return false;
        Prefers.put(PREF_CODE, code);
        Prefers.put(PREF_DEVICE_ID, getDeviceId());
        Prefers.put(PREF_TIME, System.currentTimeMillis());
        notifyActivated();
        return true;
    }

    public static boolean validateCode(String code) {
        if (code == null) return false;
        code = code.replace("-", "").replace(" ", "");
        if (code.length() != 30) return false;
        if (!hasRequiredCharTypes(code)) return false;
        try {
            String tsPart = code.substring(0, 8);
            String hmacPart = code.substring(8, 16);
            long expiryHour = decodeBase36Reverse(tsPart);
            String expectedHmac = computeHmac(expiryHour);
            if (!hmacPart.equals(expectedHmac)) return false;
            long currentHour = System.currentTimeMillis() / 3600000;
            return currentHour <= expiryHour;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasRequiredCharTypes(String code) {
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : code.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private static long decodeBase36Reverse(String s) {
        String reversed = new StringBuilder(s).reverse().toString();
        long result = 0;
        for (char c : reversed.toCharArray()) {
            result = result * 36 + Character.digit(c, 36);
        }
        return result;
    }

    private static String computeHmac(long expiryHour) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(String.valueOf(expiryHour).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static class ActivationEvent {
    }
}
