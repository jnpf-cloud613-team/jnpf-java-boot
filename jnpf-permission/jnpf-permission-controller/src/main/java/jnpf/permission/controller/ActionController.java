package jnpf.permission.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.permission.entity.ActionEntity;
import jnpf.permission.model.action.ActionPagination;
import jnpf.permission.service.ActionService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "动作管理", description = "Action")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Action")
public class ActionController {

    private final ActionService actionService;

    @Operation(summary = "查询动作列表")
    @GetMapping
    public ActionResult<PageListVO<ActionEntity>> selectAction(ActionPagination actionPagination) {
        List<ActionEntity> actionList = actionService.getActionList(actionPagination);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(actionPagination, PaginationVO.class);
        return ActionResult.page(actionList, paginationVO);
    }

    @Operation(summary = "新增")
    @SaCheckPermission(value = {"permission.auth", "permission.role", "permission.action"}, mode = SaMode.OR)
    @PostMapping("")
    public ActionResult<T> insertAction(@RequestBody ActionEntity actionEntity) {
        actionEntity.setId(RandomUtil.uuId());
        Boolean b = actionService.insertOrUpdate(actionEntity);
        return Boolean.TRUE.equals(b) ? ActionResult.success(MsgCode.SU001.get()) : ActionResult.fail(MsgCode.EXIST002.get());
    }

    @Operation(summary = "更新")
    @SaCheckPermission(value = {"permission.auth", "permission.role", "permission.action"}, mode = SaMode.OR)
    @PutMapping("/{actionId}")
    public ActionResult<T> updateAction(@RequestBody ActionEntity actionEntity, @PathVariable String actionId) {
        Boolean b = actionService.insertOrUpdate(actionEntity);
        return Boolean.TRUE.equals(b) ? ActionResult.success(MsgCode.SU004.get()) : ActionResult.fail(MsgCode.EXIST002.get());
    }

    @Operation(summary = "删除")
    @SaCheckPermission(value = {"permission.auth", "permission.role", "permission.action"}, mode = SaMode.OR)
    @DeleteMapping("/{actionId}")
    public ActionResult<T> deleteAction(@PathVariable String actionId) {
        Boolean delete = actionService.deleteById(actionId);
        return Boolean.TRUE.equals(delete) ? ActionResult.success(MsgCode.SU003.get()) : ActionResult.fail(MsgCode.FA003.get());
    }


    @Operation(summary = "查询动作详情")
    @GetMapping("/{actionId}")
    public ActionResult<ActionEntity> selectById(@PathVariable String actionId) {
        ActionEntity byId = actionService.getById(actionId);
        if (byId == null) {
            return ActionResult.fail(MsgCode.FA104.get());
        }
        return ActionResult.success(actionService.getById(actionId));
    }


}
