package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleButtonEntity;
import jnpf.base.model.button.*;
import jnpf.base.service.ModuleButtonService;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.permission.entity.ActionEntity;
import jnpf.permission.model.action.ActionForm;
import jnpf.permission.service.ActionService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.RandomUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 按钮权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "按钮权限", description = "ModuleButton")
@RestController
@RequestMapping("/api/system/ModuleButton")
@RequiredArgsConstructor
public class ModuleButtonController extends SuperController<ModuleButtonService, ModuleButtonEntity> {


    private final ModuleButtonService moduleButtonService;

    private final ActionService actionService;

    /**
     * 按钮按钮权限列表
     *
     * @param menuId     功能主键
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "获取按钮权限列表")
    @Parameter(name = "menuId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{menuId}/List")
    public ActionResult<PageListVO<ButtonListVO>> list(@PathVariable("menuId") String menuId, Pagination pagination) {
        List<ModuleButtonEntity> data = moduleButtonService.getListByModuleIds(menuId, pagination);
        List<ButtonListVO> list = JsonUtil.getJsonToList(data, ButtonListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }


    /**
     * 按钮按钮权限列表
     *
     * @param menuId 功能主键
     * @return ignore
     */
    @Operation(summary = "获取按钮权限下拉框")
    @Parameter(name = "menuId", description = "功能主键", required = true)
    @GetMapping("/{menuId}/Selector")
    public ActionResult<ListVO<ButtonTreeListVO>> selectList(@PathVariable("menuId") String menuId) {
        List<ModuleButtonEntity> data = moduleButtonService.getListByModuleIds(menuId);
        List<ButtonTreeListModel> treeList = JsonUtil.getJsonToList(data, ButtonTreeListModel.class);
        List<SumTree<ButtonTreeListModel>> sumTrees = TreeDotUtils.convertListToTreeDot(treeList);
        List<ButtonTreeListVO> list = JsonUtil.getJsonToList(sumTrees, ButtonTreeListVO.class);
        ListVO<ButtonTreeListVO> treeVo = new ListVO<>();
        treeVo.setList(list);
        return ActionResult.success(treeVo);
    }


    /**
     * 获取按钮权限信息
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "获取按钮权限信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<ModuleButtonInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleButtonEntity entity = moduleButtonService.getInfo(id);
        ModuleButtonInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ModuleButtonInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建按钮权限
     *
     * @param moduleButtonCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "新建按钮权限")
    @Parameter(name = "moduleButtonCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object>create(@RequestBody ModuleButtonCrForm moduleButtonCrForm) {
        ModuleButtonEntity entity = JsonUtil.getJsonToBean(moduleButtonCrForm, ModuleButtonEntity.class);
        if (moduleButtonService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        moduleButtonService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新按钮权限
     *
     * @param id                 主键值
     * @param moduleButtonUpForm 更新参数
     * @return ignore
     */
    @Operation(summary = "更新按钮权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "moduleButtonUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody ModuleButtonUpForm moduleButtonUpForm) {
        ModuleButtonEntity entity = JsonUtil.getJsonToBean(moduleButtonUpForm, ModuleButtonEntity.class);
        if (moduleButtonService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = moduleButtonService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除按钮权限
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除按钮权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleButtonEntity entity = moduleButtonService.getInfo(id);
        if (entity != null) {
            moduleButtonService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 更新菜单状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新菜单状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object>upState(@PathVariable("id") String id) {
        ModuleButtonEntity entity = moduleButtonService.getInfo(id);
        if (entity.getEnabledMark() == null || "1".equals(String.valueOf(entity.getEnabledMark()))) {
            entity.setEnabledMark(0);
        } else {
            entity.setEnabledMark(1);
        }
        boolean flag = moduleButtonService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "根据系统动作管理选择动作")
    @PostMapping("/selectActions/{menuId}")
    @SaCheckPermission(value = {"permission.auth", "permission.role", "permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    public ActionResult<Object>selectActionBySystem(@RequestBody ActionForm actionForm, @PathVariable String menuId) {
        List<ActionEntity> ids = actionForm.getIds();
        for (ActionEntity id : ids) {
            if (moduleButtonService.isExistByEnCode(actionForm.getMenuId(), id.getEnCode(), null)) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
        }
        List<ModuleButtonEntity> jsonToList = JsonUtil.getJsonToList(ids, ModuleButtonEntity.class);
        jsonToList.forEach(item->{
                    item.setModuleId(menuId);
                    item.setParentId("-1");
                    item.setEnabledMark(1);
                    item.setId(RandomUtil.uuId());
                });

        moduleButtonService.saveBatch(jsonToList);
        return ActionResult.success(MsgCode.SU005.get());
    }

}
