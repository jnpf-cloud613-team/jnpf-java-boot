package jnpf.onlinedev.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.constant.MsgCode;
import jnpf.onlinedev.entity.VisualPersonalEntity;
import jnpf.onlinedev.model.personal.*;
import jnpf.onlinedev.service.VisualPersonalService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个性化列表视图控制器
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/6 10:25:31
 */
@Slf4j
@Tag(name = "在线开发个性化列表", description = "OnlinePersonal")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/personal")
public class VisualPersonalController {

    private final VisualPersonalService visualPersonalService;

    @Operation(summary = "列表")
    @Parameter(name = "menuId", description = "菜单id")
    @GetMapping
    public ActionResult<Object> getList(VisualPersPagiantion pagiantion) {
        StpUtil.checkPermissionOr(pagiantion.getMenuId());
        List<VisualPersonalVo> listVo = visualPersonalService.getListVo(pagiantion.getMenuId());
        return ActionResult.success(listVo);
    }

    @Operation(summary = "新建")
    @Parameter(name = "form", description = "个性化列表视图表单")
    @PostMapping
    public ActionResult<Object> create(@RequestBody VisualPersonalForm form) {
        StpUtil.checkPermissionOr(form.getMenuId());
        VisualPersonalEntity entity = JsonUtil.getJsonToBean(form, VisualPersonalEntity.class);
        if (ObjectUtil.isEmpty(form.getId()) || VisualPersConst.SYSTEM_ID.equals(form.getId())) {
            if (form.getFullName().length() > 6) {
                return ActionResult.fail(MsgCode.EXIST005.get());
            }
            if (visualPersonalService.isExistByFullName(form.getFullName(), null, form.getMenuId())) {
                return ActionResult.fail(MsgCode.EXIST001.get());
            }
            List<VisualPersonalEntity> list = visualPersonalService.getList(form.getMenuId());
            if (list.size() >= 5) {
                return ActionResult.fail(MsgCode.VS028.get());
            }
            entity.setId(RandomUtil.uuId());
            entity.setStatus(0);
            entity.setType(1);
            visualPersonalService.save(entity);
        }
        return ActionResult.success(MsgCode.SU001.get(), entity.getId());
    }

    @Operation(summary = "详情")
    @Parameter(name = "id", description = "个性化id")
    @Parameter(name = "menuId", description = "菜单id")
    @GetMapping("/{id}")
    public ActionResult<Object> info(@PathVariable("id") String id, VisualPersPagiantion pagiantion) {
        StpUtil.checkPermissionOr(pagiantion.getMenuId());
        VisualPersonalInfo info = visualPersonalService.getInfo(id);
        return ActionResult.success(info);
    }

    @Operation(summary = "修改")
    @Parameter(name = "id", description = "视图id")
    @Parameter(name = "form", description = "表单数据")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody VisualPersonalForm form) {
        StpUtil.checkPermissionOr(form.getMenuId());
        if (visualPersonalService.getById(id) != null) {
            if (form.getFullName().length() > 6) {
                return ActionResult.fail(MsgCode.EXIST005.get());
            }
            if (visualPersonalService.isExistByFullName(form.getFullName(), id, form.getMenuId())) {
                return ActionResult.fail(MsgCode.EXIST001.get());
            }
            VisualPersonalEntity entity = JsonUtil.getJsonToBean(form, VisualPersonalEntity.class);
            boolean b = visualPersonalService.updateById(entity);
            if (b) {
                return ActionResult.success(MsgCode.SU004.get());
            }
        }
        return ActionResult.success(MsgCode.FA002.get());
    }

    @Operation(summary = "设置默认")
    @Parameter(name = "id", description = "视图id")
    @Parameter(name = "menuId", description = "菜单id")
    @PutMapping("/{id}/setDefault")
    public ActionResult<Object> updateStatus(@PathVariable("id") String id, VisualPersPagiantion pagiantion) {
        StpUtil.checkPermissionOr(pagiantion.getMenuId());

        List<VisualPersonalEntity> list = visualPersonalService.getList(pagiantion.getMenuId());
        for (VisualPersonalEntity entity : list) {
            if (entity.getId().equals(id)) {
                entity.setStatus(1);
            } else {
                entity.setStatus(0);
            }
        }
        boolean b = visualPersonalService.updateBatchById(list);
        if (b) {
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.success(MsgCode.FA002.get());
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "视图id")
    @Parameter(name = "menuId", description = "菜单id")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id, VisualPersPagiantion pagiantion) {
        StpUtil.checkPermissionOr(pagiantion.getMenuId());
        if (visualPersonalService.getById(id) != null) {
            boolean b = visualPersonalService.removeById(id);
            if (b) {
                return ActionResult.success(MsgCode.SU003.get());
            }
        }
        return ActionResult.success(MsgCode.FA003.get());
    }
}
