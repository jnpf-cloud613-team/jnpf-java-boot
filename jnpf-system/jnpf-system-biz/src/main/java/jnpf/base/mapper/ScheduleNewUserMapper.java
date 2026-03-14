package jnpf.base.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ScheduleNewUserEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.List;

/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
public interface ScheduleNewUserMapper extends SuperMapper<ScheduleNewUserEntity> {


    default List<ScheduleNewUserEntity> getList(String scheduleId, Integer type) {
        QueryWrapper<ScheduleNewUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ScheduleNewUserEntity::getScheduleId, scheduleId);
        if (ObjectUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(ScheduleNewUserEntity::getType, type);
        }
        return this.selectList(queryWrapper);
    }

    default List<ScheduleNewUserEntity> getList() {
        QueryWrapper<ScheduleNewUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ScheduleNewUserEntity::getToUserId, UserProvider.getUser().getUserId());
        queryWrapper.lambda().eq(ScheduleNewUserEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

    default void create(ScheduleNewUserEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default void deleteByScheduleId(List<String> scheduleIdList) {
        if (!scheduleIdList.isEmpty()) {
            QueryWrapper<ScheduleNewUserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(ScheduleNewUserEntity::getScheduleId, scheduleIdList);
            this.deleteByIds(this.selectList(queryWrapper));
        }
    }

    default void deleteByUserId(List<String> scheduleIdList) {
        UserInfo userInfo = UserProvider.getUser();
        if (!scheduleIdList.isEmpty()) {
            UpdateWrapper<ScheduleNewUserEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().eq(ScheduleNewUserEntity::getToUserId, userInfo.getUserId());
            updateWrapper.lambda().in(ScheduleNewUserEntity::getScheduleId, scheduleIdList);
            updateWrapper.lambda().set(ScheduleNewUserEntity::getEnabledMark, 0);
            this.update(updateWrapper);
        }
    }
}
