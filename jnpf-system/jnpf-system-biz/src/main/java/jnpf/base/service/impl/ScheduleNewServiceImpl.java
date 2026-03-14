package jnpf.base.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ScheduleLogEntity;
import jnpf.base.entity.ScheduleNewEntity;
import jnpf.base.entity.ScheduleNewUserEntity;
import jnpf.base.mapper.ScheduleLogMapper;
import jnpf.base.mapper.ScheduleNewMapper;
import jnpf.base.mapper.ScheduleNewUserMapper;
import jnpf.base.model.schedule.ScheduleDetailModel;
import jnpf.base.model.schedule.ScheduleJobModel;
import jnpf.base.model.schedule.ScheduleNewTime;
import jnpf.base.service.ScheduleNewService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.util.job.ScheduleJobUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.message.model.SentMessageForm;
import jnpf.message.service.SendMsgService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 日程
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Service
@RequiredArgsConstructor
public class ScheduleNewServiceImpl extends SuperServiceImpl<ScheduleNewMapper, ScheduleNewEntity> implements ScheduleNewService {

    public static final String TIME="yyyy-MM-dd";

    
    private final ScheduleJobUtil scheduleJobUtil;
    
    private final RedisUtil redisUtil;
    
    private final UserService userService;
    
    private final SendMsgService sendMsgApi;
    
    private final ScheduleLogMapper scheduleLogMapper;
    
    private final ScheduleNewUserMapper scheduleNewUserMapper;

    @Override
    public List<ScheduleNewEntity> getList(ScheduleNewTime scheduleNewTime) {
        List<String> scheduleId = scheduleNewUserMapper.getList().stream().map(ScheduleNewUserEntity::getScheduleId).collect(Collectors.toList());
        return this.baseMapper.getList(scheduleId);
    }

    @Override
    public List<ScheduleNewEntity> getList(String groupId, Date date) {
        return this.baseMapper.getList(groupId, date);
    }

    @Override
    public List<ScheduleNewEntity> getStartDayList(String groupId, Date date) {
        return this.baseMapper.getStartDayList(groupId, date);
    }


    @Override
    public List<ScheduleNewEntity> getListAll(Date date) {
        return this.baseMapper.getListAll(date);
    }

    @Override
    public ScheduleNewEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<ScheduleNewEntity> getGroupList(ScheduleDetailModel detailModel) {
        return this.baseMapper.getGroupList(detailModel);
    }

