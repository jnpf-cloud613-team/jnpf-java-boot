package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.SubtaskDataEntity;
import jnpf.flowable.model.task.FlowModel;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/6 15:32
 */
public interface SubtaskDataMapper extends SuperMapper<SubtaskDataEntity> {

    default List<SubtaskDataEntity> getList(String parentId, String parentCode) {
        QueryWrapper<SubtaskDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SubtaskDataEntity::getParentId, parentId).eq(SubtaskDataEntity::getNodeCode, parentCode)
                .orderByAsc(SubtaskDataEntity::getSortCode);
        return this.selectList(queryWrapper);
    }

    default void save(List<FlowModel> subTaskData) {
        if (CollUtil.isEmpty(subTaskData)) {
            return;
        }
        List<SubtaskDataEntity> list = new ArrayList<>();
        for (int i = 0; i < subTaskData.size(); i++) {
            FlowModel model = subTaskData.get(i);
            SubtaskDataEntity entity = new SubtaskDataEntity();
            entity.setId(RandomUtil.uuId());
            entity.setParentId(model.getParentId());
            entity.setNodeCode(model.getSubCode());
            entity.setSubtaskJson(JsonUtil.getObjectToString(model));
            int sortCode = i + 1;
            entity.setSortCode((long) sortCode);
            list.add(entity);
        }
        if (CollUtil.isNotEmpty(list)) {
            this.insert(list);
        }
    }
}
