package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import jnpf.base.model.moduledataauthorizescheme.DataAuthorizeSchemeCrForm;
import jnpf.base.model.moduledataauthorizescheme.DataAuthorizeSchemeInfoVO;
import jnpf.base.model.moduledataauthorizescheme.DataAuthorizeSchemeListVO;
import jnpf.base.model.moduledataauthorizescheme.DataAuthorizeSchemeUpForm;
import jnpf.base.service.ModuleDataAuthorizeSchemeService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.permission.service.AuthorizeService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限方案
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据权限方案", description = "ModuleDataAuthorizeScheme")
@RestController
@RequestMapping("/api/system/ModuleDataAuthorizeScheme")
@RequiredArgsConstructor
public class ModuleDataAuthorizeSchemeController extends SuperController<ModuleDataAuthorizeSchemeService, ModuleDataAuthorizeSchemeEntity> {

   
    private final ModuleDataAuthorizeSchemeService schemeService;

  
    private final AuthorizeService authorizeService;

    /**
     * 列表
     *
     * @param moduleId 功能主键
     * @return ignore
     */
    @Operation(summary = "方案列表")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @GetMapping("/{moduleId}/List")
    public ActionResult<PageListVO<DataAuthorizeSchemeListVO>> list(@PathVariable("moduleId") String moduleId, Pagination pagination) {
        List<ModuleDataAuthorizeSchemeEntity> data = schemeService.getList(moduleId, pagination);
        List<DataAuthorizeSchemeListVO> list = JsonUtil.getJsonToList(data, DataAuthorizeSchemeListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "获取方案信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<DataAuthorizeSchemeInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleDataAuthorizeSchemeEntity entity = schemeService.getInfo(id);
        DataAuthorizeSchemeInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, DataAuthorizeSchemeInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @param dataAuthorizeSchemeCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "新建方案")
    @Parameter(name = "dataAuthorizeSchemeCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid DataAuthorizeSchemeCrForm dataAuthorizeSchemeCrForm) {
        ModuleDataAuthorizeSchemeEntity entity = JsonUtil.getJsonToBean(dataAuthorizeSchemeCrForm, ModuleDataAuthorizeSchemeEntity.class);
        // 判断fullName是否重复
        if (Boolean.TRUE.equals(schemeService.isExistByFullName(entity.getId(), entity.getFullName(), entity.getModuleId()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        // 判断encode是否重复
        if (Boolean.TRUE.equals(schemeService.isExistByEnCode(entity.getId(), entity.getEnCode(), entity.getModuleId()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        schemeService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id                        主键值
     * @param dataAuthorizeSchemeUpForm 实体对象
     * @return ignore
     */
    @Operation(summary = "更新方案")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "dataAuthorizeSchemeUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid DataAuthorizeSchemeUpForm dataAuthorizeSchemeUpForm) {
        ModuleDataAuthorizeSchemeEntity entity = JsonUtil.getJsonToBean(dataAuthorizeSchemeUpForm, ModuleDataAuthorizeSchemeEntity.class);
        // 判断encode是否重复
        if ("1".equals(String.valueOf(entity.getAllData()))) {
            return ActionResult.fail(MsgCode.SYS021.get());
        }
        // 判断fullName是否重复
        if (Boolean.TRUE.equals(schemeService.isExistByFullName(id, entity.getFullName(), entity.getModuleId()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        // 判断encode是否重复
        if (Boolean.TRUE.equals(schemeService.isExistByEnCode(id, entity.getEnCode(), entity.getModuleId()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = schemeService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(dataAuthorizeSchemeUpForm.getModuleId());
        authorizeService.removeAuthByUserOrMenu(null,arrayList);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除方案")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleDataAuthorizeSchemeEntity entity = schemeService.getInfo(id);
        if (entity != null) {
            schemeService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

}
