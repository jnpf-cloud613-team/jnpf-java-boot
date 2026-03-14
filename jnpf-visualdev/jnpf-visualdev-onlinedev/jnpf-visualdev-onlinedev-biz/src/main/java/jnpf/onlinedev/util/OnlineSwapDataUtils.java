package jnpf.onlinedev.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jnpf.base.ActionResult;
import jnpf.base.entity.*;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.mapper.VisualdevMapper;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualConst;
import jnpf.base.model.datainterface.DataInterfaceModel;
import jnpf.base.model.datainterface.DataInterfacePage;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.online.ImportDataModel;
import jnpf.base.model.online.ImportFormCheckUniqueModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.ProvinceService;
import jnpf.base.util.*;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.constant.PermissionConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.ExcelColumnAttr;
import jnpf.model.ExcelModel;
import jnpf.model.SystemParamModel;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.*;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.props.PropsModel;
import jnpf.onlinedev.model.OnlineInfoModel;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.enums.MultipleControlEnum;
import jnpf.onlinedev.model.enums.OnlineDataTypeEnum;
import jnpf.onlinedev.model.online.InterefaceParamModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.authorize.AuthorizeConditionEnum;
import jnpf.permission.service.*;
import jnpf.permissions.PermissionInterfaceImpl;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.join.EqualTo;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据解析
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/7/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineSwapDataUtils {

    private final RedisUtil redisUtil;
    private final DictionaryDataService dictionaryDataApi;
    private final UserService userApi;
    private final PositionService positionApi;
    private final ProvinceService areaApi;
    private final OrganizeService organizeApi;
    private final VisualdevMapper visualdevMapper;
    private final DataInterfaceService dataInterFaceApi;
    private final RoleService roleApi;
    private final GroupService groupApi;
    private final OnlineDevInfoUtils onlineDevInfoUtils;
    private final FlowFormDataMapper flowFormDataMapper;
    private final FlowFormDataUtil flowDataUtil;
    private final OnlineExecutor executor;
    private final UserRelationService userRelationService;
    private final DbLinkService dblinkService;

    public static final long DEFAULT_CACHE_TIME = 300L;

    //缓存系统权限数据, 组织、岗位、分组、角色、用户
    public static final boolean NEEDCACHE_SYS = true;
    //缓存远端数据, 静态、字典、接口、弹窗选择
    public static final boolean NEEDCACHE_REMOTE = true;
    //缓存关联数据, 关联表单
    public static final boolean NEEDCACHE_RELATION = true;

    public List<Map<String, Object>> getSwapList(List<Map<String, Object>> list, List<FieLdsModel> swapDataVoList, String visualDevId, boolean inlineEdit) {
        if (list.isEmpty()) {
            return list;
        }
        return getSwapList(list, swapDataVoList, visualDevId, inlineEdit, null, true, null);
    }

    public List<Map<String, Object>> getSwapInfo(List<Map<String, Object>> list, List<FieLdsModel> swapDataVoList, String visualDevId, boolean inlineEdit, Map<String, Object> mainAndMast) {
        if (list.isEmpty()) {
            return list;
        }
        return getSwapList(list, swapDataVoList, visualDevId, inlineEdit, null, false, mainAndMast);
    }

    public List<Map<String, Object>> getSwapList(List<Map<String, Object>> list, List<FieLdsModel> swapDataVoList, String visualDevId, boolean inlineEdit,
                                                 Map<String, Object> localCacheParent, boolean isList, Map<String, Object> mainAndMast) {
        try {
            DynamicDataSourceUtil.switchToDataSource(null);
            if (list.isEmpty()) {
                return list;
            }
            //主表的缓存数据继续使用, 不重新初始化
            Map<String, Object> localCache = Optional.ofNullable(localCacheParent).orElse(new ConcurrentHashMap<>());
            //初始化系统缓存-多线程
            executor.executorRedis(localCache, swapDataVoList, visualDevId, inlineEdit, list, mainAndMast);
            writeRedisAndList(localCache, swapDataVoList, visualDevId, inlineEdit, list, isList, mainAndMast);
        } catch (SQLException e) {
            log.error("数据转换，数据库异常：" + e.getMessage(), e);
            throw new DataException(e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return list;
    }

    private List<Map<String, Object>> writeRedisAndList(Map<String, Object> localCache, List<FieLdsModel> swapDataVoList, String visualDevId, boolean inlineEdit,
                                                        List<Map<String, Object>> list, boolean isList, Map<String, Object> mainAndMast) {
        String dsName = Optional.ofNullable(TenantHolder.getDatasourceId()).orElse("");

        Map<String, Object> userMap = (Map<String, Object>) localCache.get("__user_map");
        Map<String, Object> orgMap = (Map<String, Object>) localCache.get("__org_map");
        Map<String, Object> posMap = (Map<String, Object>) localCache.get("__pos_map");
        Map<String, Object> orgTreeMap = (Map<String, Object>) localCache.get("__orgTree_map");
        Map<String, Object> roleMap = (Map<String, Object>) localCache.get("__role_map");
        Map<String, Object> groupMap = (Map<String, Object>) localCache.get("__group_map");

        List<String> arrJnpfKey = Arrays.asList(JnpfKeyConsts.UPLOADFZ, JnpfKeyConsts.UPLOADIMG);
        for (int x = 0; x < list.size(); x++) {
            Map<String, Object> dataMap = list.get(x);
            if (dataMap == null) {
                dataMap = new HashMap<>();
                list.set(x, dataMap);
            }
            Map<String, Object> dataCopyMap = new HashMap<>(dataMap);
            for (FieLdsModel swapDataVo : swapDataVoList) {
                String jnpfKey = swapDataVo.getConfig().getJnpfKey();
                if (StringUtil.isEmpty(swapDataVo.getVModel())) {
                    continue;
                }
                String swapVModel = swapDataVo.getVModel();
                String vModel = inlineEdit && isList ? swapDataVo.getVModel() + "_name" : swapDataVo.getVModel();
                String dataType = swapDataVo.getConfig().getDataType();
                boolean isMultiple = Objects.nonNull(swapDataVo.getMultiple()) && swapDataVo.getMultiple();

                //clob字段转换
                FormInfoUtils.swapClob(dataMap, swapDataVo.getVModel());

                try {
                    Map<String, Map<String, Object>> dataDetailMap = new HashMap<>();
                    //关联表单获取原字段数据
                    FormPublicUtils.relationGetJnpfId(dataMap, jnpfKey, dataMap.get(swapVModel), swapVModel);
                    if (StringUtil.isEmpty(String.valueOf(dataMap.get(swapVModel))) || String.valueOf(dataMap.get(swapVModel)).equals("[]")
                            || String.valueOf(dataMap.get(swapVModel)).equals("null")) {
                        if (jnpfKey.equals(JnpfKeyConsts.CHILD_TABLE)) {
                            dataMap.put(vModel, new ArrayList<>());
                        } else if (arrJnpfKey.contains(jnpfKey)) {
                            dataMap.put(swapVModel, new ArrayList<>());
                        } else {
                            if (inlineEdit) {
                                dataMap.put(swapVModel, null);
                            }
                            dataMap.put(vModel, null);
                        }
                    } else {
                        //是否联动
                        boolean dynamicNeedCache;
                        String redisKey;
                        String separator = swapDataVo.getSeparator();
                        switch (jnpfKey) {
                            case JnpfKeyConsts.CALCULATE:
                            case JnpfKeyConsts.NUM_INPUT:
                                Object decimalValue = dataCopyMap.get(swapDataVo.getVModel());
                                Integer precision = swapDataVo.getPrecision();
                                if (decimalValue instanceof BigDecimal) {
                                    BigDecimal bd = (BigDecimal) decimalValue;
                                    String value;
                                    List<Integer> integerType = ImmutableList.of(3, 4);
                                    if (!integerType.contains(swapDataVo.getRoundType())) {
                                        if (precision != null && precision > 0) {
                                            String formatZ = "000000000000000";
                                            String format = formatZ.substring(0, precision);
                                            DecimalFormat decimalFormat = new DecimalFormat("0." + format);
                                            value = decimalFormat.format(bd);
                                        } else {
                                            value = String.valueOf(bd.stripTrailingZeros().toPlainString());
                                        }
                                    } else {
                                        //向上（下）取整，去掉末尾0
                                        value = bd.stripTrailingZeros().toPlainString();
                                    }
                                    dataMap.put(vModel, value);
                                } else {
                                    dataMap.put(vModel, decimalValue);
                                }
                                break;
                            //公司组件
                            case JnpfKeyConsts.COMSELECT:
                                //部门组件
                            case JnpfKeyConsts.DEPSELECT:
                                //所属部门
                            case JnpfKeyConsts.CURRDEPT:
                                dataMap.put(vModel, OnlinePublicUtils.getDataInMethod(orgTreeMap, dataMap.get(swapVModel), isMultiple));
                                break;
                            //所属组织
                            case JnpfKeyConsts.CURRORGANIZE:
                                //多级组织
                                getTreeName(dataMap, swapVModel, orgTreeMap, vModel);
                                break;

                            //岗位组件
                            case JnpfKeyConsts.POSSELECT:
                                //所属岗位
                            case JnpfKeyConsts.CURRPOSITION:
                                //多级组织
                                getTreeName(dataMap, swapVModel, posMap, vModel);
                                break;

                            //用户组件
                            case JnpfKeyConsts.USERSELECT:
                                //创建用户
                            case JnpfKeyConsts.CREATEUSER:
                                //修改用户
                            case JnpfKeyConsts.MODIFYUSER:
                                String userData = OnlinePublicUtils.getDataInMethod(userMap, dataMap.get(swapVModel), isMultiple);
                                dataMap.put(vModel, userData);
                                break;
                            case JnpfKeyConsts.CUSTOMUSERSELECT:
                                List<String> dataNoSwapInMethod = OnlinePublicUtils.getDataNoSwapInMethod(dataMap.get(swapVModel));
                                StringJoiner valueJoin = new StringJoiner(",");
                                for (String data : dataNoSwapInMethod) {
                                    String id = data.contains("--") ? data.substring(0, data.lastIndexOf("--")) : data;
                                    String type = data.contains("--") ? data.substring(data.lastIndexOf("--") + 2) : "";
                                    Map<String, Object> cacheMap;
                                    switch (type) {
                                        case "role":
                                            cacheMap = roleMap;
                                            break;
                                        case "position":
                                            cacheMap = posMap;
                                            break;
                                        case "company":
                                        case "department":
                                            cacheMap = orgMap;
                                            break;
                                        case "group":
                                            cacheMap = groupMap;
                                            break;
                                        case "user":
                                        default:
                                            cacheMap = userMap;
                                            break;
                                    }
                                    valueJoin.add(Optional.ofNullable(cacheMap.get(id)).orElse("").toString());
                                }
                                dataMap.put(vModel, valueJoin.toString());
                                break;
                            //角色选择
                            case JnpfKeyConsts.ROLESELECT:
                                String roleData = OnlinePublicUtils.getDataInMethod(roleMap, dataMap.get(swapVModel), isMultiple);
                                dataMap.put(vModel, roleData);
                                break;

                            case JnpfKeyConsts.GROUPSELECT:
                                String groupData = OnlinePublicUtils.getDataInMethod(groupMap, dataMap.get(swapVModel), isMultiple);
                                dataMap.put(vModel, groupData);
                                break;

                            //省市区联动
                            case JnpfKeyConsts.ADDRESS:
                                String addressValue = String.valueOf(dataMap.get(swapVModel));
                                if (Boolean.TRUE.equals(OnlinePublicUtils.getMultiple(addressValue, MultipleControlEnum.MULTIPLE_JSON_TWO.getMultipleChar()))) {
                                    String[][] data = JsonUtil.getJsonToBean(addressValue, String[][].class);
                                    List<String> proDataS = Arrays.stream(data)
                                            .flatMap(Arrays::stream)
                                            .collect(Collectors.toList());
                                    Map<String, String> provinceNames = areaApi.getProList(proDataS).stream().collect(Collectors.toMap(
                                            ProvinceEntity::getId, ProvinceEntity::getFullName
                                            , (k1, k2) -> k2
                                            , () -> new LinkedHashMap<>(proDataS.size(), 1.0F)
                                    ));
                                    List<String> addList = new ArrayList<>();
                                    for (String[] AddressData : data) {
                                        List<String> adList = new ArrayList<>();
                                        for (int i = 0; i < AddressData.length; i++) {
                                            String addressDatum = AddressData[i];
                                            String value = provinceNames.getOrDefault(addressDatum, "");
                                            adList.add(value);
                                        }
                                        addList.add(String.join("/", adList));
                                    }
                                    dataMap.put(vModel, String.join(";", addList));
                                } else if (Boolean.TRUE.equals(OnlinePublicUtils.getMultiple(addressValue, MultipleControlEnum.MULTIPLE_JSON_ONE.getMultipleChar()))) {
                                    List<String> proDataS = JsonUtil.getJsonToList(String.valueOf(dataMap.get(swapVModel)), String.class);
                                    Map<String, String> provinceNames = areaApi.getProList(proDataS).stream().collect(Collectors.toMap(
                                            ProvinceEntity::getId, ProvinceEntity::getFullName
                                            , (k1, k2) -> k2
                                            , () -> new LinkedHashMap<>(proDataS.size(), 1.0F)
                                    ));
                                    List<String> adList = new ArrayList<>();
                                    for (String addressDatum : proDataS) {
                                        String value = provinceNames.getOrDefault(addressDatum, "");
                                        adList.add(value);
                                    }
                                    dataMap.put(vModel, String.join("/", adList));
                                }
                                break;
                            //开关
                            case JnpfKeyConsts.SWITCH:
                                String switchValue = String.valueOf(dataMap.get(swapVModel)).equals("1") ? swapDataVo.getActiveTxt() : swapDataVo.getInactiveTxt();
                                dataMap.put(vModel, switchValue);
                                break;

                            case JnpfKeyConsts.CASCADER:
                            case JnpfKeyConsts.RADIO:
                            case JnpfKeyConsts.CHECKBOX:
                            case JnpfKeyConsts.SELECT:
                            case JnpfKeyConsts.TREESELECT:
                                if (StringUtil.isEmpty(separator)) {
                                    separator = "/";
                                }
                                if (JnpfKeyConsts.CHECKBOX.equals(jnpfKey)) {
                                    isMultiple = true;
                                }
                                dynamicNeedCache = swapDataVo.getConfig().getTemplateJson().isEmpty();
                                String interfacelabel = swapDataVo.getProps().getLabel() != null ? swapDataVo.getProps().getLabel() : "";
                                String interfaceValue = swapDataVo.getProps().getValue() != null ? swapDataVo.getProps().getValue() : "";
                                String interfaceChildren = swapDataVo.getProps().getChildren() != null ? swapDataVo.getProps().getChildren() : "";
                                if (dynamicNeedCache) {
                                    if (OnlineDataTypeEnum.STATIC.getType().equals(dataType)) {
                                        redisKey = String.format("%s-%s-%s", visualDevId, swapDataVo.getConfig().getRelationTable() + swapDataVo.getVModel(), OnlineDataTypeEnum.STATIC.getType());
                                    } else if (dataType.equals(OnlineDataTypeEnum.DYNAMIC.getType())) {
                                        redisKey = String.format("%s-%s-%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getConfig().getPropsUrl(), interfaceValue, interfacelabel, interfaceChildren);
                                    } else {
                                        redisKey = String.format("%s-%s-%s", dsName, OnlineDataTypeEnum.DICTIONARY.getType(), swapDataVo.getConfig().getDictionaryType());
                                    }
                                    Map<String, Object> cascaderMap;
                                    if (dataType.equals(OnlineDataTypeEnum.DICTIONARY.getType())) {
                                        List<Map<String, Object>> checkBoxList = (List<Map<String, Object>>) localCache.get(redisKey);
                                        cascaderMap = OnlinePublicUtils.getDataMap(checkBoxList, swapDataVo);
                                    } else {
                                        cascaderMap = (Map<String, Object>) localCache.get(redisKey);
                                    }
                                    dataMap.put(vModel, FormPublicUtils.getDataConversion(cascaderMap, dataMap.get(swapVModel), isMultiple, separator));

                                } else {
                                    List<TemplateJsonModel> templateJsonModels = JsonUtil.getJsonToList(swapDataVo.getConfig().getTemplateJson(), TemplateJsonModel.class);
                                    Map<String, String> systemFieldValue = userApi.getSystemFieldValue(new SystemParamModel(JsonUtil.getObjectToString(templateJsonModels)));
                                    if (dataCopyMap != null) {
                                        systemFieldValue.put(AuthorizeConditionEnum.FORMID.getCondition(), String.valueOf(dataCopyMap.get(FlowFormConstant.ID)));
                                    }
                                    Map<String, String> paramMap = new HashMap<>();
                                    for (TemplateJsonModel templateJsonModel : templateJsonModels) {
                                        String relationField = Objects.isNull(templateJsonModel.getRelationField()) ? "" : templateJsonModel.getRelationField();
                                        String field = templateJsonModel.getField();
                                        String obj = inlineEdit ? "" : Optional.ofNullable(dataCopyMap.get(relationField)).orElse("").toString();
                                        if (templateJsonModel.getSourceType() != null && !Objects.equals(templateJsonModel.getSourceType(), 1)) {
                                            String dataValue = paramSourceTypeReplaceValue(templateJsonModel, systemFieldValue);
                                            paramMap.put(field, dataValue);
                                            continue;
                                        }
                                        if (relationField.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                                            String childField = relationField.split("-")[1];
                                            obj = Optional.ofNullable(dataCopyMap.get(childField)).orElse("").toString();
                                        } else if (mainAndMast != null) {
                                            obj = Optional.ofNullable(mainAndMast.get(relationField)).orElse("").toString();
                                        }
                                        paramMap.put(field, obj);
                                    }
                                    List<Map<String, Object>> dataList = null;
                                    List<Map<String, Object>> options = new ArrayList<>();
                                    Map<String, Object> dataInterfaceMap = new HashMap<>();
                                    //缓存Key 租户-远端数据-id-base64({params})
                                    redisKey = String.format("%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getConfig().getPropsUrl(), Base64.getEncoder().encodeToString(JsonUtil.getObjectToString(paramMap).getBytes(StandardCharsets.UTF_8)));
                                    if (localCache.containsKey(redisKey) || redisUtil.exists(redisKey)) {
                                        if (localCache.containsKey(redisKey)) {
                                            dataList = (List<Map<String, Object>>) localCache.get(redisKey);
                                        } else {
                                            List<Object> tmpList = redisUtil.get(redisKey, 0, -1);
                                            List<Map<String, Object>> tmpMapList = new ArrayList<>();
                                            tmpList.forEach(item -> tmpMapList.add(JsonUtil.entityToMap(item)));
                                            dataList = tmpMapList;
                                            localCache.put(redisKey, dataList);
                                        }
                                    } else {
                                        ActionResult<Object> data = dataInterFaceApi.infoToId(swapDataVo.getConfig().getPropsUrl(), null, paramMap);
                                        if (data != null && data.getData() != null && data.getData() instanceof List) {
                                            dataList = (List<Map<String, Object>>) data.getData();
                                            if (NEEDCACHE_REMOTE && CollUtil.isNotEmpty(dataList) && !JnpfKeyConsts.TREESELECT.equals(jnpfKey)
                                                    && Boolean.TRUE.equals(swapDataVo.getConfig().getUseCache())) {
                                                redisUtil.insert(redisKey, dataList, DEFAULT_CACHE_TIME);
                                            }
                                            localCache.put(redisKey, dataList);
                                        }
                                    }
                                    if (dataList != null) {
                                        JSONArray dataAll = JsonUtil.getListToJsonArray(dataList);
                                        treeToList(interfacelabel, interfaceValue, interfaceChildren, dataAll, options);
                                        options.forEach(o -> dataInterfaceMap.put(String.valueOf(o.get(interfaceValue)), String.valueOf(o.get(interfacelabel))));
                                    }
                                    dataMap.put(vModel, FormPublicUtils.getDataConversion(dataInterfaceMap, dataMap.get(swapVModel), isMultiple, separator));
                                }
                                break;
                            case JnpfKeyConsts.RELATIONFORM:
                                //取关联表单数据 按绑定功能加字段区分数据
                                redisKey = String.format("%s-%s-%s-%s-%s", dsName, JnpfKeyConsts.RELATIONFORM, swapDataVo.getModelId(), swapDataVo.getRelationField(), dataMap.get(swapDataVo.getVModel()));
                                VisualdevModelDataInfoVO infoVO = null;
                                if (localCache.containsKey(redisKey) || redisUtil.exists(redisKey)) {
                                    infoVO = new VisualdevModelDataInfoVO();
                                    if (localCache.containsKey(redisKey)) {
                                        infoVO.setData(localCache.get(redisKey).toString());
                                    } else {
                                        infoVO.setData(redisUtil.getString(redisKey).toString());
                                        localCache.put(redisKey, infoVO.getData());
                                    }

                                } else {
                                    String keyId = String.valueOf(dataMap.get(swapVModel));
                                    VisualdevEntity entity = visualdevMapper.getInfo(swapDataVo.getModelId());
                                    String propsValue = StringUtil.isNotEmpty(swapDataVo.getPropsValue()) && swapDataVo.getPropsValue().contains(JnpfConst.FIELD_SUFFIX_JNPFID) ?
                                            swapDataVo.getPropsValue().split(JnpfConst.FIELD_SUFFIX_JNPFID)[0] : swapDataVo.getPropsValue();
                                    if (Objects.nonNull(entity)) {
                                        infoVO = this.getDetailsDataInfo(keyId, entity,
                                                OnlineInfoModel.builder().needSwap(true).needRlationFiled(false).propsValue(propsValue).build());
                                    }
                                    String data = infoVO == null ? StringUtils.EMPTY : infoVO.getData();
                                    if (NEEDCACHE_RELATION) {
                                        redisUtil.insert(redisKey, data, DEFAULT_CACHE_TIME);
                                    }
                                    localCache.put(redisKey, data);
                                }
                                if (infoVO != null && StringUtil.isNotEmpty(infoVO.getData())) {
                                    Map<String, Object> formDataMap = JsonUtil.stringToMap(infoVO.getData());
                                    String relationField = swapDataVo.getRelationField();
                                    if (formDataMap != null && !formDataMap.isEmpty()) {
                                        dataMap.put(swapDataVo.getVModel() + "_id", dataMap.get(swapVModel));
                                        dataMap.put(vModel, formDataMap.get(relationField));
                                        dataDetailMap.put(vModel, formDataMap);
                                    }
                                }
                                break;
                            case JnpfKeyConsts.POPUPSELECT:
                            case JnpfKeyConsts.POPUPTABLESELECT:
                                //是否联动
                                Map<String, String> systemFieldValue = userApi.getSystemFieldValue(new SystemParamModel(swapDataVo.getTemplateJson()));
                                if (dataCopyMap != null) {
                                    systemFieldValue.put(AuthorizeConditionEnum.FORMID.getCondition(), String.valueOf(dataCopyMap.get(FlowFormConstant.ID)));
                                }
                                List<TemplateJsonModel> templateJsonModels = JsonUtil.getJsonToList(swapDataVo.getTemplateJson(), TemplateJsonModel.class);
                                List<Map<String, Object>> mapList;
                                Map<String, Object> popMaps = new HashMap<>();
                                String value = String.valueOf(dataMap.get(swapVModel));

                                List<DataInterfaceModel> listParam = new ArrayList<>();
                                for (TemplateJsonModel templateJsonModel : templateJsonModels) {
                                    String relationField = templateJsonModel.getRelationField();
                                    DataInterfaceModel dataInterfaceModel = JsonUtil.getJsonToBean(templateJsonModel, DataInterfaceModel.class);
                                    if (templateJsonModel.getSourceType() != null && !Objects.equals(templateJsonModel.getSourceType(), 1)) {
                                        String dataValue = paramSourceTypeReplaceValue(templateJsonModel, systemFieldValue);
                                        dataInterfaceModel.setDefaultValue(dataValue);
                                        listParam.add(dataInterfaceModel);
                                        continue;
                                    }
                                    String obj = inlineEdit ? "" : Optional.ofNullable(dataCopyMap.get(relationField)).orElse("").toString();
                                    if (relationField.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                                        String childField = relationField.split("-")[1];
                                        obj = Optional.ofNullable(dataCopyMap.get(childField)).orElse("").toString();
                                    } else if (mainAndMast != null) {
                                        obj = Optional.ofNullable(mainAndMast.get(relationField)).orElse("").toString();
                                    }
                                    dataInterfaceModel.setDefaultValue(obj);
                                    listParam.add(dataInterfaceModel);
                                }
                                DataInterfacePage dataInterfacePage = new DataInterfacePage();
                                dataInterfacePage.setParamList(listParam);
                                dataInterfacePage.setInterfaceId(swapDataVo.getInterfaceId());
                                List<String> ids = new ArrayList<>();
                                if (value.startsWith("[")) {
                                    ids = JsonUtil.getJsonToList(value, String.class);
                                } else {
                                    ids.add(value);
                                }
                                dataInterfacePage.setIds(ids);
                                //缓存Key 租户-远端数据-base64({id, params, ids})
                                redisKey = String.format("%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getInterfaceId(), Base64.getEncoder().encodeToString(JsonUtil.getObjectToString(dataInterfacePage).getBytes(StandardCharsets.UTF_8)));
                                if (localCache.containsKey(redisKey) || redisUtil.exists(redisKey)) {
                                    if (localCache.containsKey(redisKey)) {
                                        mapList = (List<Map<String, Object>>) localCache.get(redisKey);
                                    } else {
                                        List<Object> tmpList = redisUtil.get(redisKey, 0, -1);
                                        List<Map<String, Object>> tmpMapList = new ArrayList<>();
                                        tmpList.forEach(item -> tmpMapList.add(JsonUtil.entityToMap(item)));
                                        mapList = tmpMapList;
                                        localCache.put(redisKey, mapList);
                                    }
                                } else {
                                    dataInterfacePage.setPropsValue(swapDataVo.getPropsValue());
                                    dataInterfacePage.setRelationField(swapDataVo.getRelationField());
                                    mapList = dataInterFaceApi.infoToInfo(swapDataVo.getInterfaceId(), dataInterfacePage);
                                    if (NEEDCACHE_REMOTE && Boolean.TRUE.equals(swapDataVo.getConfig().getUseCache())) {
                                        redisUtil.insert(redisKey, mapList, DEFAULT_CACHE_TIME);
                                    }
                                    localCache.put(redisKey, mapList);
                                }

                                StringJoiner stringJoiner = new StringJoiner(",");
                                List<String> popList = new ArrayList<>();
                                if (value.startsWith("[")) {
                                    popList = JsonUtil.getJsonToList(value, String.class);
                                } else {
                                    popList.add(value);
                                }
                                for (String va : popList) {
                                    if (!popMaps.isEmpty()) {
                                        stringJoiner.add(String.valueOf(popMaps.get(va)));
                                    } else {
                                        Map<String, Object> popMap = mapList.stream().filter(map ->
                                                Objects.equals(String.valueOf(map.get(swapDataVo.getPropsValue())), va)).findFirst().orElse(new HashMap<>());
                                        if (!popMap.isEmpty()) {
                                            dataMap.put(vModel + "_id", dataMap.get(swapVModel));
                                            stringJoiner.add(String.valueOf(popMap.get(swapDataVo.getRelationField())));
                                            dataDetailMap.put(vModel, popMap);
                                        }
                                    }
                                }
                                dataMap.put(vModel, String.valueOf(stringJoiner));
                                break;
                            case JnpfKeyConsts.MODIFYTIME:
                            case JnpfKeyConsts.CREATETIME:
//                            case JnpfKeyConsts.TIME:
                            case JnpfKeyConsts.DATE:
                            case JnpfKeyConsts.DATE_CALCULATE:
                                //判断是否为时间戳格式
                                Object dateObj = dataMap.get(swapVModel);
                                LocalDateTime dateTime = LocalDateTimeUtil.of(new Date(DateTimeFormatConstant.getDateObjToLong(dateObj)));
                                String format = DateTimeFormatConstant.getFormat(swapDataVo.getFormat());
                                if (StringUtil.isEmpty(format)) {
                                    format = DateTimeFormatConstant.YEAR_MONTH_DHMS;
                                }
                                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                                String date = dateTimeFormatter.format(dateTime);
                                dataMap.put(vModel, date);
                                if (JnpfKeyConsts.MODIFYTIME.equals(jnpfKey) || JnpfKeyConsts.CREATETIME.equals(jnpfKey)) {
                                    dataMap.put(swapDataVo.getVModel(), date);
                                }
                                break;
                            case JnpfKeyConsts.RATE:
                            case JnpfKeyConsts.SLIDER:
                                //滑块评分不需要补零转浮点型
                                Double ratevalue = (double) 0;
                                if (dataMap.get(swapVModel) != null) {
                                    ratevalue = Double.valueOf(dataMap.get(swapVModel).toString());
                                }
                                dataMap.put(vModel, ratevalue);
                                break;
                            case JnpfKeyConsts.UPLOADFZ:
                            case JnpfKeyConsts.UPLOADIMG:
                                //数据传递-乱塞有bug强行置空
                                uploadData(dataMap, swapVModel, vModel);
                                break;
                            case JnpfKeyConsts.LOCATION:
                                //定位-列表取全名。
                                if (isList) {
                                    Map<String, Object> omap = JsonUtil.stringToMap(String.valueOf(dataMap.get(swapVModel)));
                                    dataMap.put(vModel, omap.get("fullAddress") != null ? omap.get("fullAddress") : "");
                                }
                                break;
                            case JnpfKeyConsts.CHILD_TABLE:
                                List<FieLdsModel> childrens = swapDataVo.getConfig().getChildren();
                                List<Map<String, Object>> childList = (List<Map<String, Object>>) dataMap.get(swapDataVo.getVModel());
                                List<Map<String, Object>> swapList = getSwapList(childList, childrens, visualDevId, inlineEdit, localCache, isList, dataCopyMap);
                                dataMap.put(swapDataVo.getVModel(), swapList);
                                break;
                            default:
                                dataMap.put(vModel, dataMap.get(swapVModel));
                                break;
                        }
                    }
                    //关联选择属性
                    if (!dataDetailMap.isEmpty()) {
                        getDataAttr(swapDataVoList, dataMap, dataDetailMap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("在线开发转换数据错误:" + e.getMessage());
                }
            }

            //二维码 条形码最后处理
            swapCodeDataInfo(swapDataVoList, dataMap, dataCopyMap);
        }
        if (inlineEdit && isList) {
            for (Map<String, Object> map : list) {
                //行内编辑过滤子表
                swapDataVoList = swapDataVoList.stream().filter(s -> !s.getVModel().toLowerCase().contains("tablefield")).collect(Collectors.toList());
                onlineDevInfoUtils.getInitLineData(swapDataVoList, map, localCache);
            }
        }
        return list;
    }

    //图片和文件字段数据处理
    private static void uploadData(Map<String, Object> dataMap, String swapVModel, String vModel) {
        List<Map<String, Object>> fileList = new ArrayList<>();
        try {
            fileList = (List) dataMap.get(swapVModel);
        } catch (Exception e) {
            try {
                fileList = JsonUtil.getJsonToListMap(String.valueOf(dataMap.get(swapVModel)));
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            log.error(e.getMessage(), e);
        }
        dataMap.put(vModel, fileList);
    }

    private static void getTreeName(Map<String, Object> dataMap, String swapVModel, Map<String, Object> posMap, String vModel) {
        String posIds = String.valueOf(dataMap.get(swapVModel));
        StringJoiner posName = new StringJoiner(",");
        List<String> posList = new ArrayList<>();
        try {
            posList = JsonUtil.getJsonToList(posIds, String.class);
        } catch (Exception e) {
            posList.add(posIds);
        }
        if (!posList.isEmpty()) {
            for (String t : posList) {
                if (posMap.get(t) != null) {
                    posName.add(posMap.get(t).toString());
                }
            }
        }
        if (posName.length() > 0) {
            dataMap.put(vModel, posName.toString());
        } else {
            dataMap.put(vModel, " ");
        }
    }

    /**
     * 按sourceType替换数据接口参数
     */
    public String paramSourceTypeReplaceValue(TemplateJsonModel item, Map<String, String> systemFieldValue) {
        String defaultValue = "";
        if (item.getSourceType() != null) {
            switch (item.getSourceType()) {
                case 1://字段
                    defaultValue = item.getDefaultValue();
                    break;
                case 2://自定义
                    defaultValue = item.getRelationField();
                    break;
                case 3://为空
                    defaultValue = "";
                    break;
                case 4://系统参数
                    defaultValue = this.getSystemFieldValue(item, systemFieldValue);
                    break;
                default:
                    defaultValue = item.getDefaultValue();
                    break;
            }
        } else {
            defaultValue = item.getDefaultValue();
        }
        return defaultValue;
    }

    /**
     * 获取系统参数值
     *
     * @param templateJsonModel
     * @param systemFieldValue
     * @return
     */
    private String getSystemFieldValue(TemplateJsonModel templateJsonModel, Map<String, String> systemFieldValue) {
        String relationField = templateJsonModel.getRelationField();
        String dataValue;
        if (AuthorizeConditionEnum.getResListType().contains(relationField)) {
            List<String> strings = StringUtil.isNotEmpty(systemFieldValue.get(relationField)) ?
                    JsonUtil.getJsonToList(systemFieldValue.get(relationField), String.class) : Collections.emptyList();
            dataValue = CollUtil.isEmpty(strings) ? "" : String.join(",", strings);
        } else if (systemFieldValue.containsKey(relationField)) {
            dataValue = systemFieldValue.get(relationField);
        } else {
            dataValue = templateJsonModel.getDefaultValue();
        }
        return dataValue;
    }

    /**
     * 级联递归
     *
     * @param value
     * @param label
     * @param children
     * @param data
     * @param result
     */
    public static void treeToList(String value, String label, String children, JSONArray data, List<Map<String, Object>> result) {
        for (int i = 0; i < data.size(); i++) {
            JSONObject ob = data.getJSONObject(i);
            Map<String, Object> tree = new HashMap<>(16);
            tree.put(value, String.valueOf(ob.get(value)));
            tree.put(label, String.valueOf(ob.get(label)));
            result.add(tree);
            if (ob.get(children) != null) {
                JSONArray childArray = ob.getJSONArray(children);
                treeToList(value, label, children, childArray, result);
            }
        }
    }

    /**
     * 递归查询
     *
     * @param label
     * @param value
     * @param children
     * @param data
     * @param options
     */
    public static void getOptions(String label, String value, String children, JSONArray data, List<Map<String, Object>> options) {
        for (int i = 0; i < data.size(); i++) {
            JSONObject ob = data.getJSONObject(i);
            Map<String, Object> tree = new HashMap<>(16);
            tree.put(value, String.valueOf(ob.get(value)));
            tree.put(label, String.valueOf(ob.get(label)));
            options.add(tree);
            if (ob.get(children) != null) {
                JSONArray childrenArray = ob.getJSONArray(children);
                getOptions(label, value, children, childrenArray, options);
            }
        }
    }

    /**
     * 生成关联属性（弹窗选择属性,关联表单属性）
     *
     * @param fieLdsModelList
     * @param dataMap
     * @param dataDetailMap
     */
    private static void getDataAttr(List<FieLdsModel> fieLdsModelList, Map<String, Object> dataMap, Map<String, Map<String, Object>> dataDetailMap) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            if (ObjectUtil.isEmpty(fieLdsModel)) {
                continue;
            }
            ConfigModel config = fieLdsModel.getConfig();
            String jnpfKey = config.getJnpfKey();
            if (jnpfKey.equals(JnpfKeyConsts.RELATIONFORM_ATTR) || jnpfKey.equals(JnpfKeyConsts.POPUPSELECT_ATTR)) {
                //0展示数据 ? 1存储数据
                boolean isShow = fieLdsModel.getIsStorage() == 0;
                if (isShow) {
                    String relationField = fieLdsModel.getRelationField();
                    if (relationField.contains("_jnpfTable_")) {
                        relationField = relationField.split("_jnpfTable_")[0];
                    }
                    String showField = fieLdsModel.getShowField();
                    Map<String, Object> formDataMap = dataDetailMap.get(relationField);
                    if (formDataMap != null) {
                        dataMap.put(relationField + "_" + showField, formDataMap.get(showField));
                    }
                }
            }
        }
    }


    /**
     * 二维码 条形码详情数据
     *
     * @param codeList    控件集合
     * @param swapDataMap 转换后的数据
     * @param dataMap     转换前
     * @return
     */
    public static void swapCodeDataInfo(List<FieLdsModel> codeList, Map<String, Object> swapDataMap, Map<String, Object> dataMap) {
        for (FieLdsModel formModel : codeList) {
            String jnpfKey = formModel.getConfig().getJnpfKey();
            if (jnpfKey.equals(JnpfKeyConsts.QR_CODE) || jnpfKey.equals(JnpfKeyConsts.BARCODE)) {
                String codeDataType = formModel.getDataType();
                if (OnlineDataTypeEnum.RELATION.getType().equals(codeDataType)) {
                    String relationFiled = formModel.getRelationField();
                    if (StringUtil.isNotEmpty(relationFiled)) {
                        Object relationValue = dataMap.get(relationFiled);
                        if (ObjectUtil.isNotEmpty(relationValue)) {
                            swapDataMap.put(relationFiled + "_id", relationValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取系统控件缓存数据
     */
    public Map<String, Object> getlocalCache() {
        Map<String, Object> localCache = new HashMap<>();
        //读取系统控件 所需编码 id
        Map<String, Object> depMap = organizeApi.getOrgEncodeAndName("department");
        localCache.put("_dep_map", depMap);
        Map<String, Object> comMap = organizeApi.getOrgNameAndId("");
        localCache.put("_com_map", comMap);
        Map<String, String> posMap = positionApi.getPosFullNameMap();
        localCache.put("_pos_map", posMap);
        Map<String, Object> userMap = userApi.getUserNameAndIdMap();
        localCache.put("_user_map", userMap);
        Map<String, Object> roleMap = roleApi.getRoleNameAndIdMap();
        localCache.put("_role_map", roleMap);
        Map<String, Object> groupMap = groupApi.getGroupEncodeMap();
        localCache.put("_group_map", groupMap);
        Map<String, Object> allOrgsTreeName = organizeApi.getAllOrgsTreeName();
        localCache.put("_com_tree_map", allOrgsTreeName);
        return localCache;
    }

    /**
     * 获取接口api数据结果
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/1/10
     */
    public List<Map<String, Object>> getInterfaceData(VisualdevReleaseEntity visualdevEntity
            , PaginationModel paginationModel, ColumnDataModel columnDataModel) {
        List<Map<String, Object>> realList = new ArrayList<>();
        try {
            Map<String, Object> queryMap = JsonUtil.stringToMap(paginationModel.getQueryJson());

            //页签搜索
            Map<String, Object> extraMap = JsonUtil.stringToMap(paginationModel.getExtraQueryJson());
            extraMap = extraMap == null ? new HashMap<>() : extraMap;
            DataInterfaceEntity info = dataInterFaceApi.getInfo(visualdevEntity.getInterfaceId());
            //接口真分页
            if (info.getHasPage() == 1) {
                DataInterfacePage page = new DataInterfacePage();
                page.setCurrentPage(paginationModel.getCurrentPage());
                page.setPageSize(paginationModel.getPageSize());
                if ("1".equals(paginationModel.getDataType())) {
                    page.setCurrentPage(1);
                    page.setPageSize(99999999);
                }
                List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(visualdevEntity.getInterfaceParam(), DataInterfaceModel.class);
                for (DataInterfaceModel df : jsonToList) {
                    if (queryMap != null && queryMap.containsKey(df.getField()) && queryMap.get(df.getField()) != null
                            && StringUtil.isNotEmpty(queryMap.get(df.getField()).toString())) {
                        String thisValue = queryMap.get(df.getField()).toString();
                        if (extraMap.containsKey(df.getField())) {
                            thisValue = extraMap.get(df.getField()).toString();
                        }
                        if (Objects.equals(df.getSourceType(), 2)) {
                            df.setRelationField(thisValue);
                        }
                        df.setDefaultValue(thisValue);
                    } else if (extraMap.containsKey(df.getField())) {
                        String thisValue = extraMap.get(df.getField()).toString();
                        if (Objects.equals(df.getSourceType(), 2)) {
                            df.setRelationField(thisValue);
                        }
                        df.setDefaultValue(thisValue);
                    }
                }
                page.setParamList(jsonToList);
                ActionResult<Object> actionResult = dataInterFaceApi.infoToIdPageList(visualdevEntity.getInterfaceId(), page);
                if (actionResult.getCode() == 200) {
                    PageListVO<Map<String, Object>> data = JsonUtil.getJsonToBean(actionResult.getData(), PageListVO.class);
                    realList = data.getList();
                    PaginationVO pagination = data.getPagination();
                    paginationModel.setTotal(pagination.getTotal());
                }
            } else {
                Map<String, String> parameterMap = new HashMap<>();
                if (StringUtil.isNotEmpty(visualdevEntity.getInterfaceParam())) {
                    List<InterefaceParamModel> jsonToList = JsonUtil.getJsonToList(visualdevEntity.getInterfaceParam(), InterefaceParamModel.class);
                    Map<String, String> systemFieldValue = userApi.getSystemFieldValue(new SystemParamModel(visualdevEntity.getInterfaceParam()));
                    for (InterefaceParamModel mapStr : jsonToList) {
                        TemplateJsonModel jsonToBean = JsonUtil.getJsonToBean(mapStr, TemplateJsonModel.class);
                        String dataValue = paramSourceTypeReplaceValue(jsonToBean, systemFieldValue);
                        mapStr.setDefaultValue(dataValue);
                        if (mapStr.getUseSearch() != null && Objects.equals(mapStr.getUseSearch(), true)) {
                            Map<String, Object> keyJsonMap = queryMap;
                            if (keyJsonMap != null && keyJsonMap.get(mapStr.getField()) != null && StringUtil.isNotEmpty(keyJsonMap.get(mapStr.getField()).toString())) {
                                parameterMap.put(mapStr.getField(), keyJsonMap.get(mapStr.getField()).toString());
                            } else {
                                parameterMap.put(mapStr.getField(), null);
                            }
                        } else {
                            parameterMap.put(mapStr.getField(), mapStr.getDefaultValue());
                        }
                    }
                }

                //组装查询条件
                List<FieLdsModel> queryCondition = this.getQueryCondition(paginationModel, columnDataModel);
                //封装sql---sql普通查询塞参数到数据接口那边去组装sql
                OnlinePublicUtils.getViewQuerySql(info, queryCondition, parameterMap, extraMap);

                ActionResult<Object> dataInterfaceInfo = dataInterFaceApi.infoToId(visualdevEntity.getInterfaceId(), null, parameterMap);
                if (dataInterfaceInfo.getCode() == 200) {
                    List<Map<String, Object>> dataRes = (List<Map<String, Object>>) dataInterfaceInfo.getData();
                    //假查询条件-不为sql时查询在此过滤
                    List<Map<String, Object>> dataInterfaceList = OnlinePublicUtils.getViewQueryNotSql(info, queryCondition, dataRes, extraMap);

                    //判断是否有id没有则随机
                    dataInterfaceList.forEach(item -> {
                        if (item.get("id") == null) {
                            item.put("id", RandomUtil.uuId());
                        }
                        if (item.get("f_id") != null) {
                            item.put("id", item.get("f_id"));
                        }
                        if (item.get(KeyConst.CHILDREN) != null) {
                            item.remove(KeyConst.CHILDREN);
                        }
                    });

                    //排序
                    if (StringUtil.isNotEmpty(paginationModel.getSidx())) {
                        String[] split = paginationModel.getSidx().split(",");
                        Collections.sort(dataInterfaceList, (a, b) -> {
                            for (String sidx : split) {
                                String key = sidx;
                                boolean asc = true;
                                if (sidx.startsWith("-")) {
                                    key = sidx.substring(1);
                                    asc = false;
                                }
                                if (a.get(key) == null) {
                                    if (b.get(key) == null) {
                                        return 0;
                                    }
                                    return 1;
                                }
                                if (b.get(key) == null) {
                                    return -1;
                                }
                                if (!a.get(key).equals(b.get(key))) {
                                    return asc ? String.valueOf(b.get(key)).compareTo(String.valueOf(a.get(key))) :
                                            String.valueOf(a.get(key)).compareTo(String.valueOf(b.get(key)));
                                }
                            }
                            return 0;
                        });
                    }

                    if ("1".equals(paginationModel.getDataType())) {//导出全部数据用
                        return dataInterfaceList;
                    }
                    //假分页
                    if (Boolean.TRUE.equals(columnDataModel.getHasPage()) && CollUtil.isNotEmpty(dataInterfaceList)) {
                        List<List<Map<String, Object>>> partition = Lists.partition(dataInterfaceList, (int) paginationModel.getPageSize());
                        int i = (int) paginationModel.getCurrentPage() - 1;
                        realList = partition.size() > i ? partition.get(i) : Collections.emptyList();
                        paginationModel.setTotal(dataInterfaceList.size());
                    } else {
                        realList = dataInterfaceList;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("数据视图，接口请求失败!message={}", e.getMessage());
        }
        //数据添加随机id
        dataId(realList, columnDataModel.getViewKey());
        return realList;
    }

    /**
     * 数据添加随机id
     *
     * @param data
     * @param key
     */
    private void dataId(List<Map<String, Object>> data, String key) {
        for (Map<String, Object> item : data) {
            if (item.get("id") == null) {
                item.put("id", RandomUtil.uuId());
            }
            if (item.get("f_id") != null) {
                item.put("id", item.get("f_id"));
            }
            if (item.get(key) != null) {
                item.put("id", item.get(key));
            }

            if (item.get(KeyConst.CHILDREN) != null) {
                List<Map<String, Object>> children = new ArrayList<>();
                try {
                    children.addAll(JsonUtil.getJsonToListMap(String.valueOf(item.get(KeyConst.CHILDREN))));
                } catch (Exception e) {
                    log.error("子数据转换失败:" + e.getMessage());
                }
                if (!children.isEmpty()) {
                    dataId(children, null);
                    item.put(KeyConst.CHILDREN, children);
                }
            }
        }
    }

    public static List<Object> convertToList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        } else {
            List<Object> arrayList = new ArrayList<>();
            arrayList.add(obj);
            return arrayList;
        }
    }

    public static String convertValueToString(String obj, boolean mult, boolean isOrg) {
        if (StringUtil.isNotEmpty(obj)) {
            String prefix = "[";
            if (isOrg) {
                prefix = "[[";
            }
            if (mult) {
                if (!obj.startsWith(prefix)) {
                    JSONArray arr = new JSONArray();
                    if (isOrg) {
                        //组织多选为二维数组
                        arr.add(JSON.parse(obj));
                    } else {
                        arr.add(obj);
                    }
                    return arr.toJSONString();
                }
            } else {
                if (obj.startsWith(prefix)) {
                    JSONArray objects = JSON.parseArray(obj);
                    return !objects.isEmpty() ? objects.get(0).toString() : "";
                }
            }
        }
        return obj;
    }


    /**
     * 输入时表单时间字段根据格式转换去尾巴
     *
     * @param list 字段属性
     * @param map  数据
     */
    public static void swapDatetime(List<FieLdsModel> list, Map<String, Object> map) {
        List<FieLdsModel> fields = new ArrayList<>();
        FormPublicUtils.recursionFieldsExceptChild(fields, list);
        String dbType = "";
        try {
            @Cleanup Connection connection = DynamicDataSourceUtil.getCurrentConnection();
            dbType = connection.getMetaData().getDatabaseProductName().trim();
        } catch (Exception e) {
            log.error("建立数据库连接失败：" + e.getMessage(), e);
        }
        //主副表
        for (FieLdsModel field : fields) {
            String vModel = field.getVModel();
            String format = DateTimeFormatConstant.getFormat(field.getFormat());
            ConfigModel config = field.getConfig();
            if (map.get(vModel) != null) {
                String s = map.get(vModel).toString();
                if (StringUtils.isBlank(s) || "[]".equals(s) || "[[]]".equals(s)) {
                    map.replace(vModel, null);
                }
            }

            //SQL Server text字段先这样处理。
            if (map.get(vModel) == null && JnpfKeyConsts.getTextField().contains(config.getJnpfKey()) && "Microsoft SQL Server".equals(dbType)) {
                map.put(vModel, "");
            }
            if ((JnpfKeyConsts.DATE.equals(config.getJnpfKey()) || JnpfKeyConsts.DATE_CALCULATE.equals(config.getJnpfKey())) && map.get(vModel) != null) {
                Date date = new Date(Long.parseLong(String.valueOf(map.get(vModel))));
                String completionStr = "";
                switch (format) {
                    case "yyyy":
                        completionStr = "-01-01 00:00:00";
                        break;
                    case "yyyy-MM":
                        completionStr = "-01 00:00:00";
                        break;
                    case "yyyy-MM-dd":
                        completionStr = " 00:00:00";
                        break;
                    case "yyyy-MM-dd HH":
                        completionStr = ":00:00";
                        break;
                    case "yyyy-MM-dd HH:mm":
                        completionStr = ":00";
                        break;
                    default:
                        break;
                }
                String datestr = jnpf.util.DateUtil.dateToString(date, format);
                long time = jnpf.util.DateUtil.stringToDate(datestr + completionStr).getTime();
                map.replace(vModel, time);
            }
            if (JnpfKeyConsts.EDITOR.equals(config.getJnpfKey()) && map.get(vModel) != null) {
                map.replace(vModel, XSSEscape.escapeImgOnlyBase64(map.get(vModel).toString()));
            }
        }
        //子表
        for (FieLdsModel field : fields) {
            if (field.getVModel().toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                List<FieLdsModel> children = field.getConfig().getChildren();
                if (CollUtil.isNotEmpty(children)) {
                    String tableKey = field.getConfig().getTableName() + "List";
                    if (map.get(tableKey) != null) {
                        List<Object> listObj = (List) map.get(tableKey);
                        if (CollUtil.isNotEmpty(listObj)) {
                            List<Object> listObjNew = new ArrayList<>();
                            for (Object o : listObj) {
                                Map<String, Object> stringObjectMap = JsonUtil.entityToMap(o);
                                swapDatetime(children, stringObjectMap);
                                listObjNew.add(stringObjectMap);
                            }
                            if (CollUtil.isNotEmpty(listObjNew)) {
                                map.replace(tableKey, listObjNew);
                            }
                        }
                    }
                    String tableFieldKey = field.getVModel();
                    if (map.get(tableFieldKey) != null) {
                        List<Object> listObj = (List) map.get(tableFieldKey);
                        if (CollUtil.isNotEmpty(listObj)) {
                            List<Object> listObjNew = new ArrayList<>();
                            for (Object o : listObj) {
                                Map<String, Object> stringObjectMap = JsonUtil.entityToMap(o);
                                swapDatetime(children, stringObjectMap);
                                listObjNew.add(stringObjectMap);
                            }
                            if (CollUtil.isNotEmpty(listObjNew)) {
                                map.replace(tableFieldKey, listObjNew);
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * 视图条件组装
     *
     * @param paginationModel
     * @param columnDataModel
     * @return
     */
    public List<FieLdsModel> getQueryCondition(PaginationModel paginationModel, ColumnDataModel columnDataModel) {
        List<FieLdsModel> searchVOList = JsonUtil.getJsonToList(columnDataModel.getSearchList(), FieLdsModel.class);
        Map<String, Object> keyJsonMap = JsonUtil.stringToMap(paginationModel.getQueryJson());

        List<FieLdsModel> searchResList = new ArrayList<>();
        if (keyJsonMap == null) {
            return searchResList;
        }
        for (Map.Entry<String, Object> keyItem : keyJsonMap.entrySet()) {
            String key = keyItem.getKey();
            Object keyValue = keyItem.getValue();
            if (keyValue == null || ((keyValue) instanceof List && ((List<?>) keyValue).isEmpty())) {
                continue;
            }
            for (FieLdsModel item : searchVOList) {
                String vModel = item.getVModel();
                if (key.equals(vModel) && !Boolean.TRUE.equals(item.getConfig().getIsFromParam())) {
                    //非接口参数的条件
                    FieLdsModel model = BeanUtil.copyProperties(item, FieLdsModel.class);
                    String jnpfKey = model.getConfig().getJnpfKey();
                    switch (jnpfKey) {
                        case JnpfKeyConsts.COM_INPUT:
                            if (Objects.equals(model.getSearchType(), 3)) {
                                model.setSearchType(2);//单行输入范围调整为模糊
                            }
                            model.setFieldValue(String.valueOf(keyValue));
                            break;
                        case JnpfKeyConsts.NUM_INPUT:
                            model.setSearchType(3);//定义为between
                            List<Long> integerList = JsonUtil.getJsonToList(keyValue, Long.class);
                            model.setFieldValueOne(integerList.get(0));
                            model.setFieldValueTwo(integerList.get(1));
                            break;
                        case JnpfKeyConsts.DATE:
                        case JnpfKeyConsts.DATE_CALCULATE:
                            model.setSearchType(3);//定义为between
                            List<Long> dateList = JsonUtil.getJsonToList(keyValue, Long.class);
                            String timeOne = FormPublicUtils.getTimeFormat(jnpf.util.DateUtil.dateToString(new Date(dateList.get(0)), model.getFormat()));
                            String timeTwo = FormPublicUtils.getTimeFormat(jnpf.util.DateUtil.dateToString(new Date(dateList.get(1)), model.getFormat()));
                            model.setFieldValueOne(timeOne);
                            model.setFieldValueTwo(timeTwo);
                            break;
                        case JnpfKeyConsts.TIME:
                            model.setSearchType(3);//定义为between
                            List<String> stringList = JsonUtil.getJsonToList(keyValue, String.class);
                            model.setFieldValueOne(stringList.get(0));
                            model.setFieldValueTwo(stringList.get(1));
                            break;
                        case JnpfKeyConsts.SELECT:
                        case JnpfKeyConsts.ROLESELECT:
                        case JnpfKeyConsts.GROUPSELECT:
                            model.setSearchType(4);
                            List<String> dataList = new ArrayList<>();
                            try {
                                List<String> list = JsonUtil.getJsonToList(keyValue, String.class);
                                dataList.addAll(list);
                            } catch (Exception e1) {
                                dataList.add(String.valueOf(keyValue));
                            }
                            model.setDataList(dataList);
                            break;
                        case JnpfKeyConsts.POSSELECT:
                            model.setSearchType(4);
                            List<String> listPos = new ArrayList<>();
                            if (Boolean.TRUE.equals(model.getSearchMultiple())) {
                                listPos = JsonUtil.getJsonToBean(keyValue, List.class);
                            } else {
                                String posId = JsonUtil.getJsonToBean(keyValue, String.class);
                                listPos.add(posId);
                            }
                            //包含子岗位
                            if (Objects.equals(model.getSelectRange(), "2")) {
                                List<PositionEntity> childList = positionApi.getListByParentIds(listPos);
                                listPos.addAll(childList.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                                //包含子孙岗位
                            } else if (Objects.equals(model.getSelectRange(), "3")) {
                                List<PositionEntity> childList = positionApi.getProgeny(listPos, 1);
                                listPos.addAll(childList.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                            }
                            model.setDataList(listPos);
                            break;
                        case JnpfKeyConsts.COMSELECT:
                            model.setSearchType(4);
                            List<String> listOrg = new ArrayList<>();
                            if (Boolean.TRUE.equals(model.getSearchMultiple())) {
                                listOrg = JsonUtil.getJsonToBean(keyValue, List.class);
                            } else {
                                String orgId = JsonUtil.getJsonToBean(keyValue, String.class);
                                listOrg.add(orgId);
                            }
                            //包含子组织
                            if (Objects.equals(model.getSelectRange(), "2")) {
                                List<OrganizeEntity> childList = organizeApi.getListByParentIds(listOrg);
                                listOrg.addAll(childList.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                                //包含子孙组织
                            } else if (Objects.equals(model.getSelectRange(), "3")) {
                                List<OrganizeEntity> childList = organizeApi.getProgeny(listOrg, 1);
                                listOrg.addAll(childList.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                            }
                            model.setDataList(listOrg);
                            break;
                        case JnpfKeyConsts.USERSELECT:
                            model.setSearchType(4);
                            List<String> listUser = new ArrayList<>();
                            if (Boolean.TRUE.equals(model.getSearchMultiple())) {
                                List<String> list = JsonUtil.getJsonToBean(keyValue, List.class);
                                listUser.addAll(list);
                            } else {
                                listUser.add(String.valueOf(keyValue));
                            }
                            //包含当前用户及下属
                            if (CollUtil.isNotEmpty(listUser)) {
                                List<String> posIds = userRelationService.getListByUserIdAll(listUser).stream()
                                        .filter(t -> PermissionConst.POSITION.equals(t.getObjectType()))
                                        .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
                                if (Objects.equals(model.getSelectRange(), "2")) {
                                    List<UserEntity> childList = userRelationService.getUserAndSub(posIds, null);
                                    listUser.addAll(childList.stream().map(UserEntity::getId).collect(Collectors.toList()));
                                    //包含子孙用户
                                } else if (Objects.equals(model.getSelectRange(), "3")) {
                                    List<UserEntity> childList = userRelationService.getUserProgeny(posIds, null);
                                    listUser.addAll(childList.stream().map(UserEntity::getId).collect(Collectors.toList()));
                                }
                            }

                            model.setDataList(listUser);
                            break;
                        default:
                            model.setFieldValue(String.valueOf(keyValue));
                            break;
                    }
                    searchResList.add(model);
                }
            }
        }
        return searchResList;
    }

    /**
     * 获取默认数据和下拉列表map
     *
     * @param formJson  表单设计json
     * @param selectKey 选中字段key
     */
    public ExcelModel getDefaultValue(String formJson, List<String> selectKey) {
        FormDataModel formDataModel = JsonUtil.getJsonToBean(formJson, FormDataModel.class);
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
        List<FieLdsModel> allFieLds = new ArrayList<>();
        VisualUtils.recursionFields(fieLdsModels, allFieLds);
        Map<String, String[]> optionMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        List<ExcelColumnAttr> models = new ArrayList<>();

        for (String s : selectKey.stream().filter(s -> !s.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList())) {
            FieLdsModel fieLdsModel = allFieLds.stream().filter(c -> c.getVModel().equals(s)).findFirst().orElse(null);
            assert fieLdsModel != null;
            fieLdsModel.setId(fieLdsModel.getVModel());
            models.add(ExcelColumnAttr.builder().key(fieLdsModel.getVModel()).name(fieLdsModel.getLabel()).require(fieLdsModel.getConfig().isRequired()).fontColor(IndexedColors.RED.getIndex()).build());

            dataMap.put(s, VisualUtils.exampleExcelMessage(fieLdsModel));
            String[] options = getOptions(fieLdsModel);
            if (options != null && options.length > 0) {
                optionMap.put(fieLdsModel.getVModel(), options);
            }
        }
        List<FieLdsModel> childFields = allFieLds.stream().filter(f -> f.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).collect(Collectors.toList());
        for (FieLdsModel child : childFields) {
            List<String> childList = selectKey.stream().filter(s -> s.startsWith(child.getVModel())).collect(Collectors.toList());
            childList.forEach(c -> c.replace(child.getVModel() + "-", ""));
            List<FieLdsModel> children = child.getConfig().getChildren();
            List<Map<String, Object>> childData = new ArrayList<>();
            Map<String, Object> childMap = new HashMap<>();
            for (String cl : childList) {
                String substring = cl.substring(cl.indexOf("-") + 1);
                FieLdsModel fieLdsModel = children.stream().filter(c -> c.getVModel().equals(substring)).findFirst().orElse(null);
                assert fieLdsModel != null;
                String tableModel = fieLdsModel.getConfig().getParentVModel() != null ? fieLdsModel.getConfig().getParentVModel() : child.getVModel();
                String id = tableModel + "-" + fieLdsModel.getVModel();
                fieLdsModel.setId(id);
                models.add(ExcelColumnAttr.builder().key(cl).name(fieLdsModel.getLabel()).require(fieLdsModel.getConfig().isRequired()).fontColor(IndexedColors.RED.getIndex()).build());
                childMap.put(substring, VisualUtils.exampleExcelMessage(fieLdsModel));
                String[] options = getOptions(fieLdsModel);
                if (options != null && options.length > 0) {
                    optionMap.put(id, options);
                }
            }
            childData.add(childMap);
            dataMap.put(child.getVModel(), childData);
        }
        return ExcelModel.builder().selectKey(selectKey).models(models).dataMap(dataMap).optionMap(optionMap).build();
    }

    /**
     * 根据配置获取下拉列表
     *
     * @param model
     * @return
     */
    public String[] getOptions(FieLdsModel model) {
        ConfigModel config = model.getConfig();
        String jnpfKey = config.getJnpfKey();
        String[] options = null;
        if (JnpfKeyConsts.SWITCH.equals(jnpfKey)) {
            options = new String[2];
            options[0] = model.getActiveTxt();
            options[1] = model.getInactiveTxt();
        }
        boolean multiple = model.getMultiple();
        if ((JnpfKeyConsts.SELECT.equals(jnpfKey) && !multiple) || JnpfKeyConsts.RADIO.equals(jnpfKey)) {
            String selectOptions = model.getOptions();
            PropsModel props = model.getProps();
            String labelkey = props.getLabel();
            String dataType = model.getConfig().getDataType();
            if ("static".equals(dataType)) {
                List<Map<String, Object>> list = JSON.parseObject(selectOptions, new TypeReference<List<Map<String, Object>>>() {
                });
                options = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    String label = (String) map.get(props.getLabel());
                    options[i] = label;
                }
            } else if ("dictionary".equals(dataType)) {
                List<DictionaryDataEntity> list = dictionaryDataApi.getList(model.getConfig().getDictionaryType(), true);
                options = list.stream().map(DictionaryDataEntity::getFullName).collect(Collectors.toList()).toArray(new String[0]);
            } else if ("dynamic".equals(dataType)) {
                ActionResult<Object> result = dataInterFaceApi.infoToId(model.getConfig().getPropsUrl(), null, new HashMap<>());
                if (result != null && result.getData() != null && result.getData() instanceof List) {
                    List<Map<String, Object>> arr = (List) result.getData();
                    options = new String[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        Map<String, Object> data = arr.get(i);
                        options[i] = (String) data.get(labelkey);
                    }
                }
            }
        }
        return options;
    }

    /**
     * 获取流程任务状态
     *
     * @param list 功能数据列表
     * @throws WorkFlowException
     */
    public void getFlowStatus(List<Map<String, Object>> list) {
        for (Map<String, Object> item : list) {
            getFLowFields(item);
            //流程状态添加
            if (item != null && item.get(FlowFormConstant.FLOW_STATE) == null) {
                Object flowState = null;
                if (flowState == null && item.get(TableFeildsEnum.FLOWSTATE.getField()) != null) {
                    flowState = item.get(TableFeildsEnum.FLOWSTATE.getField());
                }
                if (flowState == null && item.get(TableFeildsEnum.FLOWSTATE.getField().toUpperCase()) != null) {
                    flowState = item.get(TableFeildsEnum.FLOWSTATE.getField().toUpperCase());
                }
                if (flowState == null && item.get(FlowFormConstant.FLOW_STATE.toUpperCase()) != null) {
                    flowState = item.get(FlowFormConstant.FLOW_STATE.toUpperCase());
                }
                if (flowState == null && item.get(FlowFormConstant.FLOW_STATE.toLowerCase()) != null) {
                    flowState = item.get(FlowFormConstant.FLOW_STATE.toLowerCase());
                }
                if (flowState == null) {
                    flowState = 0;
                } else {
                    flowState = flowState instanceof BigDecimal ? ((BigDecimal) flowState).intValue() : Integer.parseInt(flowState.toString());
                }
                item.put(FlowFormConstant.FLOW_STATE, flowState);
            }
        }
    }

    /**
     * 获取流程相关字段
     *
     * @param item
     */
    public void getFLowFields(Map<String, Object> item) {
        Map<String, Object> t = new CaseInsensitiveMap<>(item);
        String flowTaskId = "";
        if (t.get(FlowFormConstant.FLOWTASKID) != null) {
            flowTaskId = t.get(FlowFormConstant.FLOWTASKID).toString();
        }
        if (t.get(TableFeildsEnum.FLOWTASKID.getField()) != null) {
            flowTaskId = t.get(TableFeildsEnum.FLOWTASKID.getField()).toString();
        }
        item.put(FlowFormConstant.FLOWTASKID, flowTaskId);
        String flowId = "";
        if (t.get(FlowFormConstant.FLOWID) != null) {
            flowId = t.get(FlowFormConstant.FLOWID).toString();
        }
        if (t.get(TableFeildsEnum.FLOWID.getField()) != null) {
            flowId = t.get(TableFeildsEnum.FLOWID.getField()).toString();
        }
        item.put(FlowFormConstant.FLOWID, flowId);
    }

    /**
     * 主附表单行输入验证
     */
    public void checkUnique(List<FieLdsModel> modelList, Map<String, Object> data, List<String> errList, ImportFormCheckUniqueModel uniqueModel) {
        String baseType = "";
        try {
            DbLinkEntity linkEntity = uniqueModel.getLinkEntity();
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection connection = DynamicDataSourceUtil.getCurrentConnection();
            baseType = connection.getMetaData().getDatabaseProductName().trim();
        } catch (Exception e) {
            log.error("唯一验证数据链接失败：" + e.getMessage(), e);
        }

        for (int i = 0; i < modelList.size(); i++) {
            FieLdsModel swapDataVo = modelList.get(i);
            String jnpfKey = swapDataVo.getConfig().getJnpfKey();
            Object valueO = data.get(swapDataVo.getVModel());
            String label = swapDataVo.getConfig().getLabel();
            String value = String.valueOf(valueO);
            boolean unique = swapDataVo.getConfig().getUnique();
            if (ObjectUtil.isNotEmpty(valueO) && JnpfKeyConsts.COM_INPUT.equals(jnpfKey) && uniqueModel.isMain()) {
                boolean isBreak = false;
                if (StringUtil.isNotEmpty(swapDataVo.getMaxlength()) && value.length() > Integer.valueOf(swapDataVo.getMaxlength())) {
                    errList.set(i, label + "值超出最多输入字符限制");
                    isBreak = true;
                }
                if (unique && eachCheck(errList, uniqueModel, value, swapDataVo, i, baseType)) {
                    isBreak = true;
                }
                if (isBreak) {
                    break;
                }
            }
        }
    }

    private boolean eachCheck(List<String> errList, ImportFormCheckUniqueModel uniqueModel, String value, FieLdsModel swapDataVo, int i, String databaseProductName) {
        String label = swapDataVo.getConfig().getLabel();
        try {
            List<TableModel> tableList = uniqueModel.getTableModelList();
            //表格中出现多个的唯一值判断
            boolean exists = false;
            List<ImportDataModel> importDataModel = uniqueModel.getImportDataModel();
            List<Map<String, Object>> successList = importDataModel.stream().map(ImportDataModel::getResultData).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(successList)) {
                for (int uniqueIndex = 0; uniqueIndex < successList.size(); uniqueIndex++) {
                    if (value.equals(String.valueOf(successList.get(uniqueIndex).get(swapDataVo.getVModel())))) {
                        errList.set(i, label + "值已存在");
                        exists = true;
                        break;
                    }
                }
            }
            if (exists) {
                return true;
            }
            String tableName = Optional.ofNullable(swapDataVo.getConfig().getRelationTable()).orElse(swapDataVo.getConfig().getTableName());
            //验证唯一
            SqlTable sqlTable = SqlTable.of(tableName);
            TableModel thisModel = tableList.stream().filter(t -> t.getTable().equals(tableName)).findFirst().orElse(null);
            String key = flowDataUtil.getKey(thisModel, tableName);
            String vModelThis = swapDataVo.getVModel();

            String foriegKey = "";
            String columnName = "";
            boolean isMain = uniqueModel.isMain();
            TableModel mainTableModel = new TableModel();
            TableModel tableModel = new TableModel();
            for (TableModel item : tableList) {
                if (Objects.equals(item.getTypeId(), "1")) {
                    mainTableModel = item;
                }

                if (StringUtil.isNotEmpty(swapDataVo.getConfig().getRelationTable())) {
                    //子表判断
                    if (swapDataVo.getConfig().getRelationTable().equals(item.getTable())) {
                        tableModel = item;
                    }
                } else {
                    //主副表判断
                    if (swapDataVo.getConfig().getTableName().equals(item.getTable())) {
                        tableModel = item;
                    }
                }
            }

            if (tableModel != null) {
                String fieldName = vModelThis;
                if (vModelThis.contains(JnpfConst.SIDE_MARK)) {
                    fieldName = vModelThis.split(JnpfConst.SIDE_MARK)[1];
                    isMain = false;
                    foriegKey = tableModel.getTableField();
                }
                String finalFieldName = fieldName;
                TableFields tableFields = tableModel.getFields().stream().filter(t -> t.getField().equals(finalFieldName)).findFirst().orElse(null);
                if (tableFields != null) {
                    columnName = StringUtil.isNotEmpty(tableFields.getField()) ? tableFields.getField() : fieldName;
                }
            }

            List<BasicColumn> selectKey = new ArrayList<>();
            selectKey.add(sqlTable.column(columnName));
            selectKey.add(sqlTable.column(key));
            if (StringUtil.isNotEmpty(foriegKey)) {
                String finalForiegKey = foriegKey;
                TableFields tableFields = tableModel.getFields().stream().filter(t -> t.getField().equals(finalForiegKey)).findFirst().orElse(null);
                if (tableFields != null) {
                    foriegKey = StringUtil.isNotEmpty(tableFields.getField()) ? tableFields.getField() : finalForiegKey;
                }
                selectKey.add(sqlTable.column(foriegKey));
            }

            SqlTable sqlMainTable = SqlTable.of(mainTableModel.getTable());
            String mainKey = flowDataUtil.getKey(mainTableModel, databaseProductName);
            String taskIdField = TableFeildsEnum.FLOWTASKID.getField();
            if (databaseProductName.contains("Oracle") || databaseProductName.contains("DM DBMS")) {
                taskIdField = TableFeildsEnum.FLOWTASKID.getField().toUpperCase();
            }
            if (StringUtil.isNotEmpty(uniqueModel.getFlowId())) {
                selectKey.add(sqlMainTable.column(taskIdField));
            }

            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where;
            //是否主表
            if (isMain) {
                where = SqlBuilder
                        .select(selectKey)
                        .from(sqlTable)
                        .where(sqlTable.column(columnName), SqlBuilder.isEqualTo(value));
            } else {
                where = SqlBuilder
                        .select(selectKey)
                        .from(sqlMainTable)
                        .leftJoin(sqlTable)
                        .on(sqlTable.column(tableModel.getTableField()), new EqualTo(sqlMainTable.column(tableModel.getRelationField())))
                        .where(sqlTable.column(columnName), SqlBuilder.isEqualTo(value));
            }
            //是流程
            if (StringUtil.isNotEmpty(uniqueModel.getFlowId())) {
                where.and(sqlMainTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isIn(uniqueModel.getFlowIdList()));
            } else {
                where.and(sqlMainTable.column(TableFeildsEnum.FLOWID.getField()), SqlBuilder.isNull());
            }
            //开启逻辑删除
            if (Boolean.TRUE.equals(uniqueModel.getLogicalDelete())) {
                where.and(sqlMainTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
            }

            //业务主键存在的话需要剔除当前数据
            if (StringUtil.isNotEmpty(uniqueModel.getId())) {
                where.and(sqlTable.column(mainKey), SqlBuilder.isNotEqualTo(uniqueModel.getId()));
            }

            SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
            List<Map<String, Object>> mapList = flowFormDataMapper.selectManyMappedRows(render);
            int count = mapList.size();
            if (count > 0) {
                errList.set(i, label + "值已存在");
                return true;
            }
        } catch (Exception e) {
            errList.set(i, label + "值不正确");
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return false;
    }


    public VisualdevModelDataInfoVO getDetailsDataInfo(String id, VisualdevEntity visualdevEntity, OnlineInfoModel infoModel) {
        VisualdevModelDataInfoVO vo = new VisualdevModelDataInfoVO();
        Map<String, Object> allDataMap = new HashMap<>();
        Map<String, Object> allDataResMap = new HashMap<>();
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        boolean logicalDelete = formData.getLogicalDelete();
        //权限参数
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        boolean needP = false;
        List<String> formPerList = new ArrayList<>();
        if (columnDataModel != null && StringUtil.isNotEmpty(infoModel.getMenuId())) {
            needP = columnDataModel.getUseFormPermission();
            Map<String, Object> pMap = PermissionInterfaceImpl.getFormMap();
            if (pMap.get(infoModel.getMenuId()) != null) {
                formPerList = JsonUtil.getJsonToList(pMap.get(infoModel.getMenuId()), ModuleFormModel.class).stream()
                        .map(ModuleFormModel::getEnCode).collect(Collectors.toList());
            }
        }

        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModelList = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        List<FormAllModel> formAllModel = new ArrayList<>();
        if (CollUtil.isNotEmpty(infoModel.getFormAllModel())) {
            formAllModel = infoModel.getFormAllModel();
        } else {
            RecursionForm recursionForm = new RecursionForm(list, tableModelList);
            FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        }
        //form的属性
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        List<String> relationFiled = Arrays.asList(JnpfKeyConsts.RELATIONFORM, JnpfKeyConsts.RELATIONFORM_ATTR);

        TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);

        DbLinkEntity linkEntity = "0".equals(visualdevEntity.getDbLinkId()) ? null : dblinkService.getInfo(visualdevEntity.getDbLinkId());
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            String databaseProductName = conn.getMetaData().getDatabaseProductName();
            String dbType = conn.getMetaData().getDatabaseProductName().trim();
            boolean toUpperCase = databaseProductName.equalsIgnoreCase("oracle") || databaseProductName.equalsIgnoreCase("DM DBMS");
            //获取主键
            TableFields tableFields = mainTable.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                    && !t.getField().toLowerCase().contains(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
            String pKeyName;
            if (Objects.nonNull(tableFields)) {
                pKeyName = tableFields.getField();
            } else {
                pKeyName = toUpperCase ? TableFeildsEnum.FID.getField().toUpperCase() : TableFeildsEnum.FID.getField();
            }
            SqlTable mainSqlTable = SqlTable.of(mainTable.getTable());
            //查询主表数据
            Map<String, Object> mainAllMap = searchMainData(id, infoModel, mainTable, tableModelList);
            if (mainAllMap.size() == 0) {
                return vo;
            }
            //是否去除关联表单及关联表单字段
            if (!infoModel.isNeedRlationFiled()) {
                mast = mast.stream().filter(t -> !relationFiled.contains(t.getFormColumnModel().getFieLdsModel().getConfig().getJnpfKey())).collect(Collectors.toList());
            }
            //主表
            List<String> mainTableFields = mast.stream().filter(m -> StringUtil.isNotEmpty(m.getFormColumnModel().getFieLdsModel().getVModel()))
                    .map(s -> s.getFormColumnModel().getFieLdsModel().getVModel()).collect(Collectors.toList());
            //开启权限移除字段
            if (needP) {
                if (CollUtil.isEmpty(formPerList)) {
                    mainTableFields = Collections.emptyList();
                } else {
                    List<String> newList = new ArrayList<>();
                    for (String item : mainTableFields) {
                        if (formPerList.contains(item)) {
                            newList.add(item);
                        }
                    }
                    mainTableFields = newList;
                }
            }
            List<BasicColumn> mainTableBasicColumn = mainTableFields.stream().map(m -> SqlTable.of(mainTable.getTable()).column(m)).collect(Collectors.toList());
            //无字段时查询主键
            mainTableBasicColumn.add(SqlTable.of(mainTable.getTable()).column(pKeyName));

            SelectStatementProvider mainRender = SqlBuilder.select(mainTableBasicColumn).from(mainSqlTable).where(mainSqlTable.column(pKeyName),
                    SqlBuilder.isEqualTo(mainAllMap.get(pKeyName))).build().render(RenderingStrategies.MYBATIS3);
            List<Map<String, Object>> mapList = flowFormDataMapper.selectManyMappedRows(mainRender);

            if (ObjectUtil.isNotEmpty(mapList) && !mapList.isEmpty()) {
                allDataMap.putAll(mapList.get(0));
            }

            //列表子表
            Map<String, List<FormMastTableModel>> groupByTableNames = mastTable.stream().map(mt -> mt.getFormMastTableModel()).collect(Collectors.groupingBy(ma -> ma.getTable()));
            Iterator<Map.Entry<String, List<FormMastTableModel>>> entryIterator = groupByTableNames.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, List<FormMastTableModel>> next = entryIterator.next();
                String childTableName = next.getKey();
                List<FormMastTableModel> childMastTableList = next.getValue();
                //是否去除关联表单及关联表单字段
                if (!infoModel.isNeedRlationFiled()) {
                    childMastTableList = childMastTableList.stream().filter(t -> !relationFiled.contains(t.getMastTable().getFieLdsModel().getConfig().getJnpfKey())).collect(Collectors.toList());
                }
                //开启权限移除字段
                if (needP) {
                    if (CollUtil.isEmpty(formPerList)) {
                        childMastTableList = Collections.emptyList();
                    } else {
                        List<FormMastTableModel> newList = new ArrayList<>();
                        for (FormMastTableModel item : childMastTableList) {
                            if (formPerList.contains(item.getVModel())) {
                                newList.add(item);
                            }
                        }
                        childMastTableList = newList;
                    }
                }
                TableModel childTableModel = tableModelList.stream().filter(t -> t.getTable().equals(childTableName)).findFirst().orElse(null);
                SqlTable mastSqlTable = SqlTable.of(childTableName);
                List<BasicColumn> mastTableBasicColumn = childMastTableList.stream().filter(m -> StringUtil.isNotEmpty(m.getField()))
                        .map(m -> mastSqlTable.column(m.getField())).collect(Collectors.toList());
                //添加副表关联字段，不然数据会空没有字段名称
                mastTableBasicColumn.add(mastSqlTable.column(childTableModel.getTableField()));

                //主表主键
                String mainField = childTableModel.getRelationField();
                Object mainValue = new CaseInsensitiveMap<>(mainAllMap).get(mainField);
                //子表外键
                String childFoIdFiled = childTableModel.getTableField();
                //外键字段是否varchar转换
                TableFields fogIdField = childTableModel.getFields().stream().filter(t -> t.getField().equals(childFoIdFiled)).findFirst().orElse(null);
                boolean fogIdTypeString = Objects.nonNull(fogIdField) && fogIdField.getDataType().toLowerCase().contains("varchar");
                if (fogIdTypeString) {
                    mainValue = mainValue.toString();
                }

                SelectStatementProvider mastRender = SqlBuilder.select(mastTableBasicColumn).from(mastSqlTable).where(mastSqlTable.column(childFoIdFiled),
                        SqlBuilder.isEqualTo(mainValue)).build().render(RenderingStrategies.MYBATIS3);
                List<Map<String, Object>> childMapList = flowFormDataMapper.selectManyMappedRows(mastRender);
                if (CollUtil.isNotEmpty(childMapList)) {
                    Map<String, Object> soloDataMap = childMapList.get(0);
                    Map<String, Object> renameKeyMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : soloDataMap.entrySet()) {
                        FormMastTableModel model = childMastTableList.stream().filter(child -> child.getField().equalsIgnoreCase(String.valueOf(entry.getKey()))).findFirst().orElse(null);
                        if (model != null) {
                            renameKeyMap.put(model.getVModel(), entry.getValue());
                        }
                    }
                    List<Map<String, Object>> mapList1 = new ArrayList<>();
                    mapList1.add(renameKeyMap);
                    allDataMap.putAll(mapList1.get(0));
                }
            }

            //设计子表
            boolean finalNeedP = needP;
            List<String> finalFormPerList = formPerList;
            table.stream().map(t -> t.getChildList()).forEach(
                    t1 -> {
                        String childTableName = t1.getTableName();
                        TableModel tableModel = tableModelList.stream().filter(tm -> tm.getTable().equals(childTableName)).findFirst().orElse(null);
                        SqlTable childSqlTable = SqlTable.of(childTableName);
                        List<FormColumnModel> chilFieldList = t1.getChildList().stream().filter(t2 -> StringUtil.isNotEmpty(t2.getFieLdsModel().getVModel())).collect(Collectors.toList());
                        String tableModelName = t1.getTableModel();
                        //开启权限移除字段
                        if (finalNeedP) {
                            if (CollUtil.isEmpty(finalFormPerList)) {
                                chilFieldList = Collections.emptyList();
                            } else {
                                List<FormColumnModel> newList = new ArrayList<>();
                                for (FormColumnModel item : chilFieldList) {
                                    if (finalFormPerList.contains(tableModelName + "-" + item.getFieLdsModel().getVModel())) {
                                        newList.add(item);
                                    }
                                }
                                chilFieldList = newList;
                            }
                        }
                        List<BasicColumn> childFields = chilFieldList.stream().map(t2 -> childSqlTable.column(t2.getFieLdsModel().getVModel())).collect(Collectors.toList());

                        childFields.add(childSqlTable.column(tableModel.getTableField()));
                        String childKeyName = flowDataUtil.getKey(tableModel, dbType);
                        childFields.add(childSqlTable.column(childKeyName));
                        //主表主键
                        String mainField = tableModel.getRelationField();
                        Object mainValue = new CaseInsensitiveMap<>(mainAllMap).get(mainField);
                        //子表外键
                        String childFoIdFiled = tableModel.getTableField();
                        //外键字段是否varchar转换
                        TableFields fogIdField = tableModel.getFields().stream().filter(t -> t.getField().equals(childFoIdFiled)).findFirst().orElse(null);
                        boolean fogIdTypeString = Objects.nonNull(fogIdField) && fogIdField.getDataType().toLowerCase().contains("varchar");
                        if (fogIdTypeString) {
                            mainValue = mainValue.toString();
                        }

                        QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder childWhere = SqlBuilder.select(childFields).from(childSqlTable).where();
                        childWhere.and(childSqlTable.column(tableModel.getTableField()), SqlBuilder.isEqualTo(mainValue));
                        //逻辑删除不展示
                        if (logicalDelete) {
                            childWhere.and(childSqlTable.column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
                        }
                        SelectStatementProvider childRender = childWhere.build().render(RenderingStrategies.MYBATIS3);
                        List<Map<String, Object>> childMapList = flowFormDataMapper.selectManyMappedRows(childRender);
                        if (ObjectUtil.isNotEmpty(childMapList)) {
                            Map<String, Object> childMap = new HashMap<>(1);
                            childMap.put(t1.getTableModel(), childMapList);
                            allDataMap.putAll(childMap);
                        }
                    }
            );
            //数据转换
            List<FieLdsModel> fields = new ArrayList<>();
            OnlinePublicUtils.recursionFields(fields, list);

            //添加id属性
            List<Map<String, Object>> dataList = FormPublicUtils.addIdToList(Arrays.asList(allDataMap), pKeyName);
            //详情没有区分行内编辑
            if (infoModel.isNeedSwap()) {
                allDataResMap = this.getSwapInfo(dataList, fields, visualdevEntity.getId(), false, null).get(0);
            } else {
                allDataResMap = allDataMap;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataException(e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        vo.setId(allDataResMap.get(FlowFormConstant.ID));
        vo.setData(JsonUtilEx.getObjectToString(allDataResMap));
        return vo;
    }

    /**
     * 根据指定字段查询主表数据
     *
     * @param id
     * @param model
     * @param mainTable
     * @param tableModelList
     * @return
     */
    private Map<String, Object> searchMainData(String id, OnlineInfoModel model, TableModel mainTable, List<TableModel> tableModelList) {
        SqlTable mainSqlTable = SqlTable.of(mainTable.getTable());
        TableFields mainKeyModel = mainTable.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                && !t.getField().toLowerCase().contains(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
        if (mainKeyModel == null) {
            throw new DataException("表不存在");
        }
        String propsValue = model.getPropsValue();
        SqlColumn<Object> column;
        TableFields storedFieldModel;
        if (StringUtil.isNotEmpty(propsValue)) {
            if (propsValue.contains(JnpfConst.SIDE_MARK)) {
                String[] split = propsValue.split(JnpfConst.SIDE_MARK);
                String thisTable = split[0].substring(5);
                String thisField = split[1];
                TableModel thisTableModel = tableModelList.stream().filter(t -> t.getTable().equalsIgnoreCase(thisTable)).findFirst().orElse(null);
                if (thisTableModel == null) {
                    throw new DataException("表不存在");
                }
                storedFieldModel = thisTableModel.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(thisField)).findFirst().orElse(null);
                if (storedFieldModel == null) {
                    throw new DataException("字段不存在");
                }
                SqlTable sqlTable = SqlTable.of(split[0].substring(5));
                column = sqlTable.column(storedFieldModel.getField());
            } else {
                storedFieldModel = mainTable.getFields().stream().filter(t -> t.getField().equalsIgnoreCase(propsValue)).findFirst().orElse(null);
                if (storedFieldModel == null) {
                    throw new DataException("字段不存在");
                }
                column = mainSqlTable.column(storedFieldModel.getField());
            }
        } else {
            storedFieldModel = mainKeyModel;
            column = mainSqlTable.column(storedFieldModel.getField());
        }
        //查询的字段-字段类型转换
        Object idObj = id;
        if (VisualConst.DB_INT_ALL.contains(storedFieldModel.getDataType().toLowerCase())) {
            idObj = Long.parseLong(id);
        }

        QueryExpressionDSL<SelectModel> from = SqlBuilder.select(mainSqlTable.allColumns()).from(mainSqlTable);
        QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = from.where(column, SqlBuilder.isEqualTo(idObj));
        SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
        List<Map<String, Object>> maps = flowFormDataMapper.selectManyMappedRows(render);
        if (CollUtil.isNotEmpty(maps)) {
            return maps.get(0);
        }
        return new HashMap<>();
    }
}
