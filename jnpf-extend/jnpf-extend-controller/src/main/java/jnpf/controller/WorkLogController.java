package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.entity.WorkLogEntity;
import jnpf.exception.DataException;
import jnpf.model.worklog.WorkLogCrForm;
import jnpf.model.worklog.WorkLogInfoVO;
import jnpf.model.worklog.WorkLogListVO;
import jnpf.model.worklog.WorkLogUpForm;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.service.WorkLogService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * 工作日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "app工作日志", description = "WorkLog")
@RestController
@RequestMapping("/api/extend/WorkLog")
@RequiredArgsConstructor
public class WorkLogController extends SuperController<WorkLogService, WorkLogEntity> {


    private final WorkLogService workLogService;

    private final UserService userService;

    /**
     * 列表(我发出的)
     *
     * @param pageModel 分页模型
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping("/Send")
    @SaCheckPermission("reportinglog")
    public ActionResult<PageListVO<WorkLogListVO>> getSendList(Pagination pageModel) {
        List<WorkLogEntity> data = workLogService.getSendList(pageModel);
        List<WorkLogListVO> list = JsonUtil.getJsonToList(data, WorkLogListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pageModel, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 列表(我收到的)
     *
     * @param pageModel 分页模型
     * @return
     */
    @GetMapping("/Receive")
    @SaCheckPermission("reportinglog")
    public ActionResult<PageListVO<WorkLogListVO>> getReceiveList(Pagination pageModel) {
        List<WorkLogEntity> data = workLogService.getReceiveList(pageModel);
        List<WorkLogListVO> list = JsonUtil.getJsonToList(data, WorkLogListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pageModel, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("reportinglog")
    public ActionResult<WorkLogInfoVO> info(@PathVariable("id") String id) throws DataException {
        WorkLogEntity entity = workLogService.getInfo(id);
        StringJoiner userName = new StringJoiner(",");
        StringJoiner userIds = new StringJoiner(",");
        List<String> userId = Arrays.asList(entity.getToUserId().split(","));
        List<UserEntity> userList = userService.getUserName(userId);
        for (UserEntity user : userList) {
            userIds.add(user.getId());
            userName.add(user.getRealName() + "/" + user.getAccount());
        }
        entity.setToUserId(userName.toString());
        WorkLogInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, WorkLogInfoVO.class);
        vo.setUserIds(userIds.toString());
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @param workLogCrForm 日志模型
     * @return
     */
    @Operation(summary = "新建")
    @PostMapping
    @Parameter(name = "workLogCrForm", description = "日志模型",required = true)
    @SaCheckPermission("reportinglog")
    public ActionResult<Object>create(@RequestBody @Valid WorkLogCrForm workLogCrForm) {
        WorkLogEntity entity = JsonUtil.getJsonToBean(workLogCrForm, WorkLogEntity.class);
        workLogService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id            主键
     * @param workLogUpForm 日志模型
     * @return
     */
    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @Parameter(name = "workLogUpForm", description = "日志模型",required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("reportinglog")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid WorkLogUpForm workLogUpForm) {
        WorkLogEntity entity = JsonUtil.getJsonToBean(workLogUpForm, WorkLogEntity.class);
        boolean flag = workLogService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("reportinglog")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        WorkLogEntity entity = workLogService.getInfo(id);
        if (entity != null) {
            workLogService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }
}

