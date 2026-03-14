package jnpf.base.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.DataInterfaceVariateEntity;
import jnpf.base.entity.InterfaceOauthEntity;
import jnpf.base.mapper.DataInterfaceLogMapper;
import jnpf.base.mapper.DataInterfaceMapper;
import jnpf.base.mapper.DataInterfaceVariateMapper;
import jnpf.base.mapper.InterfaceOauthMapper;
import jnpf.base.model.datainterface.*;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DataInterfaceUserService;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.util.DataInterfaceParamUtil;
import jnpf.base.util.interfaceutil.InterfaceUtil;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.MsgCode;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.JdbcUtil;
import jnpf.exception.DataException;
import jnpf.model.SystemParamModel;
import jnpf.permission.model.authorize.AuthorizeConditionEnum;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.script.ScriptException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataInterfaceServiceImpl extends SuperServiceImpl<DataInterfaceMapper, DataInterfaceEntity> implements DataInterfaceService {

    public static final String UTF_8 = "UTF-8";
    public static final String ERROR_CODE = "errorCode";


    private static final Set<String> MARKERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DataInterfaceVarConst.ORGANDSUB,
            DataInterfaceVarConst.ORGANIZEANDPROGENY,
            DataInterfaceVarConst.USERANDSUB,
            DataInterfaceVarConst.USERANDPROGENY,
            DataInterfaceVarConst.POSITIONANDSUB,
            DataInterfaceVarConst.POSITIONANDPROGENY,
            DataInterfaceVarConst.CHARORG,
            DataInterfaceVarConst.ORG,
            DataInterfaceVarConst.POSITIONID,
            DataInterfaceVarConst.USER
    )));

    private final ConfigValueUtil configValueUtil;

    private final DataSourceUtil dataSourceUtils;

    private final DbLinkService dblinkService;

    private final UserService userApi;

    private final DataInterfaceUserService dataInterfaceUserService;


    private final DataInterfaceLogMapper dataInterfaceLogMapper;

    private final DataInterfaceVariateMapper dataInterfaceVariateMapper;

    private final InterfaceOauthMapper interfaceOauthMapper;

    @Override
    public List<DataInterfaceEntity> getList(PaginationDataInterface pagination, Integer isSelector) {
        return this.baseMapper.getList(pagination, isSelector);
    }

    @Override
    public List<DataInterfaceEntity> getList(PaginationDataInterfaceSelector pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<DataInterfaceEntity> getList(boolean filterPage) {
        return this.baseMapper.getList(filterPage);
    }

    @Override
    public DataInterfaceEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(DataInterfaceEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(DataInterfaceEntity entity, String id) throws DataException {
        return this.baseMapper.update(entity, id);
    }

    @Override
    public void delete(DataInterfaceEntity entity) {
        LambdaQueryWrapper<DataInterfaceVariateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataInterfaceVariateEntity::getInterfaceId, entity.getId());
        dataInterfaceVariateMapper.deleteByIds(dataInterfaceVariateMapper.selectList(wrapper));
        this.removeById(entity.getId());
    }

    @Override
    public boolean isExistByFullNameOrEnCode(String id, String fullName, String enCode) {
        return this.baseMapper.isExistByFullNameOrEnCode(id, fullName, enCode);
    }

    @Override
    public ActionResult infoToIdPageList(String id, DataInterfacePage page) {
        DataInterfaceEntity entity = this.getInfo(id);
        if (entity == null) {
            return ActionResult.page(new ArrayList<>(), JsonUtil.getJsonToBean(new Pagination(), PaginationVO.class));
        }
        if (entity.getHasPage() == 1) {
            Map<String, String> map = null;
            if (page.getParamList() != null) {
                map = new HashMap<>();
                List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(page.getParamList(), DataInterfaceModel.class);
                this.paramSourceTypeReplaceValue(jsonToList, map);
            }
            Pagination pagination = new Pagination();
            pagination.setPageSize(page.getPageSize());
            pagination.setCurrentPage(page.getCurrentPage());
            pagination.setKeyword(page.getKeyword());
            return infoToId(id, null, map, null, null, null, pagination, null);
        } else {
            String dataProcessing = null;
            if (StringUtil.isNotEmpty(entity.getDataJsJson())) {
                dataProcessing = entity.getDataJsJson();
            }
            List<Map<String, Object>> dataList = new ArrayList<>();

            Map<String, String> map = null;
            if (page.getParamList() != null) {
                map = new HashMap<>();
                List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(page.getParamList(), DataInterfaceModel.class);
                this.paramSourceTypeReplaceValue(jsonToList, map);

            }
            ActionResult<Object> result = infoToId(id, null, map);
            if (result.getData() instanceof List) {
                dataList = (List<Map<String, Object>>) result.getData();
            }

            PaginationVO pagination;
            page.setTotal(dataList.size());
            if (StringUtil.isNotEmpty(page.getKeyword()) && StringUtil.isNotEmpty(page.getColumnOptions())) {
                String[] colOptions = page.getColumnOptions().split(",");
                dataList = dataList.stream().filter(t -> {
                    boolean isFit = false;
                    for (String c : colOptions) {
                        if (String.valueOf(t.get(c)).contains(page.getKeyword())) {
                            isFit = true;
                            break;
                        }
                    }
                    return isFit;
                }).collect(Collectors.toList());
            }
            page.setTotal(dataList.size());
            dataList = PageUtil.getListPage((int) page.getCurrentPage(), (int) page.getPageSize(), dataList);
            pagination = JsonUtil.getJsonToBean(page, PaginationVO.class);
            return ActionResult.page(dataList, pagination, dataProcessing);
        }
    }

    @Override
    public List<Map<String, Object>> infoToInfo(String id, DataInterfacePage page) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, String> map = null;
        DataInterfaceEntity entity = this.getInfo(id);
        if (entity == null) {
            return new ArrayList<>();
        }
        try {
            if (entity.getHasPage() == 1) {
                if (page.getParamList() != null) {
                    map = new HashMap<>();
                    List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(page.getParamList(), DataInterfaceModel.class);
                    this.paramSourceTypeReplaceValue(jsonToList, map);
                }
                Map<String, Object> showMap = new HashMap<>();
                if (page.getIds() instanceof List) {
                    List<Object> ids = (List<Object>) page.getIds();
                    Map<String, String> finalMap = map;
                    ids.forEach(t -> {
                        showMap.put(page.getPropsValue(), t);
                        ActionResult<Object> result = infoToId(id, null, finalMap, null, null, null, null, showMap);
                        if (result.getData() instanceof Map) {
                            Map<String, Object> objectMap = (Map<String, Object>) result.getData();
                            if (!objectMap.isEmpty()) {
                                List<Map> mapList = JsonUtil.getJsonToList(objectMap.get("list"), Map.class);
                                if (mapList != null && !mapList.isEmpty()) {
                                    list.add(mapList.get(0));
                                } else {
                                    list.add(objectMap);
                                }
                            }
                        } else if (result.getData() instanceof List) {
                            List<Map> list1 = (List<Map>) result.getData();
                            if (!list1.isEmpty()) {
                                list.add(list1.get(0));
                            }
                        }
                    });
                }
            } else {
                if (page.getIds() != null) {
                    if (page.getParamList() != null) {
                        map = new HashMap<>();
                        List<DataInterfaceModel> jsonToList = JsonUtil.getJsonToList(page.getParamList(), DataInterfaceModel.class);
                        this.paramSourceTypeReplaceValue(jsonToList, map);
                    }
                    ActionResult<Object> result = infoToId(id, null, map);
                    List<Map<String, Object>> dataList;
                    if (result.getData() instanceof List) {
                        dataList = (List<Map<String, Object>>) result.getData();
                        List<Object> ids = (List<Object>) page.getIds();
                        List<Map<String, Object>> finalDataList = dataList;
                        ids.forEach(t -> {
                            list.add(finalDataList.stream().filter(data -> data.get(page.getPropsValue()) != null
                                    && t.toString().equals(data.get(page.getPropsValue()).toString())).findFirst().orElse(new HashMap<>()));
                        });
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
        return list;
    }

    @Override
    public ActionResult<Object> infoToId(String id, String tenantId, Map<String, String> map) {
        return infoToId(id, tenantId, map, null, null, null, null, null);
    }

    @Override
    public ActionResult<Object> infoToId(String id, String tenantId, Map<String, String> map, String token, String appId, String invokType, Pagination pagination, Map<String, Object> showMap) {
        DataInterfaceEntity entity = this.getInfo(id);
        if (entity == null) {
            return ActionResult.success(new ArrayList<>());
        }
        // 开始调用的时间
        LocalDateTime dateTime = LocalDateTime.now();
        //调用时间
        int invokWasteTime = 0;
        // 有设置默认值的直接赋值
        replaceDefaultVale(entity.getParameterJson(), map);
        // 验证参数必填或类型
        String checkRequestParams = checkRequestParams(entity.getParameterJson(), map);
        if (StringUtil.isNotEmpty(checkRequestParams)) {
            return ActionResult.fail(checkRequestParams);
        }
        Object callJs = null;
        try {

            if (pagination == null) {
                pagination = new Pagination();
            }

            // 数据配置
            String dataConfigJson = entity.getDataConfigJson();
            DataConfigJsonModel configJsonModel = JsonUtil.getJsonToBean(dataConfigJson, DataConfigJsonModel.class);
            // 如果是静态数据
            if (entity.getType() == 2) {
                String staticData = configJsonModel.getStaticData();
                Object object = callStaticData(staticData);
                handlePostVariate(entity, object);
                return ActionResult.success(object);
            } else if (entity.getType() == 3) {
                // HTTP调用或HTTPS调用
                JSONObject jsonObject = new JSONObject();
                if (showMap == null) {
                    if (entity.getHasPage() == 0) {
                        pagination = null;
                    }
                    //HTTP调用或HTTPS调用
                    jsonObject = callHTTP(map, token, pagination, null, configJsonModel.getApiData());
                } else {
                    String echoJson = entity.getDataEchoJson();
                    DataConfigJsonModel echoJsonModel = JsonUtil.getJsonToBean(echoJson, DataConfigJsonModel.class);
                    jsonObject = callHTTP(map, token, null, showMap, echoJsonModel.getApiData());
                }
                if (Objects.nonNull(jsonObject) && "1".equals(jsonObject.get(ERROR_CODE))) {
                    return ActionResult.fail(MsgCode.SYS121.get());
                }
                // 判断返回参数长度和key是否跟内置的一致
                if (jsonObject == null) {
                    return ActionResult.fail(MsgCode.SYS122.get());
                }
                handlePostVariate(entity, jsonObject);
                Object js = JScriptUtil.callJs(entity.getDataExceptionJson(), jsonObject.get("data") == null ? new ArrayList<>() : jsonObject.get("data"));
                if ((js instanceof Boolean && !BooleanUtil.toBoolean(String.valueOf(js)))) {
                    // 继续执行接口
                    if (showMap == null) {
                        // 处理变量
                        handlerVariate(configJsonModel.getApiData());
                        jsonObject = callHTTP(map, token, pagination, null, configJsonModel.getApiData());
                    } else {
                        String echoJson = entity.getDataEchoJson();
                        DataConfigJsonModel echoJsonModel = JsonUtil.getJsonToBean(echoJson, DataConfigJsonModel.class);
                        // 处理变量
                        handlerVariate(echoJsonModel.getApiData());
                        jsonObject = callHTTP(map, token, null, showMap, echoJsonModel.getApiData());
                    }
                }
                if (isInternal(jsonObject)) {
                    callJs = JScriptUtil.callJs(entity.getDataJsJson(), jsonObject.get("data") == null ? new ArrayList<>() : jsonObject.get("data"));
                } else {
                    callJs = JScriptUtil.callJs(entity.getDataJsJson(), jsonObject);
                }
            } else if (entity.getType() == 1) {
                UserInfo oldUser = null;
                if (token != null) {
                    oldUser = UserProvider.getUser();
                    UserInfo userInfo = UserProvider.getUser(token);
                    UserProvider.setLocalLoginUser(userInfo);
                }
                try {
                    if (showMap == null) {
                        SqlDateModel sqlData = configJsonModel.getSqlData();

                        List<Map<String, Object>> sqlMapList = executeSql(entity, 0, map, pagination, null, sqlData);
                        handlePostVariate(entity, sqlMapList);

                        if (entity.getHasPage() == 1) {
                            DataConfigJsonModel pageJsonModel = JsonUtil.getJsonToBean(entity.getDataConfigJson(), DataConfigJsonModel.class);
                            List<Map<String, Object>> maps = executeSql(entity, 1, map, pagination, null, pageJsonModel.getSqlData());
                            if (maps.get(0) != null) {
                                pagination.setTotal(Long.parseLong(String.valueOf(maps.get(0).values().iterator().next())));
                            }

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("list", sqlMapList);
                            obj.put("pagination", JsonUtil.getJsonToBean(pagination, PaginationVO.class));
                            callJs = JScriptUtil.callJs(entity.getDataJsJson(), obj);
                            return ActionResult.success(callJs);
                        } else {
                            callJs = JScriptUtil.callJs(entity.getDataJsJson(), sqlMapList == null ? new ArrayList<>() : sqlMapList);
                        }
                    } else {
                        DataConfigJsonModel echoJsonModel = JsonUtil.getJsonToBean(entity.getDataEchoJson(), DataConfigJsonModel.class);
                        List<Map<String, Object>> sqlMapList = executeSql(entity, 2, map, pagination, showMap, echoJsonModel.getSqlData());
                        if (entity.getHasPage() == 1) {
                            DataConfigJsonModel pageJsonModel = JsonUtil.getJsonToBean(entity.getDataConfigJson(), DataConfigJsonModel.class);
                            List<Map<String, Object>> maps = executeSql(entity, 1, map, pagination, null, pageJsonModel.getSqlData());
                            if (maps.get(0) != null) {
                                pagination.setTotal(Long.parseLong(String.valueOf(maps.get(0).values().iterator().next())));
                            }

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("list", sqlMapList);
                            obj.put("pagination", JsonUtil.getJsonToBean(pagination, PaginationVO.class));
                            callJs = JScriptUtil.callJs(entity.getDataJsJson(), obj);
                            return ActionResult.success(callJs);
                        } else {
                            callJs = JScriptUtil.callJs(entity.getDataJsJson(), CollUtil.isEmpty(sqlMapList) ? new ArrayList<>() : sqlMapList.get(0));

                        }
                    }
                } finally {
                    if (oldUser != null) {
                        UserProvider.setLocalLoginUser(oldUser);
                    }
                }
            }
            if (callJs instanceof Exception) {
                return ActionResult.success(MsgCode.SYS123.get(((Exception) callJs).getMessage()));
            }
            return ActionResult.success(callJs);
        } catch (Exception e) {
            log.error("错误提示:" + e.getMessage());
            // 本地调试时打印出问题
            e.printStackTrace();
            return ActionResult.fail(MsgCode.SYS122.get());
        } finally {
            // 调用时间
            invokWasteTime = invokTime(dateTime);
            // 添加调用日志
            dataInterfaceLogMapper.create(id, invokWasteTime, appId, invokType);
        }
    }

    /**
     * 预览时赋值变量
     *
     * @param entity
     * @param object
     */
    private void handlePostVariate(DataInterfaceEntity entity, Object object) {
        // 如果是鉴权的话，需要赋值value
        if (entity.getIsPostPosition() == 1) {
            List<DataInterfaceVariateEntity> list = dataInterfaceVariateMapper.getList(entity.getId(), null);
            list.forEach(t -> {
                try {
                    Object o;
                    if (object instanceof JSONObject && isInternal((JSONObject) object)) {
                        o = JScriptUtil.callJs(t.getExpression(), ((JSONObject) object).get("data"));
                    } else {
                        o = JScriptUtil.callJs(t.getExpression(), object);
                    }
                    if (o != null) {
                        t.setValue(o.toString());
                        dataInterfaceVariateMapper.updateById(t);
                    }
                } catch (ScriptException e) {
                    log.error(e.getMessage());
                }
            });
        }
    }

    @Override
    public List<DataInterfaceEntity> getList(List<String> ids) {
        return this.baseMapper.getList(ids);
    }

    /**
     * 处理静态数据
     */
    private Object callStaticData(String staticData) {
        Object obj;
        try {
            Object parse = JSON.parse(staticData);
            if (parse instanceof JSONArray) {
                obj = JsonUtil.getJsonToListMap(staticData);
            } else {
                obj = JsonUtil.stringToMap(staticData);
            }
        } catch (Exception e) {
            obj = staticData;
        }
        if (ObjectUtils.isEmpty(obj)) {
            return new ArrayList<>();
        }
        return obj;
    }

    /**
     * 有设置默认值的直接赋值
     *
     * @param parameterJson
     * @param map
     */
    private void replaceDefaultVale(String parameterJson, Map<String, String> map) {
        List<DataInterfaceModel> dataInterfaceModelList = JsonUtil.getJsonToList(parameterJson, DataInterfaceModel.class);
        if (ObjectUtils.isNotEmpty(dataInterfaceModelList)) {
            if (map == null) {
                map = new HashMap<>(16);
            }
            for (DataInterfaceModel dataInterfaceModel : dataInterfaceModelList) {
                String field = dataInterfaceModel.getField();
                String defaultValue = dataInterfaceModel.getDefaultValue();
                if (!map.containsKey(field) || StringUtil.isEmpty(map.get(field))) {
                    map.put(field, defaultValue == null ? "" : defaultValue);
                }
            }
        }
    }

    /**
     * 判断是不是内部接口
     *
     * @param jsonObject
     * @return
     */
    private boolean isInternal(JSONObject jsonObject) {
        return jsonObject != null
                && jsonObject.size() == 3
                && jsonObject.get("code") != null
                && jsonObject.get("msg") != null
                && jsonObject.get("data") != null;
    }

    /**
     * 检查参数是够必填或类型是否正确
     *
     * @param parameterJson
     * @param map
     * @param sql           预留参数
     */
    private String checkRequestParams(String parameterJson, Map<String, String> map) {
        if (map == null || StringUtil.isEmpty(parameterJson)) {
            return "";
        }
        StringBuilder message = new StringBuilder();
        List<DataInterfaceModel> dataInterfaceModelList = JsonUtil.getJsonToList(parameterJson, DataInterfaceModel.class);
        dataInterfaceModelList.stream().anyMatch(model -> {
            // 验证是否必填
            if (model.getRequired() == 1) {
                String value = map.get(model.getField());
                if (StringUtil.isEmpty(value)) {
                    message.append(model.getField()).append("不能为空");
                }
            }
            if (message.length() == 0 && model.getDataType() != null) {
                String value = map.get(model.getField());
                // 判断是整形
                if (StringUtil.isNotEmpty(value) && "int".equals(model.getDataType())) {
                    try {
                        Integer.parseInt(value);
                    } catch (Exception e) {
                        message.append(model.getField()).append("类型必须为整型");
                    }
                } else if (StringUtil.isNotEmpty(value) && "datetime".equals(model.getDataType())) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        map.put(model.getField(), DateUtil.dateFormat(formatter.parse(value)));
                    } catch (Exception e) {
                        try {
                            map.put(model.getField(), DateUtil.dateFormat(new Date(Long.valueOf(value))));
                        } catch (Exception ex) {
                            message.append(model.getField() + "类型必须为日期时间型");
                        }
                    }
                } else if (StringUtil.isNotEmpty(value) && "decimal".equals(model.getDataType())) {
                    try {
                        Double.valueOf(value);
                    } catch (Exception e) {
                        message.append(model.getField()).append("类型必须为浮点型");
                    }
                }
            }

            return message.length() > 0;
        });
        return message.toString();
    }


    @Override
    public ActionResult<Object> infoToIdNew(String id, String tenantId, DataInterfaceActionModel model) {
        //鉴权验证
        // 获取token
        String authorSignature = ServletUtil.getRequest().getHeader(Constants.AUTHORIZATION);
        String[] authorSignatureArr = authorSignature.split(":");
        if (authorSignatureArr.length != 3) {
            return ActionResult.fail(ActionResultCode.VALIDATEERROR.getMessage());
        }
        String appId = authorSignatureArr[0];
        String author = authorSignatureArr[2];
        Map<String, String> map = model.getMap();
        String interfaceUserToken = null;
        InterfaceOauthEntity infoByAppId = interfaceOauthMapper.getInfoByAppId(appId);
        //未提供app相关，接口认证失效，接口不在授权列表时无权访问
        if (infoByAppId == null || infoByAppId.getEnabledMark() == 0 || !infoByAppId.getDataInterfaceIds().contains(id)) {
            return ActionResult.fail(MsgCode.FA021.get());
        }
        if (infoByAppId.getVerifySignature() == 1) {//验证开启
            try {
                //验证请求有效期1分钟内
                String ymdateStr = ServletUtil.getRequest().getHeader(InterfaceUtil.YMDATE);
                Date ymdate = new Date(Long.parseLong(ymdateStr));
                Date time = DateUtil.dateAddMinutes(ymdate, 1);
                if (DateUtil.getNowDate().after(time)) {
                    return ActionResult.fail(MsgCode.SYS124.get());
                }
                //验证签名有效性
                boolean flag = InterfaceUtil.verifySignature(infoByAppId.getAppSecret(), author);
                if (!flag) {
                    return ActionResult.fail(ActionResultCode.VALIDATEERROR.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ActionResult.fail(ActionResultCode.VALIDATEERROR.getMessage());
            }
        } else {//验证未开启，直接使用秘钥进行验证
            if (!infoByAppId.getAppSecret().equals(author)) {
                return ActionResult.fail(MsgCode.SYS125.get());
            }
        }
        //验证使用期限
        Date usefulLife = infoByAppId.getUsefulLife();
        if (infoByAppId.getUsefulLife() != null && usefulLife.before(DateUtil.getNowDate())) {//空值无限期
            return ActionResult.fail(MsgCode.SYS126.get());
        }
        try {
            //用户秘钥获取token
            interfaceUserToken = dataInterfaceUserService.getInterfaceUserToken(model.getTenantId(), infoByAppId.getId(), ServletUtil.getRequest().getHeader(InterfaceUtil.USERKEY));
        } catch (Exception e) {
            return ActionResult.fail(e.getMessage());
        }
        //黑白名单验证
        String ipwhiteList = StringUtil.isNotEmpty(infoByAppId.getWhiteList()) ? infoByAppId.getWhiteList() : "";//ip白名单
        String ipAddr = IpUtil.getIpAddr();
        if (StringUtil.isNotEmpty(ipwhiteList) && !ipwhiteList.contains(ipAddr)) {//不属于白名单
            return ActionResult.fail(MsgCode.LOG010.get());
        }
        //以下调用接口
        return infoToId(id, null, map, interfaceUserToken, infoByAppId.getAppId(), model.getInvokType(), null, null);
    }


    @Override
    public DataInterfaceActionModel checkParams(Map<String, String> map) {
        return this.baseMapper.checkParams(map);
    }

    /**
     * 执行SQL
     *
     * @param entity
     * @param sqlType
     * @param map
     * @return
     * @throws DataException
     */
    private List<Map<String, Object>> executeSql(DataInterfaceEntity entity, int sqlType, Map<String, String> map,
                                                 Pagination pagination, Map<String, Object> showMap,
                                                 SqlDateModel sqlDateModel) throws Exception {
        Map<String, Object> mapData = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                mapData.put(key, map.get(key));
            }
        }
        DataSourceUtil linkEntity = dblinkService.getInfo(sqlDateModel.getDbLinkId());
        String sql = sqlDateModel.getSql();
        if (entity.getHasPage() == 1) {
            if (sqlType == 1) {
                DataConfigJsonModel dataConfigJsonModel = JsonUtil.getJsonToBean(entity.getDataCountJson(), DataConfigJsonModel.class);
                if (dataConfigJsonModel != null) {
                    SqlDateModel countSqlDateModel = JsonUtil.getJsonToBean(dataConfigJsonModel.getSqlData(), SqlDateModel.class);
                    sql = countSqlDateModel.getSql();
                }
            } else if (sqlType == 2) {
                DataConfigJsonModel dataConfigJsonModel = JsonUtil.getJsonToBean(entity.getDataEchoJson(), DataConfigJsonModel.class);
                if (dataConfigJsonModel != null) {
                    SqlDateModel countSqlDateModel = JsonUtil.getJsonToBean(dataConfigJsonModel.getSqlData(), SqlDateModel.class);
                    sql = countSqlDateModel.getSql();
                }
            }
        }
        UserInfo userInfo = UserProvider.getUser();
        if (linkEntity == null) {
            linkEntity = dataSourceUtils;
        }
        // 系统内置参数替换
        Map<Double, DataInterfaceMarkModel> systemParameter = systemParameter(sql, userInfo, pagination, showMap);
        // 自定义参数替换
        sql = customizationParameter(entity.getParameterJson(), sql, mapData, systemParameter);

        // 处理SQL
        List<Object> values = new ArrayList<>(systemParameter.size());
        // 参数替换为占位符
        sql = getHandleArraysSql(sql, values, systemParameter);
        if (showMap != null) {
            sql = sql.replace(DataInterfaceVarConst.SHOWKEY, showMap.keySet().iterator().next());
        }

        //封装sql---视图查询 -重新封装sql
        if (StringUtil.isNotEmpty(sql) && Objects.nonNull(map)
                && StringUtil.isNotEmpty(map.get("searchSqlStr")) && Objects.equals(entity.getAction(), 3)) {
            if (sql.trim().endsWith(";")) {
                sql = sql.trim();
                sql = sql.substring(0, sql.length() - 1);
            }
            sql = "select * from (" + sql + ") t where " + map.get("searchSqlStr");
            values.addAll(JsonUtil.getJsonToList(map.get("searchValues"), String.class));
        }
        //封装sql结束---

        log.info("当前执行SQL：{}", sql);
        if (entity.getHasPage() == 1 && (sql.contains(";") && sql.trim().indexOf(";") != sql.trim().length() - 1)) {
            return Collections.emptyList();
        }
        if (entity.getAction() != null && entity.getAction() != 3) {
            JdbcUtil.creUpDe(new PrepSqlDTO(sql, values).withConn(linkEntity, null));
            return Collections.emptyList();
        }
        String objectToString = JsonUtil.getObjectToStringAsDate(JdbcUtil.queryList(new PrepSqlDTO(sql, values).withConn(linkEntity, null)).setIsAlias(true).get());
        return JsonUtil.getJsonToListMap(objectToString);
    }

    /**
     * 自定义参数替换
     *
     * @param parameterJson   参数配置
     * @param sql             sql
     * @param map             参数
     * @param systemParameter 参数集合
     */
    @Override
    public String customizationParameter(String parameterJson, String sql, Map<String, Object> map,
                                         Map<Double, DataInterfaceMarkModel> systemParameter) {
        List<DataInterfaceModel> dataInterfaceModelList = StringUtil.isNotEmpty(parameterJson) ? JsonUtil.getJsonToList(parameterJson, DataInterfaceModel.class) : new ArrayList<>();
        if (StringUtil.isNotEmpty(sql) && Objects.nonNull(map)) {
            Map<String, String> placeholderMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                // 验证参数key对比
                Object tmpValue = map.get(key);
                if (tmpValue != null) {
                    //参数前方 上个参数后方的语句中是否有 in
                    String sqlarr1 = sql.split("\\{" + key + "}")[0];
                    String[] sqlarr2 = sqlarr1.split("}");
                    String sql1 = sqlarr2.length > 1 ? sqlarr2[sqlarr2.length - 1] : sqlarr2[0];
                    boolean isInSql = sql1.toLowerCase().contains(" in ");
                    List<Object> valueList = new ArrayList<>();
                    if (isInSql) {
                        valueList = Arrays.asList(String.valueOf(tmpValue).split(","));
                    } else {
                        valueList.add(tmpValue);
                    }
                    StringBuilder placeholder = new StringBuilder("?");
                    for (int i = 1; i < valueList.size(); i++) {
                        placeholder.append(",?");
                    }
                    String finalSql = sql;
                    valueList.forEach(t -> {
                        Object b = t;
                        for (DataInterfaceModel model : dataInterfaceModelList) {
                            if (model.getField().equals(key) && model.getDataType() != null) {
                                // 判断是整形
                                if ("int".equals(model.getDataType())) {
                                    b = ObjectUtil.isNull(t) ? null : Integer.parseInt(String.valueOf(t));
                                } else if ("decimal".equals(model.getDataType())) {
                                    b = ObjectUtil.isNull(t) ? null : Double.valueOf(String.valueOf(t));
                                } else if ("datetime".equals(model.getDataType()) && ObjectUtil.isNull(t)) {
                                    b = null;
                                }
                            }
                        }
                        DataInterfaceParamUtil.getParamModel(systemParameter, finalSql, "{" + key + "}", b);
                    });
                    placeholderMap.put(key, placeholder.toString());
                } else {
                    DataInterfaceParamUtil.getParamModel(systemParameter, sql, "{" + key + "}", null);
                    placeholderMap.put(key, "?");
                }
            }
            for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                String key = entry.getKey();
                sql = sql.replaceAll("\\{" + key + "}", placeholderMap.get(key));
            }
        }
        return sql;
    }

    /**
     * 参数替换为占位符
     */
    public String getHandleArraysSql(String sql, List<Object> values, Map<Double, DataInterfaceMarkModel> systemParameter) {
        if (StringUtil.isNotEmpty(sql)) {
            for (Map.Entry<Double, DataInterfaceMarkModel> entry : systemParameter.entrySet()) {
                Double aDouble = entry.getKey();
                Object value = systemParameter.get(aDouble).getValue();
                values.add(value);
            }
            for (Map.Entry<Double, DataInterfaceMarkModel> entry : systemParameter.entrySet()) {
                Double aDouble = entry.getKey();
                DataInterfaceMarkModel dataInterfaceMarkModel = systemParameter.get(aDouble);
                List<String> collect = MARKERS.stream().filter(t -> t.equals(dataInterfaceMarkModel.getMarkName())).collect(Collectors.toList());
                if (collect.isEmpty()) continue;
                if (dataInterfaceMarkModel.getValue() instanceof List) {
                    List list = (List) dataInterfaceMarkModel.getValue();
                    StringBuilder placeholder = new StringBuilder("?");
                    int index = 0;
                    boolean addOrSet = false;
                    for (Object obj : list) {
                        if (!addOrSet) {
                            // 得到下标
                            int i = values.indexOf(dataInterfaceMarkModel.getValue());
                            values.set(i, obj);
                            addOrSet = true;
                            i++;
                            index = i;
                        } else {
                            placeholder.append(",?");
                            values.add(index, obj);
                        }
                    }
                    sql = sql.replaceAll(collect.get(0), placeholder.toString());
                }

            }
            sql = sql.replaceAll(DataInterfaceVarConst.KEYWORD, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.USER, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.ORG, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.POSITIONID, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.OFFSETSIZE, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.PAGESIZE, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.SHOWVALUE, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.ID, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.ID_LOT, "?");
            sql = sql.replaceAll(DataInterfaceVarConst.FORM_ID, "?");
        }
        return sql;
    }

    /**
     * HTTP调用
     *
     * @return get
     */
    private JSONObject callHTTP(Map<String, String> map,
                                String token, Pagination pagination, Map<String, Object> showMap,
                                ApiDateModel apiDateModel) throws UnsupportedEncodingException {
        JSONObject get = new JSONObject();
        StringBuilder path = new StringBuilder(apiDateModel.getUrl());
        // 请求方法
        String requestMethod = apiDateModel.getMethod() == 1 ? "GET" : "POST";
        // 获取请求头参数
        List<HeadModel> header = apiDateModel.getHeader();
        // 自定义参数
        List<HeadModel> query = apiDateModel.getQuery();
        String body = apiDateModel.getBody();
        int bodyType = apiDateModel.getBodyType() == 1 ? 0 : apiDateModel.getBodyType();
        //判断是否为http或https
        if (StringUtil.isNotEmpty(path.toString()) && path.toString().startsWith("/")) {
            path.insert(0, configValueUtil.getApiDomain());
            if (StringUtil.isEmpty(token)) {
                token = UserProvider.getUser().getToken();
            }
        } else {
            token = null;
        }
        if (path.toString().startsWith("http")) {
            String showKey = null;
            Object showValue = null;
            if (showMap != null && !showMap.isEmpty()) {
                showKey = showMap.keySet().iterator().next();
                showValue = showMap.values().iterator().next();
            }

            // 替换url上的回显参数
            path = new StringBuilder(path.toString().replace("{" + DataInterfaceVarConst.SHOWKEY.replace("@", "") + "}", showKey != null ? showKey : ""));
            path = new StringBuilder(path.toString().replace("{" + DataInterfaceVarConst.SHOWVALUE.replace("@", "") + "}", showValue != null ? URLEncoder.encode(String.valueOf(showValue), UTF_8) : ""));
            //请求参数解析
            if (query != null) {
                // 判断是否为get，get从url上拼接
                path.append(!path.toString().contains("?") ? "?" : "&");
                for (HeadModel headModel : query) {
                    if ("1".equals(headModel.getSource())) {
                        String value = map == null || StringUtil.isEmpty(headModel.getDefaultValue()) || StringUtil.isEmpty(map.get(headModel.getDefaultValue()))
                                ? "" : map.get(headModel.getDefaultValue());
                        path.append(headModel.getField()).append("=").append(URLEncoder.encode(value, UTF_8)).append("&");
                    }
                    if ("2".equals(headModel.getSource())) {
                        DataInterfaceVariateEntity variateEntity = dataInterfaceVariateMapper.getInfo(headModel.getDefaultValue());
                        path.append(headModel.getField()).append("=").append(variateEntity.getValue()).append("&");
                    }
                    if ("3".equals(headModel.getSource())) {
                        path.append(headModel.getField()).append("=").append(URLEncoder.encode(StringUtil.isNotEmpty(headModel.getDefaultValue()) ? headModel.getDefaultValue() : ""
//                                .replaceAll("'", "")
                                , UTF_8)).append("&");
                    }
                    // 分页参数
                    if ("4".equals(headModel.getSource())) {
                        Map<String, Object> map1 = JsonUtil.entityToMap(pagination);

                        Object urlValue = map1.get(headModel.getDefaultValue());
                        if (urlValue instanceof String && ObjectUtil.isNotNull(urlValue)) {
                            path.append(headModel.getField()).append("=").append(URLEncoder.encode(String.valueOf(urlValue), UTF_8)).append("&");
                        } else {
                            path.append(headModel.getField()).append("=").append(urlValue).append("&");
                        }
                    }
                    // 回显参数
                    if ("5".equals(headModel.getSource())) {
                        if (DataInterfaceVarConst.SHOWKEY.equals(headModel.getDefaultValue())) {
                            if (showKey != null) {
                                path.append(headModel.getField()).append("=").append(URLEncoder.encode(showKey, UTF_8)).append("&");
                            }
                        } else {
                            if (showValue != null) {
                                path.append(headModel.getField()).append("=").append(URLEncoder.encode(String.valueOf(showValue), UTF_8)).append("&");
                            } else {
                                path.append(headModel.getField()).append("&");
                            }
                        }
                    }
                }
            }

            String jsonObjects = "";
            if (bodyType == 2) {
                StringJoiner bodyParam = new StringJoiner("&");
                List<HeadModel> bodyJson = JsonUtil.getJsonToList(body, HeadModel.class);
                for (HeadModel headModel : bodyJson) {
                    if ("1".equals(headModel.getSource())) {
                        String value = map == null || StringUtil.isEmpty(headModel.getDefaultValue()) || StringUtil.isEmpty(map.get(headModel.getDefaultValue()))
                                ? "" : map.get(headModel.getDefaultValue());
                        bodyParam.add(headModel.getField() + "=" + URLEncoder.encode(value, UTF_8));
                    }
                    if ("2".equals(headModel.getSource())) {
                        DataInterfaceVariateEntity variateEntity = dataInterfaceVariateMapper.getInfo(headModel.getDefaultValue());
                        bodyParam.add(headModel.getField() + "=" + variateEntity.getValue());
                    }
                    if ("3".equals(headModel.getSource())) {
                        bodyParam.add(headModel.getField() + "=" + headModel.getDefaultValue());
                    }
                }
                jsonObjects += bodyParam.toString();
            } else if (bodyType == 3 || bodyType == 4) {
                // 优先替换变量
                Pattern compile = Pattern.compile("\\{@\\w+}");
                Matcher matcher = compile.matcher(body);
                while (matcher.find()) {
                    // 得到参数
                    String group = matcher.group();
                    String variate = group.replace("{", "").replace("}", "").replace("@", "");
                    DataInterfaceVariateEntity dataInterfaceVariateEntity = dataInterfaceVariateMapper.getInfoByFullName(variate);
                    if (dataInterfaceVariateEntity != null) {
                        body = body.replace(group, dataInterfaceVariateEntity.getValue());
                    }
                }
                Pattern compile1 = Pattern.compile("\\{\\w+}");
                Matcher matcher1 = compile1.matcher(body);
                while (matcher1.find()) {
                    // 得到参数
                    String group = matcher1.group();
                    String param = group.replace("{", "").replace("}", "");
                    String value = map != null ? map.get(param) : null;
                    if (pagination != null && DataInterfaceVarConst.KEYWORD.equals("@" + param)) {
                        value = pagination.getKeyword();
                    }
                    if (pagination != null && DataInterfaceVarConst.CURRENTPAGE.equals("@" + param)) {
                        value = pagination.getCurrentPage() + "";
                    }
                    if (pagination != null && DataInterfaceVarConst.PAGESIZE.equals("@" + param)) {
                        value = pagination.getPageSize() + "";
                    }
                    body = body.replace(group, value);
                }
                jsonObjects = body;
            }

            jsonObjects = StringUtil.isEmpty(jsonObjects) ? null : jsonObjects;
            if (apiDateModel.getMethod() == 1) {
                jsonObjects = "";
            }
            JSONObject headerJson = new JSONObject();
            // 请求头
            for (HeadModel headModel : header) {
                if ("1".equals(headModel.getSource())) {
                    if (map != null && map.containsKey(headModel.getDefaultValue())) {
                        String value = map.get(headModel.getDefaultValue());
                        headerJson.put(headModel.getField(), value
                        );
                    } else {
                        if (null != map) {
                            headerJson.put(headModel.getField(), map.get(headModel.getDefaultValue()));
                        }

                    }
                }
                if ("2".equals(headModel.getSource())) {
                    DataInterfaceVariateEntity variateEntity = dataInterfaceVariateMapper.getInfo(headModel.getDefaultValue());
                    headerJson.put(headModel.getField(), variateEntity.getValue());
                }
                if ("3".equals(headModel.getSource())) {
                    headerJson.put(headModel.getField(), headModel.getDefaultValue());
                }
                // 分页参数
                if ("4".equals(headModel.getSource())) {
                    Map<String, Object> map1 = JsonUtil.entityToMap(pagination);

                    Object urlValue = map1.get(headModel.getDefaultValue());
                    headerJson.put(headModel.getField(), urlValue);
                }
                // 回显参数
                if ("5".equals(headModel.getSource())) {
                    if (DataInterfaceVarConst.SHOWKEY.equals(headModel.getDefaultValue())) {
                        headerJson.put(headModel.getField(), showKey);
                    } else {
                        headerJson.put(headModel.getField(), showValue);
                    }
                }
            }
            get = HttpUtil.httpRequest(path.toString(), requestMethod, jsonObjects, token, !headerJson.isEmpty() ? JsonUtil.getObjectToString(headerJson) : null, String.valueOf(bodyType));
            return get;
        } else {
            get.put(ERROR_CODE, "1");
            return get;
        }
    }

    /**
     * 处理变量
     *
     * @param apiDateModel
     */
    public void handlerVariate(ApiDateModel apiDateModel) {
        Set<String> variate = new HashSet<>();
        // 获取请求头参数
        List<HeadModel> header = apiDateModel.getHeader();
        header.forEach(headModel -> {
            if ("2".equals(headModel.getSource())) {
                variate.add(headModel.getDefaultValue());
            }
        });
        // 自定义参数
        List<HeadModel> query = apiDateModel.getQuery();
        query.forEach(headModel -> {
            if ("2".equals(headModel.getSource())) {
                variate.add(headModel.getDefaultValue());
            }
        });
        if (ObjectUtil.equal(apiDateModel.getBodyType(), 1) || ObjectUtil.equal(apiDateModel.getBodyType(), 2)) {
            List<HeadModel> bodyJson = JsonUtil.getJsonToList(apiDateModel.getBody(), HeadModel.class);
            if (bodyJson != null) {
                bodyJson.forEach(headModel -> {
                    if ("2".equals(headModel.getSource())) {
                        variate.add(headModel.getDefaultValue());
                    }
                });
            }
        }
        List<DataInterfaceVariateEntity> variateEntities = dataInterfaceVariateMapper.getListByIds(new ArrayList<>(variate));
        List<String> collect = variateEntities.stream().map(DataInterfaceVariateEntity::getInterfaceId).distinct().collect(Collectors.toList());
        List<DataInterfaceEntity> list = this.getList(collect);
        Map<String, Object> results = new HashMap<>();
        Map<String, String> updates = new HashMap<>();
        list.forEach(t -> {
            try {
                DataConfigJsonModel dataConfigJsonModel = JsonUtil.getJsonToBean(t.getDataConfigJson(), DataConfigJsonModel.class);
                JSONObject jsonObject = callHTTP(null, UserProvider.getToken(), new Pagination(), null, JsonUtil.getJsonToBean(dataConfigJsonModel.getApiData(), ApiDateModel.class));
                if (Objects.nonNull(jsonObject) && "1".equals(jsonObject.get(ERROR_CODE))) {
                    log.error("接口暂只支持HTTP和HTTPS方式");
                    return;
                }
                // 判断返回参数长度和key是否跟内置的一致
                if (jsonObject == null) {
                    log.error("接口请求失败");
                    return;
                }
                if (isInternal(jsonObject)) {
                    results.put(t.getId(), JScriptUtil.callJs(t.getDataJsJson(), jsonObject.get("data") == null ? new ArrayList<>() : jsonObject.get("data")));
                } else {
                    results.put(t.getId(), JScriptUtil.callJs(t.getDataJsJson(), jsonObject));
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        variateEntities.forEach(t -> {
            if (results.containsKey(t.getInterfaceId())) {
                try {
                    updates.put(t.getId(), String.valueOf(JScriptUtil.callJs(t.getExpression(), results.get(t.getInterfaceId()))));
                } catch (ScriptException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
        dataInterfaceVariateMapper.update(updates, variateEntities);
    }

    /**
     * 处理系统参数
     *
     * @param sql
     * @return
     */
    private Map<Double, DataInterfaceMarkModel> systemParameter(String sql, UserInfo userInfo, Pagination pagination, Map<String, Object> showMap) {
        Map<Double, DataInterfaceMarkModel> paramValue = new TreeMap<>(systemParameterOne(sql, userInfo));
        //关键字
        if (sql.contains(DataInterfaceVarConst.KEYWORD)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.KEYWORD, pagination.getKeyword());
        }
        // 当前页数
        if (sql.contains(DataInterfaceVarConst.OFFSETSIZE)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.OFFSETSIZE, pagination.getPageSize() * (pagination.getCurrentPage() - 1));
        }
        // 每页行数
        if (sql.contains(DataInterfaceVarConst.PAGESIZE)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.PAGESIZE, pagination.getPageSize());
        }
        // 每页行数
        if (sql.contains(DataInterfaceVarConst.SHOWVALUE)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.SHOWVALUE, showMap.values().iterator().next());
        }
        return paramValue;
    }

    /**
     * 处理系统参数
     *
     * @param sql
     * @param userInfo
     * @return
     */
    public Map<Double, DataInterfaceMarkModel> systemParameterOne(String sql, UserInfo userInfo) {
        Map<Double, DataInterfaceMarkModel> paramValue = new TreeMap<>();
        if (sql.contains(DataInterfaceVarConst.ID_LOT)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.ID_LOT, null);
        }
        // 生成雪花id
        if (sql.contains(DataInterfaceVarConst.ID)) {
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.ID, RandomUtil.uuId());
        }
        Map<String, String> systemFieldValue = userApi.getSystemFieldValue(new SystemParamModel(sql));

        //当前用户
        if (hasCurrentUser(sql)) {
            String userId = userInfo.getUserId();
            DataInterfaceParamUtil.getParamModel(paramValue, sql, DataInterfaceVarConst.USER, userId);
        }

        AuthorizeConditionEnum.getResListType().stream().forEach(t -> {
            Object value = "";
            List<String> dataList = StringUtil.isNotEmpty(systemFieldValue.get(t)) ?
                    JsonUtil.getJsonToList(systemFieldValue.get(t), String.class) : Collections.emptyList();
            if (CollUtil.isNotEmpty(dataList)) {
                value = dataList;
            }
            DataInterfaceParamUtil.getParamModel(paramValue, sql, t, value);
        });

        return paramValue;
    }

    /**
     * 是否包含当前用户
     * （当前用户及下属key包含当前用户key，constains判断不准确）
     *
     * @param str
     * @return
     */
    public static boolean hasCurrentUser(String str) {
        int index = 0;
        int count = 0;
        while ((index = str.indexOf(DataInterfaceVarConst.USERANDSUB, index)) != -1) {
            count++;
            index += DataInterfaceVarConst.USERANDSUB.length();
        }
        int index2 = 0;
        int count2 = 0;
        while ((index2 = str.indexOf(DataInterfaceVarConst.USER, index2)) != -1) {
            count2++;
            index2 += DataInterfaceVarConst.USER.length();
        }
        return count != count2;
    }

    /**
     * 计算执行时间
     *
     * @param dateTime
     * @return
     */
    public int invokTime(LocalDateTime dateTime) {
        //调用时间
        return (int) (System.currentTimeMillis() - dateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
    }

    /**
     * 按sourceType替换数据接口参数
     *
     * @param listJson
     * @param map
     */
    @Override
    public void paramSourceTypeReplaceValue(List<DataInterfaceModel> listJson, Map<String, String> map) {
        Map<String, String> systemFieldValue = userApi.getSystemFieldValue(new SystemParamModel(JsonUtil.getObjectToString(listJson)));

        for (DataInterfaceModel item : listJson) {
            if (item.getSourceType() != null) {
                switch (item.getSourceType()) {
                    case 1://字段
                        map.put(item.getField(), item.getDefaultValue());
                        break;
                    case 2://自定义
                        map.put(item.getField(), item.getRelationField());
                        break;
                    case 3://为空
                        map.put(item.getField(), "");
                        break;
                    case 4://系统参数
                        map.put(item.getField(), this.getSystemFieldValue(item, systemFieldValue));
                        break;
                    default:
                        map.put(item.getField(), item.getDefaultValue());
                        break;
                }
            } else {
                map.put(item.getField(), item.getDefaultValue());
            }
        }
    }

    /**
     * 获取系统参数值
     *
     * @param templateJsonModel
     * @return
     */
    private String getSystemFieldValue(DataInterfaceModel templateJsonModel, Map<String, String> systemFieldValue) {
        String relationField = templateJsonModel.getRelationField();
        String dataValue;
        if (AuthorizeConditionEnum.FORMID.getCondition().equals(relationField)) {
            dataValue = String.valueOf(templateJsonModel.getDefaultValue());
        } else if (AuthorizeConditionEnum.getResListType().contains(relationField)) {
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

}
