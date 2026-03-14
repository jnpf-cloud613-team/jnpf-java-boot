package jnpf.yozo.utils;

import java.util.Objects;

public class HttpRequestUtils {
    private HttpRequestUtils() {
    }

    public static class StringUtils {
        private StringUtils() {
        }

        public static boolean isNotEmpty(String data) {
            return data != null && !data.trim().isEmpty();
        }

        public static boolean equals(String cs1, String cs2) {
            return Objects.equals(cs1, cs2);
        }
    }
}