    @Override
    @DSTransactional
    public void create(ScheduleNewEntity entity, List<String> toUserIds, String groupId, String operationType, List<String> idList) throws WorkFlowException {
        UserInfo userInfo = UserProvider.getUser();
        if (StringUtil.isEmpty(entity.getCreatorUserId())) {
            entity.setCreatorUserId(userInfo.getUserId());
        }
        boolean isUser = toUserIds.contains(entity.getCreatorUserId());
        if (!isUser) {
            toUserIds.add(entity.getCreatorUserId());
        }
        time(entity);
        long time = entity.getEndDay().getTime() - entity.getStartDay().getTime();
        //间隔时间
        List<Date> dataList = new ArrayList<>();
        List<ScheduleNewEntity> listAll = new ArrayList<>();
        DateUtil.getNextDate(0, String.valueOf(entity.getRepetition()), entity.getStartDay(), entity.getRepeatTime(), dataList);
        if (CollUtil.isEmpty(dataList)) {
            throw new WorkFlowException(MsgCode.SYS132.get());
        }
        for (Date date : dataList) {
            ScheduleNewEntity scheduleEntity = JsonUtil.getJsonToBean(entity, ScheduleNewEntity.class);
            scheduleEntity.setStartDay(date);
            scheduleEntity.setEndDay(new Date(date.getTime() + time));
            if (!Objects.equals(scheduleEntity.getReminderTime(), -2)) {
                boolean isAllDay = Objects.equals(scheduleEntity.getAllDay(), 1);
                if (isAllDay) {
                    int oneMinutes = 8 * 60;
                    int twoMinutes = 9 * 60;
                    int threeMinutes = 10 * 60;
                    int day = 1440;
                    Integer reminderTime = scheduleEntity.getReminderTime();
                    switch (reminderTime) {
                        case 4:
                        case 5:
                        case 6:
                            oneMinutes = oneMinutes - day;
                            twoMinutes = twoMinutes - day;
                            threeMinutes = threeMinutes - day;
                            break;
                        case 7:
                        case 8:
                        case 9:
                            oneMinutes = oneMinutes - day * 2;
                            twoMinutes = twoMinutes - day * 2;
                            threeMinutes = threeMinutes - day * 2;
                            break;
                        case 10:
                        case 11:
                        case 12:
                            oneMinutes = oneMinutes - day * 7;
                            twoMinutes = twoMinutes - day * 7;
                            threeMinutes = threeMinutes - day * 7;
                            break;
                        default:
                            break;
                    }
                    List<Integer> oneList = new ArrayList<>();
                    oneList.add(1);
                    oneList.add(4);
                    oneList.add(7);
                    oneList.add(10);
                    List<Integer> twoList = new ArrayList<>() ;
                    twoList.add(2);
                    twoList.add(5);
                    twoList.add(8);
                    twoList.add(11);
                    List<Integer> threeList = new ArrayList<>() ;
                    threeList.add(3);
                    threeList.add(6);
                    threeList.add(9);
                    threeList.add(12);
                    int pushTime = 0;
                    if (oneList.contains(reminderTime)) {
                        pushTime = oneMinutes;
                    } else if (twoList.contains(reminderTime)) {
                        pushTime = twoMinutes;
                    } else if (threeList.contains(reminderTime)) {
                        pushTime = threeMinutes;
                    }
                    scheduleEntity.setPushTime(DateUtil.dateAddMinutes(scheduleEntity.getStartDay(), pushTime));
                } else {
                    int reminderTime = scheduleEntity.getReminderTime() > 0 ? scheduleEntity.getReminderTime() : 0;
                    scheduleEntity.setPushTime(DateUtil.dateAddMinutes(scheduleEntity.getStartDay(), -reminderTime));
                }
            }
            listAll.add(scheduleEntity);
        }
        List<ScheduleJobModel> scheduleJobList = new ArrayList<>();
        String id = "";
        for (int i = 0; i < listAll.size(); i++) {
            ScheduleNewEntity scheduleEntity = listAll.get(i);
            scheduleEntity.setId(RandomUtil.uuId());
            scheduleEntity.setCreatorTime(new Date());
            scheduleEntity.setCreatorUserId(entity.getCreatorUserId());
            scheduleEntity.setGroupId(groupId);
            scheduleEntity.setEnabledMark(1);
            this.saveOrUpdate(scheduleEntity);
            for (String toUserId : toUserIds) {
                ScheduleNewUserEntity userEntity = new ScheduleNewUserEntity();
                userEntity.setScheduleId(scheduleEntity.getId());
                userEntity.setToUserId(toUserId);
                userEntity.setEnabledMark(1);
                userEntity.setType(!isUser && entity.getCreatorUserId().equals(toUserId) ? 1 : 2);
                scheduleNewUserMapper.create(userEntity);
            }
            boolean isTime = ObjectUtil.isNotEmpty(scheduleEntity.getPushTime()) && scheduleEntity.getPushTime().getTime() >= System.currentTimeMillis();
            ScheduleJobModel jobModel = new ScheduleJobModel();
            jobModel.setId(scheduleEntity.getId());
            jobModel.setScheduleTime(scheduleEntity.getPushTime());
            jobModel.setUserInfo(userInfo);
            jobModel.setUserList(toUserIds);
            if (isTime) {
                scheduleJobList.add(jobModel);
            }
            //操作日志
            ScheduleLogEntity logEntity = JsonUtil.getJsonToBean(entity, ScheduleLogEntity.class);
            logEntity.setOperationType(operationType);
            logEntity.setUserId(JsonUtil.getObjectToString(toUserIds));
            logEntity.setScheduleId(scheduleEntity.getId());
            scheduleLogMapper.create(logEntity);
        }
        if ("1".equals(operationType)) {
            ScheduleDetailModel model = new ScheduleDetailModel();
            model.setGroupId(groupId);
            model.setId(id);
            model.setType("2");
            UserInfo info = JsonUtil.getJsonToBean(userInfo, UserInfo.class);
            UserEntity user = userService.getInfo(entity.getCreatorUserId());
            if (null != user) {
                info.setUserId(user.getId());
                info.setUserName(user.getRealName());
                info.setUserAccount(user.getAccount());
            }
            if (StringUtils.isNotBlank(id)) {
                msg(toUserIds, info, model, entity, "PZXTRC001");
            }
        }
        //推送任务调度
        job(scheduleJobList);
    }

