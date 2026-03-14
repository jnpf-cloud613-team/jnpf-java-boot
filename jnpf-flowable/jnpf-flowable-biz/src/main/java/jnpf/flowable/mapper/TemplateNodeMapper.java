package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.NodeEnum;
import jnpf.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface TemplateNodeMapper extends SuperMapper<TemplateNodeEntity> {

    default List<TemplateNodeEntity> getList(String flowId) {
        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateNodeEntity::getFlowId, flowId);
        return this.selectList(queryWrapper);
    }

    default List<TemplateNodeEntity> getList(List<String> flowIds, String nodeType) {
        if (flowIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TemplateNodeEntity::getFlowId, flowIds);
        if (StringUtils.isNotBlank(nodeType)) {
            queryWrapper.lambda().eq(TemplateNodeEntity::getNodeType, nodeType);
        }
        return this.selectList(queryWrapper);
    }

    default List<TemplateNodeEntity> getListLikeUserId(String userId) {
        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().like(TemplateNodeEntity::getNodeJson, userId);
        return this.selectList(queryWrapper);
    }

    default TemplateNodeEntity getInfo(String id) throws WorkFlowException {
        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateNodeEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(TemplateNodeEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default boolean update(String id, TemplateNodeEntity entity) {
        entity.setId(id);
        return this.updateById(entity) > 0;
    }

    default void delete(TemplateNodeEntity entity) {
        if (entity != null) {
            this.deleteById(entity.getId());
        }
    }

    default void deleteList(List<String> idList) {
        if (!idList.isEmpty()) {
            QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(TemplateNodeEntity::getId, idList);
            this.delete(queryWrapper);
        }
    }

    default void delete(List<String> idList) {
        if (!idList.isEmpty()) {
            this.deleteByIds(idList);
        }
    }


    default List<TemplateNodeEntity> getListStart(List<TemplateJsonEntity> listOfEnable) {
        List<String> flowIds = listOfEnable.stream().map(TemplateJsonEntity::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(flowIds)) {
            QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(TemplateNodeEntity::getFlowId, TemplateNodeEntity::getFormId);
            queryWrapper.lambda().eq(TemplateNodeEntity::getNodeType, NodeEnum.START.getType()).in(TemplateNodeEntity::getFlowId, flowIds);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

}
