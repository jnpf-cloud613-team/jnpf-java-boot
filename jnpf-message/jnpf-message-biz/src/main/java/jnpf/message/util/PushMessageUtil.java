package jnpf.message.util;

import com.alibaba.fastjson.JSONObject;
import jnpf.base.UserInfo;
import jnpf.message.entity.MessageReceiveEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 消息推送工具类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-07
 */
@Component
public class PushMessageUtil {
    PushMessageUtil() {
    }

    /**
     * 工作流消息发送
     *
     * @param userInfo
     */
    public static void pushMessage(Map<String, MessageReceiveEntity> map, UserInfo userInfo, int messageType) {
        for (Map.Entry<String, MessageReceiveEntity> userItem : map.entrySet()) {
            String userId = userItem.getKey();
            MessageReceiveEntity entity = userItem.getValue();
            for (OnlineUserModel item : OnlineUserProvider.getOnlineUserList()) {
                if (userId.equals(item.getUserId()) && Objects.equals(userInfo.getTenantId(), item.getTenantId())) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "messagePush");
                    jsonObject.put("unreadNoticeCount", 1);
                    jsonObject.put("messageType", messageType);
                    jsonObject.put("userId", userInfo.getUserId());
                    jsonObject.put("toUserId", userId);
                    jsonObject.put("title", entity.getTitle());
                    jsonObject.put("id", entity.getId());
                    jsonObject.put("messageDefaultTime", entity.getLastModifyTime() != null ? entity.getLastModifyTime().getTime() : null);
                    OnlineUserProvider.sendMessage(item, jsonObject);
                }
            }
        }
    }

}
