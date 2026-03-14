package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.TaskLineEntity;
import jnpf.util.RandomUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/23 17:35
 */
public interface TaskLineMapper extends SuperMapper<TaskLineEntity> {

    default List<TaskLineEntity> getList(String taskId) {
        QueryWrapper<TaskLineEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TaskLineEntity::getTaskId, taskId);
        return this.selectList(queryWrapper);
    }

    default void create(String taskId, Map<String, Boolean> conditionResMap) {
        if (CollUtil.isEmpty(conditionResMap)) {
            return;
        }
        List<TaskLineEntity> createList = new ArrayList<>();
        List<TaskLineEntity> updateList = new ArrayList<>();

        List<TaskLineEntity> list = this.getList(taskId);

        if (CollUtil.isNotEmpty(list)) {
            conditionResMap.forEach((k, v) -> {
                TaskLineEntity entity = list.stream().filter(e -> ObjectUtil.equals(k, e.getLineKey())).findFirst().orElse(null);
                if (null != entity) {
                    Boolean b = conditionResMap.get(entity.getLineKey());
                    if (null != b) {
                        entity.setLineValue(String.valueOf(b));
                        updateList.add(entity);
                    }
                } else {
                    entity = new TaskLineEntity();
                    entity.setId(RandomUtil.uuId());
                    entity.setTaskId(taskId);
                    entity.setLineKey(k);
                    entity.setLineValue(String.valueOf(v));
                    createList.add(entity);
                }
            });
        } else {
            conditionResMap.forEach((k, v) -> {
                TaskLineEntity entity = new TaskLineEntity();
                entity.setId(RandomUtil.uuId());
                entity.setTaskId(taskId);
                entity.setLineKey(k);
                entity.setLineValue(String.valueOf(v));
                createList.add(entity);
            });
        }

        if (CollUtil.isNotEmpty(createList)) {
            this.insert(createList);
        }
        if (CollUtil.isNotEmpty(updateList)) {
            this.updateById(updateList);
        }
    }

    default List<String> getLineKeyList(String taskId) {
        List<String> resList = new ArrayList<>();
        List<TaskLineEntity> list = this.getList(taskId);
        Map<String, List<TaskLineEntity>> collect = list.stream().collect(Collectors.groupingBy(TaskLineEntity::getLineKey));
        collect.forEach((k, v) -> {
            List<TaskLineEntity> sortList = v.stream().sorted(Comparator.comparing(TaskLineEntity::getCreatorTime).reversed()).collect(Collectors.toList());
            boolean bo = Boolean.parseBoolean(sortList.get(0).getLineValue());
            if (bo) {
                resList.add(k);
            }
        });
        return resList;
    }

}
