package com.gamelyx.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    public static String encrypt(String content, String key) {
        try {
            // Asegurar que la clave tenga exactamente 32 bytes para AES-256
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                // Ajustar clave a exactamente 32 bytes
                byte[] fixedKey = new byte[32];
                if (keyBytes.length < 32) {
                    // Rellenar con ceros si es más corta
                    System.arraycopy(keyBytes, 0, fixedKey, 0, keyBytes.length);
                } else {
                    // Truncar si es más larga
                    System.arraycopy(keyBytes, 0, fixedKey, 0, 32);
                }
                keyBytes = fixedKey;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] encryptedContent = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            
            // Combinar IV + contenido cifrado
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedContent.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedContent, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedContent.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el mensaje", e);
        }
    }

    public static String decrypt(String encryptedContent, String key) {
        try {
            // Asegurar que la clave tenga exactamente 32 bytes para AES-256
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                // Ajustar clave a exactamente 32 bytes
                byte[] fixedKey = new byte[32];
                if (keyBytes.length < 32) {
                    // Rellenar con ceros si es más corta
                    System.arraycopy(keyBytes, 0, fixedKey, 0, keyBytes.length);
                } else {
                    // Truncar si es más larga
                    System.arraycopy(keyBytes, 0, fixedKey, 0, 32);
                }
                keyBytes = fixedKey;
            }
            
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            
            byte[] decodedContent = Base64.getDecoder().decode(encryptedContent);
            
            // Extraer IV (primeros 12 bytes)
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedContent, 0, iv, 0, GCM_IV_LENGTH);
            
            // Extraer contenido cifrado (resto)
            byte[] encrypted = new byte[decodedContent.length - GCM_IV_LENGTH];
            System.arraycopy(decodedContent, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] decryptedContent = cipher.doFinal(encrypted);
            
            return new String(decryptedContent, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el mensaje", e);
        }
    }

    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar clave de cifrado", e);
        }
    }
}