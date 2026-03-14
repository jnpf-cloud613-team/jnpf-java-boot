package jnpf.base.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.entity.BaseLangEntity;
import jnpf.base.model.language.BaseLangForm;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/20 10:02:18
 */
public interface BaseLangMapper extends SuperMapper<BaseLangEntity> {

    default void create(BaseLangForm form) {
        List<BaseLangEntity> baseLangEntities = this.getByEnodeLang(form.getEnCode(), null);
        if (CollUtil.isNotEmpty(baseLangEntities)) {
            throw new DataException(MsgCode.SYS051.get());
        }
        Map<String, String> map = form.getMap();
        boolean atLeastOne = false;
        if (MapUtils.isNotEmpty(map)) {
            atLeastOne = map.values().stream().anyMatch(v -> v != null && !v.trim().isEmpty());
        }
        if (!atLeastOne) {
            throw new DataException(MsgCode.SYS052.get());
        }
        String groupId = RandomUtil.uuId();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            BaseLangEntity entity = JsonUtil.getJsonToBean(form, BaseLangEntity.class);
            entity.setGroupId(groupId);
            entity.setLanguage(key);
            entity.setFullName(map.get(key));
            saveEntity(entity);
        }
    }

    default void update(BaseLangForm form) {
        List<BaseLangEntity> langList = getByGroupId(form.getId());
        if (CollectionUtils.isEmpty(langList)) {
            throw new DataException(MsgCode.FA002.get());
        }
        List<BaseLangEntity> baseLangEntities = getByEnodeLang(form.getEnCode(), null);
        List<BaseLangEntity> hasLang = baseLangEntities.stream().filter(t -> !t.getGroupId().equals(form.getId())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(hasLang)) {
            throw new DataException(MsgCode.SYS051.get());
        }

        Map<String, String> map = form.getMap();
        boolean atLeastOne = false;
        if (MapUtils.isNotEmpty(map)) {
            atLeastOne = map.values().stream().anyMatch(v -> v != null && !v.trim().isEmpty());
        }
        if (!atLeastOne) {
            throw new DataException(MsgCode.SYS052.get());
        }
        List<String> keyList = new ArrayList<>(map.keySet());
        String groupId = "";
        List<BaseLangEntity> deleteList = new ArrayList<>();
        for (BaseLangEntity t : langList) {
            if (!keyList.contains(t.getLanguage())) {
                deleteList.add(t);
            }
            groupId = t.getGroupId();
        }
        //删除移除的
        if (CollectionUtils.isNotEmpty(deleteList)) {
            this.deleteByIds(deleteList.stream().map(BaseLangEntity::getId).collect(Collectors.toList()));
        }

        for (String key : keyList) {
            BaseLangEntity entity = langList.stream().filter(t -> key.equals(t.getLanguage())).findFirst().orElse(new BaseLangEntity());
            if (StringUtil.isNotEmpty(entity.getId())) {
                entity.setEnCode(form.getEnCode());
                entity.setType(form.getType());
                entity.setFullName(map.get(key));
                entity.setLastModifyTime(DateUtil.getNowDate());
                entity.setLastModifyUserId(UserProvider.getUser().getUserId());
                this.updateById(entity);
            } else {
                entity.setId(RandomUtil.uuId());
                entity.setGroupId(groupId);
                entity.setEnCode(form.getEnCode());
                entity.setType(form.getType());
                entity.setLanguage(key);
                entity.setFullName(map.get(key));
                entity.setCreatorTime(new Date());
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                this.insert(entity);
            }
        }
    }

    default BaseLangForm getInfo(String groupId) {
        List<BaseLangEntity> byEnodeLang = getByGroupId(groupId);
        BaseLangForm form = new BaseLangForm();
        form.setId(groupId);
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (BaseLangEntity entity : byEnodeLang) {
            form.setEnCode(entity.getEnCode());
            form.setType(entity.getType());
            map.put(entity.getLanguage(), entity.getFullName());
        }
        form.setMap(map);
        return form;
    }

    default void delete(String groupId) {
        QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(BaseLangEntity::getId);
        queryWrapper.lambda().eq(BaseLangEntity::getGroupId, groupId);
        this.deleteByIds(this.selectList(queryWrapper));
    }

    default void saveEntity(BaseLangEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    /**
     * 根据翻译标记和语种获取数据
     *
     * @param enCode
     * @param language
     * @return
     */
    default List<BaseLangEntity> getByEnodeLang(String enCode, String language) {
        QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(enCode)) {
            queryWrapper.lambda().eq(BaseLangEntity::getEnCode, enCode);
        }
        if (StringUtil.isNotEmpty(language)) {
            queryWrapper.lambda().eq(BaseLangEntity::getLanguage, language);
        }
        return this.selectList(queryWrapper);
    }

    /**
     * 根据翻译标记id获取数据
     *
     * @param groupId
     * @return
     */
    default List<BaseLangEntity> getByGroupId(String groupId) {
        QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseLangEntity::getGroupId, groupId);
        queryWrapper.lambda().orderByAsc(BaseLangEntity::getSortCode).orderByAsc(BaseLangEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<BaseLangEntity> getServerLang(Locale locale) {
        QueryWrapper<BaseLangEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseLangEntity::getLanguage, locale.toLanguageTag());
        List<Integer> type = ImmutableList.of(0, 1);
        queryWrapper.lambda().in(BaseLangEntity::getType, type);
        return this.selectList(queryWrapper);
    }
}