    @Override
    @DSTransactional
    public boolean update(String id, ScheduleNewEntity entity, List<String> toUserIds, String type) throws WorkFlowException {
        UserInfo userInfo = UserProvider.getUser();
        ScheduleNewEntity info = getInfo(id);
        boolean flag = false;
        String groupId = RandomUtil.uuId();
        if (info != null) {
            //删除一个还是多个
            String delGroupId = info.getGroupId();
            Date startDay = "2".equals(type) ? info.getStartDay() : null;
            ArrayList<ScheduleNewEntity> scheduleNewEntities = new ArrayList<>();
            scheduleNewEntities.add(info);
            List<ScheduleNewEntity> deleteList = "1".equals(type) ? scheduleNewEntities: getList(delGroupId, startDay);
            repeat(type, info);
            updateStartDay(delGroupId, type, startDay);
            List<String> scheduleIdList = deleteList.stream().map(ScheduleNewEntity::getId).collect(Collectors.toList());
            deleteScheduleList(scheduleIdList);
            create(entity, toUserIds, groupId, "2", scheduleIdList);
            ScheduleDetailModel detailModel = new ScheduleDetailModel();
            detailModel.setGroupId(groupId);
            List<ScheduleNewEntity> groupList = getGroupList(detailModel);
            ScheduleDetailModel model = new ScheduleDetailModel();
            model.setGroupId(groupId);
            model.setId(!groupList.isEmpty() ? groupList.get(0).getId() : id);
            model.setType("2");
            entity.setSend("");
            msg(toUserIds, userInfo, model, entity, "PZXTRC002");
            flag = true;
        }
        return flag;
    }


    @Override
    @DSTransactional
    public void deleteScheduleList(List<String> idList) {
        if (!idList.isEmpty()) {
            QueryWrapper<ScheduleNewEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(ScheduleNewEntity::getId, idList);
            this.baseMapper.deleteByIds(this.baseMapper.selectList(queryWrapper));
            this.clearIgnoreLogicDelete();
            scheduleNewUserMapper.deleteByScheduleId(idList);
            scheduleLogMapper.delete(idList, "3");
        }
    }

    @Override
    public boolean update(String id, ScheduleNewEntity entity) {
        entity.setId(id);
        return updateById(entity);
    }

