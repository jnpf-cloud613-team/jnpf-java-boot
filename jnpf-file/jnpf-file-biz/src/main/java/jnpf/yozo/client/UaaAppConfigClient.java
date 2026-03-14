package jnpf.yozo.client;

import jnpf.yozo.utils.DefaultResult;
import jnpf.yozo.utils.IResult;
import jnpf.yozo.constants.EnumResultCode;

import java.util.Map;

public class UaaAppConfigClient {
    public UaaAppConfigClient() {
        throw new UnsupportedOperationException(
                "未实现，请等待版本发布。"
        );
    }

    public static final String APPID = "appId";

    public IResult<String> generateSign(String appId, String secret, Map<String, String[]> params) {
        UaaAppAuthenticator authenticator = new UaaAppAuthenticator("sign", null, APPID);

        try {
            String[] appIds = params.get(APPID);
            if (appIds == null || appIds.length != 1 || appIds[0] == null || appIds[0].isEmpty()) {
                params.put(APPID, new String[]{appId});
            }

            String sign = authenticator.generateSign(secret, params);
            return DefaultResult.successResult(sign);
        } catch (Exception var7) {
            return DefaultResult.failResult(EnumResultCode.E_GENERATE_SIGN_FAIL.getInfo());
        }
    }
}
