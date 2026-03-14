package jnpf.base.util.dataset;

import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.model.dataset.DataSetConfig;
import jnpf.base.model.dataset.DataSetOptions;
import jnpf.base.model.dataset.DataSetSwapModel;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DictionaryDataService;
import jnpf.permission.service.*;
import jnpf.util.*;
import jnpf.util.data.DataSourceContextHolder;
import jnpf.util.visiual.DataTypeConst;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * 数据集数据转换多线程
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/7/15 16:45:09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSetExecutor {
    
    private final RedisUtil redisUtil;
    
    private final  UserService userApi;
    
    private final  OrganizeService organizeApi;
    
    private final  PositionService positionApi;
    
    private final  RoleService roleApi;
    
    private final  GroupService groupApi;
    
    private final  DictionaryDataService dictionaryDataApi;
    
    private final  DataInterfaceService dataInterFaceApi;


    private String tenantId = "";
    private static long aLong = 300;
    public static final String S_S_S ="%s-%s-%s";


    /**
     * 添加多线程
     */
    public void executorRedis(Map<String, Object> localCache, String printId, List<DataSetSwapModel> swapList, Map<String, Object> map) {
        tenantId = Optional.ofNullable(DataSourceContextHolder.getDatasourceId()).orElse("");
        Map<String, OateSetExParam> listExecutor = new HashMap<>();
        if (MapUtils.isNotEmpty(map)) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if (Objects.isNull(map.get(key))) continue;
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get(key);
                boolean needOrg = false;
                boolean needDep = false;
                boolean needPos = false;
                boolean needUser = false;
                boolean needRole = false;
                boolean needGroup = false;
                for (Map<String, Object> item : list) {
                    for (DataSetSwapModel model : swapList) {
                        String field = model.getField();
                        String type = model.getType();
                        if(StringUtils.isNotEmpty(field) && StringUtils.isNotEmpty(type)) {
                            String[] fields = field.split("\\.");

                            DataSetConfig config = model.getConfig();
                            if (!key.equals(fields[0]) || item.get(fields[1]) == null || "".equals(item.get(fields[1]))) {
                                continue;
                            }
                            String redisKey;
                            switch (type) {
                                case DataSetConstant.KEY_ORG:
                                    needOrg = true;
                                    break;
                                case DataSetConstant.KEY_DEP:
                                    needDep = true;
                                    break;
                                case DataSetConstant.KEY_POS:
                                    needPos = true;
                                    break;
                                case DataSetConstant.KEY_USER:
                                    needUser = true;
                                    break;
                                case DataSetConstant.KEY_ROLE:
                                    needRole = true;
                                    break;
                                case DataSetConstant.KEY_GROUP:
                                    needGroup = true;
                                    break;
                                case DataSetConstant.KEY_USERS:
                                    needOrg = true;
                                    needDep = true;
                                    needPos = true;
                                    needUser = true;
                                    needRole = true;
                                    needGroup = true;
                                    break;
                                case DataSetConstant.KEY_SELECT:
                                    if (DataTypeConst.STATIC.equals(config.getDataType())) {
                                        redisKey = String.format(S_S_S, printId, field, DataTypeConst.STATIC);
                                        if (!localCache.containsKey(redisKey)) {
                                            listExecutor.putIfAbsent(redisKey, new OateSetExParam(redisKey, DataSetConstant.KEY_SELECT, null, config));
                                        }
                                    }
                                    if (DataTypeConst.DICTIONARY.equals(config.getDataType())) {
                                        redisKey = String.format(S_S_S, tenantId, DataTypeConst.DICTIONARY, model.getConfig().getDictionaryType());
                                        if (!localCache.containsKey(redisKey)) {
                                            listExecutor.putIfAbsent(redisKey, new OateSetExParam(redisKey, DataSetConstant.KEY_SELECT, null, config));
                                        }
                                    }
                                    if (DataTypeConst.DYNAMIC.equals(config.getDataType())) {
                                        redisKey = String.format(S_S_S, tenantId, DataTypeConst.DYNAMIC, model.getConfig().getPropsUrl());
                                        if (!localCache.containsKey(redisKey)) {
                                            listExecutor.putIfAbsent(redisKey, new OateSetExParam(redisKey, DataSetConstant.KEY_SELECT, null, config));
                                        }
                                    }
                                    needGroup = true;
                                    break;
                                default:
                                    break;
                            }
                        }

                    }
                }
                //添加系统缓存
                Map<String, String> reidsKeyMap = new LinkedHashMap<>();
                if (needOrg) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_ORG_TREE, DataSetConstant.KEY_ORG);
                if (needDep) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_DEP, DataSetConstant.KEY_DEP);
                if (needPos) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_POS, DataSetConstant.KEY_POS);
                if (needUser) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_USER, DataSetConstant.KEY_USER);
                if (needRole) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_ROLE, DataSetConstant.KEY_ROLE);
                if (needGroup) reidsKeyMap.put(tenantId + CacheKeyUtil.SYS_GROUP, DataSetConstant.KEY_GROUP);
                for (Map.Entry<String, String> stringEntry : reidsKeyMap.entrySet()) {
                    String redisKey = stringEntry.getKey();
                    if (!localCache.containsKey(redisKey)) {
                        listExecutor.putIfAbsent(redisKey, new OateSetExParam(redisKey, reidsKeyMap.get(redisKey), null, null));
                    }
                }
            }

            //执行多线程方法
            if (!listExecutor.isEmpty()) {
                this.execute(localCache, listExecutor);
            }
        }
    }

    /**
     * 执行多线程
     */
    private void execute(Map<String, Object> localCache, Map<String, OateSetExParam> listExecutor) {
        CountDownLatch countDownLatch = new CountDownLatch(listExecutor.size());
        for (Map.Entry<String, OateSetExParam> entry : listExecutor.entrySet()) {
            String key = entry.getKey();
            OateSetExParam item = listExecutor.get(key);
            String redisKey = item.getRedisKey();
            ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                try {
                    switch (item.getType()) {
                        case DataSetConstant.KEY_USER:
                            //人员
                            Map<String, Object> userMap;
                            if (redisUtil.exists(redisKey)) {
                                userMap = redisUtil.getMap(redisKey);
                                userMap = Optional.ofNullable(userMap).orElse(new HashMap<>(20));
                            } else {
                                userMap = userApi.getUserMap();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, userMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_USER, userMap);
                            break;
                        case DataSetConstant.KEY_ORG:
                            Map<String, Object> orgMap;
                            if (redisUtil.exists(redisKey)) {
                                orgMap = redisUtil.getMap(redisKey);
                                orgMap = Optional.ofNullable(orgMap).orElse(new HashMap<>(20));
                            } else {
                                orgMap = organizeApi.getAllOrgsTreeName();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, orgMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_ORG_TREE, orgMap);
                            break;
                        case DataSetConstant.KEY_DEP:
                            Map<String, Object> depMap;
                            if (redisUtil.exists(redisKey)) {
                                depMap = redisUtil.getMap(redisKey);
                                depMap = Optional.ofNullable(depMap).orElse(new HashMap<>(20));
                            } else {
                                depMap = organizeApi.getOrgMap();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, depMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_DEP, depMap);
                            break;
                        case DataSetConstant.KEY_POS:
                            Map<String, String> posMap;
                            if (redisUtil.exists(redisKey)) {
                                posMap = redisUtil.getMap(redisKey);
                                posMap = Optional.ofNullable(posMap).orElse(new HashMap<>(20));
                            } else {
                                posMap = positionApi.getPosFullNameMap();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, posMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_POS, posMap);
                            break;
                        case DataSetConstant.KEY_ROLE:
                            Map<String, Object> roleMap;
                            if (redisUtil.exists(redisKey)) {
                                roleMap = redisUtil.getMap(redisKey);
                                roleMap = Optional.ofNullable(roleMap).orElse(new HashMap<>(20));
                            } else {
                                roleMap = roleApi.getRoleMap();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, roleMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_ROLE, roleMap);
                            break;
                        case DataSetConstant.KEY_GROUP:
                            Map<String, Object> groupMap;
                            if (redisUtil.exists(redisKey)) {
                                groupMap = redisUtil.getMap(redisKey);
                                groupMap = Optional.ofNullable(groupMap).orElse(new HashMap<>(20));
                            } else {
                                groupMap = groupApi.getGroupMap();
                                if (DataSetSwapUtil.NEEDCACHE_SYS) {
                                    redisUtil.insert(redisKey, groupMap, aLong);
                                }
                            }
                            localCache.put(CacheKeyUtil.SYS_GROUP, groupMap);
                            break;

                        case DataSetConstant.KEY_SELECT:
                            DataSetConfig config = (DataSetConfig) item.getParam();
                            Map<String, String> selectMap = new HashMap<>(16);
                            List<Map<String, Object>> options = new ArrayList<>();
                            //静态数据
                            if (DataTypeConst.STATIC.equals(config.getDataType()) && !localCache.containsKey(redisKey)) {
                                    if (!redisUtil.exists(redisKey)) {
                                        if (config.getOptions() != null) {
                                            List<DataSetOptions> configOptions = config.getOptions();
                                            for (DataSetOptions dso : configOptions) {
                                                selectMap.put(dso.getId(), dso.getFullName());
                                            }
                                        }
                                        redisUtil.insert(redisKey, selectMap, aLong);
                                        localCache.put(redisKey, selectMap);
                                    } else {
                                        localCache.put(redisKey, redisUtil.getMap(redisKey));
                                    }
                                }


                            //数据字典
                            if (DataTypeConst.DICTIONARY.equals(config.getDataType()) && !localCache.containsKey(redisKey)) {
                                    if (!redisUtil.exists(redisKey)) {
                                        List<DictionaryDataEntity> list = dictionaryDataApi.getDicList(config.getDictionaryType());
                                        options = list.stream().map(dic -> {
                                            Map<String, Object> dictionaryMap = new HashMap<>(16);
                                            dictionaryMap.put("id", dic.getId());
                                            dictionaryMap.put("enCode", dic.getEnCode());
                                            dictionaryMap.put("fullName", dic.getFullName());
                                            return dictionaryMap;
                                        }).collect(Collectors.toList());
                                        String dictionaryData = JsonUtil.getObjectToString(options);
                                        redisUtil.insert(redisKey, dictionaryData, aLong);
                                        localCache.put(redisKey, options);
                                    } else {
                                        String dictionaryStringData = redisUtil.getString(redisKey).toString();
                                        localCache.put(redisKey, JsonUtil.getJsonToListMap(dictionaryStringData));
                                    }
                                }


                            //数据接口
                            if (DataTypeConst.DYNAMIC.equals(config.getDataType()) && !localCache.containsKey(redisKey)) {
                                    if (!redisUtil.exists(redisKey)) {
                                        List<Map<String,Object>> dataList = new ArrayList<>();
                                        ActionResult<Object> data = dataInterFaceApi.infoToId(config.getPropsUrl(), null, null);
                                        if (data != null && data.getData() != null && data.getData() instanceof List) {
                                                dataList = (List<Map<String, Object>>) data.getData();
                                            }

                                        String dynamicData = JsonUtil.getObjectToString(dataList);
                                        redisUtil.insert(redisKey, dynamicData, aLong);
                                        localCache.put(redisKey, dataList);
                                    } else {
                                        String dynamicDataStringData = redisUtil.getString(redisKey).toString();
                                        localCache.put(redisKey, JsonUtil.getJsonToListMap(dynamicDataStringData));
                                    }
                                }

                            break;
                        default:

                            break;
                    }
                } catch (Exception e) {
                    log.error("线程执行错误：{}", e.getMessage());
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
            log.error("线程计数错误：{}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

@Data
class OateSetExParam {
    private String redisKey;
    private String type;
    private String interfaceId;
    private Object param;

    public OateSetExParam(String redisKey, String type, String interfaceId, Object param) {
        this.redisKey = redisKey;
        this.type = type;
        this.interfaceId = interfaceId;
        this.param = param;
    }
}
