package com.xiongdwm.future_backend.utils.ecc;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ECDH 密钥交换 + AES-256-GCM 加解密工具
 * <p>
 * 流程:
 * 1. 客户端生成 ECDH 密钥对，将公钥发送给后端
 * 2. 后端生成 ECDH 密钥对，用客户端公钥 + 自身私钥派生共享密钥
 * 3. 后端将自己的公钥返回给客户端
 * 4. 双方用共享密钥做 AES-GCM 对称加解密
 */
public class ECDHKeyGen {

    private static final String CURVE_NAME = "secp256r1";
    private static final String KEY_AGREEMENT_ALGO = "ECDH";
    private static final String EC_ALGO = "EC";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12;   // bytes

    static {
        if(Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 生成 ECDH 密钥对
     */
    public static KeyPair generateKeyPair() throws Exception {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EC_ALGO, "BC");
        keyGen.initialize(ecSpec, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * 将公钥编码为 Base64（用于网络传输）
     */
    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 从 Base64 + X509 编码恢复公钥
     */
    public static PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGO, "BC");
        return keyFactory.generatePublic(spec);
    }

    /**
     * ECDH 密钥协商 -> 派生 AES-256 密钥
     *
     * @param myPrivateKey    己方私钥
     * @param otherPublicKey  对方公钥
     * @return AES SecretKey (256-bit, 取 SHA-256 of shared secret)
     */
    public static SecretKey deriveSharedSecret(PrivateKey myPrivateKey, PublicKey otherPublicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGO, "BC");
        keyAgreement.init(myPrivateKey);
        keyAgreement.doPhase(otherPublicKey, true);

        byte[] sharedSecret = keyAgreement.generateSecret();

        // 用 SHA-256 派生固定 256-bit 密钥
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derivedKey = sha256.digest(sharedSecret);

        return new SecretKeySpec(derivedKey, "AES");
    }

    /**
     * AES-256-GCM 加密
     *
     * @return Base64 编码的 (IV + ciphertext + tag)
     */
    public static String encrypt(SecretKey key, String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // IV(12) + ciphertext+tag 拼接
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * AES-256-GCM 解密
     *
     * @param encryptedBase64 Base64 编码的 (IV + ciphertext + tag)
     */
    public static String decrypt(SecretKey key, String encryptedBase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

        byte[] ciphertext = new byte[decoded.length - GCM_IV_LENGTH];
        System.arraycopy(decoded, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, "UTF-8");
    }

    // ======================== HMAC-SHA256 签名验证 ========================

    /**
     * HMAC-SHA256 签名
     *
     * @param key     AES 密钥（同时用作 HMAC 密钥）
     * @param message 待签名的消息
     * @return hex 编码的签名字符串
     */
    public static String hmacSign(SecretKey key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec hmacKey = new SecretKeySpec(key.getEncoded(), "HmacSHA256");
        mac.init(hmacKey);
        byte[] result = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(result);
    }

    /**
     * 验证 HMAC-SHA256 签名
     *
     * @param key       AES 密钥
     * @param message   原始消息  (METHOD\npath\ntimestamp\nbody)
     * @param signature 前端传来的 hex 签名
     * @return 签名是否匹配
     */
    public static boolean verifySignature(SecretKey key, String message, String signature) {
        try {
            String expected = hmacSign(key, message);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
