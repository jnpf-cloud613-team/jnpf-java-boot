package jnpf.base.controller;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.ScheduleNewEntity;
import jnpf.base.entity.ScheduleNewUserEntity;
import jnpf.base.model.schedule.*;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.ScheduleNewApi;
import jnpf.base.service.ScheduleNewService;
import jnpf.base.service.ScheduleNewUserService;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.message.entity.SendMessageConfigEntity;
import jnpf.message.service.SendMessageConfigService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 日程
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Tag(name = "日程", description = "Schedule")
@RestController
@RequestMapping("/api/system/Schedule")
@RequiredArgsConstructor
public class ScheduleNewController extends SuperController<ScheduleNewService, ScheduleNewEntity> implements ScheduleNewApi {


    
    private final UserService userService;
    
    private final  SendMessageConfigService messageTemplateConfigService;
    
    private final  DictionaryDataService dictionaryDataService;
    
    private final  ScheduleNewService scheduleNewService;
    
    private final  ScheduleNewUserService scheduleNewUserService;

    /**
     * 获取日程安排列表
     *
     * @param scheduleNewTime 分页模型
     * @return
     */
    @Operation(summary = "获取日程安排列表")
    @GetMapping
    public ActionResult<ListVO<ScheduleNewListVO>> list(ScheduleNewTime scheduleNewTime) {
        List<ScheduleNewEntity> list = scheduleNewService.getList(scheduleNewTime);
        Date start = DateUtil.stringToDates(scheduleNewTime.getStartTime());
        Date end = DateUtil.stringToDates(scheduleNewTime.getEndTime());
        List<Date> dataAll = DateUtil.getAllDays(start, end);
        List<ScheduleNewEntity> result = new ArrayList<>();
        if (!list.isEmpty()) {
            for (Date date : dataAll) {
                for (ScheduleNewEntity entity : list) {
                    Date startDay = DateUtil.stringToDates(DateUtil.daFormat(entity.getStartDay()));
                    Date endDay = DateUtil.stringToDates(DateUtil.daFormat(entity.getEndDay()));
                    if(DateUtil.isEffectiveDate(date,startDay,endDay)){
                        result.add(entity);
                    }
                }
            }
        }
        for (ScheduleNewEntity item : result) {
            ScheduleNewEntity entity = BeanUtil.copyProperties(item, ScheduleNewEntity.class);
            if (entity.getAllDay() == 1) {
                entity.setEndDay(DateUtil.dateAddSeconds(entity.getEndDay(), 1));
            }
        }
        List<ScheduleNewListVO> vo = JsonUtil.getJsonToList(result, ScheduleNewListVO.class);
        ListVO<ScheduleNewListVO> listVO = new ListVO<>();
        listVO.setList(vo);
        return ActionResult.success(listVO);
    }

