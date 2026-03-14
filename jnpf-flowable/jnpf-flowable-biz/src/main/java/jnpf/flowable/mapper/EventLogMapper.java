package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface EventLogMapper extends SuperMapper<EventLogEntity> {

    default List<EventLogEntity> getList(String taskId) {
        return getList(taskId, null);
    }

    default List<EventLogEntity> getList(String taskId, List<String> nodeCode) {
        if (StringUtils.isBlank(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<EventLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EventLogEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().in(EventLogEntity::getNodeCode, nodeCode);
        }
        queryWrapper.lambda().orderByDesc(EventLogEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default void delete(String taskId, List<String> nodeCode) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        QueryWrapper<EventLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EventLogEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().in(EventLogEntity::getNodeCode, nodeCode);
        }
        this.delete(queryWrapper);
    }

    default void create(EventLogEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setCreatorTime(new Date());
        entity.setSortCode(0L);
        this.insert(entity);
    }
}
