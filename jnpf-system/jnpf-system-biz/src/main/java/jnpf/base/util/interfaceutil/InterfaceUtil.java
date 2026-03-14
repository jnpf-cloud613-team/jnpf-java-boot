package jnpf.base.util.interfaceutil;

import cn.hutool.core.collection.CollUtil;
import jnpf.util.DateUtil;
import jnpf.util.ServletUtil;
import jnpf.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 接口工具类
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/13 10:38
 */
@Slf4j
public class InterfaceUtil {

    InterfaceUtil(){

    }
    public static final String ALGORITH_FORMAC = "HmacSHA256";
    public static final String HOST = "Host";
    public static final String YMDATE = "YmDate";
    public static final String CONTENT_TYPE = " Content-Type";
    public static final String CHARSET_NAME = "utf-8";
    public static final String USERKEY = "UserKey";

    /**
     * 验证签名
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/6/14
     */
    public static boolean verifySignature(String secret, String author) throws NoSuchAlgorithmException, InvalidKeyException{
        String method = ServletUtil.getRequest().getMethod();
        String url = ServletUtil.getRequest().getRequestURI();
        String ymdate = ServletUtil.getRequest().getHeader(YMDATE);
        String host = ServletUtil.getRequest().getHeader(HOST);
        String source = new StringBuilder()
                .append(method).append('\n')
                .append(url).append('\n')
                .append(ymdate).append('\n')
                .append(host).append('\n').toString();
        Mac mac = Mac.getInstance(ALGORITH_FORMAC);
        SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decodeBase64(secret), ALGORITH_FORMAC);
        mac.init(secretKeySpec);
        String signature = Hex.encodeHexString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        return author.equals(signature);
    }

    /**
     * map转 name=value&name=value格式
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/6/14
     */
    public static String createLinkStringByGet(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder prestr = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            value = URLEncoder.encode(value, "UTF-8");
            if (i == keys.size() - 1) {//拼接时，不包括最后一个&字符
                prestr.append(key).append("=").append(value);
            } else {
                prestr.append(key).append("=").append(value).append("&");
            }
        }
        return prestr.toString();
    }

    /**
     * 判断map内有没有指定key的值
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/6/14
     */
    public static boolean checkParam(Map<String, String> map, String str) {
        if (CollUtil.isEmpty(map)) {
            return false;
        }
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        return map.get(str) != null && StringUtil.isNotEmpty(map.get(str));
    }

    //
    public static Map<String, String> getAuthorization(String intefaceId, String appId, String appSecret) {
        Map<String, String> resultMap = new HashMap<>();
        try {
            String method = ServletUtil.getRequest().getMethod();
            String url = "/api/system/DataInterface/" + intefaceId + "/Actions/Response";
            String ymdate = "" + DateUtil.getNowDate().getTime();
            String host = ServletUtil.getRequest().getHeader(HOST);
            String source = new StringBuilder()
                    .append(method).append('\n')
                    .append(url).append('\n')
                    .append(ymdate).append('\n')
                    .append(host).append('\n').toString();
            Mac mac = Mac.getInstance(ALGORITH_FORMAC);
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decodeBase64(appSecret), ALGORITH_FORMAC);
            mac.init(secretKeySpec);
            String signature = Hex.encodeHexString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
            resultMap.put(YMDATE, ymdate);
            resultMap.put("Authorization", appId + "::" + signature);
            return resultMap;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return resultMap;
    }
}

