package jnpf.onlinedev.util;

import com.alibaba.fastjson.JSONArray;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.model.datainterface.DataInterfaceModel;
import jnpf.base.model.datainterface.DataInterfacePage;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DictionaryDataService;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TemplateJsonModel;
import jnpf.onlinedev.model.enums.CacheKeyEnum;
import jnpf.onlinedev.model.enums.OnlineDataTypeEnum;
import jnpf.permission.service.*;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import jnpf.util.ThreadPoolExecutorUtil;
import jnpf.util.data.DataSourceContextHolder;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static jnpf.onlinedev.util.OnlineSwapDataUtils.NEEDCACHE_REMOTE;

/**
 * 在线开发数据缓存获取，多线程
 *
 * @author JNPF开发平台组
 * @version V3.5.x
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2023/12/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineExecutor {
    private final RedisUtil redisUtil;
    private final UserService userApi;
    private final OrganizeService organizeApi;
    private final PositionService positionApi;
    private final RoleService roleApi;
    private final GroupService groupApi;
    private final DictionaryDataService dictionaryDataApi;
    private final DataInterfaceService dataInterFaceApi;

    private String dsName = "";
    private static final long DEFAULT_CACHE_TIME = OnlineSwapDataUtils.DEFAULT_CACHE_TIME;
    private static final String KEY_USER = "user";
    private static final String KEY_ORG = "org";
    private static final String KEY_POS = "pos";
    private static final String KEY_ROLE = "role";
    private static final String KEY_GROUP = "group";
    private static final String KEY_POP = "pop";
    private static final String KEY_SELECT = "select";
    private static final String KEY_DATATYPE = "datatype";
    private static final String KEY_ORGTREE = "orgtree";

    /**
     * 遍历需要多线程缓存
     */
    public void executorRedis(Map<String, Object> localCache, List<FieLdsModel> swapDataVoList, String visualDevId, boolean inlineEdit,
                              List<Map<String, Object>> list, Map<String, Object> mainAndMast) {
        dsName = Optional.ofNullable(DataSourceContextHolder.getDatasourceId()).orElse("");
        Map<String, OnlineExecutorParam> listExecutor = new HashMap<>();
        for (int x = 0; x < list.size(); x++) {
            Map<String, Object> dataMap = list.get(x);
            if (dataMap == null) {
                continue;
            }
            for (FieLdsModel swapDataVo : swapDataVoList) {
                String jnpfKey = swapDataVo.getConfig().getJnpfKey();
                if (StringUtil.isEmpty(swapDataVo.getVModel())) {
                    continue;
                }
                String dataType = swapDataVo.getConfig().getDataType();
                String redisKey;
                boolean needUser = false;
                boolean needOrg = false;
                boolean needPos = false;
                boolean needRole = false;
                boolean needGroup = false;
                boolean needOrgTree = false;

                switch (jnpfKey) {
                    //用户组件
                    case JnpfKeyConsts.USERSELECT:
                        //创建用户
                    case JnpfKeyConsts.CREATEUSER:
                        //修改用户
                    case JnpfKeyConsts.MODIFYUSER:
                        needUser = true;
                        break;
                    //公司组件
                    case JnpfKeyConsts.COMSELECT:
                        //部门组件
                    case JnpfKeyConsts.DEPSELECT:
                        //所属部门
                    case JnpfKeyConsts.CURRDEPT:
                        //所属组织
                    case JnpfKeyConsts.CURRORGANIZE:
                        needOrg = true;
                        needOrgTree = true;
                        break;
                    //岗位组件
                    case JnpfKeyConsts.POSSELECT:
                        //所属岗位
                    case JnpfKeyConsts.CURRPOSITION:
                        needPos = true;
                        break;
                    //角色选择
                    case JnpfKeyConsts.ROLESELECT:
                        needRole = true;
                        break;
                    //分组选择
                    case JnpfKeyConsts.GROUPSELECT:
                        needGroup = true;
                        break;
                    //用户选择组件
                    case JnpfKeyConsts.CUSTOMUSERSELECT:
                        needUser = needOrg = needPos = needGroup = needRole = true;
                        break;
                    case JnpfKeyConsts.POPUPSELECT:
                    case JnpfKeyConsts.POPUPTABLESELECT:
                        popuHasRedis(localCache, inlineEdit, mainAndMast, swapDataVo, dataMap, listExecutor);
                        break;
                    case JnpfKeyConsts.CASCADER:
                    case JnpfKeyConsts.RADIO:
                    case JnpfKeyConsts.CHECKBOX:
                    case JnpfKeyConsts.SELECT:
                    case JnpfKeyConsts.TREESELECT:
                        //动态
                        List<TemplateJsonModel> templateList = JsonUtil.getJsonToList(swapDataVo.getConfig().getTemplateJson(), TemplateJsonModel.class);
                        if (!templateList.isEmpty()) {
                            Map<String, String> paramMap = new HashMap<>();
                            for (TemplateJsonModel templateJsonModel : templateList) {
                                String relationField = templateJsonModel.getRelationField();
                                String field = templateJsonModel.getField();
                                String obj = inlineEdit ? "" : Optional.ofNullable(dataMap.get(relationField)).orElse("").toString();
                                if (StringUtil.isEmpty(relationField)) {
                                    paramMap.put(field, templateJsonModel.getDefaultValue());
                                    continue;
                                }
                                if (relationField.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                                    String childField = relationField.split("-")[1];
                                    obj = Optional.ofNullable(dataMap.get(childField)).orElse("").toString();
                                } else if (mainAndMast != null) {
                                    obj = Optional.ofNullable(mainAndMast.get(relationField)).orElse("").toString();
                                }
                                paramMap.put(field, obj);
                            }
                            //缓存Key 租户-远端数据-id-base64({params})
                            redisKey = String.format("%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getConfig().getPropsUrl(),
                                    Base64.getEncoder().encodeToString(JsonUtil.getObjectToString(paramMap).getBytes(StandardCharsets.UTF_8)));

                            if (!localCache.containsKey(redisKey)) {
                                listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_SELECT, swapDataVo.getConfig().getPropsUrl(), paramMap, swapDataVo.getConfig().getUseCache()));
                            }
                        }
                        break;
                    default:
                        break;
                }
                if (dataType != null) {
                    //数据接口的数据存放
                    String label = swapDataVo.getProps().getLabel() != null ? swapDataVo.getProps().getLabel() : "";
                    String value = swapDataVo.getProps().getValue() != null ? swapDataVo.getProps().getValue() : "";
                    String children = swapDataVo.getProps().getChildren() != null ? swapDataVo.getProps().getChildren() : "";
                    if (swapDataVo.getConfig().getJnpfKey().equals(JnpfKeyConsts.POPUPSELECT) || swapDataVo.getConfig().getJnpfKey().equals(JnpfKeyConsts.POPUPTABLESELECT)) {
                        label = swapDataVo.getRelationField();
                        value = swapDataVo.getPropsValue();
                    }
                    //静态数据
                    if (dataType.equals(OnlineDataTypeEnum.STATIC.getType())) {
                        redisKey = String.format("%s-%s-%s", visualDevId, swapDataVo.getConfig().getRelationTable() + swapDataVo.getVModel(), OnlineDataTypeEnum.STATIC.getType());
                        if (!localCache.containsKey(redisKey)) {
                            listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_DATATYPE, null, swapDataVo));
                        }
                    }
                    //远端数据
                    if (dataType.equals(OnlineDataTypeEnum.DYNAMIC.getType())) {
                        //联动状态下不做缓存， 具体查数据时做缓存
                        boolean dynamicIsNeedCache = swapDataVo.getConfig().getTemplateJson().isEmpty();
                        if (dynamicIsNeedCache) {
                            redisKey = String.format("%s-%s-%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getConfig().getPropsUrl(), value, label, children);
                            if (!localCache.containsKey(redisKey)) {
                                listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_DATATYPE, null, swapDataVo, swapDataVo.getConfig().getUseCache()));
                            }
                        }
                    }
                    //数据字典
                    if (dataType.equals(OnlineDataTypeEnum.DICTIONARY.getType())) {
                        redisKey = String.format("%s-%s-%s", dsName, OnlineDataTypeEnum.DICTIONARY.getType(), swapDataVo.getConfig().getDictionaryType());
                        if (!localCache.containsKey(redisKey)) {
                            listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_DATATYPE, null, swapDataVo));
                        }
                    }
                }

                if (needUser) {
                    //人员
                    redisKey = dsName + CacheKeyEnum.USER.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_USER, null, null));
                    }
                }
                if (needOrg) {
                    //组织
                    redisKey = dsName + CacheKeyEnum.ORG.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_ORG, null, null));
                    }
                }
                if (needPos) {
                    //岗位
                    redisKey = dsName + CacheKeyEnum.POS.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_POS, null, null));
                    }
                }
                if (needRole) {
                    //角色
                    redisKey = dsName + CacheKeyEnum.ROLE.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_ROLE, null, null));
                    }
                }
                if (needGroup) {
                    //分组
                    redisKey = dsName + CacheKeyEnum.GROUP.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_GROUP, null, null));
                    }
                }
                if (needOrgTree) {
                    //分组
                    redisKey = dsName + CacheKeyEnum.ORGTREE.getName();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_ORGTREE, null, null));
                    }
                }
            }
        }
        //执行多线程方法
        if (!listExecutor.isEmpty()) {
            this.execute(localCache, listExecutor);
        }
    }

    //弹窗选择是否缓存
    private boolean popuHasRedis(Map<String, Object> localCache, boolean inlineEdit, Map<String, Object> mainAndMast, FieLdsModel swapDataVo, Map<String, Object> dataMap, Map<String, OnlineExecutorParam> listExecutor) {
        String redisKey;
        String swapVModel = swapDataVo.getVModel();
        List<TemplateJsonModel> templateJsonModels = JsonUtil.getJsonToList(swapDataVo.getTemplateJson(), TemplateJsonModel.class);
        if (dataMap.get(swapVModel) == null) return true;
        String value = String.valueOf(dataMap.get(swapVModel));
        List<DataInterfaceModel> listParam = new ArrayList<>();
        for (TemplateJsonModel templateJsonModel : templateJsonModels) {
            String relationField = templateJsonModel.getRelationField();
            DataInterfaceModel dataInterfaceModel = JsonUtil.getJsonToBean(templateJsonModel, DataInterfaceModel.class);
            if (StringUtil.isEmpty(relationField)) {
                listParam.add(dataInterfaceModel);
                continue;
            }
            String obj = inlineEdit ? "" : Optional.ofNullable(dataMap.get(relationField)).orElse("").toString();
            if (relationField.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String childField = relationField.split("-")[1];
                obj = Optional.ofNullable(dataMap.get(childField)).orElse("").toString();
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
        redisKey = String.format("%s-%s-%s-%s", dsName, OnlineDataTypeEnum.DYNAMIC.getType(), swapDataVo.getInterfaceId(),
                Base64.getEncoder().encodeToString(JsonUtil.getObjectToString(dataInterfacePage).getBytes(StandardCharsets.UTF_8)));

        if (!localCache.containsKey(redisKey)) {
            dataInterfacePage.setPropsValue(swapDataVo.getPropsValue());
            dataInterfacePage.setRelationField(swapDataVo.getRelationField());
            listExecutor.putIfAbsent(redisKey, new OnlineExecutorParam(redisKey, KEY_POP, swapDataVo.getInterfaceId(), dataInterfacePage, swapDataVo.getConfig().getUseCache()));
        }
        return false;
    }

    /**
     * 执行多线程
     */
    private void execute(Map<String, Object> localCache, Map<String, OnlineExecutorParam> listExecutor) {
        CountDownLatch countDownLatch = new CountDownLatch(listExecutor.size());
        for (Map.Entry<String, OnlineExecutorParam> keyItem : listExecutor.entrySet()) {
            OnlineExecutorParam item = keyItem.getValue();
            String redisKey = item.getRedisKey();
            ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                try {
                    switch (item.getType()) {
                        case KEY_USER:
                            //人员
                            Map<String, Object> userMap;
                            if (redisUtil.exists(redisKey)) {
                                userMap = redisUtil.getMap(redisKey);
                                userMap = Optional.ofNullable(userMap).orElse(new HashMap<>(20));
                            } else {
                                userMap = userApi.getUserMap();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, userMap, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__user_map", userMap);
                            break;
                        case KEY_ORG:
                            Map<String, Object> orgMap;
                            if (redisUtil.exists(redisKey)) {
                                orgMap = redisUtil.getMap(redisKey);
                                orgMap = Optional.ofNullable(orgMap).orElse(new HashMap<>(20));
                            } else {
                                orgMap = organizeApi.getOrgMap();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, orgMap, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__org_map", orgMap);
                            break;
                        case KEY_POS:
                            Map<String, String> posMap;
                            if (redisUtil.exists(redisKey)) {
                                posMap = redisUtil.getMap(redisKey);
                                posMap = Optional.ofNullable(posMap).orElse(new HashMap<>(20));
                            } else {
                                posMap = positionApi.getPosFullNameMap();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, posMap, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__pos_map", posMap);
                            break;
                        case KEY_ROLE:
                            Map<String, Object> roleMap;
                            if (redisUtil.exists(redisKey)) {
                                roleMap = redisUtil.getMap(redisKey);
                                roleMap = Optional.ofNullable(roleMap).orElse(new HashMap<>(20));
                            } else {
                                roleMap = roleApi.getRoleMap();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, roleMap, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__role_map", roleMap);
                            break;
                        case KEY_GROUP:
                            Map<String, Object> groupMap;
                            if (redisUtil.exists(redisKey)) {
                                groupMap = redisUtil.getMap(redisKey);
                                groupMap = Optional.ofNullable(groupMap).orElse(new HashMap<>(20));
                            } else {
                                groupMap = groupApi.getGroupMap();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, groupMap, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__group_map", groupMap);
                            break;
                        case KEY_POP:
                            List<Map<String, Object>> mapList = null;
                            if (!redisUtil.exists(redisKey)) {
                                mapList = dataInterFaceApi.infoToInfo(item.getInterfaceId(), (DataInterfacePage) item.getParam());
                                if (NEEDCACHE_REMOTE && mapList != null && !mapList.isEmpty() && item.getUseCache()) {
                                    redisUtil.insert(item.getRedisKey(), mapList, DEFAULT_CACHE_TIME);
                                }
                            } else {
                                List<Object> tmpList = redisUtil.get(redisKey, 0, -1);
                                List<Map<String, Object>> tmpMapList = new ArrayList<>();
                                tmpList.forEach(itemx -> tmpMapList.add(JsonUtil.entityToMap(itemx)));
                                mapList = tmpMapList;
                            }
                            localCache.put(item.getRedisKey(), mapList);
                            break;
                        case KEY_SELECT:
                            List<Map<String, Object>> dataList = null;
                            if (!redisUtil.exists(redisKey)) {
                                ActionResult<Object> data = dataInterFaceApi.infoToId(item.getInterfaceId(), null, (Map) item.getParam());
                                if (data != null && data.getData() != null && data.getData() instanceof List) {
                                    dataList = (List<Map<String, Object>>) data.getData();
                                    if (NEEDCACHE_REMOTE && CollectionUtils.isNotEmpty(dataList) && item.getUseCache()) {
                                        redisUtil.insert(redisKey, dataList, DEFAULT_CACHE_TIME);
                                    }
                                }
                            } else {
                                List<Object> tmpList = redisUtil.get(redisKey, 0, -1);
                                List<Map<String, Object>> tmpMapList = new ArrayList<>();
                                tmpList.forEach(itemx -> tmpMapList.add(JsonUtil.entityToMap(itemx)));
                                dataList = tmpMapList;
                            }
                            localCache.put(redisKey, dataList);
                            break;
                        case KEY_DATATYPE:
                            //数据接口的数据存放
                            FieLdsModel swapDataVo = (FieLdsModel) item.getParam();
                            String dataType = swapDataVo.getConfig().getDataType();
                            String label = swapDataVo.getProps().getLabel() != null ? swapDataVo.getProps().getLabel() : "";
                            String value = swapDataVo.getProps().getValue() != null ? swapDataVo.getProps().getValue() : "";
                            String children = swapDataVo.getProps().getChildren() != null ? swapDataVo.getProps().getChildren() : "";
                            List<Map<String, Object>> options = new ArrayList<>();
                            if (swapDataVo.getConfig().getJnpfKey().equals(JnpfKeyConsts.POPUPSELECT) || swapDataVo.getConfig().getJnpfKey().equals(JnpfKeyConsts.POPUPTABLESELECT)) {
                                label = swapDataVo.getRelationField();
                                value = swapDataVo.getPropsValue();
                            }
                            Map<String, String> dataInterfaceMap = new HashMap<>(16);
                            String finalValue = value;
                            String finalLabel = label;
                            //静态数据
                            if (dataType.equals(OnlineDataTypeEnum.STATIC.getType()) && !localCache.containsKey(redisKey)) {
                                if (!redisUtil.exists(redisKey)) {
                                    if (swapDataVo.getOptions() != null) {
                                        options = JsonUtil.getJsonToListMap(swapDataVo.getOptions());
                                        OnlineSwapDataUtils.getOptions(label, value, children, JsonUtil.getListToJsonArray(options), options);
                                    } else {
                                        options = JsonUtil.getJsonToListMap(swapDataVo.getOptions());
                                    }

                                    options.stream().forEach(o -> dataInterfaceMap.put(String.valueOf(o.get(finalValue)), String.valueOf(o.get(finalLabel))));
                                    if (NEEDCACHE_REMOTE) {
                                        redisUtil.insert(redisKey, dataInterfaceMap, DEFAULT_CACHE_TIME);
                                    }
                                    localCache.put(redisKey, dataInterfaceMap);
                                } else {
                                    localCache.put(redisKey, redisUtil.getMap(redisKey));
                                }
                            }
                            //远端数据
                            if (dataType.equals(OnlineDataTypeEnum.DYNAMIC.getType())) {
                                //联动状态下不做缓存， 具体查数据时做缓存
                                boolean dynamicIsNeedCache = swapDataVo.getConfig().getTemplateJson().isEmpty();
                                if (dynamicIsNeedCache && !localCache.containsKey(redisKey)) {
                                    if (!redisUtil.exists(redisKey)) {
                                        ActionResult<Object> dataRes = dataInterFaceApi.infoToId(swapDataVo.getConfig().getPropsUrl(), null, null);
                                        if (dataRes != null && dataRes.getData() != null) {
                                            List<Map<String, Object>> dataList2 = new ArrayList<>();
                                            if (dataRes.getData() instanceof List) {
                                                dataList2 = (List<Map<String, Object>>) dataRes.getData();
                                            }
                                            JSONArray dataAll = JsonUtil.getListToJsonArray(dataList2);
                                            OnlineSwapDataUtils.treeToList(label, value, children, dataAll, options);
                                            options.stream().forEach(o -> dataInterfaceMap.put(String.valueOf(o.get(finalValue)), String.valueOf(o.get(finalLabel))));
                                            if (NEEDCACHE_REMOTE && CollectionUtils.isNotEmpty(dataList2) && item.getUseCache()) {
                                                redisUtil.insert(redisKey, dataInterfaceMap, DEFAULT_CACHE_TIME);
                                            }
                                            localCache.put(redisKey, dataInterfaceMap);
                                        }
                                    } else {
                                        localCache.put(redisKey, redisUtil.getMap(redisKey));
                                    }
                                }
                            }
                            //数据字典
                            if (dataType.equals(OnlineDataTypeEnum.DICTIONARY.getType()) && !localCache.containsKey(redisKey)) {
                                if (!redisUtil.exists(redisKey)) {
                                    List<DictionaryDataEntity> list = dictionaryDataApi.getDicList(swapDataVo.getConfig().getDictionaryType());
                                    options = list.stream().map(dic -> {
                                        Map<String, Object> dictionaryMap = new HashMap<>(16);
                                        dictionaryMap.put("id", dic.getId());
                                        dictionaryMap.put("enCode", dic.getEnCode());
                                        dictionaryMap.put("fullName", dic.getFullName());
                                        return dictionaryMap;
                                    }).collect(Collectors.toList());
                                    String dictionaryData = JsonUtil.getObjectToString(options);
                                    if (NEEDCACHE_REMOTE) {
                                        redisUtil.insert(redisKey, dictionaryData, DEFAULT_CACHE_TIME);
                                    }
                                    localCache.put(redisKey, options);
                                } else {
                                    String dictionaryStringData = redisUtil.getString(redisKey).toString();
                                    localCache.put(redisKey, JsonUtil.getJsonToListMap(dictionaryStringData));
                                }
                            }
                            break;
                        case KEY_ORGTREE:
                            Map<String, Object> orgTree;
                            if (redisUtil.exists(redisKey)) {
                                orgTree = redisUtil.getMap(redisKey);
                                orgTree = Optional.ofNullable(orgTree).orElse(new HashMap<>(20));
                            } else {
                                orgTree = organizeApi.getAllOrgsTreeName();
                                if (OnlineSwapDataUtils.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, orgTree, DEFAULT_CACHE_TIME);
                                }
                            }
                            localCache.put("__orgTree_map", orgTree);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    log.error("线程执行错误：" + e.getMessage());
                } finally {
                    //每执行一次数值减少一
                    countDownLatch.countDown();
                    //也可以给await()设置超时时间，如果超过300s（也可以是时，分）则不再等待，直接执行下面代码。
                }
            });
        }

        try {
            //等待计数器归零
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("线程计数错误：" + e.getMessage());
        }
    }
}

@Data
class OnlineExecutorParam {
    private String redisKey;
    private String type;
    private String interfaceId;
    private Object param;
    private Boolean useCache;

    public OnlineExecutorParam(String redisKey, String type, String interfaceId, Object param) {
        this.redisKey = redisKey;
        this.type = type;
        this.interfaceId = interfaceId;
        this.param = param;
        this.useCache = false;
    }

    public OnlineExecutorParam(String redisKey, String type, String interfaceId, Object param, Boolean useCache) {
        this.redisKey = redisKey;
        this.type = type;
        this.interfaceId = interfaceId;
        this.param = param;
        this.useCache = useCache;
    }
}
