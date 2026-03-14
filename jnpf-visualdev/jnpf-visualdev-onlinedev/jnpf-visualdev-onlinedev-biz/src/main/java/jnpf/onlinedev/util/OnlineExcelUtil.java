package jnpf.onlinedev.util;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.ProvinceEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.online.ExcelImportModel;
import jnpf.base.model.online.ImportFormCheckUniqueModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.ProvinceService;
import jnpf.base.util.FormCheckUtils;
import jnpf.base.util.FormPublicUtils;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.emnus.SysParamEnum;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.OnlineCusCheckModel;
import jnpf.model.visualjson.TemplateJsonModel;
import jnpf.model.visualjson.UploaderTemplateModel;
import jnpf.model.visualjson.config.RegListModel;
import jnpf.onlinedev.model.VisualErrInfo;
import jnpf.onlinedev.model.VisualParamModel;
import jnpf.onlinedev.model.enums.OnlineDataTypeEnum;
import jnpf.onlinedev.model.online.ValidationRes;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineExcelUtil {
    private final DictionaryDataService dictionaryDataApi;
    private final UserService userApi;
    private final PositionService positionApi;
    private final ProvinceService areaApi;
    private final OrganizeService organizeApi;
    private final VisualdevModelDataService visualdevModelDataService;
    private final DataInterfaceService dataInterFaceApi;
    private final DbLinkService dataSourceApi;
    private final UserRelationService userRelationApi;
    private final FormCheckUtils formCheckUtils;
    private final WorkFlowApi workFlowApi;
    private final OnlineSwapDataUtils onlineSwapDataUtils;

    private static final String VALUE_NOTIN_RANGE = "值不在范围内";
    private static final String VALUE_THAN_MAX = "值不能大于最大值";
    private static final String VALUE_ERR = "值不正确";
    private static final String VALUE_LESS_MIN = "值不能小于最小值";

    public ExcelImportModel createExcelData(List<Map<String, Object>> dataList, VisualDevJsonModel visualJsonModel, VisualdevEntity visualdevEntity) throws WorkFlowException {
        ExcelImportModel excelImportModel = new ExcelImportModel();
        Integer primaryKeyPolicy = visualJsonModel.getFormData().getPrimaryKeyPolicy();

        String uploaderTemplateJson = visualJsonModel.getColumnData().getUploaderTemplateJson();
        UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(uploaderTemplateJson, UploaderTemplateModel.class);
        String dataType = uploaderTemplateModel.getDataType();
        ImportFormCheckUniqueModel uniqueModel = new ImportFormCheckUniqueModel();
        uniqueModel.setMain(true);
        uniqueModel.setDbLinkId(visualJsonModel.getDbLinkId());
        uniqueModel.setUpdate(dataType.equals("2"));
        uniqueModel.setPrimaryKeyPolicy(primaryKeyPolicy);
        uniqueModel.setLogicalDelete(visualJsonModel.getFormData().getLogicalDelete());
        uniqueModel.setTableModelList(visualJsonModel.getVisualTables());
        DbLinkEntity linkEntity = dataSourceApi.getInfo(visualJsonModel.getDbLinkId());
        uniqueModel.setLinkEntity(linkEntity);
        //流程表单导入，传流程大id查询小idlist用于过滤数据
        String mainFlowID = null;
        if (StringUtil.isNotEmpty(visualJsonModel.getFlowId())) {
            List<TemplateJsonEntity> flowVersionIds = workFlowApi.getFlowIdsByTemplate(visualJsonModel.getFlowId());
            uniqueModel.setFlowId(visualJsonModel.getFlowId());
            uniqueModel.setFlowIdList(flowVersionIds.stream().map(TemplateJsonEntity::getId).distinct().collect(Collectors.toList()));
            mainFlowID = flowVersionIds.stream().filter(t -> Objects.equals(t.getState(), 1)).findFirst().orElse(new TemplateJsonEntity()).getId();
        }

        //获取缓存
        Map<String, Object> localCache = onlineSwapDataUtils.getlocalCache();
        List<Map<String, Object>> failResult = new ArrayList<>();

        List<VisualdevModelDataInfoVO> dataInfo = new ArrayList<>();
        try {
            for (int i = 0, len = dataList.size(); i < len; i++) {
                Map<String, Object> data = dataList.get(i);
                //导入时默认第一个流程
                data.put(FlowFormConstant.FLOWID, mainFlowID);
                Map<String, Object> resultMap = new HashMap<>(data);
                StringJoiner errInfo = new StringJoiner(",");
                Map<String, Object> errorMap = new HashMap<>(data);

                List<String> errList = this.checkExcelData(visualJsonModel.getFormListModels(), data, localCache, resultMap, errorMap, uniqueModel);

                //业务主键判断--导入新增或者跟新
                if (hasError(visualJsonModel, linkEntity, resultMap, uniqueModel, errorMap, failResult)) continue;

                onlineSwapDataUtils.checkUnique(visualJsonModel.getFormListModels(), data, errList, uniqueModel);

                errList.stream().forEach(t -> {
                    if (StringUtil.isNotEmpty(t)) {
                        errInfo.add(t);
                    }
                });
                if (errInfo.length() > 0) {
                    errorMap.put(KeyConst.ERRORS_INFO, errInfo.toString());
                    failResult.add(errorMap);
                } else {
                    handleData(visualdevEntity, uniqueModel, resultMap, dataInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new WorkFlowException(MsgCode.IMP004.get());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        excelImportModel.setFnum(failResult.size());
        excelImportModel.setSnum(dataList.size() - failResult.size());
        excelImportModel.setResultType(!failResult.isEmpty() ? 1 : 0);
        excelImportModel.setFailResult(failResult);
        excelImportModel.setDataInfoList(dataInfo);
        return excelImportModel;
    }

    //判断业务主键，或者流程是否发起
    private boolean hasError(VisualDevJsonModel visualJsonModel, DbLinkEntity linkEntity, Map<String, Object> resultMap, ImportFormCheckUniqueModel uniqueModel, Map<String, Object> errorMap, List<Map<String, Object>> failResult) throws SQLException {
        VisualErrInfo visualErrInfo;
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            visualErrInfo = formCheckUtils.checkBusinessKey(visualJsonModel.getFormListModels(), resultMap,
                    visualJsonModel.getVisualTables(), visualJsonModel.getFormData(), null);
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        boolean hasErr = false;
        if (uniqueModel.isUpdate()) {
            if (ObjectUtil.isNotEmpty(visualErrInfo) && StringUtil.isNotEmpty(visualErrInfo.getId())) {
                uniqueModel.setId(visualErrInfo.getId());
                //判断流程是否已发起
                if (StringUtil.isNotEmpty(visualErrInfo.getFlowTaskId())) {
                    String finalTaskId = visualErrInfo.getFlowTaskId();
                    List<String> flowIdList = new ArrayList<>();
                    flowIdList.add(finalTaskId);
                    List<TaskEntity> tasks = workFlowApi.getInfosSubmit(flowIdList.toArray(new String[]{}), TaskEntity::getStatus, TaskEntity::getId);
                    if (!tasks.isEmpty()) {
                        boolean errorMsg = tasks.stream().filter(t -> Objects.equals(t.getStatus(), 0)).count() == 0;
                        String msg = "已发起流程，导入失败";
                        if (errorMsg) {
                            errorMap.put(KeyConst.ERRORS_INFO, msg);
                            failResult.add(errorMap);
                            hasErr = true;
                        }
                    }
                }
            }
        } else {
            if (ObjectUtil.isNotEmpty(visualErrInfo) && StringUtil.isNotEmpty(visualErrInfo.getErrMsg())) {
                errorMap.put(KeyConst.ERRORS_INFO, visualErrInfo.getErrMsg());
                failResult.add(errorMap);
                hasErr = true;
            }
        }
        return hasErr;
    }

    private void handleData(VisualdevEntity visualdevEntity, ImportFormCheckUniqueModel uniqueModel, Map<String, Object> resultMap, List<VisualdevModelDataInfoVO> dataInfo) throws WorkFlowException {
        VisualdevModelDataInfoVO infoVO = new VisualdevModelDataInfoVO();

        if (StringUtil.isNotEmpty(uniqueModel.getId())) {
            visualdevModelDataService.visualUpdate(
                    VisualParamModel.builder().visualdevEntity(visualdevEntity).data(resultMap).id(uniqueModel.getId()).isUpload(true).build());
            infoVO.setId(uniqueModel.getId());
            infoVO.setIntegrateId(uniqueModel.getId());
            infoVO.setData(JsonUtil.getObjectToString(resultMap));
        } else {
            DataModel dataModel = visualdevModelDataService.visualCreate(
                    VisualParamModel.builder().visualdevEntity(visualdevEntity).data(resultMap).isUpload(true).build());
            infoVO.setId(dataModel.getMainId());
            infoVO.setData(JsonUtil.getObjectToString(resultMap));
        }
        dataInfo.add(infoVO);
    }


    public List<String> checkExcelData(List<FieLdsModel> modelList, Map<String, Object> data, Map<String, Object> localCache, Map<String, Object> insMap,
                                       Map<String, Object> errorMap, ImportFormCheckUniqueModel uniqueModel) {
        log.info("错误信息：" + JsonUtil.getObjectToString(errorMap));
        List<String> errList = new ArrayList<>(modelList.size());
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userApi.getInfo(userInfo.getUserId());

        //读取系统控件 所需编码 id
        Map<String, Object> depMap = (Map<String, Object>) localCache.get("_dep_map");
        Map<String, Object> comMap = (Map<String, Object>) localCache.get("_com_map");
        Map<String, Object> posMap = (Map<String, Object>) localCache.get("_pos_map");
        //key value对调
        posMap = posMap.entrySet().stream().collect(Collectors.toMap(t -> String.valueOf(t.getValue()), Map.Entry::getKey, (e, r) -> r));
        Map<String, Object> userMap = (Map<String, Object>) localCache.get("_user_map");
        Map<String, Object> roleMap = (Map<String, Object>) localCache.get("_role_map");
        Map<String, Object> groupMap = (Map<String, Object>) localCache.get("_group_map");
        Map<String, Object> allOrgsTreeName = (Map<String, Object>) localCache.get("_com_tree_map");

        //系统参数，及xxxx--当前组织数据缓存
        Map<String, List<String>> allTypeMap = new HashMap<>();
        allTypeMap = localCache.containsKey(KeyConst.ALL_TYPE_MAP) ? (Map<String, List<String>>) localCache.get(KeyConst.ALL_TYPE_MAP) : allTypeMap;

        //异常数据
        for (int i = 0; i < modelList.size(); i++) {
            FieLdsModel swapDataVo = modelList.get(i);
            errList.add(i, "");
            try {
                String jnpfKey = swapDataVo.getConfig().getJnpfKey();
                Object valueO = data.get(swapDataVo.getVModel());
                String label = swapDataVo.getConfig().getLabel();
                String value = String.valueOf(valueO);
                boolean required = swapDataVo.getConfig().isRequired();
                if (JnpfKeyConsts.getUploadMaybeNull().contains(jnpfKey) ||
                        valueO == null || "null".equals(valueO) || StringUtil.isEmpty(String.valueOf(valueO))) {
                    //不支持导入控件
                    if (JnpfKeyConsts.getUploadMaybeNull().contains(jnpfKey)) {
                        insMap.put(swapDataVo.getVModel(), null);
                    }
                    //是否必填
                    if (required && (valueO == null || "null".equals(valueO) || StringUtil.isEmpty(String.valueOf(valueO)))) {
                        errList.set(i, label + "不能为空");
                    }
                    continue;
                }

                boolean multiple = swapDataVo.getMultiple();
                if (JnpfKeyConsts.CHECKBOX.equals(jnpfKey)) {
                    multiple = true;
                }

                boolean valueMul = value.contains(",");
                value = value.trim();
                List<String> valueList = valueMul ? Arrays.asList(value.split(",")) : new ArrayList<>();
                if (!valueMul) {
                    valueList.add(value);
                }
                String ableIds = swapDataVo.getAbleIds() != null ? swapDataVo.getAbleIds() : "[]";
                List<String> ableList = JsonUtil.getJsonToList(ableIds, String.class);
                //处理自定义范围：系统参数及、选中组织、选中组织及子组织、选中组织及子孙组织、
                OnlineCusCheckModel cusCheckModel = getSystemParamIds(ableList, allTypeMap, jnpfKey);
                cusCheckModel.setControlType(jnpfKey);

                List<String> dataList;
                ValidationRes valRes;
                switch (jnpfKey) {
                    case JnpfKeyConsts.NUM_INPUT:
                        valRes = validatInput(value, swapDataVo);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        }
                        break;
                    case JnpfKeyConsts.COMSELECT:
                        valRes = validatComSelect(value, swapDataVo, allOrgsTreeName);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        } else {
                            insMap.put(swapDataVo.getVModel(), valRes.getValue());
                            if (KeyConst.CUSTOM.equals(swapDataVo.getSelectType())) {
                                cusCheckModel.setDataList(valRes.getList());
                                checkCustomControl(cusCheckModel, errList, i, label);
                            }
                        }
                        break;
                    case JnpfKeyConsts.DEPSELECT:
                        dataList = checkOptionsControl(swapDataVo, insMap, depMap, valueList, errList, i);
                        if (dataList.size() == valueList.size() && swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                            cusCheckModel.setDataList(dataList);
                            checkCustomControl(cusCheckModel, errList, i, label);
                        }
                        break;
                    case JnpfKeyConsts.POSSELECT:
                        dataList = checkOptionsControl(swapDataVo, insMap, posMap, valueList, errList, i);
                        if (dataList.size() == valueList.size() && swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                            cusCheckModel.setDataList(dataList);
                            checkCustomControl(cusCheckModel, errList, i, label);
                        }
                        break;
                    case JnpfKeyConsts.USERSELECT:
                        dataList = checkOptionsControl(swapDataVo, insMap, userMap, valueList, errList, i);
                        if (dataList.size() == valueList.size() && swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                            cusCheckModel.setDataList(dataList);
                            checkCustomControl(cusCheckModel, errList, i, label);
                        }
                        break;
                    case JnpfKeyConsts.CUSTOMUSERSELECT:
                        boolean cusUserErrorHapen = false;
                        if (!multiple && valueList.size() > 1) {
                            //非多选填入多选值
                            cusUserErrorHapen = true;
                            errList.set(i, label + VALUE_ERR);
                        }
                        if (!cusUserErrorHapen) {
                            boolean cusUserErrorHapen1 = false;
                            List<String> cusUserList = new ArrayList<>();
                            for (String va : valueList) {
                                if (StringUtil.isEmpty(va)) {
                                    cusUserErrorHapen1 = true;
                                    break;
                                }
                                String type = null;
                                String id = null;
                                if (groupMap.get(va) != null) {
                                    type = "group";
                                    id = groupMap.get(va).toString();
                                } else if (roleMap.get(va) != null) {
                                    type = "role";
                                    id = roleMap.get(va).toString();
                                } else if (depMap.get(va) != null) {
                                    type = "department";
                                    id = depMap.get(va).toString();
                                } else if (comMap.get(va) != null) {
                                    type = "company";
                                    id = comMap.get(va).toString();
                                } else if (posMap.get(va) != null) {
                                    type = "position";
                                    id = posMap.get(va).toString();
                                } else if (userMap.get(va) != null) {
                                    type = "user";
                                    id = userMap.get(va).toString();
                                }
                                if (type == null && id == null) {
                                    cusUserErrorHapen1 = true;
                                } else {
                                    String lastCusId = id + "--" + type;
                                    cusUserList.add(lastCusId);
                                }
                            }
                            if (cusUserErrorHapen1) {
                                errList.set(i, label + VALUE_ERR);
                            } else {
                                insMap.put(swapDataVo.getVModel(), !multiple ? cusUserList.get(0) : JsonUtil.getObjectToString(cusUserList));
                                if (swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                                    cusCheckModel.setDataList(cusUserList);
                                    checkCustomControl(cusCheckModel, errList, i, label);
                                }
                            }
                        }
                        break;
                    case JnpfKeyConsts.ROLESELECT:
                        dataList = checkOptionsControl(swapDataVo, insMap, roleMap, valueList, errList, i);
                        if (dataList.size() == valueList.size() && swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                            cusCheckModel.setDataList(dataList);
                            checkCustomControl(cusCheckModel, errList, i, label);
                        }
                        break;
                    case JnpfKeyConsts.GROUPSELECT:
                        dataList = checkOptionsControl(swapDataVo, insMap, groupMap, valueList, errList, i);
                        if (dataList.size() == valueList.size() && swapDataVo.getSelectType().equals(KeyConst.CUSTOM)) {
                            cusCheckModel.setDataList(dataList);
                            checkCustomControl(cusCheckModel, errList, i, label);
                        }
                        break;
                    case JnpfKeyConsts.ADDRESS:
                        valRes = validateValue(value, swapDataVo, areaApi);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        } else {
                            insMap.put(swapDataVo.getVModel(), valRes.getValue());
                        }
                        break;
                    /**
                     * 系统控件
                     */
                    case JnpfKeyConsts.CURRORGANIZE:
                        List<UserRelationEntity> orgRelations = userRelationApi.getListByUserId(userInfo.getUserId(), PermissionConst.ORGANIZE);
                        insMap.put(swapDataVo.getVModel(), !orgRelations.isEmpty() ? orgRelations.get(0).getObjectId() : null);
                        break;
                    case JnpfKeyConsts.CURRDEPT:
                        List<UserRelationEntity> depUserRelations = userRelationApi.getListByUserId(userInfo.getUserId(), PermissionConst.DEPARTMENT);
                        insMap.put(swapDataVo.getVModel(), !depUserRelations.isEmpty() ? depUserRelations.get(0).getObjectId() : null);
                        break;
                    case JnpfKeyConsts.CREATEUSER:
                        insMap.put(swapDataVo.getVModel(), userEntity.getId());
                        break;
                    case JnpfKeyConsts.CREATETIME:
                        insMap.put(swapDataVo.getVModel(), DateUtil.getNow());
                        break;
                    case JnpfKeyConsts.MODIFYTIME:
                        break;
                    case JnpfKeyConsts.MODIFYUSER:
                        break;
                    case JnpfKeyConsts.CURRPOSITION:
                        insMap.put(swapDataVo.getVModel(), userEntity.getPositionId());
                        break;
                    /**
                     * 基础控件
                     */
                    case JnpfKeyConsts.SWITCH:
                        String activeTxt = swapDataVo.getActiveTxt();
                        String inactiveTxt = swapDataVo.getInactiveTxt();
                        if (value.equals(activeTxt)) {
                            insMap.put(swapDataVo.getVModel(), 1);
                        } else if (value.equals(inactiveTxt)) {
                            insMap.put(swapDataVo.getVModel(), 0);
                        } else {
                            errList.set(i, label + VALUE_ERR);
                        }
                        break;
                    case JnpfKeyConsts.RATE:
                        valRes = validatRate(value, swapDataVo);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        } else {
                            insMap.put(swapDataVo.getVModel(), valRes.getValue());
                        }
                        break;
                    case JnpfKeyConsts.SLIDER:
                        valRes = validatSlider(value, swapDataVo);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        } else {
                            insMap.put(swapDataVo.getVModel(), valRes.getValue());
                        }
                        break;
                    case JnpfKeyConsts.TEXTAREA:
                        if (StringUtil.isNotEmpty(swapDataVo.getMaxlength()) && value.length() > Integer.valueOf(swapDataVo.getMaxlength())) {
                            errList.set(i, label + "值超出最多输入字符限制");
                            break;
                        }
                        break;
                    case JnpfKeyConsts.COM_INPUT:
                        //只验证子表。主副表在外面做唯一验证
                        boolean unique = swapDataVo.getConfig().getUnique();
                        if (!uniqueModel.isMain()) {
                            if (StringUtil.isNotEmpty(swapDataVo.getMaxlength()) && value.length() > Integer.valueOf(swapDataVo.getMaxlength())) {
                                errList.set(i, label + "值超出最多输入字符限制");
                                break;
                            }
                            boolean comInputError = false;
                            if (unique && insMap.get("child_table_list") != null) {
                                //子表重复只判断同一个表单
                                List<Map<String, Object>> childList = uniqueModel.getChildMap();
                                String finalValue = value;
                                for (int j = 0; j < childList.size(); j++) {
                                    Map<String, Object> t = childList.get(j);
                                    if (finalValue.equals(t.get(swapDataVo.getVModel()))) {
                                        comInputError = true;
                                        errList.set(i, label + "值已存在");
                                        uniqueModel.setChildIndex(j);
                                        break;
                                    }
                                }
                            }
                            //验证正则
                            if (StringUtil.isNotEmpty(swapDataVo.getConfig().getRegList())) {
                                List<RegListModel> regList = JsonUtil.getJsonToList(swapDataVo.getConfig().getRegList(), RegListModel.class);
                                for (RegListModel regListModel : regList) {
                                    //处理正则格式
                                    String reg = regListModel.getPattern();
                                    if (reg.startsWith("/") && reg.endsWith("/")) {
                                        reg = reg.substring(1, reg.length() - 1);
                                    }
                                    boolean matches = value.matches(reg);
                                    if (!matches) {
                                        comInputError = true;
                                        errList.set(i, label + regListModel.getMessage());
                                    }
                                }
                            }
                            if (!comInputError) {
                                insMap.put(swapDataVo.getVModel(), value);
                            }
                        }
                        break;
                    case JnpfKeyConsts.TIME:
                        valRes = validatTime(value, swapDataVo, data);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        }
                        break;
                    case JnpfKeyConsts.DATE:
                    case JnpfKeyConsts.DATE_CALCULATE:
                        valRes = validatDate(value, swapDataVo, data);
                        if (!valRes.isSuccess()) {
                            errList.set(i, valRes.getMsg());
                        } else {
                            insMap.put(swapDataVo.getVModel(), valRes.getValue());
                        }
                        break;
                    /**
                     * 子表
                     */
                    case JnpfKeyConsts.CHILD_TABLE:
                        StringJoiner childJoiner = new StringJoiner(",");
                        List<Map<String, Object>> childAllData = (List<Map<String, Object>>) data.get(swapDataVo.getVModel());
                        List<Map<String, Object>> childTable = new ArrayList<>(childAllData.size());
                        uniqueModel.setChildMap(new ArrayList<>());
                        //子表条数限制
                        if (Boolean.TRUE.equals(swapDataVo.getIsNumLimit()) && childAllData.size() > swapDataVo.getNumLimit()) {
                            errList.set(i, MsgCode.VS033.get(swapDataVo.getConfig().getLabel()));
                            break;
                        }
                        for (int childI = 0, childLen = childAllData.size(); childI < childLen; childI++) {
                            Map<String, Object> item = childAllData.get(childI);
                            Map<String, Object> childMap = new HashMap<>(item);
                            childMap.put("mainAndMast", data);
                            childMap.put("child_table_list", data.get(swapDataVo.getVModel()));
                            Map<String, Object> childTableMap = new HashMap<>(childMap);
                            Map<String, Object> childerrorMap = new HashMap<>(childMap);
                            uniqueModel.setMain(false);
                            StringJoiner childJoiner1 = new StringJoiner(",");
                            List<String> childErrList = this.checkExcelData(swapDataVo.getConfig().getChildren(), childMap, localCache, childTableMap, childerrorMap, uniqueModel);
                            childErrList.stream().forEach(t -> {
                                if (StringUtil.isNotEmpty(t)) {
                                    childJoiner1.add(t);
                                }
                            });
                            List<Map<String, Object>> childList = uniqueModel.getChildMap();
                            if (childJoiner1.length() > 0) {
                                if (uniqueModel.isUpdate() && uniqueModel.getChildIndex() != null
                                        && childJoiner1.toString().split(",").length == 1 && childJoiner1.toString().contains("值已存在")) {
                                    childList.set(uniqueModel.getChildIndex(), childTableMap);
                                } else {
                                    childJoiner.add(childJoiner1.toString());
                                }
                            } else {
                                childList.add(childTableMap);
                                childTable.add(childTableMap);
                            }
                        }
                        if (childJoiner.length() == 0) {
                            insMap.put(swapDataVo.getVModel(), childTable);
                        } else {
                            errList.set(i, childJoiner.toString());
                        }
                        uniqueModel.setMain(true);
                        break;
                    default:
                        break;

                }
                /**
                 * 数据接口
                 */
                checkDataType(localCache, insMap, swapDataVo, valueList, errList, i);
            } catch (Exception e) {
                e.printStackTrace();
                errList.set(i, e.getMessage());
            }
        }
        localCache.put(KeyConst.ALL_TYPE_MAP, allTypeMap);

        return errList;
    }

    //有数据类型的验证
    private void checkDataType(Map<String, Object> localCache, Map<String, Object> insMap, FieLdsModel swapDataVo,
                               List<String> valueList, List<String> errList, int i) {
        String dataType = swapDataVo.getConfig().getDataType();

        if (dataType != null) {
            List<Map<String, Object>> options = new ArrayList<>();
            String dataLabel = swapDataVo.getProps().getLabel() != null ? swapDataVo.getProps().getLabel() : "";
            String dataValue = swapDataVo.getProps().getValue() != null ? swapDataVo.getProps().getValue() : "";
            String children = swapDataVo.getProps().getChildren() != null ? swapDataVo.getProps().getChildren() : "";

            String localCacheKey;
            Map<String, Object> dataInterfaceMap = new HashMap<>();
            //静态数据
            if (dataType.equals(OnlineDataTypeEnum.STATIC.getType())) {
                localCacheKey = String.format("%s-%s", swapDataVo.getConfig().getRelationTable() + swapDataVo.getVModel(), OnlineDataTypeEnum.STATIC.getType());
                if (!localCache.containsKey(localCacheKey)) {
                    if (swapDataVo.getOptions() != null) {
                        options = JsonUtil.getJsonToListMap(swapDataVo.getOptions());
                        String children1 = swapDataVo.getProps().getChildren();
                        JSONArray staticData = JsonUtil.getListToJsonArray(options);
                        OnlineSwapDataUtils.getOptions(dataLabel, dataValue, children1, staticData, options);
                    } else {
                        options = JsonUtil.getJsonToListMap(swapDataVo.getOptions());
                    }
                    Map<String, Object> finalDataInterfaceMap = new HashMap<>(16);
                    String finalDataLabel = dataLabel;
                    String finalDataValue = dataValue;
                    options.stream().forEach(o -> finalDataInterfaceMap.put(String.valueOf(o.get(finalDataLabel)), o.get(finalDataValue)));
                    localCache.put(localCacheKey, finalDataInterfaceMap);
                    dataInterfaceMap = finalDataInterfaceMap;
                } else {
                    dataInterfaceMap = (Map<String, Object>) localCache.get(localCacheKey);
                }

                checkFormDataInteface(swapDataVo, insMap, dataInterfaceMap, valueList, errList, i);
                //远端数据
            } else if (dataType.equals(OnlineDataTypeEnum.DYNAMIC.getType())) {
                localCacheKey = String.format("%s-%s-%s-%s", OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getConfig().getPropsUrl(), dataValue, dataLabel);
                if (!localCache.containsKey(localCacheKey)) {
                    List<TemplateJsonModel> templateJson = swapDataVo.getConfig().getTemplateJson();
                    Map<String, String> param = new HashMap<>();
                    for (TemplateJsonModel tm : templateJson) {
                        param.put(tm.getField(), tm.getDefaultValue());
                    }
                    ActionResult<Object> actionResult = dataInterFaceApi.infoToId(swapDataVo.getConfig().getPropsUrl(), null, param);
                    if (actionResult != null && actionResult.getData() != null) {
                        List<Map<String, Object>> dycDataList = new ArrayList<>();
                        if (actionResult.getData() instanceof List) {
                            dycDataList = (List<Map<String, Object>>) actionResult.getData();
                        }
                        JSONArray dataAll = JsonUtil.getListToJsonArray(dycDataList);
                        OnlineSwapDataUtils.treeToList(dataLabel, dataValue, children, dataAll, options);
                        Map<String, Object> finalDataInterfaceMap1 = new HashMap<>(16);
                        String finalDataLabel2 = dataLabel;
                        String finalDataValue1 = dataValue;
                        options.stream().forEach(o -> finalDataInterfaceMap1.put(String.valueOf(o.get(finalDataLabel2)), String.valueOf(o.get(finalDataValue1))));
                        dataInterfaceMap = finalDataInterfaceMap1;
                        localCache.put(localCacheKey, dataInterfaceMap);
                    }
                } else {
                    dataInterfaceMap = (Map<String, Object>) localCache.get(localCacheKey);
                }
                checkFormDataInteface(swapDataVo, insMap, dataInterfaceMap, valueList, errList, i);
                //数据字典
            } else if (dataType.equals(OnlineDataTypeEnum.DICTIONARY.getType())) {
                localCacheKey = String.format("%s-%s", OnlineDataTypeEnum.DICTIONARY.getType(), swapDataVo.getConfig().getDictionaryType());
                dataLabel = swapDataVo.getProps().getLabel();
                dataValue = swapDataVo.getProps().getValue();
                if (!localCache.containsKey(localCacheKey)) {
                    List<DictionaryDataEntity> list = dictionaryDataApi.getDicList(swapDataVo.getConfig().getDictionaryType());
                    options = list.stream().map(dic -> {
                        Map<String, Object> dictionaryMap = new HashMap<>(16);
                        dictionaryMap.put("id", dic.getId());
                        dictionaryMap.put("enCode", dic.getEnCode());
                        dictionaryMap.put("fullName", dic.getFullName());
                        return dictionaryMap;
                    }).collect(Collectors.toList());
                    localCache.put(localCacheKey, options);
                } else {
                    options = (List<Map<String, Object>>) localCache.get(localCacheKey);
                }
                Map<String, Object> finalDataInterfaceMap1 = new HashMap<>(16);
                String finalDataLabel3 = dataLabel;
                String finalDataValue3 = dataValue;
                options.stream().forEach(o -> finalDataInterfaceMap1.put(String.valueOf(o.get(finalDataLabel3)), o.get(finalDataValue3)));

                checkFormDataInteface(swapDataVo, insMap, finalDataInterfaceMap1, valueList, errList, i);
            }
        }
    }

    //地区控件验证
    private static ValidationRes validateValue(String value, FieLdsModel swapDataVo, ProvinceService areaApi) {
        String label = swapDataVo.getConfig().getLabel();
        boolean multiple = swapDataVo.getMultiple();
        List<String> valueList = Arrays.asList(value.split(","));
        List<String[]> addressArray = new ArrayList<>();
        List<String> addressIdList = new ArrayList<>();

        if (!multiple && valueList.size() > 1) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        for (String addressValue : valueList) {
            // 验证单个地址值
            if (StringUtil.isEmpty(addressValue)) {
                return ValidationRes.failure(label + VALUE_ERR);
            }

            // 分割地址并验证格式
            String[] addressParts = addressValue.split("/");
            if (addressParts.length != swapDataVo.getLevel() + 1) {
                return ValidationRes.failure(label + "值的格式不正确");
            }

            // 处理地址层级
            List<String> addressIds = processAddressHierarchy(addressParts, areaApi);
            if (addressIds.isEmpty()) {
                return ValidationRes.failure(label + VALUE_ERR);
            }

            addressIdList.addAll(addressIds);
            addressArray.add(addressIds.toArray(new String[0]));
        }

        // 根据是否多选返回相应格式
        String formattedValue = multiple ? JsonUtil.getObjectToString(addressArray)
                : JsonUtil.getObjectToString(addressIdList);
        return ValidationRes.success(formattedValue);
    }

    private static List<String> processAddressHierarchy(String[] addressParts, ProvinceService areaApi) {
        List<String> addressIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        for (String addressPart : addressParts) {
            ProvinceEntity province = areaApi.getInfo(addressPart, parentIds);
            if (province == null) {
                return Collections.emptyList();
            }
            addressIds.add(province.getId());
            parentIds.add(province.getId());
        }
        return addressIds;
    }

    //处理自定义范围：系统参数及、选中组织、选中组织及子组织、选中组织及子孙组织、
    public OnlineCusCheckModel getSystemParamIds(List<String> ableList, Map<String, List<String>> allTypeMap, String jnpfKey) {
        UserInfo userInfo = UserProvider.getUser();
        OnlineCusCheckModel cusCheckModel = new OnlineCusCheckModel();
        for (String item : ableList) {
            List<String> itemList = new ArrayList<>();
            if (DataInterfaceVarConst.ORG.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAbleComIds().addAll(allTypeMap.get(item));
                } else {
                    itemList = userInfo.getOrganizeIds();
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAbleComIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.ORGANDSUB.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAbleComIds().addAll(allTypeMap.get(item));
                } else {
                    List<OrganizeEntity> listByParentIds = organizeApi.getListByParentIds(userInfo.getOrganizeIds());
                    itemList.addAll(userInfo.getOrganizeIds());
                    itemList.addAll(listByParentIds.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAbleComIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.ORGANIZEANDPROGENY.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAbleComIds().addAll(allTypeMap.get(item));
                } else {
                    List<OrganizeEntity> listByParentIds = organizeApi.getProgeny(userInfo.getOrganizeIds(), null);
                    itemList.addAll(listByParentIds.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAbleComIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.POSITIONID.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAblePosIds().addAll(allTypeMap.get(item));
                } else {
                    itemList = userInfo.getPositionIds();
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAblePosIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.POSITIONANDSUB.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAblePosIds().addAll(allTypeMap.get(item));
                } else {
                    List<PositionEntity> listByParentIds = positionApi.getListByParentIds(userInfo.getPositionIds());
                    itemList.addAll(userInfo.getPositionIds());
                    itemList.addAll(listByParentIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAblePosIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.POSITIONANDPROGENY.equals(item)) {
                if (allTypeMap.containsKey(item)) {
                    cusCheckModel.getAblePosIds().addAll(allTypeMap.get(item));
                } else {
                    List<PositionEntity> listByParentIds = positionApi.getProgeny(userInfo.getPositionIds(), null);
                    itemList.addAll(listByParentIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                    allTypeMap.put(item, itemList);
                    cusCheckModel.getAblePosIds().addAll(itemList);
                }
            } else if (DataInterfaceVarConst.USER.equals(item)) {
                cusCheckModel.getAbleUserIds().add(userInfo.getUserId());
            } else {
                String[] split = item.split("--");
                if (split.length > 1) {
                    if (SysParamEnum.ORG.getCode().equalsIgnoreCase(split[1])) {
                        cusCheckModel.getAbleComIds().add(split[0]);
                    } else if (SysParamEnum.SUBORG.getCode().equalsIgnoreCase(split[1])) {
                        if (allTypeMap.containsKey(item)) {
                            cusCheckModel.getAbleComIds().addAll(allTypeMap.get(item));
                        } else {
                            List<OrganizeEntity> listByParentIds = organizeApi.getListByParentIds(Arrays.asList(split[0]));
                            itemList.add(split[0]);
                            itemList.addAll(listByParentIds.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                            List<PositionEntity> listByOrgIds = positionApi.getListByOrgIds(itemList);
                            itemList.addAll(listByOrgIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                            allTypeMap.put(item, itemList);
                            cusCheckModel.getAbleComIds().addAll(itemList);
                        }
                    } else if (SysParamEnum.PROGENYORG.getCode().equalsIgnoreCase(split[1])) {
                        if (allTypeMap.containsKey(item)) {
                            cusCheckModel.getAbleComIds().addAll(allTypeMap.get(item));
                        } else {
                            List<OrganizeEntity> listByParentIds = organizeApi.getProgeny(Arrays.asList(split[0]), null);
                            itemList.addAll(listByParentIds.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                            List<PositionEntity> listByOrgIds = positionApi.getListByOrgIds(itemList);
                            itemList.addAll(listByOrgIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                            allTypeMap.put(item, itemList);
                            cusCheckModel.getAbleComIds().addAll(itemList);
                        }
                    } else if (SysParamEnum.POS.getCode().equalsIgnoreCase(split[1])) {
                        cusCheckModel.getAblePosIds().add(split[0]);
                    } else if (SysParamEnum.SUBPOS.getCode().equalsIgnoreCase(split[1])) {
                        if (allTypeMap.containsKey(item)) {
                            cusCheckModel.getAblePosIds().addAll(allTypeMap.get(item));
                        } else {
                            List<PositionEntity> listByParentIds = positionApi.getListByParentIds(Arrays.asList(split[0]));
                            itemList.addAll(Arrays.asList(split[0]));
                            itemList.addAll(listByParentIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                            allTypeMap.put(item, itemList);
                            cusCheckModel.getAblePosIds().addAll(itemList);
                        }
                    } else if (SysParamEnum.PROGENYPOS.getCode().equalsIgnoreCase(split[1])) {
                        if (allTypeMap.containsKey(item)) {
                            cusCheckModel.getAblePosIds().addAll(allTypeMap.get(item));
                        } else {
                            List<PositionEntity> listByParentIds = positionApi.getProgeny(Arrays.asList(split[0]), null);
                            itemList.addAll(listByParentIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                            allTypeMap.put(item, itemList);
                            cusCheckModel.getAblePosIds().addAll(itemList);
                        }
                    } else if (SysParamEnum.ROLE.getCode().equalsIgnoreCase(split[1])) {
                        cusCheckModel.getAbleRoleIds().add(split[0]);
                    } else if (SysParamEnum.GROUP.getCode().equalsIgnoreCase(split[1])) {
                        cusCheckModel.getAbleGroupIds().add(split[0]);
                    } else if (SysParamEnum.USER.getCode().equalsIgnoreCase(split[1])) {
                        cusCheckModel.getAbleUserIds().add(split[0]);
                    }
                } else {
                    if (JnpfKeyConsts.COMSELECT.equalsIgnoreCase(jnpfKey)) {
                        cusCheckModel.getAbleComIds().add(item);
                    }
                    if (JnpfKeyConsts.ROLESELECT.equalsIgnoreCase(jnpfKey)) {
                        cusCheckModel.getAbleRoleIds().add(item);
                    }
                    if (JnpfKeyConsts.GROUPSELECT.equalsIgnoreCase(jnpfKey)) {
                        cusCheckModel.getAbleGroupIds().add(item);
                    }
                    if (JnpfKeyConsts.USERSELECT.equalsIgnoreCase(jnpfKey) || JnpfKeyConsts.CUSTOMUSERSELECT.equalsIgnoreCase(jnpfKey)) {
                        cusCheckModel.getAbleUserIds().add(item);
                    }
                }
            }

        }
        return cusCheckModel;
    }

    public void checkCustomControl(OnlineCusCheckModel cusCheckModel, List<String> errList, int i, String label) {
        boolean contains;
        List<String> ableIdsAll = new ArrayList<>();
        List<String> ableDepIds = cusCheckModel.getAbleDepIds();
        List<String> ableGroupIds = cusCheckModel.getAbleGroupIds();
        List<String> ablePosIds = cusCheckModel.getAblePosIds();
        List<String> ableRoleIds = cusCheckModel.getAbleRoleIds();
        List<String> ableUserIds = cusCheckModel.getAbleUserIds();
        List<String> ableComIds = cusCheckModel.getAbleComIds();
        List<String> dataList = cusCheckModel.getDataList();
        String controlType = cusCheckModel.getControlType();
        switch (controlType) {
            case JnpfKeyConsts.GROUPSELECT:
                ableIdsAll.addAll(ableGroupIds);
                break;
            case JnpfKeyConsts.ROLESELECT:
                ableIdsAll.addAll(ableRoleIds);
                break;
            case JnpfKeyConsts.DEPSELECT:
                ableIdsAll.addAll(ableDepIds);
                break;
            case JnpfKeyConsts.COMSELECT:
                ableIdsAll.addAll(ableComIds);
                break;
            case JnpfKeyConsts.CUSTOMUSERSELECT:

                for (String id : ableGroupIds) {
                    ableIdsAll.add(id + "--group");
                }
                for (String id : ablePosIds) {
                    ableIdsAll.add(id + "--pos");
                }
                for (String id : ableRoleIds) {
                    ableIdsAll.add(id + "--role");
                }
                for (String id : ableUserIds) {
                    ableIdsAll.add(id + "--user");
                }
                for (String id : ableComIds) {
                    ableIdsAll.add(id + "--org");
                }
                ableIdsAll.addAll(ableDepIds);
                ableIdsAll.addAll(ableGroupIds);
                ableIdsAll.addAll(ablePosIds);
                ableIdsAll.addAll(ableRoleIds);
                ableIdsAll.addAll(ableUserIds);
                ableIdsAll.addAll(ableComIds);
                List<UserRelationEntity> listByObjectIdAll = userRelationApi.getListByObjectIdAll(ableIdsAll);
                for (UserRelationEntity userRelationEntity : listByObjectIdAll) {
                    ableIdsAll.add(userRelationEntity.getUserId() + "--user");
                }
                break;
            case JnpfKeyConsts.USERSELECT:
                List<String> objIds = new ArrayList<>();
                if (!ableComIds.isEmpty()) {
                    List<String> lastIds = new ArrayList<>();
                    for (String str : ableComIds) {
                        lastIds.add(str);
                    }
                    objIds.addAll(lastIds);
                }
                if (!ableDepIds.isEmpty()) {
                    List<String> lastIds = new ArrayList<>();
                    for (String str : ableDepIds) {
                        lastIds.add(str);
                    }
                    objIds.addAll(lastIds);
                }
                if (!ableGroupIds.isEmpty()) {
                    objIds.addAll(ableGroupIds);
                }
                if (!ablePosIds.isEmpty()) {
                    objIds.addAll(ablePosIds);
                }
                if (!ableRoleIds.isEmpty()) {
                    objIds.addAll(ableRoleIds);
                }
                List<String> userIds = userRelationApi.getListByObjectIdAll(objIds).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                userIds.addAll(ableUserIds);
                ableIdsAll.addAll(userIds);
                break;
            case JnpfKeyConsts.POSSELECT:
                List<String> posIds = new ArrayList<>();
                if (!ableDepIds.isEmpty()) {
                    List<String> depIds = positionApi.getListByOrganizeId(ableDepIds, false).stream().map(PositionEntity::getId).collect(Collectors.toList());
                    posIds.addAll(depIds);
                }
                if (!ablePosIds.isEmpty()) {
                    posIds.addAll(ablePosIds);
                }
                ableIdsAll.addAll(posIds);
                break;
            default:
                break;
        }
        if (!ableIdsAll.isEmpty()) {
            for (String id : dataList) {
                contains = ableIdsAll.contains(id);
                if (!contains) {
                    errList.set(i, label + VALUE_NOTIN_RANGE);
                    break;
                }
            }
        }
    }

    public List<String> checkOptionsControl(FieLdsModel swapDataVo, Map<String, Object> insMap, Map<String, Object> cacheMap,
                                            List<String> valueList, List<String> errList, int i) {
        boolean multiple = swapDataVo.getMultiple();
        String vModel = swapDataVo.getVModel();
        String label = swapDataVo.getConfig().getLabel();

        boolean error = false;
        if (!multiple && valueList.size() > 1) {
            //非多选填入多选值
            error = true;
            errList.set(i, label + VALUE_ERR);
        }
        List<String> dataList = new ArrayList<>();
        if (!error) {
            boolean errorHapen = false;
            for (String va : valueList) {
                if (StringUtil.isEmpty(va)) {
                    errorHapen = true;
                } else if (cacheMap.get(va) == null) {
                    errorHapen = true;
                } else {
                    dataList.add(cacheMap.get(va).toString());
                }
            }
            if (errorHapen) {
                errList.set(i, label + VALUE_ERR);
            } else {
                insMap.put(vModel, !multiple ? dataList.get(0) : JsonUtil.getObjectToString(dataList));
            }
        }
        return dataList;
    }

    public void checkFormDataInteface(FieLdsModel swapDataVo, Map<String, Object> insMap, Map<String, Object> cacheMap,
                                      List<String> valueList, List<String> errList, int i) {
        boolean multiple = swapDataVo.getMultiple();
        String vModel = swapDataVo.getVModel();
        String label = swapDataVo.getConfig().getLabel();
        String jnpfKey = swapDataVo.getConfig().getJnpfKey();
        boolean isCascader = JnpfKeyConsts.CASCADER.equals(jnpfKey);
        if (JnpfKeyConsts.CHECKBOX.equals(jnpfKey)) {
            multiple = true;
        }
        List<String[]> staticStrData = new ArrayList<>();
        List<String> staticStrDataList1 = new ArrayList<>();
        //单选给多选直接报错
        if (!multiple && valueList.size() > 1) {
            errList.set(i, label + VALUE_ERR);
            return;
        }
        boolean hasError = false;
        boolean takeOne = false;
        for (String dicValue : valueList) {
            if (isCascader) {
                List<String> staticStrDataList2 = new ArrayList<>();
                if (!multiple && valueList.size() > 1) hasError = true;
                if (dicValue.contains("/")) {
                    String[] split = dicValue.split("/");
                    for (String s : split) {
                        Object s1 = cacheMap.get(s);
                        if (s1 != null) {
                            staticStrDataList2.add(s1.toString());
                            staticStrDataList1.add(s1.toString());
                        } else {
                            hasError = true;
                        }
                    }
                    staticStrData.add(staticStrDataList2.toArray(new String[staticStrDataList2.size()]));
                } else {
                    if (cacheMap.get(dicValue) == null) {
                        hasError = true;
                    } else {
                        staticStrDataList1.add(cacheMap.get(dicValue).toString());
                        staticStrData.add(new String[]{cacheMap.get(dicValue).toString()});
                    }
                }
            } else {
                takeOne = true;
                Object s1 = cacheMap.get(dicValue);
                if (s1 != null) {
                    staticStrDataList1.add(s1.toString());
                } else {
                    hasError = true;
                }
            }
        }

        if (hasError) {
            errList.set(i, label + VALUE_ERR);
        } else {
            String v;
            if (multiple) {
                v = takeOne ? JsonUtil.getObjectToString(staticStrDataList1) : JsonUtil.getObjectToString(staticStrData);
            } else {
                v = takeOne ? staticStrDataList1.get(0) : JsonUtil.getObjectToString(staticStrDataList1);
            }
            insMap.put(vModel, v);
        }
    }

    //验证组织选择
    private static ValidationRes validatComSelect(String value, FieLdsModel swapDataVo, Map<String, Object> allOrgsTreeName) {
        String label = swapDataVo.getConfig().getLabel();
        boolean multiple = swapDataVo.getMultiple();
        List<String> valueList = Arrays.asList(value.split(","));

        if (!multiple && valueList.size() > 1) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        //验证值是否正确
        List<String> comOneList = new ArrayList<>();
        for (String comValue : valueList) {
            if (StringUtil.isEmpty(comValue)) {
                return ValidationRes.failure(label + VALUE_ERR);
            }
            boolean find = false;
            for (Map.Entry<String, Object> keyItem : allOrgsTreeName.entrySet()) {
                Object o = keyItem.getValue();
                if (comValue.equals(o.toString())) {
                    comOneList.add(keyItem.getKey());
                    find = true;
                    break;
                }
            }
            if (!find) {
                return ValidationRes.failure(label + VALUE_ERR);
            }
        }

        //判断是否是可选范围
        String thisValue;
        if (!multiple) {
            thisValue = !comOneList.isEmpty() ? comOneList.get(0) : "";
        } else {
            thisValue = JsonUtil.getObjectToString(comOneList);
        }
        return ValidationRes.success(thisValue, comOneList);
    }

    //验证日期
    private static ValidationRes validatDate(String value, FieLdsModel swapDataVo, Map<String, Object> data) {
        String jnpfKey = swapDataVo.getConfig().getJnpfKey();
        String format = swapDataVo.getFormat();
        String label = swapDataVo.getConfig().getLabel();
        Date valueDate = null;
        try {
            if (format.length() != value.length()) throw new DataException();
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            formatter.setLenient(false);
            valueDate = formatter.parse(value);
        } catch (Exception e) {
            return ValidationRes.failure(label + VALUE_ERR);
        }

        //判断时间是否在设置范围内
        boolean timeHasRangeError = FormPublicUtils.dateTimeCondition(swapDataVo, format, value, data, jnpfKey);
        if (timeHasRangeError) {
            return ValidationRes.failure(label + VALUE_NOTIN_RANGE);
        }
        return ValidationRes.success(valueDate.getTime());
    }

    //验证时间
    private static ValidationRes validatTime(String value, FieLdsModel swapDataVo, Map<String, Object> data) {
        String jnpfKey = swapDataVo.getConfig().getJnpfKey();
        String label = swapDataVo.getConfig().getLabel();
        //格式
        String dataFomrat = "yyyy-MM-dd " + swapDataVo.getFormat();
        String valueTime = jnpf.util.DateUtil.daFormat(new Date()) + " " + value;
        try {
            if (swapDataVo.getFormat().length() != value.length()) throw new DataException();
            SimpleDateFormat formatter = new SimpleDateFormat(dataFomrat);
            formatter.setLenient(false);
            formatter.parse(valueTime);
        } catch (Exception e) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        //判断时间是否在设置范围内
        boolean timeHasRangeError = FormPublicUtils.dateTimeCondition(swapDataVo, dataFomrat, valueTime, data, jnpfKey);
        if (timeHasRangeError) {
            return ValidationRes.failure(label + VALUE_NOTIN_RANGE);
        }
        return ValidationRes.success("");
    }

    //验证滑块
    private static ValidationRes validatSlider(String value, FieLdsModel swapDataVo) {
        String label = swapDataVo.getConfig().getLabel();
        BigDecimal ivalue = null;
        try {
            ivalue = new BigDecimal(value);
        } catch (Exception e) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        if (swapDataVo.getMin() != null) {
            BigDecimal min = new BigDecimal(swapDataVo.getMin());
            if (ivalue.compareTo(min) < 0) {
                return ValidationRes.failure(label + VALUE_LESS_MIN);
            }
        }
        if (swapDataVo.getMax() != null) {
            BigDecimal max = new BigDecimal(swapDataVo.getMax());
            if (ivalue.compareTo(max) > 0) {
                return ValidationRes.failure(label + VALUE_THAN_MAX);
            }
        }
        return ValidationRes.success(ivalue);
    }

    //验证比率
    private static ValidationRes validatRate(String value, FieLdsModel swapDataVo) {
        String label = swapDataVo.getConfig().getLabel();
        Double ratevalue = null;
        try {
            ratevalue = Double.valueOf(value);
        } catch (Exception e) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        Double maxvalue = Double.valueOf(0);
        if (swapDataVo.getCount() != -1) {
            maxvalue = Double.valueOf(swapDataVo.getCount());
        }
        if (ratevalue > maxvalue) {
            return ValidationRes.failure(label + VALUE_THAN_MAX);
        }
        if (Boolean.TRUE.equals(swapDataVo.getAllowhalf())) {
            if (ratevalue % 0.5 != 0 || ratevalue < 0) {
                return ValidationRes.failure(label + VALUE_ERR);
            }
        } else {
            if (ratevalue % 1 != 0 || ratevalue < 0) {
                return ValidationRes.failure(label + VALUE_ERR);
            }
        }
        return ValidationRes.success(ratevalue);
    }

    private static ValidationRes validatInput(String value, FieLdsModel swapDataVo) {
        String label = swapDataVo.getConfig().getLabel();
        String regNum = "-?\\d+(\\.\\d+)?";
        if (StringUtil.isNotEmpty(value) && !value.matches(regNum)) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        //有精度，验证精度
        BigDecimal valueDecimal = null;
        try {
            valueDecimal = new BigDecimal(value);
        } catch (Exception e) {
            log.error("数字转换失败：" + e.getMessage(), e);
        }
        if (valueDecimal == null) {
            return ValidationRes.failure(label + VALUE_ERR);
        }
        if (swapDataVo.getPrecision() != null && valueDecimal.scale() > swapDataVo.getPrecision()) {
            return ValidationRes.failure(label + "值的精度不正确");
        }
        if (swapDataVo.getMin() != null && valueDecimal.compareTo(new BigDecimal(swapDataVo.getMin())) < 0) {
            return ValidationRes.failure(label + VALUE_LESS_MIN);
        }
        if (swapDataVo.getMax() != null && valueDecimal.compareTo(new BigDecimal(swapDataVo.getMax())) > 0) {
            return ValidationRes.failure(label + VALUE_THAN_MAX);
        }
        return ValidationRes.success();
    }
}
