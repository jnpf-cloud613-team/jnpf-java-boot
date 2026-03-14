package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.TemplateParamEntity;

import java.util.List;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface TemplateParamMapper extends SuperMapper<TemplateParamEntity> {

    default TemplateParamEntity getInfo(String id) {
        QueryWrapper<TemplateParamEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateParamEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<TemplateParamEntity> getDetailListByParentId(String id) {
        QueryWrapper<TemplateParamEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateParamEntity::getTemplateId, id);
        return this.selectList(queryWrapper);
    }
}
