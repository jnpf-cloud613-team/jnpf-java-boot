package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ScheduleLogEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
public interface ScheduleLogMapper extends SuperMapper<ScheduleLogEntity> {

    default List<ScheduleLogEntity> getListAll(List<String> scheduleIdList) {
        List<ScheduleLogEntity> list = new ArrayList<>();
        QueryWrapper<ScheduleLogEntity> queryWrapper = new QueryWrapper<>();
        if (!scheduleIdList.isEmpty()) {
            queryWrapper.lambda().in(ScheduleLogEntity::getScheduleId, scheduleIdList);
            queryWrapper.lambda().orderByDesc(ScheduleLogEntity::getCreatorTime);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default ScheduleLogEntity getInfo(String id) {
        QueryWrapper<ScheduleLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ScheduleLogEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(ScheduleLogEntity entity) {
        UserInfo userInfo = UserProvider.getUser();
        entity.setId(RandomUtil.uuId());
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(userInfo.getUserId());
        this.insert(entity);
    }

    default void delete(List<String> scheduleIdList, String operationType) {
        List<ScheduleLogEntity> listAll = getListAll(scheduleIdList);
        for (ScheduleLogEntity scheduleLogEntity : listAll) {
            scheduleLogEntity.setOperationType(operationType);
            create(scheduleLogEntity);
        }
    }

    default boolean update(String id, ScheduleLogEntity entity) {
        entity.setId(id);
        return SqlHelper.retBool(this.updateById(entity));
    }
}
