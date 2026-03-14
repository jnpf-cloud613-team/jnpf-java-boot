
package jnpf.message.service;


import jnpf.base.service.SuperService;


import java.util.*;

import jnpf.message.entity.SendConfigTemplateEntity;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
public interface SendConfigTemplateService extends SuperService<SendConfigTemplateEntity> {

    SendConfigTemplateEntity getInfo(String id);

    List<SendConfigTemplateEntity> getDetailListByParentId(String id);

    /**
     * 根据消息发送配置id获取启用的配置模板
     * @param id
     * @return
     */
    List<SendConfigTemplateEntity> getConfigTemplateListByConfigId(String id);

    boolean isUsedAccount(String accountId);

    boolean isUsedTemplate(String templateId);
}
