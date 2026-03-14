package jnpf.yozo.client;

import jnpf.yozo.utils.SecretSignatureUtils;
import jnpf.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class UaaAppAuthenticator implements AppAuthenticator {
    public static final String OPEN_PARAM_PREFIX = "y_";
    public static final String OPEN_PARAM_APPID = "y_appid";
    public static final String OPEN_PARAM_SIGN = "y_SIGN";
    private String signParamName;
    private String paramNamePrefix;
    private String appidParamName;

    public UaaAppAuthenticator() {
    }

    public UaaAppAuthenticator(String signParamName, String paramNamePrefix, String appidParamName) {
        this.signParamName = signParamName;
        this.paramNamePrefix = paramNamePrefix;
        this.appidParamName = appidParamName;
    }

    public String getSignParamName() {
        return this.signParamName;
    }

    public void setSignParamName(String signParamName) {
        this.signParamName = signParamName;
    }

    public String getParamNamePrefix() {
        return this.paramNamePrefix;
    }

    public void setParamNamePrefix(String paramNamePrefix) {
        this.paramNamePrefix = paramNamePrefix;
    }

    public String getAppidParamName() {
        return this.appidParamName;
    }

    public void setAppidParamName(String appidParamName) {
        this.appidParamName = appidParamName;
    }

    public String generateSign(String secret, Map<String, String[]> params) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String fullParamStr = this.uniqSortParams(params);
        return SecretSignatureUtils.hmacSHA256(fullParamStr, secret);
    }

    private String uniqSortParams(Map<String, String[]> params) {
        boolean prefix = StringUtil.isNotEmpty(this.paramNamePrefix);
        params.remove(this.signParamName);
        String[] paramKeys = new String[params.keySet().size()];
        int idx = 0;
        Iterator<String> var5 = params.keySet().iterator();

        while(true) {
            String param;
            do {
                if (!var5.hasNext()) {
                    Arrays.sort(paramKeys, 0, idx);
                    StringBuilder builder = new StringBuilder();
                    String[] var16 = paramKeys;
                    int var7 = paramKeys.length;

                    for(int var8 = 0; var8 < var7; ++var8) {
                        String key = var16[var8];
                        String[] values = params.get(key);
                        if (values != null && values.length > 0) {
                            Arrays.sort(values);
                            String[] var11 = values;
                            int var12 = values.length;

                            for(int var13 = 0; var13 < var12; ++var13) {
                                String val = var11[var13];
                                builder.append(key).append("=").append(val);
                            }
                        } else {
                            builder.append(key).append("=");
                        }
                    }

                    return builder.toString();
                }

                param = var5.next();
            } while(prefix && param.startsWith(this.paramNamePrefix));

            paramKeys[idx++] = param;
        }
    }
}
