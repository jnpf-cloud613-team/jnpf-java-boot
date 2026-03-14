package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.ModuleFormEntity;
import jnpf.base.model.form.*;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.service.ModuleFormService;
import jnpf.base.service.ModuleService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.*;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.ReflectionUtil;
import jnpf.util.StringUtil;
import jnpf.util.context.SpringContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表单权限
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-09-14
 */
@Tag(name = "表单权限", description = "ModuleForm")
@RestController
@RequestMapping("/api/system/ModuleForm")
@RequiredArgsConstructor
public class ModuleFormController extends SuperController<ModuleFormService, ModuleFormEntity> {

    public static final String FIELD = "field";
    public static final String FIELD_NAME = "fieldName";

    private final ModuleFormService moduleFormService;

    private final ModuleService moduleService;

    /**
     * 获取表单权限列表
     *
     * @param moduleId   功能主键
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "获取表单权限列表")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{moduleId}/Fields")
    public ActionResult<PageListVO<ModuleFormListVO>> getList(@PathVariable("moduleId") String moduleId, Pagination pagination) {
        List<ModuleFormEntity> list = moduleFormService.getList(moduleId, pagination);
        List<ModuleFormListVO> voList = JsonUtil.getJsonToList(list, ModuleFormListVO.class);
        voList.stream().forEach(t -> {
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
     * 菜单数据权限
     *
     * @param moduleId 功能主键
     * @return ignore
     */
    @Operation(summary = "菜单数据权限")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{moduleId}/FieldList")
    public ActionResult<List<Map<String, String>>> fieldList(@PathVariable("moduleId") String moduleId) {
        List<Map<String, String>> list = new ArrayList<>();
        // 得到菜单id
        ModuleEntity entity = moduleService.getInfo(moduleId);
        PropertyJsonModel model = JsonUtil.getJsonToBean(entity.getPropertyJson(), PropertyJsonModel.class);
        if (model == null) {
            model = new PropertyJsonModel();
        }
        // 得到bean
        Object bean = SpringContext.getBean("visualdevServiceImpl");
        Object method = ReflectionUtil.invokeMethod(bean, "getInfo", new Class[]{String.class}, new Object[]{model.getModuleId()});
        Map<String, Object> map = JsonUtil.entityToMap(method);
        if (map != null && map.containsKey("formData")) {
            FormDataModel formDataModel = JsonUtil.getJsonToBean(String.valueOf(map.get("formData")), FormDataModel.class);
            List<FieLdsModel> fieLdsModelList = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
            RecursionForm recursionForm = new RecursionForm();
            recursionForm.setList(fieLdsModelList);
            recursionForm.setTableModelList(JsonUtil.getJsonToList(String.valueOf(map.get("tables")), TableModel.class));
            List<FormAllModel> formAllModel = new ArrayList<>();
            FormCloumnUtil.recursionForm(recursionForm, formAllModel);
            for (FormAllModel allModel : formAllModel) {
                if (FormEnum.TABLE.getMessage().equals(allModel.getJnpfKey())) {
                    FormColumnTableModel childList = allModel.getChildList();
                    Map<String, String> map1 = new HashedMap<>();
                    map1.put(FIELD, childList.getTableModel());
                    map1.put(FIELD_NAME, childList.getLabel());
                    list.add(map1);
                } else if (FormEnum.MAST.getMessage().equals(allModel.getJnpfKey())) {
                    FormColumnModel formColumnModel = allModel.getFormColumnModel();
                    FieLdsModel fieLdsModel = formColumnModel.getFieLdsModel();
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        Map<String, String> map1 = new HashedMap<>();
                        map1.put(FIELD, fieLdsModel.getVModel());
                        map1.put(FIELD_NAME, fieLdsModel.getConfig().getLabel());
                        list.add(map1);
                    }
                } else if (FormEnum.MAST_TABLE.getMessage().equals(allModel.getJnpfKey())) {
                    FormMastTableModel formColumnModel = allModel.getFormMastTableModel();
                    FieLdsModel fieLdsModel = formColumnModel.getMastTable().getFieLdsModel();
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        Map<String, String> map1 = new HashedMap<>();
                        map1.put(FIELD, fieLdsModel.getVModel());
                        map1.put(FIELD_NAME, fieLdsModel.getConfig().getLabel());
                        list.add(map1);
                    }
                }
            }
        }
        return ActionResult.success(list);
    }

    /**
     * 获取表单权限信息
     *
     * @param id 主键值
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "获取表单权限信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @GetMapping("/{id}")
    public ActionResult<ModuleFormInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleFormEntity entity = moduleFormService.getInfo(id);
        String enCode = entity.getEnCode();
        if (StringUtil.isNotEmpty(enCode)) {
            if (enCode.contains("-") && entity.getFieldRule() == 2) {
                enCode = enCode.substring(enCode.indexOf("-") + 1);
                entity.setEnCode(enCode);
            }
            if (entity.getFieldRule() == 1 && entity.getBindTable() != null) {
                entity.setEnCode(enCode.replace(JnpfConst.SIDE_MARK_PRE + entity.getBindTable() + JnpfConst.SIDE_MARK, ""));
            }
        }
        ModuleFormInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ModuleFormInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建表单权限
     *
     * @param moduleFormCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "新建表单权限")
    @Parameter(name = "moduleFormCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid ModuleFormCrForm moduleFormCrForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(moduleFormCrForm.getModuleId());
        ModuleFormEntity entity = JsonUtil.getJsonToBean(moduleFormCrForm, ModuleFormEntity.class);

        if (moduleEntity != null) {

            if (entity.getFieldRule() == 1 && StringUtil.isNotEmpty(moduleFormCrForm.getBindTable())) {
                String enCode = JnpfConst.SIDE_MARK_PRE + moduleFormCrForm.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }

            if (entity.getFieldRule() == 2 && StringUtil.isNotEmpty(moduleFormCrForm.getChildTableKey())) {
                String enCode = moduleFormCrForm.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
        }
        if (moduleFormService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        moduleFormService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新表单权限
     *
     * @param id               主键值
     * @param moduleFormUpForm 实体对象
     * @return ignore
     */
    @Operation(summary = "更新表单权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "moduleFormUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ModuleFormUpForm moduleFormUpForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(moduleFormUpForm.getModuleId());
        ModuleFormEntity entity = JsonUtil.getJsonToBean(moduleFormUpForm, ModuleFormEntity.class);
        if (moduleEntity != null) {
            if (entity.getFieldRule() == 1 && StringUtil.isNotEmpty(moduleFormUpForm.getBindTable())) {
                String enCode = JnpfConst.SIDE_MARK_PRE + moduleFormUpForm.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }

            if (entity.getFieldRule() == 2 && StringUtil.isNotEmpty(moduleFormUpForm.getChildTableKey())) {
                String enCode = moduleFormUpForm.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
        }
        if (moduleFormService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = moduleFormService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除表单权限
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "删除表单权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleFormEntity entity = moduleFormService.getInfo(id);
        if (entity != null) {
            moduleFormService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 批量新建
     *
     * @param formBatchForm 批量表单模型
     * @return
     */
    @Operation(summary = "批量新建表单权限")
    @Parameter(name = "formBatchForm", description = "批量表单模型", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping("/Actions/Batch")
    public ActionResult<Object>batchCreate(@RequestBody @Valid FormBatchForm formBatchForm) {
        List<ModuleFormEntity> entitys = formBatchForm.getFormJson() != null ? JsonUtil.getJsonToList(formBatchForm.getFormJson(), ModuleFormEntity.class) : new ArrayList<>();
        List<String> name = new ArrayList<>();
        for (ModuleFormEntity entity : entitys) {
            if (entity.getFieldRule() == 1) {
                String enCode = JnpfConst.SIDE_MARK_PRE + entity.getBindTable() + JnpfConst.SIDE_MARK + entity.getEnCode();
                entity.setEnCode(enCode);
            }
            if (entity.getFieldRule() == 2) {
                String enCode = entity.getChildTableKey() + "-" + entity.getEnCode();
                entity.setEnCode(enCode);
            }
            entity.setModuleId(formBatchForm.getModuleId());
            if (moduleFormService.isExistByEnCode(entity.getModuleId(), entity.getEnCode(), null)) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            if (name.contains(entity.getEnCode())) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            name.add(entity.getEnCode());
        }
        moduleFormService.create(entitys);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新表单权限状态
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "更新表单权限状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object>upState(@PathVariable("id") String id) {
        ModuleFormEntity entity = moduleFormService.getInfo(id);
        entity.setEnabledMark("1".equals(String.valueOf(entity.getEnabledMark())) ? 0 : 1);
        boolean flag = moduleFormService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

}
