package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleColumnEntity;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.model.column.*;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.service.ModuleColumnService;
import jnpf.base.service.ModuleService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.ReflectionUtil;
import jnpf.util.StringUtil;
import jnpf.util.context.SpringContext;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 列表权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "列表权限", description = "ModuleColumn")
@Validated
@RestController
@RequestMapping("/api/system/ModuleColumn")
@RequiredArgsConstructor
public class ModuleColumnController extends SuperController<ModuleColumnService, ModuleColumnEntity> {

  
    private final ModuleColumnService moduleColumnService;

    private final ModuleService moduleService;

    /**
     * 获取列表权限信息列表
     *
     * @param moduleId   功能主键
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "获取列表权限列表")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{moduleId}/Fields")
    public ActionResult<PageListVO<ColumnListVO>> getList(@PathVariable("moduleId") String moduleId, Pagination pagination) {
        List<ModuleColumnEntity> list = moduleColumnService.getList(moduleId, pagination);
        List<ColumnListVO> voList = JsonUtil.getJsonToList(list, ColumnListVO.class);
        voList.forEach(t -> {
            String enCode = t.getEnCode();
            if (StringUtil.isNotEmpty(enCode)) {
                if (enCode.contains("-")) {
                    enCode = enCode.substring(enCode.indexOf("-") + 1);
                }
                t.setEnCode(enCode.replace(JnpfConst.SIDE_MARK_PRE + t.getBindTable() + JnpfConst.SIDE_MARK, ""));
            }
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 菜单列表权限
     *
     * @param moduleId 功能主键
     * @return ignore
     */
    @Operation(summary = "菜单列表权限")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{moduleId}/FieldList")
    public ActionResult<List<Map<String, String>>> fieldList(@PathVariable("moduleId") String moduleId) {
        List<Map<String, String>> list = new ArrayList<>();
        // 得到菜单id
        ModuleEntity entity = moduleService.getInfo(moduleId);
        if (entity != null) {
            PropertyJsonModel model = JsonUtil.getJsonToBean(entity.getPropertyJson(), PropertyJsonModel.class);
            if (model == null) {
                model = new PropertyJsonModel();
            }
            // 得到bean
            Object bean = SpringContext.getBean("visualdevServiceImpl");
            Object method = ReflectionUtil.invokeMethod(bean, "getInfo", new Class[]{String.class}, new Object[]{model.getModuleId()});
            Map<String, Object> map = JsonUtil.entityToMap(method);
            boolean isPc = entity.getCategory().equalsIgnoreCase("web");
            if (map != null) {
                Object columnData = isPc ? map.get("columnData") : map.get("appColumnData");
                if (Objects.nonNull(columnData)) {
                    ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData.toString(), ColumnDataModel.class);
                    List<ColumnListField> columnListFields = JsonUtil.getJsonToList(columnDataModel.getDefaultColumnList(), ColumnListField.class);
                    if (Objects.nonNull(columnListFields)) {
                        columnListFields.forEach(col -> {
                            Map<String, String> dataMap = new HashMap<>();
                            dataMap.put("field", col.getProp());
                            dataMap.put("fieldName", col.getLabel());
                            list.add(dataMap);
                        });
                    }
                }
            }
        }
        
        return ActionResult.success(list);
    }

    /**
     * 获取列表权限信息
     *
     * @param id 主键值
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "获取列表权限信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @GetMapping("/{id}")
    public ActionResult<ModuleColumnInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleColumnEntity entity = moduleColumnService.getInfo(id);
        String enCode = entity.getEnCode();
        if (StringUtil.isNotEmpty(enCode)) {
            if (enCode.contains("-") && entity.getFieldRule() == 2) {
                enCode = enCode.substring(enCode.indexOf("-") + 1);
                entity.setEnCode(enCode);
            }
            if (Objects.equals(entity.getFieldRule(), 1) && entity.getBindTable() != null) {
                entity.setEnCode(enCode.replace(JnpfConst.SIDE_MARK_PRE + entity.getBindTable() + JnpfConst.SIDE_MARK, ""));
            }
        }
        ModuleColumnInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ModuleColumnInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建列表权限
     *
     * @param moduleColumnCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "新建列表权限")
    @Parameter(name = "moduleColumnCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid ModuleColumnCrForm moduleColumnCrForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(moduleColumnCrForm.getModuleId());
        ModuleColumnEntity entity = JsonUtil.getJsonToBean(moduleColumnCrForm, ModuleColumnEntity.class);

        if (moduleEntity != null) {
            if (entity.getFieldRule() == 1 && StringUtil.isNotEmpty(moduleColumnCrForm.getBindTable())) {
                String enCode = JnpfConst.SIDE_MARK_PRE + moduleColumnCrForm.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }

            if (entity.getFieldRule() == 2 && StringUtil.isNotEmpty(moduleColumnCrForm.getChildTableKey())) {
                String enCode = moduleColumnCrForm.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
        }
        if (moduleColumnService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        moduleColumnService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新列表权限
     *
     * @param id                 主键值
     * @param moduleColumnUpForm 实体对象
     * @return ignore
     */
    @Operation(summary = "更新列表权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "moduleColumnUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ModuleColumnUpForm moduleColumnUpForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(moduleColumnUpForm.getModuleId());
        ModuleColumnEntity entity = JsonUtil.getJsonToBean(moduleColumnUpForm, ModuleColumnEntity.class);
        if (moduleEntity != null) {
            if (entity.getFieldRule() == 1 && StringUtil.isNotEmpty(moduleColumnUpForm.getBindTable())) {
                String enCode = JnpfConst.SIDE_MARK_PRE + moduleColumnUpForm.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }

            if (entity.getFieldRule() == 2 && StringUtil.isNotEmpty(moduleColumnUpForm.getChildTableKey())) {
                String enCode = moduleColumnUpForm.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
        }
        if (moduleColumnService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = moduleColumnService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除列表权限
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除列表权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleColumnEntity entity = moduleColumnService.getInfo(id);
        if (entity != null) {
            moduleColumnService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 更新列表权限状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新列表权限状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object>upState(@PathVariable("id") String id) {
        ModuleColumnEntity entity = moduleColumnService.getInfo(id);
        if (entity.getEnabledMark() == null || "1".equals(String.valueOf(entity.getEnabledMark()))) {
            entity.setEnabledMark(0);
        } else {
            entity.setEnabledMark(1);
        }
        boolean flag = moduleColumnService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 批量新建
     *
     * @param columnBatchForm 权限模型
     * @return ignore
     */
    @Operation(summary = "批量新建列表权限")
    @Parameter(name = "columnBatchForm", description = "权限模型", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping("/Actions/Batch")
    public ActionResult<Object>batchCreate(@RequestBody @Valid ColumnBatchForm columnBatchForm) {
        List<ModuleColumnEntity> entitys = columnBatchForm.getColumnJson() != null ? JsonUtil.getJsonToList(columnBatchForm.getColumnJson(), ModuleColumnEntity.class) : new ArrayList<>();
        List<String> name = new ArrayList<>();
        for (ModuleColumnEntity entity : entitys) {
            entity.setModuleId(columnBatchForm.getModuleId());
            if (entity.getFieldRule() == 1) {
                String enCode = JnpfConst.SIDE_MARK_PRE + entity.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }
            if (entity.getFieldRule() == 2) {
                String enCode = entity.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
            if (moduleColumnService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), null)) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            if (name.contains(entity.getEnCode())) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            name.add(entity.getEnCode());
        }
        moduleColumnService.create(entitys);
        return ActionResult.success(MsgCode.SU001.get());
    }
}
