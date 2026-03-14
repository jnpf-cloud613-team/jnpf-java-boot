package jnpf.yozo.client;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface AppAuthenticator {
    String generateSign(String var1, Map<String, String[]> var2) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException;
}
