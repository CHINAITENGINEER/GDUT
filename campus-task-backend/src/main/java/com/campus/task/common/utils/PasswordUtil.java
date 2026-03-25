package com.campus.task.common.utils;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具类（优先使用 BCrypt，兼容历史 SHA256+盐）
 */
@Component
public class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 生成随机盐值
     */
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt).substring(0, SALT_LENGTH);
    }

    /**
     * 加密密码
     *
     * @param password 明文密码
     * @param salt     兼容参数，新逻辑不再依赖
     * @return BCrypt 哈希
     */
    public String encode(String password, String salt) {
        return BCRYPT.encode(password);
    }

    /**
     * 校验密码
     *
     * @param rawPassword     用户输入的明文密码
     * @param encodedPassword 数据库存储的密码
     * @param salt            历史盐值（仅旧版 SHA256+盐 使用）
     */
    public boolean matches(String rawPassword, String encodedPassword, String salt) {
        if (encodedPassword == null) {
            return false;
        }
        // 新用户：BCrypt
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return BCRYPT.matches(rawPassword, encodedPassword);
        }
        // 兼容老用户：SHA256(raw + salt)
        if (salt == null) {
            return false;
        }
        return DigestUtil.sha256Hex(rawPassword + salt).equals(encodedPassword);
    }
}
