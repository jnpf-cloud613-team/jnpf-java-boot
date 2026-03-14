package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.SmsFieldEntity;
import jnpf.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface SmsFieldMapper extends SuperMapper<SmsFieldEntity> {

    default SmsFieldEntity getInfo(String id) {
        QueryWrapper<SmsFieldEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsFieldEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<SmsFieldEntity> getDetailListByParentId(String id) {
        QueryWrapper<SmsFieldEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsFieldEntity::getTemplateId, id);
        return this.selectList(queryWrapper);
    }

    default Map<String, Object> getParamMap(String templateId, Map<String, Object> map) {
        Map<String, Object> paramMap = new HashMap<>();
        List<SmsFieldEntity> list = this.getDetailListByParentId(templateId);
        if (list != null && !list.isEmpty()) {
            for (SmsFieldEntity entity : list) {
                if (map.containsKey(entity.getField())) {
                    for (Map.Entry<String, Object> item : map.entrySet()) {
                        String key = item.getKey();
                        Object value = item.getValue();
                        if (key.equals(entity.getField())) {
                            paramMap.put(entity.getSmsField(), value);
                            if (StringUtil.isNotEmpty(String.valueOf(entity.getIsTitle())) && !"null".equals(String.valueOf(entity.getIsTitle())) && entity.getIsTitle() == 1) {
                                paramMap.put("title", value);
                            }
                        }
                    }
                    if (entity.getField().equals("@FlowLink")) {
                        paramMap.put(entity.getSmsField(), "@FlowLink");
                    }
                }
            }
        }
        return paramMap;
    }
}
