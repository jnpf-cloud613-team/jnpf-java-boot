package jnpf.message.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.SmsFieldEntity;
import jnpf.message.mapper.SmsFieldMapper;
import jnpf.message.service.SmsFieldService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class SmsFieldServiceImpl extends SuperServiceImpl<SmsFieldMapper, SmsFieldEntity> implements SmsFieldService {

    @Override
    public SmsFieldEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<SmsFieldEntity> getDetailListByParentId(String id) {
        return this.baseMapper.getDetailListByParentId(id);
    }

    @Override
    public Map<String, Object> getParamMap(String templateId, Map<String, Object> map) {
        return this.baseMapper.getParamMap(templateId, map);
    }
}