    @Override
    @DSTransactional
    public void delete(ScheduleNewEntity entity, String type) {
        if (entity != null) {
            UserInfo userInfo = UserProvider.getUser();
            String userId = userInfo.getUserId();
            String groupId = entity.getGroupId();
            String delGroupId = entity.getGroupId();
            Date startDay = "2".equals(type) ? entity.getStartDay() : null;
            ArrayList<ScheduleNewEntity> scheduleNewEntities = new ArrayList<>();
            scheduleNewEntities.add(entity);
            List<ScheduleNewEntity> deleteList = "1".equals(type) ? scheduleNewEntities : getList(delGroupId, startDay);
            List<String> scheduleIdList = deleteList.stream().map(ScheduleNewEntity::getId).collect(Collectors.toList());
            if (entity.getCreatorUserId().equals(userId)) {
                repeat(type, entity);
                List<String> toUserIds = scheduleNewUserMapper.getList(entity.getId(), null).stream().map(ScheduleNewUserEntity::getToUserId).collect(Collectors.toList());
                deleteScheduleList(scheduleIdList);
                ScheduleDetailModel model = new ScheduleDetailModel();
                model.setGroupId(groupId);
                model.setId(entity.getId());
                model.setType("3");
                entity.setSend("");
                msg(toUserIds, userInfo, model, entity, "PZXTRC003");
            } else {
                //操作日志
                scheduleLogMapper.delete(scheduleIdList, "4");
                scheduleNewUserMapper.deleteByUserId(scheduleIdList);
            }
        }
    }

    @Override
    public void scheduleMessage(ScheduleJobModel scheduleModel) {
        ScheduleNewEntity info = getInfo(scheduleModel.getId());
        if (info != null) {
            List<ScheduleNewEntity> listAll = new ArrayList<>();
            listAll.add(info);
            for (ScheduleNewEntity entity : listAll) {
                UserInfo userInfo = scheduleModel.getUserInfo();
                UserEntity userEntity = userService.getInfo(entity.getCreatorUserId());
                List<String> toUserIds = scheduleNewUserMapper.getList(entity.getId(), null).stream().map(ScheduleNewUserEntity::getToUserId).collect(Collectors.toList());
                ScheduleDetailModel model = new ScheduleDetailModel();
                model.setGroupId(entity.getGroupId());
                model.setId(entity.getId());
                if (userEntity != null) {
                    userInfo.setUserId(userEntity.getId());
                    userInfo.setUserName(userEntity.getRealName());
                }
                model.setType("1");
                msg(toUserIds, userInfo, model, entity, "PZXTRC001");
            }
        }
    }

    private void time(ScheduleNewEntity entity) {
        // 判断是否全天
        if (entity.getAllDay() != 1) {
            String startDate = DateUtil.dateToString(entity.getStartDay(), TIME) + " " + entity.getStartTime() + ":00";
            Date star = DateUtil.stringToDate(startDate);
            entity.setStartDay(star);
            if (entity.getDuration() != -1) {
                Date end = DateUtil.dateAddMinutes(entity.getStartDay(), entity.getDuration());
                entity.setEndDay(end);
            } else {
                String endDate = DateUtil.dateToString(entity.getEndDay(), TIME) + " " + entity.getEndTime() + ":00";
                Date end = DateUtil.stringToDate(endDate);
                entity.setEndDay(end);
            }
        } else {
            String startDate = DateUtil.dateToString(entity.getStartDay(), TIME) + " " + "00:00:00";
            Date star = DateUtil.stringToDate(startDate);
            entity.setStartDay(star);
            entity.setStartTime("00:00");
            String endDate = DateUtil.dateToString(entity.getEndDay(), TIME) + " " + "23:59:59";
            Date end = DateUtil.stringToDate(endDate);
            entity.setEndDay(end);
            entity.setEndTime("23:59");
        }
        Date repeatTime = entity.getRepeatTime();
        if (repeatTime != null) {
            String repeat = DateUtil.dateToString(repeatTime, TIME) + " " + "23:59:59";
            Date repeatDate = DateUtil.stringToDate(repeat);
            entity.setRepeatTime(repeatDate);
        }
    }

