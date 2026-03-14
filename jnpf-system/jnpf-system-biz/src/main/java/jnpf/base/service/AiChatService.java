package jnpf.base.service;

import jnpf.base.entity.AiChatEntity;
import jnpf.base.model.ai.AiChatVo;
import jnpf.base.model.ai.AiForm;
import jnpf.base.model.ai.AiHisVo;

import java.util.List;

/**
 * ai会话服务
 *
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 10:10:10
 */
public interface AiChatService extends SuperService<AiChatEntity> {

    /**
     * ai对话发送
     *
     * @param keyword
     */
    String send(String keyword);

    /**
     * ai会话列表
     */
    List<AiChatVo> historyList();

    /**
     * ai会话记录
     */
    List<AiHisVo> historyGet(String id);

    /**
     * 会话记录保存
     */
    String historySave(AiForm form);

    /**
     * 删除ai会话记录
     *
     * @param id
     */
    void delete(String id);

}
