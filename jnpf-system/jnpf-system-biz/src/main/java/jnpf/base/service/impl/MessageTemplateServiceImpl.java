package jnpf.base.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.Pagination;
import jnpf.base.entity.MessageTemplateEntity;
import jnpf.base.mapper.MessageTemplateMapper;
import jnpf.base.service.MessageTemplateService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息模板
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年12月8日17:40:37
 */
@Service
public class MessageTemplateServiceImpl extends SuperServiceImpl<MessageTemplateMapper, MessageTemplateEntity> implements MessageTemplateService {

    @Override
    public List<MessageTemplateEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<MessageTemplateEntity> getList(Pagination pagination, Boolean filter) {
        return this.baseMapper.getList(pagination, filter);
    }

    @Override
    public MessageTemplateEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public void create(MessageTemplateEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    @DSTransactional
    public boolean update(String id, MessageTemplateEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(MessageTemplateEntity entity) {
        this.baseMapper.deleteById(entity.getId());
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

}




