package jnpf.message.util;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.Session;
import jnpf.consts.AuthConsts;
import jnpf.util.UserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static jnpf.consts.AuthConsts.TOKEN_PREFIX;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 10:51
 */
@Slf4j
public class OnlineUserProvider {
    OnlineUserProvider() {
    }

    private static final Object lock = new Object();
    /**
     * 在线用户
     */
    private static final List<OnlineUserModel> onlineUserList = new ArrayList<>();

    public static List<OnlineUserModel> getOnlineUserList() {
        return OnlineUserProvider.onlineUserList;
    }


    public static void addModel(OnlineUserModel model) {
        synchronized (onlineUserList) {
            OnlineUserProvider.onlineUserList.add(model);
        }
    }

    public static void removeModel(OnlineUserModel onlineUserModel) {
        synchronized (onlineUserList) {
            onlineUserList.remove(onlineUserModel);
        }
    }


    // =================== Websocket相关操作 ===================

    /**
     * 根据Token精准推送Websocket 登出消息
     *
     * @param token
     */
    public static void removeWebSocketByToken(String... token) {
        List<String> tokens = Arrays.stream(token).map(t -> t.contains(AuthConsts.TOKEN_PREFIX) ? t : TOKEN_PREFIX + " " + t).collect(Collectors.toList());
        //清除websocket登录状态
        List<OnlineUserModel> users = OnlineUserProvider.getOnlineUserList().stream().filter(t -> tokens.contains(t.getToken())).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(users)) {
            for (OnlineUserModel user : users) {
                OnlineUserProvider.logoutWS(user, null);
                //先移除对象， 并推送下线信息， 避免网络原因导致就用户未断开 新用户连不上WebSocket
                OnlineUserProvider.removeModel(user);
                //通知所有在线，有用户离线
            }
        }
    }

    /**
     * 根据用户ID 推送全部Websocket 登出消息
     *
     * @param userId
     */
    public static void removeWebSocketByUser(String userId) {
        List<String> tokens = StpUtil.getTokenValueListByLoginId(UserProvider.splicingLoginId(userId));
        removeWebSocketByToken(tokens.toArray(new String[tokens.size()]));
    }

    /**
     * 发送用户退出消息
     *
     * @param session
     */
    public static void logoutWS(OnlineUserModel onlineUserModel, Session session) {
        JSONObject obj = new JSONObject();
        obj.put("method", "logout");
        obj.put("token", onlineUserModel.getToken());
        sendMessage(onlineUserModel, obj);
    }


    /**
     * 发送关闭WebSocket消息, 前端不在重连
     *
     * @param session
     */
    public static void closeFrontWs(OnlineUserModel onlineUserModel, Session session) {
        JSONObject obj = new JSONObject();
        obj.put("method", "closeSocket");
        if (onlineUserModel != null) {
            sendMessage(onlineUserModel, obj);
        } else {
            sendMessage(session, obj);
        }
    }


    public static void sendMessage(OnlineUserModel onlineUserModel, Object message) {
        if (onlineUserModel == null || onlineUserModel.getWebSocket() == null) {
            return;
        }
        Session session = onlineUserModel.getWebSocket();
        synchronized (session) {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(JSON.toJSONString(message));
                } else {
                    handleClosedSession(onlineUserModel, session);
                }
            } catch (Exception e) {
                log.debug("WS消息发送失败: tenantId={}, sessionId={}, userId={}, token={}, message={}",
                        onlineUserModel.getTenantId(), session.getId(), onlineUserModel.getUserId(),
                        onlineUserModel.getToken(), message, e);
            }
        }
    }

    private static void handleClosedSession(OnlineUserModel onlineUserModel, Session session) {
        log.debug("WS未打开: tenantId={}, sessionId={}, userId={}, token={}",
                onlineUserModel.getTenantId(), session.getId(), onlineUserModel.getUserId(),
                onlineUserModel.getToken());

        try {
            session.close();
        } catch (Exception e) {
            // 忽略关闭异常
        } finally {
            OnlineUserProvider.removeModel(onlineUserModel);
        }
    }

    public static void sendMessage(Session session, Object message) {
        OnlineUserModel onlineUserModel = OnlineUserProvider.getOnlineUserList().stream()
                .filter(t -> t.getConnectionId().equals(session.getId()))
                .findFirst()
                .orElse(null);

        if (onlineUserModel == null) {
            onlineUserModel = new OnlineUserModel();
            onlineUserModel.setWebSocket(session);
        }

        synchronized (lock) {
            sendMessage(onlineUserModel, message);
        }
    }
}
