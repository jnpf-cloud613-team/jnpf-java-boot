package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.ModuleDataAuthorizeEntity;
import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.moduledataauthorize.DataAuthorizeCrForm;
import jnpf.base.model.moduledataauthorize.DataAuthorizeInfoVO;
import jnpf.base.model.moduledataauthorize.DataAuthorizeListVO;
import jnpf.base.model.moduledataauthorize.DataAuthorizeUpForm;
import jnpf.base.service.ModuleDataAuthorizeSchemeService;
import jnpf.base.service.ModuleDataAuthorizeService;
import jnpf.base.service.ModuleService;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.emnus.SearchMethodEnum;
import jnpf.exception.DataException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.*;
import jnpf.permission.model.authorize.AuthorizeConditionEnum;
import jnpf.permission.model.authorize.ConditionModel;
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
import java.util.StringJoiner;


/**
 * 数据权限配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据权限字段管理", description = "ModuleDataAuthorize")
@RestController
@RequestMapping("/api/system/ModuleDataAuthorize")
@RequiredArgsConstructor
public class ModuleDataAuthorizeController extends SuperController<ModuleDataAuthorizeService, ModuleDataAuthorizeEntity> {


    public static final String FIELD = "field";
    public static final String FIELD_NAME = "fieldName";

    private final ModuleDataAuthorizeService dataAuthorizeService;

    private final ModuleDataAuthorizeSchemeService dataAuthorizeSchemeService;

    private final ModuleService moduleService;

    /**
     * 获取数据权限配置信息列表
     *
     * @param moduleId 功能主键
     * @return ignore
     */
    @Operation(summary = "获取字段列表")
    @Parameter(name = "moduleId", description = "功能主键", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{moduleId}/List")
    public ActionResult<ListVO<DataAuthorizeListVO>> list(@PathVariable("moduleId") String moduleId) {
        List<ModuleDataAuthorizeEntity> data = dataAuthorizeService.getList(moduleId);
        List<DataAuthorizeListVO> list = JsonUtil.getJsonToList(data, DataAuthorizeListVO.class);
        list.forEach(t -> {
            String conditionSymbol = StringUtil.isNotEmpty(t.getConditionSymbol()) ? t.getConditionSymbol() : "";
            StringJoiner symbolJoiner = new StringJoiner(",");
            String[] symbolSplit = conditionSymbol.split(",");
            for (String id : symbolSplit) {
                SearchMethodEnum itemMethod = SearchMethodEnum.getSearchMethod(id);
                if (itemMethod != null) {
                    symbolJoiner.add(itemMethod.getMessage());
                }
            }
            t.setConditionText(StringUtil.isNotEmpty(t.getConditionText()) ? t.getConditionText() : "");
            StringJoiner textJoiner = new StringJoiner(",");
            String conditionText = StringUtil.isNotEmpty(t.getConditionText()) ? t.getConditionText() : "";
            String[] textSplit = conditionText.split(",");
            for (String id : textSplit) {
                AuthorizeConditionEnum itemMethod = AuthorizeConditionEnum.getByMessage(id);
                if (itemMethod != null) {
                    textJoiner.add(itemMethod.getMessage());
                }
            }
            t.setConditionSymbolName(symbolJoiner.toString());
            t.setConditionName(textJoiner.toString());
            if (StringUtil.isNotEmpty(t.getBindTable())) {
                t.setEnCode(StringUtil.isNotEmpty(t.getEnCode()) ? t.getEnCode().replace(t.getBindTable() + ".", "") : "");
            }
        });

        ListVO<DataAuthorizeListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
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
                    List<FormColumnModel> childList1 = childList.getChildList();
                    for (FormColumnModel formColumnModel : childList1) {
                        FieLdsModel fieLdsModel = formColumnModel.getFieLdsModel();
                        if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                            Map<String, String> map1 = new HashedMap<>();
                            map1.put(FIELD, fieLdsModel.getVModel());
                            map1.put(FIELD_NAME, fieLdsModel.getConfig().getLabel());
                            list.add(map1);
                        }
                    }
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
     * 获取数据权限配置信息
     *
     * @param id 主键值
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "获取数据权限配置信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<DataAuthorizeInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleDataAuthorizeEntity entity = dataAuthorizeService.getInfo(id);
        ModuleEntity moduleEntity = moduleService.getInfo(entity.getModuleId());
        if (moduleEntity != null && StringUtil.isNotEmpty(entity.getBindTable())) {
            entity.setEnCode(StringUtil.isNotEmpty(entity.getEnCode()) ? entity.getEnCode().replace(entity.getBindTable() + ".", "") : "");
        }
        DataAuthorizeInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, DataAuthorizeInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建数据权限配置
     *
     * @param dataAuthorizeCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "新建数据权限配置")
    @Parameter(name = "dataAuthorizeCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid DataAuthorizeCrForm dataAuthorizeCrForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(dataAuthorizeCrForm.getModuleId());
        ModuleDataAuthorizeEntity entity = JsonUtil.getJsonToBean(dataAuthorizeCrForm, ModuleDataAuthorizeEntity.class);
        entity.setPropertyJson(dataAuthorizeCrForm.getChildTableKey());
        if (moduleEntity != null && moduleEntity.getType() == 3 && entity.getFieldRule() != 0 && StringUtil.isNotEmpty(entity.getBindTable())) {
            String enCode = entity.getBindTable() + "." + entity.getEnCode();
            entity.setEnCode(enCode);
        }
        dataAuthorizeService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新数据权限配置
     *
     * @param id                  主键值
     * @param dataAuthorizeUpForm 实体对象
     * @return ignore
     */
    @Operation(summary = "更新数据权限配置")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "dataAuthorizeUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid DataAuthorizeUpForm dataAuthorizeUpForm) {
        ModuleEntity moduleEntity = moduleService.getInfo(dataAuthorizeUpForm.getModuleId());
        ModuleDataAuthorizeEntity entity = JsonUtil.getJsonToBean(dataAuthorizeUpForm, ModuleDataAuthorizeEntity.class);
        if (moduleEntity != null && moduleEntity.getType() == 3 && entity.getFieldRule() == 1 && StringUtil.isNotEmpty(entity.getBindTable())) {
            String enCode = entity.getBindTable() + "." + entity.getEnCode();
            entity.setEnCode(enCode);
        }
        entity.setPropertyJson(dataAuthorizeUpForm.getChildTableKey());
        boolean flag = dataAuthorizeService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除数据权限配置
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除数据权限配置")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        ModuleDataAuthorizeEntity entity = dataAuthorizeService.getInfo(id);
        //菜单id
        String moduleId = entity.getModuleId();
        //该菜单下的数据权限方案
        List<ModuleDataAuthorizeSchemeEntity> list = dataAuthorizeSchemeService.getList(moduleId);

        String schemeName = null;
        for (ModuleDataAuthorizeSchemeEntity schemeEntity : list) {
            List<ConditionModel> conditionModels = JsonUtil.getJsonToList(schemeEntity.getConditionJson(), ConditionModel.class);
            if (conditionModels != null) {
                for (ConditionModel conditionModel : conditionModels) {
                    List<ConditionModel.ConditionItemModel> groups = conditionModel.getGroups();
                    for (ConditionModel.ConditionItemModel conditionItemModel : groups) {
                        if (conditionItemModel.getField().equalsIgnoreCase(entity.getEnCode())) {
                            schemeName = schemeEntity.getFullName();
                            break;
                        }
                    }
                }
            }
        }
        if (StringUtil.isNotEmpty(schemeName)) {
            return ActionResult.fail(MsgCode.SYS020.get(schemeName));
        }
        dataAuthorizeService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

}
