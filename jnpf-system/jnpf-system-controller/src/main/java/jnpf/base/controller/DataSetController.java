package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.DataSetEntity;
import jnpf.base.model.dataset.*;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DataSetService;
import jnpf.base.util.dataset.DataSetConstant;
import jnpf.base.util.dataset.DataSetSwapUtil;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.DbSensitiveConstant;
import jnpf.constant.MsgCode;
import jnpf.model.SystemParamModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.ParameterUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据集控制器
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/7 10:09:18
 */
@Tag(name = "数据集", description = "DataSet")
@RestController
@RequestMapping("/api/system/DataSet")
@RequiredArgsConstructor
public class DataSetController {


    private final DataSetService dataSetService;
    
    private final UserService userApi;
    
    private final DataSetSwapUtil dataSetSwapUtil;
    
    private final DataInterfaceService dataInterfaceService;

    @Operation(summary = "列表")
    @SaCheckPermission(value = {"onlineDev.printDev", "onlineDev.report"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<List<SumTree<TableTreeModel>>> list(DataSetPagination pagination) throws Exception {
        List<DataSetEntity> list = dataSetService.getList(pagination);
        List<SumTree<TableTreeModel>> res = new ArrayList<>();
        for (DataSetEntity item : list) {
            if (StringUtil.isNotEmpty(ParameterUtil.checkContainsSensitive(item.getDataConfigJson(), DbSensitiveConstant.PRINT_SENSITIVE))) {
                return ActionResult.fail(MsgCode.SYS047.get());
            }
            SumTree<TableTreeModel> printTableFields = dataSetService.getTabFieldStruct(item);
            res.add(printTableFields);
        }
        return ActionResult.success(res);
    }

    @Operation(summary = "获取字段")
    @SaCheckPermission(value = {"onlineDev.printDev", "onlineDev.report"}, mode = SaMode.OR)
    @PostMapping("/fields")
    public ActionResult<List<SumTree<TableTreeModel>>> getOneFields(@RequestBody DataSetForm form) throws Exception {
        DataSetEntity item = JsonUtil.getJsonToBean(form, DataSetEntity.class);
        SumTree<TableTreeModel> printTableFields = dataSetService.getTabFieldStruct(item);
        return ActionResult.success(printTableFields.getChildren());
    }

    @Operation(summary = "获取数据")
    @PostMapping("/Data")
    public ActionResult<Object> getData(@RequestBody DataSetQuery query) {
        String moduleId = query.getModuleId();
        StpUtil.checkPermissionOr(moduleId, "onlineDev.report");
        Map<String, Object> dataMapOrList = dataSetService.getDataList(query);
        String id = query.getId();
        String convertConfig = query.getConvertConfig();
        Map<String, List<Map<String, Object>>> resultData = new HashMap<>();
        dataSetSwapUtil.swapData(id, convertConfig, dataMapOrList);
        for (Map.Entry<String, Object> entry : dataMapOrList.entrySet()) {
            String key = entry.getKey();
            if (dataMapOrList.get(key) instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) dataMapOrList.get(key);
                resultData.put(key, list);
            }
        }

        return ActionResult.success(resultData);
    }

    @Operation(summary = "参数转换")
    @PostMapping("/parameterData")
    public ActionResult<Object> getParameterData(@RequestBody DataSetQuery query) {
        String moduleId = query.getModuleId();
        StpUtil.checkPermissionOr(moduleId, "onlineDev.report");
        Map<String, Object> queryMap = query.getMap() != null ? query.getMap() : new HashMap<>();
        List<FieLdsModel> queryList = StringUtil.isNotEmpty(query.getQueryList()) ? JsonUtil.getJsonToList(query.getQueryList(), FieLdsModel.class) : new ArrayList<>();
        Map<String, Map<String, Object>> dataMap = new HashMap<>();
        Map<String, List<String>> paramMap = ImmutableMap.of(
                DataSetConstant.KEY_USER, ImmutableList.of(DataInterfaceVarConst.USER, DataInterfaceVarConst.USERANDSUB, DataInterfaceVarConst.USERANDPROGENY),
                DataSetConstant.KEY_ORG, ImmutableList.of(DataInterfaceVarConst.ORG, DataInterfaceVarConst.ORGANDSUB, DataInterfaceVarConst.ORGANIZEANDPROGENY),
                DataSetConstant.KEY_POS, ImmutableList.of(DataInterfaceVarConst.POSITIONID, DataInterfaceVarConst.POSITIONANDSUB, DataInterfaceVarConst.POSITIONANDPROGENY)
        );
        Map<String, String> paramSystemMap = userApi.getSystemFieldValue(new SystemParamModel());
        List<DataSetSwapModel> convertList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            List<String> list = paramMap.get(key) != null ? paramMap.get(key) : new ArrayList<>();
            for (String id : list) {
                DataSetSwapModel swapModel = new DataSetSwapModel();
                swapModel.setType(key);
                swapModel.setField(id + "." + id);
                swapModel.setConfig(new DataSetConfig());
                convertList.add(swapModel);
                Map<String, Object> systemMap = new HashMap<>();
                String systemValue = paramSystemMap.get(id);
                try {
                    List<String> value = JsonUtil.getJsonToList(systemValue, String.class);
                    systemMap.put(id, !value.isEmpty() ? value : "");
                } catch (Exception e) {
                    systemMap.put(id, systemValue);
                }
                dataMap.put(id, systemMap);
            }

        }
        for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
            String key = entry.getKey();
            String vModel = key.replace("-", ".");
            String[] name = key.split("-");
            FieLdsModel fieLdsModel = queryList.stream().filter(t -> Objects.equals(t.getVModel(), vModel)).findFirst().orElse(null);
            if (fieLdsModel != null) {
                //转换配置
                ConfigModel config = fieLdsModel.getConfig();
                String type = fieLdsModel.getType();
                DataSetConfig dataSetConfig = new DataSetConfig();
                dataSetConfig.setDataType(config.getDataType());
                dataSetConfig.setOptions(JsonUtil.getJsonToList(config.getOptions(), DataSetOptions.class));
                dataSetConfig.setDictionaryType(config.getDictionaryType());
                dataSetConfig.setPropsValue(config.getPropsValue());
                dataSetConfig.setPropsLabel(config.getPropsLabel());
                dataSetConfig.setPropsUrl(config.getPropsUrl());
                dataSetConfig.setFormat(config.getFormat());
                Object value = queryMap.get(key);
                switch (type) {
                    case JnpfKeyConsts.NUM_INPUT:
                        type = DataSetConstant.KEY_NUMBER;
                        break;
                    case JnpfKeyConsts.COMSELECT:
                        type = DataSetConstant.KEY_ORG;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;
                    case JnpfKeyConsts.DEPSELECT:
                        type = DataSetConstant.KEY_DEP;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;
                    case JnpfKeyConsts.ROLESELECT:
                        type = DataSetConstant.KEY_ROLE;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;
                    case JnpfKeyConsts.POSSELECT:
                        type = DataSetConstant.KEY_POS;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;
                    case JnpfKeyConsts.USERSELECT:
                        type = DataSetConstant.KEY_USERS;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;
                    case JnpfKeyConsts.GROUPSELECT:
                        type = DataSetConstant.KEY_GROUP;
                        if (value instanceof List) {
                            value = JsonUtil.getObjectToString(value);
                        }
                        break;

                    default:
                        break;
                }

                DataSetSwapModel swapModel = new DataSetSwapModel();
                swapModel.setType(type);
                swapModel.setField(fieLdsModel.getVModel());
                swapModel.setConfig(dataSetConfig);
                convertList.add(swapModel);
                //数据
                String model = name[0];
                Map<String, Object> map = dataMap.get(model) != null ? dataMap.get(model) : new HashMap<>();
                map.put(name[1], value);
                dataMap.put(model, map);
            }
        }
        String convertConfig = JsonUtil.getObjectToString(convertList);
        Map<String, Object> dataMapOrList = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> dataList = new ArrayList<>();
            dataList.add(dataMap.get(key));
            dataMapOrList.put(key, dataList);
        }
        dataSetSwapUtil.swapData(RandomUtil.uuId(), convertConfig, dataMapOrList);
        return ActionResult.success(dataMap);
    }

    @Operation(summary = "列表")
    @SaCheckPermission(value = {"onlineDev.printDev", "onlineDev.report"}, mode = SaMode.OR)
    @GetMapping("/getList")
    public ActionResult<List<DataSetInfo>> getList(DataSetPagination pagination) {
        List<DataSetEntity> list = dataSetService.getList(pagination);
        List<DataSetInfo> dataSetInfoList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(list)) {
            for (DataSetEntity item : list) {
                DataSetInfo bean = JsonUtil.getJsonToBean(item, DataSetInfo.class);
                try {
                    SumTree<TableTreeModel> printTableFields = dataSetService.getTabFieldStruct(item);
                    bean.setChildren(printTableFields.getChildren());
                    dataSetInfoList.add(bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (Objects.equals(item.getType(), 3)) {
                    DataInterfaceEntity info = dataInterfaceService.getInfo(item.getInterfaceId());
                    if (info != null) {
                        bean.setTreePropsName(info.getFullName());
                    }
                }
            }
        }
        return ActionResult.success(dataSetInfoList);
    }

    @Operation(summary = "批量保存")
    @Parameter(name = "form", description = "数据集表单")
    @SaCheckPermission(value = {"onlineDev.printDev", "onlineDev.report"}, mode = SaMode.OR)
    @PostMapping("/save")
    public ActionResult<Object> saveList(@RequestBody DataForm form) {
        List<DataSetInfo> list = form.getList() != null ? form.getList() : new ArrayList<>();
        dataSetService.create(JsonUtil.getJsonToList(list, DataSetForm.class), form.getObjectType(), form.getObjectId());
        return ActionResult.success(MsgCode.SU002.get());
    }

    @Operation(summary = "数据预览")
    @SaCheckPermission(value = {"onlineDev.printDev", "onlineDev.report"}, mode = SaMode.OR)
    @PostMapping("/getPreviewData")
    public ActionResult<DataSetViewInfo> getPreviewData(@RequestBody DataSetForm dataSetForm) {
        if (Objects.equals(dataSetForm.getType(), 3)) {
            DataSetViewInfo info = dataSetService.getPreviewDataInterface(dataSetForm);
            return ActionResult.success(info);
        }
        DataSetViewInfo info = dataSetService.getPreviewData(dataSetForm);
        return ActionResult.success(info);
    }
}