    private void msg(List<String> toUserIds, UserInfo userInfo, ScheduleDetailModel model, ScheduleNewEntity entity, String templateId) {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("@Title", entity.getTitle());
        parameterMap.put("@CreatorUserName", userInfo.getUserName() + "/" + userInfo.getUserAccount());
        parameterMap.put("@SendTime", DateUtil.getNow().substring(11));
        parameterMap.put("@Content", StringUtil.isNotEmpty(entity.getContent()) ? entity.getContent() : "");
        parameterMap.put("@StartDate", DateUtil.daFormat(entity.getStartDay()));
        parameterMap.put("@StartTime", entity.getStartTime());
        parameterMap.put("@EndDate", DateUtil.daFormat(entity.getEndDay()));
        parameterMap.put("@EndTime", entity.getEndTime());
        parameterMap.put("Title", entity.getTitle());
        parameterMap.put("CreatorUserName", userInfo.getUserName());
        parameterMap.put("Content", StringUtil.isNotEmpty(entity.getContent()) ? entity.getContent() : "");
        parameterMap.put("StartDate", DateUtil.daFormat(entity.getStartDay()));
        parameterMap.put("StartTime", entity.getStartTime());
        parameterMap.put("EndDate", DateUtil.daFormat(entity.getEndDay()));
        parameterMap.put("EndTime", entity.getEndTime());
        SentMessageForm sentMessageForm = new SentMessageForm();
        sentMessageForm.setToUserIds(toUserIds);
        sentMessageForm.setUserInfo(userInfo);
        sentMessageForm.setParameterMap(parameterMap);
        sentMessageForm.setTitle(entity.getTitle());
        sentMessageForm.setTemplateId(StringUtil.isNotEmpty(entity.getSend()) ? entity.getSend() : templateId);
        sentMessageForm.setContent(JsonUtil.getObjectToString(model));
        Map<String, String> contentMsg = JsonUtil.entityToMaps(model);
        sentMessageForm.setContentMsg(contentMsg);
        sentMessageForm.setId(model.getId());
        sentMessageForm.setType(4);
        sendMsgApi.sendScheduleMessage(sentMessageForm);
    }

    private void job(List<ScheduleJobModel> scheduleJobList) {
        scheduleJobUtil.insertRedis(scheduleJobList, redisUtil);
    }

    private void updateStartDay(String groupId, String type, Date startDay) {
        if ("2".equals(type)) {
            Date startData = DateUtil.stringToDate(DateUtil.dateToString(startDay, TIME) + " " + "00:00:00");
            List<ScheduleNewEntity> startDayList = getStartDayList(groupId, startData);
            if (!startDayList.isEmpty()) {
                Date start = startDayList.get(0).getStartDay();
                for (ScheduleNewEntity entity : startDayList) {
                    Date repeatTime = entity.getRepeatTime();
                    if (repeatTime != null) {
                        String repeat = DateUtil.dateToString(start, TIME) + " " + "23:59:59";
                        Date repeatDate = DateUtil.stringToDate(repeat);
                        entity.setRepeatTime(repeatDate);
                        update(entity.getId(), entity);
                    }
                }
            }
        }
    }

    private void repeat(String type, ScheduleNewEntity info) {
        Date repeat = info.getRepeatTime();
        String groupId = info.getGroupId();
        List<String> typeList = new ArrayList<>() ;
        typeList.add("2");
        if (typeList.contains(type) && ObjectUtil.isNotEmpty(repeat)) {
            List<ScheduleNewEntity> list = getList(groupId, null);
            List<ScheduleNewEntity> collect = list.stream().filter(t -> t.getStartDay().getTime() < info.getStartDay().getTime()).sorted(Comparator.comparing(ScheduleNewEntity::getStartDay).reversed()).collect(Collectors.toList());
            for (ScheduleNewEntity scheduleNewEntity : collect) {
                String dateString = DateUtil.getDateString(collect.get(0).getStartDay(), TIME) + " 23:59:59";
                Date repeatTime = DateUtil.stringToDate(dateString);
                scheduleNewEntity.setRepeatTime(repeatTime);
                update(scheduleNewEntity.getId(), scheduleNewEntity);
            }
        }
    }
}