    /**
     * 获取日程安排列表
     *
     * @param scheduleNewTime 分页模型
     * @return
     */
    @Operation(summary = "获取日程安排列表")
    @GetMapping("/AppList")
    public ActionResult<ScheduleNewAppListVO> selectList(ScheduleNewTime scheduleNewTime) {
        Map<String, Object> signMap = new HashMap<>(16);
        List<ScheduleNewEntity> list = scheduleNewService.getList(scheduleNewTime);
        Date start = DateUtil.stringToDates(scheduleNewTime.getStartTime());
        Date end = DateUtil.stringToDates(scheduleNewTime.getEndTime());
        ArrayList<Date> arrayList = new ArrayList<>();
        arrayList.add(start);
        arrayList.add(end);
        if(StringUtils.isNotEmpty(scheduleNewTime.getDateTime())){
            arrayList.add(DateUtil.strToDate(scheduleNewTime.getDateTime()));
        }
        Date minDate =new Date();
        Date maxDate = new Date();
        Optional<Date> min = arrayList.stream().min(Date::compareTo);
        if (min.isPresent()) {
            minDate = min.get();
        }
        Optional<Date> max = arrayList.stream().max(Date::compareTo);
        if (max.isPresent()) {
            maxDate = max.get();
        }

        List<Date> dataAll = DateUtil.getAllDays(minDate, maxDate);
        ScheduleNewAppListVO vo = new ScheduleNewAppListVO();
        String pattern = "yyyyMMdd";
        String dateTime = StringUtils.isEmpty(scheduleNewTime.getDateTime()) ? DateUtil.dateNow(pattern) : scheduleNewTime.getDateTime().replace("-", "");
        List<ScheduleNewEntity> todayList = new ArrayList<>();
        for (Date date : dataAll) {
            String time = DateUtil.dateToString(date, pattern);
            List<ScheduleNewEntity> result = new ArrayList<>();
            for (ScheduleNewEntity entity : list) {
                Date startDay = DateUtil.stringToDates(DateUtil.daFormat(entity.getStartDay()));
                Date endDay = DateUtil.stringToDates(DateUtil.daFormat(entity.getEndDay()));
                if(DateUtil.isEffectiveDate(date,startDay,endDay)){
                    result.add(entity);
                }
            }
            signMap.put(time, result.size());
            if(time.equals(dateTime)){
                todayList.addAll(result);
            }
        }
        vo.setSignList(signMap);
        vo.setTodayList(JsonUtil.getJsonToList(todayList, ScheduleNewListVO.class));
        return ActionResult.success(vo);
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取日程安排信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<ScheduleNewInfoVO> info(@PathVariable("id") String id) {
        ScheduleNewEntity entity = scheduleNewService.getInfo(id);
        ScheduleNewInfoVO vo = JsonUtil.getJsonToBean(entity, ScheduleNewInfoVO.class);
        if (vo != null) {
            if (!Objects.equals(entity.getCreatorUserId(),UserProvider.getLoginUserId())){
                return ActionResult.fail(MsgCode.AD104.get());
            }
            SendMessageConfigEntity config = StringUtil.isNotEmpty(vo.getSend()) ? messageTemplateConfigService.getInfo(vo.getSend()) : null;
            vo.setSendName(config!=null?config.getFullName():"");
            List<String> toUserIds = scheduleNewUserService.getList(entity.getId(),2).stream().map(ScheduleNewUserEntity::getToUserId).collect(Collectors.toList());
            vo.setToUserIds(toUserIds);
            return ActionResult.success(vo);
        }
        return ActionResult.fail(MsgCode.FA001.get());
    }

    /**
     * 信息
     *
     * @param detailModel 查询模型
     * @return
     */
    @Operation(summary = "获取日程安排信息")
    @GetMapping("/detail")
    public ActionResult<ScheduleNewDetailInfoVO> detail(ScheduleDetailModel detailModel) {
        List<ScheduleNewEntity> groupList = scheduleNewService.getGroupList(detailModel);
        ScheduleNewEntity entity = !groupList.isEmpty() ? groupList.get(0) : null;
        boolean isVO = entity != null;
        if (isVO) {
            ScheduleNewDetailInfoVO vo = JsonUtil.getJsonToBean(entity, ScheduleNewDetailInfoVO.class);
            DictionaryDataEntity info = dictionaryDataService.getInfo(entity.getCategory());
            vo.setCategory(info != null ? info.getFullName() : "");
            String s="2".equals(vo.getUrgent()) ? "重要" : "紧急";
            vo.setUrgent("1".equals(vo.getUrgent()) ? "普通" : s);
            UserEntity infoById = userService.getInfo(vo.getCreatorUserId());
            vo.setCreatorUserId(infoById != null ? infoById.getRealName() + "/" + infoById.getAccount() : "");
            List<String> toUserIds = scheduleNewUserService.getList(entity.getId(),2).stream().map(ScheduleNewUserEntity::getToUserId).collect(Collectors.toList());
            List<String> userIdList = new ArrayList<>(toUserIds);
            userIdList.add(entity.getCreatorUserId());
            if (!userIdList.contains(UserProvider.getLoginUserId())){
                return ActionResult.fail(MsgCode.AD104.get());
            }
            List<UserEntity> userName = userService.getUserName(toUserIds);
            StringJoiner joiner = new StringJoiner(",");
            for (UserEntity userEntity : userName) {
                joiner.add(userEntity.getRealName() + "/" + userEntity.getAccount());
            }
            vo.setToUserIds(joiner.toString());
            return ActionResult.success(vo);
        }
        return ActionResult.fail(MsgCode.SYS042.get());
    }

    /**
     * 新建
     *
     * @param scheduleCrForm 日程模型
     * @return
     */
    @Operation(summary = "新建日程安排")
    @PostMapping
    @Parameter(name = "scheduleCrForm", description = "日程模型",required = true)
    public ActionResult<Object> create(@RequestBody @Valid ScheduleNewCrForm scheduleCrForm) {
        if (scheduleCrForm.paramCheck()) {
            return ActionResult.fail(scheduleCrForm.getErrMsg());
        }
        ScheduleNewEntity entity = JsonUtil.getJsonToBean(scheduleCrForm, ScheduleNewEntity.class);
        try {
            scheduleNewService.create(entity, scheduleCrForm.getToUserIds(), RandomUtil.uuId(),"1",new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return ActionResult.fail(e.getMessage());
        }
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id             主键
     * @param scheduleUpForm 日程模型
     * @param type           1.此日程 2.此日程及后续 3.所有日程
     * @return
     */
    @Operation(summary = "更新日程安排")
    @PutMapping("/{id}/{type}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "scheduleUpForm", description = "日程模型", required = true)
    @Parameter(name = "type", description = "类型", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid ScheduleNewUpForm scheduleUpForm, @PathVariable("type") String type) {
        if("1".equals(type)){
            scheduleUpForm.setRepeatTime(null);
            scheduleUpForm.setRepetition(1);
        }
        if (scheduleUpForm.paramCheck()) {
            return ActionResult.fail(scheduleUpForm.getErrMsg());
        }
        ScheduleNewEntity info = scheduleNewService.getInfo(id);
        if (!Objects.equals(info.getCreatorUserId(),UserProvider.getLoginUserId())){
            return ActionResult.fail(MsgCode.AD104.get());
        }
        ScheduleNewEntity entity = JsonUtil.getJsonToBean(scheduleUpForm, ScheduleNewEntity.class);
        boolean flag = false;
        try {
            flag = scheduleNewService.update(id, entity, scheduleUpForm.getToUserIds(), type);
        } catch (Exception e) {
            e.printStackTrace();
            return ActionResult.fail(e.getMessage());
        }
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id   主键
     * @param type           1.此日程 2.此日程及后续 3.所有日程
     * @return
     */
    @Operation(summary = "删除日程安排")
    @DeleteMapping("/{id}/{type}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "type", description = "类型", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id, @PathVariable("type") String type) {
        ScheduleNewEntity entity = scheduleNewService.getInfo(id);
        if (entity != null) {
            List<String> toUserIds = scheduleNewUserService.getList(entity.getId(), 2).stream().map(ScheduleNewUserEntity::getToUserId).collect(Collectors.toList());
            if (!Objects.equals(entity.getCreatorUserId(), UserProvider.getLoginUserId()) && !toUserIds.contains(UserProvider.getLoginUserId())) {
                return ActionResult.fail(MsgCode.AD104.get());
            }
            scheduleNewService.delete(entity, type);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

}
