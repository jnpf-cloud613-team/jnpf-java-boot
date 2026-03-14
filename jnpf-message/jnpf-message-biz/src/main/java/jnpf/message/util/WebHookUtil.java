package jnpf.message.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import jnpf.constant.KeyConst;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WebHookUtil {
    WebHookUtil() {
    }

    private static Logger logger = LoggerFactory.getLogger(WebHookUtil.class);

    private static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 把timestamp+"\n"+密钥当做签名字符串并计算签名
     *
     * @param secret    bearer令牌
     * @param timestamp 当前时间的时间戳格式
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private static String genSign(String secret, Long timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        //把timestamp+"\n"+密钥当做签名字符串
        String stringToSign = timestamp + "\n" + secret;

        //使用HmacSHA256算法计算签名
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(signData));
    }

    /**
     * 发送POST请求，参数是Map, contentType=x-www-form-urlencoded
     *
     * @param url
     * @param mapParam
     * @return
     */
    public static String sendPostByMap(String url, Map<String, Object> mapParam) {
        Map<String, String> headParam = new HashMap<>();
        headParam.put("Content-type", "application/json;charset=UTF-8");
        return sendPost(url, mapParam, headParam);
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, Map<String, Object> param, Map<String, String> headParam) {
        StringBuilder result = new StringBuilder();

        try {
            // 使用 URI 替换过时的 URL 构造函数
            URI uri = new URI(url);
            URL realUrl = uri.toURL();

            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();

            // 设置通用的请求属性 请求头
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Fiddler");

            if (headParam != null) {
                for (Map.Entry<String, String> entry : headParam.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 使用 try-with-resources 自动关闭资源
            try (PrintWriter out = new PrintWriter(conn.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

                // 发送请求参数
                out.print(JSON.toJSONString(param));
                // flush输出流的缓冲
                out.flush();

                // 读取URL的响应
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            }

        } catch (Exception e) {
            logger.error("发送 POST 请求出现异常！URL: {}", url, e);
        }

        return result.toString();
    }

    /**
     * 发送文字消息
     *
     * @param msg 需要发送的消息
     * @return
     * @throws Exception
     */
    public static String sendTextMsg(String msg) {
        JSONObject text = new JSONObject();
        text.put(KeyConst.CONTENT, msg);
        JSONObject reqBody = new JSONObject();
        reqBody.put(KeyConst.MSG_TYPE, KeyConst.TEXT);
        reqBody.put(KeyConst.TEXT, text);
        reqBody.put("safe", 0);

        return reqBody.toString();
    }


    /**
     * 对接企业微信机器人发送消息（webhook）
     *
     * @content：要发送的消息 WECHAT_GROUP：机器人的webhook
     */
    public static JSONObject callWeChatBot(String url, String content) {
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .build();
        MediaType mediaType = MediaType.parse(CONTENT_TYPE_JSON);
        content = sendTextMsg(content);
        RequestBody body = RequestBody.create(content, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", CONTENT_TYPE_JSON)
                .build();
        String result = "";
        try (Response response = client.newCall(request).execute()) {
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return JSON.parseObject(result);
    }


    /**
     * 对接钉钉机器人发送消息（webhook）
     *
     * @param url 钉钉上获取到的webhook的值
     * @param msg 发送的消息内容
     */
    public static JSONObject sendDDMessage(String url, String msg) {
        //钉钉的webhook
        //请求的JSON数据，这里用map在工具类里转成json格式
        Map<String, Object> json = new HashMap<>();
        Map<String, Object> text = new HashMap<>();
        json.put(KeyConst.MSG_TYPE, KeyConst.TEXT);
        text.put(KeyConst.CONTENT, msg);
        json.put(KeyConst.TEXT, text);
        //发送post请求
        String response = WebHookUtil.sendPostByMap(url, json);
        return JSON.parseObject(response);
    }

    /**
     * 通过加签方式调用钉钉机器人发送消息给所有人
     */
    public static JSONObject sendDingDing(String url, String secret, String content) {
        try {
            //钉钉机器人地址（配置机器人的webhook）
            Long timestamp = System.currentTimeMillis();
            String sign = genSign(secret, timestamp);
            String dingUrl = url + "&timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, "UTF-8");
            //是否通知所有人
            boolean isAtAll = true;
            //消息内容
            Map<String, String> contentMap = Maps.newHashMap();
            contentMap.put(KeyConst.CONTENT, content);
            //通知人
            Map<String, Object> atMap = Maps.newHashMap();
            //1.是否通知所有人
            atMap.put("isAtAll", isAtAll);
            Map<String, Object> reqMap = Maps.newHashMap();
            reqMap.put(KeyConst.MSG_TYPE, KeyConst.TEXT);
            reqMap.put(KeyConst.TEXT, contentMap);
            reqMap.put("at", atMap);
            //推送消息（http请求）
            String result = sendPostByMap(dingUrl, reqMap);
            return JSON.parseObject(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

}
