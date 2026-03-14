package jnpf.base.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.entity.BaseLangEntity;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.mapper.BaseLangMapper;
import jnpf.base.model.language.BaseLangForm;
import jnpf.base.model.language.BaseLangListVO;
import jnpf.base.model.language.BaseLangPage;
import jnpf.base.service.BaseLangService;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.ConfigConst;
import jnpf.constant.GlobalConst;
import jnpf.consts.ProjectEventConst;
import jnpf.event.ProjectEventListener;
import jnpf.i18n.config.I18nProperties;
import jnpf.i18n.constant.I18nConst;
import jnpf.module.ProjectEventBuilder;
import jnpf.module.ProjectEventInstance;
import jnpf.util.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 多语言
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/28 16:05:49
 */
@Service
@RequiredArgsConstructor
public class BaseLangServiceImpl extends SuperServiceImpl<BaseLangMapper, BaseLangEntity> implements BaseLangService {

    public static final String LABEL = "label";


    private final ConfigValueUtil configValueUtil;

    private final I18nProperties i18nProperties;


    private final DictionaryDataService dictionaryDataApi;

    // 未使用的情况下, 默认48小时失效, 使用后重新计算缓存时效 租户/语种/前端json
    private TimedCache<String, Map<String, String>> tenantMessageProperties = CacheUtil.newTimedCache(48 * 60 * 60000L);
    // 租户语言加载锁
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public BaseLangListVO getList(Pagination pagination) {
        List<DictionaryDataEntity> langTypeList = dictionaryDataApi.getListByTypeDataCode(ConfigConst.BASE_LANGUAGE);
        String path = FileUploadUtils.getLocalBasePath() + configValueUtil.getBaseLanguagePath();
        List<Map<String, Object>> tableHead = new ArrayList<>();
        List<Map<String, String>> list = new ArrayList<>();
        List<Map<String, String>> resList = new ArrayList<>();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("prop", "code");
        hashMap.put(LABEL, "翻译标记");
        tableHead.add(hashMap);
        //不同语言的转换map
        Map<String, LinkedHashMap<String, String>> langKeyMap = new HashMap<>();

        for (DictionaryDataEntity item : langTypeList) {
            HashMap<String, Object> objectHashMap = new HashMap<>();
            objectHashMap.put("prop", item.getEnCode());
            objectHashMap.put(LABEL, item.getFullName());
            tableHead.add(objectHashMap);
            String filePath = path + "java/" + item.getEnCode() + ".json";
            LinkedHashMap<String, String> resMap = new LinkedHashMap<>();
            try {
                File file = new File(filePath);
                @Cleanup FileInputStream fileInputStream = new FileInputStream(file);
                LinkedHashMap<String, Object> jsonObject = JSON.parseObject(fileInputStream, LinkedHashMap.class);
                this.parseMap(jsonObject, resMap, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            langKeyMap.put(item.getEnCode(), resMap);
        }


        for (DictionaryDataEntity item : langTypeList) {
            LinkedHashMap<String, String> eachMap = langKeyMap.get(item.getEnCode());
            for (Map.Entry<String, String> entry : eachMap.entrySet()) {
                String key = entry.getKey();
                Map<String, String> map = list.stream().filter(t -> t.get("code").equals(key)).findFirst().orElse(null);
                if (map != null) {
                    map.put(item.getEnCode(), eachMap.get(key));
                } else {
                    map = new HashMap<>();
                    map.put("code", key);
                    map.put(item.getEnCode(), eachMap.get(key));
                    list.add(map);
                }
            }
        }
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            list = list.stream().filter(t -> {
                for (Map.Entry<String, String> entry : t.entrySet()) {
                    String tkey = entry.getKey();
                    if (t.get(tkey).contains(pagination.getKeyword())) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }

        //假分页
        if (CollectionUtils.isNotEmpty(list)) {
            List<List<Map<String, String>>> partition = Lists.partition(list, (int) pagination.getPageSize());
            int i = (int) pagination.getCurrentPage() - 1;
            resList = partition.size() > i ? partition.get(i) : Collections.emptyList();
            pagination.setTotal(list.size());
        }

        BaseLangListVO vo = new BaseLangListVO();
        PaginationVO page = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        vo.setList(resList);
        vo.setTableHead(tableHead);
        vo.setPagination(page);
        return vo;
    }

    /**
     * 多层map，解析成单层key，value
     *
     * @param sourMap
     * @param resMap
     * @param supKey
     */
    public void parseMap(Map<String, Object> sourMap, Map<String, String> resMap, String supKey) {
        for (Map.Entry<String, Object> entry : sourMap.entrySet()) {
            String key = entry.getKey();
            if (sourMap.get(key).toString().startsWith("{") && sourMap.get(key).toString().endsWith("}")) {
                LinkedHashMap<String, Object> childSourMap = JSON.parseObject(sourMap.get(key).toString(), LinkedHashMap.class);
                String newSupKey = key;
                if (StringUtil.isNotEmpty(supKey)) {
                    newSupKey = supKey + "." + key;
                }
                this.parseMap(childSourMap, resMap, newSupKey);
            } else {
                String value = sourMap.get(key).toString();
                if (supKey != null) {
                    String skey = supKey + "." + key;
                    resMap.put(skey, value);
                } else {
                    resMap.put(key, value);
                }
            }
        }
    }

    /**
     * 根据不同语种查询列表最大列表
     *
     * @param pagination
     * @param langTypeList
     * @param idList
     * @return
     */
    private IPage<BaseLangEntity> getList(BaseLangPage pagination, List<DictionaryDataEntity> langTypeList, List<String> idList) {
        IPage<BaseLangEntity> listRes = null;
        for (DictionaryDataEntity item : langTypeList) {
            String enCode = item.getEnCode();
            QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(BaseLangEntity::getLanguage, enCode);
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.lambda().in(BaseLangEntity::getId, idList);
            }
            if (Objects.nonNull(pagination.getType())) {
                queryWrapper.lambda().eq(BaseLangEntity::getType, pagination.getType());
            }
            queryWrapper.lambda().orderByDesc(BaseLangEntity::getCreatorTime);
            Page<BaseLangEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<BaseLangEntity> list = this.page(page, queryWrapper);
            if (listRes == null) {
                listRes = list;
            } else {
                if (listRes.getTotal() < list.getTotal()) {
                    listRes = list;
                }
            }
        }
        return listRes;
    }

    @Override
    public BaseLangListVO list(BaseLangPage pagination) {
        List<DictionaryDataEntity> langTypeList = dictionaryDataApi.getListByTypeDataCode(ConfigConst.BASE_LANGUAGE);
        List<Map<String, Object>> tableHead = new ArrayList<>();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("prop", "code");
        hashMap.put(LABEL, "翻译标记");
        tableHead.add(hashMap);
        for (DictionaryDataEntity item : langTypeList) {
            HashMap<String, Object> objectHashMap = new HashMap<>();
            objectHashMap.put("prop", item.getEnCode());
            objectHashMap.put(LABEL, item.getFullName());
            tableHead.add(objectHashMap);
        }
        String keyword = pagination.getKeyword();
        List<String> idList = new ArrayList<>();
        if (StringUtil.isNotEmpty(keyword)) {
            QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().and(t -> t.like(BaseLangEntity::getEnCode, keyword)
                    .or().like(BaseLangEntity::getFullName, keyword));
            List<BaseLangEntity> list = list(queryWrapper);
            if (CollectionUtils.isEmpty(list)) {
                return new BaseLangListVO(new ArrayList<>(), tableHead, pagination);
            } else {
                idList = list.stream().map(BaseLangEntity::getId).collect(Collectors.toList());
            }
        }
        IPage<BaseLangEntity> list = getList(pagination, langTypeList, idList);
        List<Map<String, String>> resList = new ArrayList<>();
        if (null != list) {
            List<BaseLangEntity> baseLangEntities = pagination.setData(list.getRecords(), list.getTotal());


            for (BaseLangEntity item : baseLangEntities) {
                String groupId = item.getGroupId();
                Map<String, String> resMap = new LinkedHashMap<>();
                List<BaseLangEntity> byGroupId = this.getByGroupId(item.getGroupId());
                resMap.put("id", groupId);
                resMap.put("code", item.getEnCode());
                resMap.put("typeName", Objects.equals(item.getType(), 0) ? "客户端" : "服务端");
                for (BaseLangEntity entity : byGroupId) {
                    resMap.put(entity.getLanguage(), entity.getFullName());
                }
                resList.add(resMap);
            }

        }

        BaseLangListVO vo = new BaseLangListVO();
        PaginationVO pagevo = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        vo.setList(resList);
        vo.setTableHead(tableHead);
        vo.setPagination(pagevo);
        return vo;
    }

    @Override
    public void create(BaseLangForm form) {
        this.baseMapper.create(form);
        this.createReids(form.getType());
    }

    @Override
    public void update(BaseLangForm form) {
        this.baseMapper.update(form);
        this.createReids(form.getType());
    }

    @Override
    public BaseLangForm getInfo(String groupId) {
        return this.baseMapper.getInfo(groupId);
    }

    @Override
    public void delete(String groupId) {
        List<BaseLangEntity> byGroupId = getByGroupId(groupId);
        Integer type = byGroupId.get(0).getType();
        this.baseMapper.delete(groupId);
        this.createReids(type);
    }

    @Override
    public void importSaveOrUpdate(List<BaseLangEntity> list) {
        Map<String, String> enCodeGroupId = new HashMap<>();
        boolean hasFront = false;
        boolean hasServer = false;
        for (BaseLangEntity item : list) {
            if (Objects.equals(item.getType(), 0)) {
                hasFront = true;
            }
            if (Objects.equals(item.getType(), 1) || Objects.equals(item.getType(), 2)) {
                hasServer = true;
            }
            List<BaseLangEntity> byEnodeLang = getByEnodeLang(item.getEnCode());
            String groupId = RandomUtil.uuId();
            if (CollectionUtils.isNotEmpty(byEnodeLang)) {
                groupId = byEnodeLang.get(0).getGroupId();
                if (!enCodeGroupId.containsKey(byEnodeLang.get(0).getEnCode())) {
                    enCodeGroupId.put(item.getEnCode(), groupId);
                }
                BaseLangEntity entity = byEnodeLang.stream().filter(t -> t.getLanguage().equals(item.getLanguage())).findFirst().orElse(null);
                if (entity != null) {
                    boolean modify = false;
                    if (!Objects.equals(entity.getType(), item.getType())) {
                        entity.setType(item.getType());
                        modify = true;
                    }
                    if (!Objects.equals(entity.getFullName(), item.getFullName())) {
                        entity.setFullName(item.getFullName());
                        modify = true;
                    }
                    if (modify) {
                        entity.setLastModifyTime(new Date());
                        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
                        this.updateById(entity);
                    }
                } else {
                    entity = JsonUtil.getJsonToBean(item, BaseLangEntity.class);
                    entity.setGroupId(groupId);
                    saveEntity(entity);
                }
            } else {
                if (enCodeGroupId.containsKey(item.getEnCode())) {
                    groupId = enCodeGroupId.get(item.getEnCode());
                } else {
                    enCodeGroupId.put(item.getEnCode(), groupId);
                }
                BaseLangEntity entity = BeanUtil.copyProperties(item, BaseLangEntity.class);
                entity.setGroupId(groupId);
                saveEntity(entity);
            }
        }
        if (hasFront) {
            this.createReids(0);
        }
        if (hasServer) {
            this.createReids(1);
        }
    }

    private void saveEntity(BaseLangEntity entity) {
        this.baseMapper.saveEntity(entity);
    }

    /**
     * 根据翻译标记和语种获取数据
     *
     * @param enCode
     * @return
     */
    private List<BaseLangEntity> getByEnodeLang(String enCode) {
        return this.baseMapper.getByEnodeLang(enCode, null);
    }

    /**
     * 根据翻译标记id获取数据
     *
     * @param groupId
     * @return
     */
    private List<BaseLangEntity> getByGroupId(String groupId) {
        return this.baseMapper.getByGroupId(groupId);
    }

    /**
     * 创建修改添加缓存触发
     *
     * @param type
     */
    private void createReids(Integer type) {
        String tenantId = GlobalConst.DEFAULT_TENANT_VALUE;
        if (configValueUtil.isMultiTenancy()) {
            tenantId = TenantHolder.getDatasourceId();
        }
        String cacheType = I18nConst.CACHE_KEY_FRONT;
        if (Objects.equals(type, 1)) {
            cacheType = I18nConst.CACHE_KEY_SERVER;
        }
        String redisKey = cacheType + tenantId;
        PublishEventUtil.publish(new ProjectEventBuilder(redisKey, System.currentTimeMillis()).setMessageModel(ProjectEventConst.EVENT_PUBLISH_MODEL_BROADCASTING));
    }

    /**
     * 组装语种json
     *
     * @param language
     * @return
     */
    private String makeLanguageJson(String language) {
        QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseLangEntity::getLanguage, language);
        queryWrapper.lambda().eq(BaseLangEntity::getType, 0);
        JSONObject json = new JSONObject();
        List<BaseLangEntity> list = list(queryWrapper);
        for (BaseLangEntity baseLangEntity : list) {
//            不需要组装成对象。（组装成对象父级key如果被存储信息了转换json对象有问题）
            json.put(baseLangEntity.getEnCode(), baseLangEntity.getFullName());
        }
        return json.toJSONString();
    }

    /**
     * 递归添加属性
     *
     * @param enCode
     * @param json
     * @param fullName
     */
    private void addJsonTree(String enCode, JSONObject json, String fullName) {
        if (enCode.contains(".")) {
            String one = enCode.substring(0, enCode.indexOf("."));
            String two = enCode.substring(enCode.indexOf(".") + 1);
            JSONObject towJson = new JSONObject();
            if (json.containsKey(one)) {
                towJson = (JSONObject) json.get(one);
            }
            addJsonTree(two, towJson, fullName);
            json.put(one, towJson);
        } else {
            json.computeIfAbsent(enCode, k -> fullName);
        }
    }

    /**
     * 监听缓存修改语种信息
     *
     * @param redisEvent
     */
    @ProjectEventListener(channelRegex = I18nConst.CACHE_KEY_FRONT + ".*")
    public void onRedisKeySetEvent(ProjectEventInstance redisEvent) {
        String key = redisEvent.getChannel();
        // 获取多租户编码
        key = key.substring(I18nConst.CACHE_KEY_FRONT.length());
        tenantMessageProperties.remove(key);
    }

    /**
     * 初始化语种
     *
     * @param tenantId
     * @param locale
     */
    public void loadTenantMessage(String tenantId, Locale locale) {
        String languageJson = this.makeLanguageJson(locale.toLanguageTag());
        Map<String, String> tenantLangJson = tenantMessageProperties.get(tenantId);
        if (tenantLangJson == null) {
            tenantLangJson = new HashMap<>();
            tenantMessageProperties.put(tenantId, tenantLangJson);
        }
        tenantLangJson.put(locale.toLanguageTag(), languageJson);
    }

    /**
     * 前端获取语种json
     *
     * @param locale 语种
     * @return
     */
    @Override
    public String getLanguageJson(Locale locale) {
        // 默认租户或者当前租户
        String tenantId = GlobalConst.DEFAULT_TENANT_VALUE;
        if (configValueUtil.isMultiTenancy()) {
            tenantId = TenantHolder.getDatasourceId();
        }
        // 开启租户未获取到租户 不进行翻译获取
        if (tenantId != null) {
            String language;
            // 租户配置中的语言配置
            String languageTag = locale.toLanguageTag();
            if (!tenantMessageProperties.containsKey(tenantId) || !tenantMessageProperties.get(tenantId).containsKey(languageTag)) {
                // 租户其他线程正在加载多语言则直接返回
                ReentrantLock lock = lockMap.computeIfAbsent(tenantId, k -> new ReentrantLock());
                boolean isLock = false;
                try {
                    // 如果其他线程正在处理租户翻译则最多等待十秒钟
                    isLock = lock.tryLock(5L, TimeUnit.SECONDS);
                    if (isLock && (!tenantMessageProperties.containsKey(tenantId) || !tenantMessageProperties.get(tenantId).containsKey(languageTag))) {
                        loadTenantMessage(tenantId, locale);
                    }

                } catch (InterruptedException e) {
                    // 清除中断状态
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                } finally {
                    if (isLock) {
                        lock.unlock();
                    }
                }
            }
            language = tenantMessageProperties.get(tenantId).get(languageTag);
            // 语言不存在翻译配置, 强制返回默认配置语言, 已经是默认语言无法处理
            if ((language == null || "{}".equals(language)) && !Objects.equals(locale.toLanguageTag(), i18nProperties.getDefaultLanguage())) {
                return getLanguageJson(Locale.forLanguageTag(i18nProperties.getDefaultLanguage()));
            }
            return language;
        }
        return null;
    }

    @Override
    public List<BaseLangEntity> getServerLang(Locale locale) {
        return this.baseMapper.getServerLang(locale);
    }
}
