package jnpf.base.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ScheduleNewEntity;
import jnpf.base.model.schedule.ScheduleDetailModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;

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
public interface ScheduleNewMapper extends SuperMapper<ScheduleNewEntity> {

    default List<ScheduleNewEntity> getList(List<String> scheduleId) {
        if (scheduleId.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ScheduleNewEntity::getId, scheduleId);
        queryWrapper.lambda().orderByDesc(ScheduleNewEntity::getAllDay);
        queryWrapper.lambda().orderByAsc(ScheduleNewEntity::getStartDay);
        queryWrapper.lambda().orderByAsc(ScheduleNewEntity::getEndDay);
        queryWrapper.lambda().orderByDesc(ScheduleNewEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ScheduleNewEntity> getList(String groupId, Date date) {
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(groupId)) {
            queryWrapper.lambda().eq(ScheduleNewEntity::getGroupId, groupId);
        }
        if (ObjectUtil.isNotEmpty(date)) {
            queryWrapper.lambda().ge(ScheduleNewEntity::getStartDay, date);
        }
        return this.selectList(queryWrapper);
    }

    default List<ScheduleNewEntity> getStartDayList(String groupId, Date date) {
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(groupId)) {
            queryWrapper.lambda().eq(ScheduleNewEntity::getGroupId, groupId);
        }
        if (ObjectUtil.isNotEmpty(date)) {
            queryWrapper.lambda().le(ScheduleNewEntity::getStartDay, date);
        }
        queryWrapper.lambda().orderByDesc(ScheduleNewEntity::getStartDay);
        return this.selectList(queryWrapper);
    }


    default List<ScheduleNewEntity> getListAll(Date date) {
        if (date == null) {
            date = new Date();
        }
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        int seconds = 10;
        Date end = DateUtil.dateAddSeconds(date, seconds);
        Date start = DateUtil.dateAddSeconds(date, -seconds);
        queryWrapper.lambda().between(ScheduleNewEntity::getPushTime, start, end);
        return this.selectList(queryWrapper);
    }

    default ScheduleNewEntity getInfo(String id) {
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ScheduleNewEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<ScheduleNewEntity> getGroupList(ScheduleDetailModel detailModel) {
        QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
        String id = detailModel.getId();
        String groupId = detailModel.getGroupId();
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().eq(ScheduleNewEntity::getId, id);
        } else {
            queryWrapper.lambda().eq(ScheduleNewEntity::getGroupId, groupId);
        }
        queryWrapper.lambda().orderByAsc(ScheduleNewEntity::getStartDay);
        return this.selectList(queryWrapper);
    }

}
