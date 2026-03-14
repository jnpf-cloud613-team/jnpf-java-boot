package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.NodeRecordEntity;
import jnpf.flowable.enums.NodeStateEnum;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/25 17:06
 */
public interface NodeRecordMapper extends SuperMapper<NodeRecordEntity> {


    default List<NodeRecordEntity> getNodeRecord(String taskId) {
        QueryWrapper<NodeRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(NodeRecordEntity::getTaskId, taskId).ne(NodeRecordEntity::getNodeStatus, NodeStateEnum.BACK.getCode())
                .orderByDesc(NodeRecordEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<NodeRecordEntity> getList(String taskId) {
        QueryWrapper<NodeRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(NodeRecordEntity::getTaskId, taskId).orderByAsc(NodeRecordEntity::getCreatorTime);
        return this.selectList(wrapper);
    }


    default void create(NodeRecordModel model) {
        NodeRecordEntity entity = JsonUtil.getJsonToBean(model, NodeRecordEntity.class);
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }


    default void update(NodeRecordModel model) {
        QueryWrapper<NodeRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(NodeRecordEntity::getTaskId, model.getTaskId())
                .eq(NodeRecordEntity::getNodeCode, model.getNodeCode());
        if (ObjectUtil.isNotEmpty(model.getNodeId())) {
            wrapper.lambda().eq(NodeRecordEntity::getNodeId, model.getNodeId());
        }
        wrapper.lambda().orderByDesc(NodeRecordEntity::getCreatorTime);
        List<NodeRecordEntity> list = this.selectList(wrapper);
        if (CollUtil.isNotEmpty(list)) {
            NodeRecordEntity entity = list.get(0);
            entity.setNodeStatus(model.getNodeStatus());
            this.updateById(entity);
        }
    }
}
