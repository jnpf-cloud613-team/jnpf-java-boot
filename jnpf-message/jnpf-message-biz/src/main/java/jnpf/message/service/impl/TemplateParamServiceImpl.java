package jnpf.message.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.TemplateParamEntity;
import jnpf.message.mapper.TemplateParamMapper;
import jnpf.message.service.TemplateParamService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class TemplateParamServiceImpl extends SuperServiceImpl<TemplateParamMapper, TemplateParamEntity> implements TemplateParamService {

    @Override
    public TemplateParamEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<TemplateParamEntity> getDetailListByParentId(String id) {
        return this.baseMapper.getDetailListByParentId(id);
    }

}
