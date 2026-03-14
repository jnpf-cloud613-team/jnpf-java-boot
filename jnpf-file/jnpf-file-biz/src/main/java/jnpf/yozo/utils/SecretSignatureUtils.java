package jnpf.yozo.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecretSignatureUtils {
    public static final String SHA256 = "HmacSHA256";

    private SecretSignatureUtils() {
    }

    public static String hmacSHA256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), SHA256);
        mac.init(secretKey);
        byte[] array = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        for (byte item : array) {
            sb.append(Integer.toHexString(item & 255 | 256), 1, 3);
        }

        return sb.toString().toUpperCase();
    }
}
