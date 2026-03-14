package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.SendConfigTemplateEntity;

import java.util.List;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
public interface SendConfigTemplateMapper extends SuperMapper<SendConfigTemplateEntity> {

    default SendConfigTemplateEntity getInfo(String id) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<SendConfigTemplateEntity> getDetailListByParentId(String id) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getSendConfigId, id);
        return this.selectList(queryWrapper);
    }

    default List<SendConfigTemplateEntity> getConfigTemplateListByConfigId(String id) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getSendConfigId, id);
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

    default boolean isUsedAccount(String accountId) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getAccountConfigId, accountId);
        return this.selectList(queryWrapper) != null && !this.selectList(queryWrapper).isEmpty();
    }

    default boolean isUsedTemplate(String templateId) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getTemplateId, templateId);
        return this.selectList(queryWrapper) != null && !this.selectList(queryWrapper).isEmpty();
    }
}
