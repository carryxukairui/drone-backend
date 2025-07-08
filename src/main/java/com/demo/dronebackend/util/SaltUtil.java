package com.demo.dronebackend.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class SaltUtil {

    private static final String ALGORITHM = "SHA1PRNG"; // 或 "NativePRNG"、"DRBG"
    private static final int DEFAULT_SALT_LENGTH = 16;

    private SaltUtil() { }

    /**
     * 生成指定长度的随机盐，返回 Base64 编码字符串。
     *
     * @param lengthBytes 盐的字节长度，推荐 16 或 32
     * @return Base64 编码的盐字符串
     */
    public static String generateSalt(int lengthBytes) {
        try {
            SecureRandom sr = SecureRandom.getInstance(ALGORITHM);
            byte[] salt = new byte[lengthBytes];
            sr.nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        } catch (NoSuchAlgorithmException e) {
            // 不应该发生，fallback 到默认构造
            byte[] salt = new byte[lengthBytes];
            new SecureRandom().nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }
    }

    /**
     * 生成默认长度（16 字节）的随机盐。
     */
    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }
}
