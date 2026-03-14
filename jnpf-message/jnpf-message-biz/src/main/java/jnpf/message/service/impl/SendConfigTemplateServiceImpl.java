package jnpf.message.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.SendConfigTemplateEntity;
import jnpf.message.mapper.SendConfigTemplateMapper;
import jnpf.message.service.SendConfigTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
@Service
public class SendConfigTemplateServiceImpl extends SuperServiceImpl<SendConfigTemplateMapper, SendConfigTemplateEntity> implements SendConfigTemplateService {

    @Override
    public SendConfigTemplateEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<SendConfigTemplateEntity> getDetailListByParentId(String id) {
        return this.baseMapper.getDetailListByParentId(id);
    }

    @Override
    public List<SendConfigTemplateEntity> getConfigTemplateListByConfigId(String id) {
        return this.baseMapper.getConfigTemplateListByConfigId(id);
    }

    @Override
    public boolean isUsedAccount(String accountId) {
        return this.baseMapper.isUsedAccount(accountId);
    }

    @Override
    public boolean isUsedTemplate(String templateId) {
        return this.baseMapper.isUsedTemplate(templateId);
    }
}
