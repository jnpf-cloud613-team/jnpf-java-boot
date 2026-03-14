package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.RevokeEntity;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/24 13:39
 */
public interface RevokeMapper extends SuperMapper<RevokeEntity> {

    default RevokeEntity getRevokeTask(String revokeTaskId) {
        QueryWrapper<RevokeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RevokeEntity::getRevokeTaskId, revokeTaskId);
        return this.selectOne(queryWrapper);
    }

    default Boolean checkExist(String taskId) {
        QueryWrapper<RevokeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RevokeEntity::getTaskId, taskId).isNull(RevokeEntity::getDeleteMark);
        return this.selectCount(queryWrapper) == 0;
    }

    default void deleteRevoke(String revokeTaskId) {
        RevokeEntity revokeEntity = this.getRevokeTask(revokeTaskId);
        if (null != revokeEntity) {
            revokeEntity.setDeleteMark(-1);
            revokeEntity.setDeleteTime(new Date());
            revokeEntity.setDeleteUserId(UserProvider.getLoginUserId());
            this.updateById(revokeEntity);
        }
    }

    default List<String> getByTaskId(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        QueryWrapper<RevokeEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(RevokeEntity::getTaskId, ids);
        List<RevokeEntity> list = this.selectList(wrapper);
        return list.stream().map(RevokeEntity::getRevokeTaskId).collect(Collectors.toList());
    }
}
