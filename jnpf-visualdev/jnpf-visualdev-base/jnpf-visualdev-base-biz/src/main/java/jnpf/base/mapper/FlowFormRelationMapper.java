package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jnpf.base.entity.FlowFormRelationEntity;
import jnpf.util.RandomUtil;

import java.util.List;

/**
 * 流程表单关联
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/30 18:00
 */
public interface FlowFormRelationMapper extends SuperMapper<FlowFormRelationEntity> {

    default void saveFlowIdByFormIds(String flowId, List<String> formIds) {
        QueryWrapper<FlowFormRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(FlowFormRelationEntity::getFlowId, flowId);
        List<FlowFormRelationEntity> list = this.selectList(queryWrapper);
        this.deleteByIds(list);
        if (CollectionUtils.isNotEmpty(formIds)) {
            for (String formId : formIds) {
                FlowFormRelationEntity entity = new FlowFormRelationEntity();
                entity.setFlowId(flowId);
                entity.setId(RandomUtil.uuId());
                entity.setFormId(formId);
                this.insert(entity);
            }
        }
    }

    default List<FlowFormRelationEntity> getListByFormId(String formId) {
        QueryWrapper<FlowFormRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(FlowFormRelationEntity::getFormId, formId);
        return this.selectList(queryWrapper);
    }
}
