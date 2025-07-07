package com.demo.dronebackend.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public final class MD5Util {


    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();


    private MD5Util() { }

    /**
     * 对输入字符串做 MD5 哈希，并返回 hex 格式结果
     */
    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不存在", e);
        }
    }

    /**
     * 带盐的 MD5 哈希：先将 password + salt 拼接，然后做 MD5
     */
    public static String hash(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("password 和 salt 都不能为空");
        }
        return md5Hex(password + salt);
    }

    /**
     * 将字节数组转成十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2]     = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
