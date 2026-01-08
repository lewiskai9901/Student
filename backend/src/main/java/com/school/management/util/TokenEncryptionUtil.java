package com.school.management.util;

import com.school.management.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Token加密工具类
 * 使用AES-GCM模式加密敏感Token数据
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
public class TokenEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // 从环境变量或配置获取加密密钥
    private static volatile byte[] encryptionKey = null;

    /**
     * 初始化加密密钥
     * 应在应用启动时调用
     */
    public static void initKey(String secret) {
        try {
            // 使用SHA-256将密钥转换为固定长度的256位密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encryptionKey = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            log.info("Token加密密钥初始化成功");
        } catch (Exception e) {
            log.error("Token加密密钥初始化失败", e);
            throw new BusinessException("系统初始化失败");
        }
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return encryptionKey != null;
    }

    /**
     * 加密Token
     *
     * @param plainToken 明文Token
     * @return 加密后的Token（Base64编码）
     */
    public static String encrypt(String plainToken) {
        if (plainToken == null || plainToken.isEmpty()) {
            return plainToken;
        }

        if (encryptionKey == null) {
            log.warn("Token加密密钥未初始化，跳过加密");
            return plainToken;
        }

        try {
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 创建加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // 加密
            byte[] encryptedBytes = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

            // 组合IV和加密数据
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Token加密失败", e);
            throw new BusinessException("Token处理失败");
        }
    }

    /**
     * 解密Token
     *
     * @param encryptedToken 加密的Token（Base64编码）
     * @return 解密后的明文Token
     */
    public static String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return encryptedToken;
        }

        if (encryptionKey == null) {
            log.warn("Token加密密钥未初始化，跳过解密");
            return encryptedToken;
        }

        try {
            // 解码Base64
            byte[] combined = Base64.getDecoder().decode(encryptedToken);

            // 如果数据太短，可能是未加密的旧数据
            if (combined.length < GCM_IV_LENGTH + 16) {
                log.debug("数据长度不足，可能是未加密的旧Token");
                return encryptedToken;
            }

            // 提取IV和加密数据
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] encryptedBytes = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            // 创建解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // 解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Base64解码失败，可能是未加密的旧Token
            log.debug("Base64解码失败，返回原始Token");
            return encryptedToken;
        } catch (Exception e) {
            log.error("Token解密失败", e);
            // 解密失败时返回原值，兼容旧数据
            return encryptedToken;
        }
    }

    /**
     * 计算Token的哈希值（用于比较而非存储完整Token）
     *
     * @param token Token字符串
     * @return SHA-256哈希值的十六进制表示
     */
    public static String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Token哈希计算失败", e);
            return null;
        }
    }
}
