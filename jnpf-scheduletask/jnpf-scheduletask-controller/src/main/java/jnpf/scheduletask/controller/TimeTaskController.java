package jnpf.scheduletask.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.Pagination;
import jnpf.constant.MsgCode;
import jnpf.scheduletask.entity.HandlerNameEntity;
import jnpf.scheduletask.entity.TimeTaskEntity;
import jnpf.exception.DataException;
import jnpf.scheduletask.model.*;
import jnpf.scheduletask.rest.RestScheduleTaskUtil;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.UserProvider;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务调度
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "任务调度", description = "TimeTask")
@RestController
@RequestMapping("/api/scheduletask")
public class TimeTaskController {

    

    /**
     * 获取任务调度列表
     *
     * @param pagination
     * @return
     */
    @Operation(summary = "获取任务调度列表")
    @SaCheckPermission("sysService.task")
    @GetMapping
    public JSONObject list(Pagination pagination) {
        UserInfo userInfo = UserProvider.getUser();
        return RestScheduleTaskUtil.getList(pagination, userInfo);
    }

    /**
     * 获取本地任务列表
     *
     * @return
     */
    @Operation(summary = "获取任务调度列表")
    @SaCheckPermission("sysService.task")
    @GetMapping("/TaskMethods")
    public ActionResult<List<TaskMethodsVO>> taskMethods() {
        List<TaskMethodsVO> list = new ArrayList<>(16);
        // 获取所有handlerName
        List<HandlerNameEntity> handlerNameEntities = RestScheduleTaskUtil.getHandlerList();
        for (HandlerNameEntity entity : handlerNameEntities) {
            TaskMethodsVO taskMethodsVO = new TaskMethodsVO();
            taskMethodsVO.setId(entity.getId());
            taskMethodsVO.setFullName(entity.getHandlerName());
            list.add(taskMethodsVO);
        }
        return ActionResult.success(list);
    }

    /**
     * 获取任务调度日志列表
     *
     * @param pagination
     * @param taskId     任务Id
     * @return
     */
    @Operation(summary = "获取任务调度日志列表")
    @SaCheckPermission("sysService.task")
    @GetMapping("/{id}/TaskLog")
    public JSONObject list(@PathVariable("id") String taskId, TaskPage pagination) {
        return RestScheduleTaskUtil.getLogList(taskId, UserProvider.getUser(), pagination);
    }

    /**
     * 获取任务调度信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取任务调度信息")
    @SaCheckPermission("sysService.task")
    @GetMapping("/Info/{id}")
    public ActionResult<TaskInfoVO> info(@PathVariable("id") String id) throws DataException {
        TimeTaskEntity entity = RestScheduleTaskUtil.getInfo(id, UserProvider.getUser());
        TaskInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, TaskInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建任务调度
     *
     * @param taskCrForm
     * @return
     */
    @Operation(summary = "新建任务调度")
    @SaCheckPermission("sysService.task")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid TaskCrForm taskCrForm) {
        taskCrForm.setUserInfo(UserProvider.getUser());
        JSONObject jsonObject = RestScheduleTaskUtil.create(taskCrForm);
        return JsonUtil.getJsonToBean(jsonObject, ActionResult.class);
    }

    /**
     * 修改任务调度
     *
     * @param id         主键值
     * @param taskUpForm
     * @return
     */
    @Operation(summary = "修改任务调度")
    @SaCheckPermission("sysService.task")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid TaskUpForm taskUpForm) {
        taskUpForm.setUserInfo(UserProvider.getUser());
        JSONObject jsonObject = RestScheduleTaskUtil.update(id, taskUpForm);
        return JsonUtil.getJsonToBean(jsonObject, ActionResult.class);
    }

    /**
     * 删除任务
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "删除任务")
    @SaCheckPermission("sysService.task")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        JSONObject jsonObject = RestScheduleTaskUtil.delete(id, UserProvider.getUser());
        return JsonUtil.getJsonToBean(jsonObject, ActionResult.class);
    }

    /**
     * 停止任务调度
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "停止任务调度")
    @SaCheckPermission("sysService.task")
    @PutMapping("/{id}/Actions/Stop")
    public ActionResult<Object> stop(@PathVariable("id") String id) {
        UpdateTaskModel updateTaskModel = new UpdateTaskModel();
        TimeTaskEntity entity = RestScheduleTaskUtil.getInfo(id, UserProvider.getUser());
        if (entity != null) {
            entity.setEnabledMark(0);
            entity.setRunCount(entity.getRunCount());
            updateTaskModel.setEntity(entity);
            updateTaskModel.setUserInfo(UserProvider.getUser());
            RestScheduleTaskUtil.updateTask(updateTaskModel);
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.fail(MsgCode.SC001.get());
    }

    /**
     * 启动任务调度
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "启动任务调度")
    @SaCheckPermission("sysService.task")
    @PutMapping("/{id}/Actions/Enable")
    public ActionResult<Object> enable(@PathVariable("id") String id) {
        UpdateTaskModel updateTaskModel = new UpdateTaskModel();
        TimeTaskEntity entity = RestScheduleTaskUtil.getInfo(id, UserProvider.getUser());
        if (entity != null) {
            entity.setEnabledMark(1);
            updateTaskModel.setEntity(entity);
            updateTaskModel.setUserInfo(UserProvider.getUser());
            RestScheduleTaskUtil.updateTask(updateTaskModel);
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.fail(MsgCode.SC001.get());
    }

}
