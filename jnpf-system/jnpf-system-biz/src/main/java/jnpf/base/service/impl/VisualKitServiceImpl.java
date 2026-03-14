package jnpf.base.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.VisualKitEntity;
import jnpf.base.mapper.DictionaryDataMapper;
import jnpf.base.mapper.VisualKitMapper;
import jnpf.base.model.visualkit.KitPagination;
import jnpf.base.model.visualkit.KitTreeVo;
import jnpf.base.model.visualkit.VisualKitForm;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualKitService;
import jnpf.constant.CodeConst;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.permission.service.CodeNumService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表单套件
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:11:35
 */
@Service
@RequiredArgsConstructor
public class VisualKitServiceImpl extends SuperServiceImpl<VisualKitMapper, VisualKitEntity> implements VisualKitService {


    private final CodeNumService codeNumService;

    private final DictionaryDataMapper dictionaryDataMapper;

    @Override
    public List<VisualKitEntity> getList(KitPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public void saveCheck(VisualKitEntity visualKitEntity, Boolean fullNameCheck, Boolean encodeCheck) {
        String fullName = visualKitEntity.getFullName();
        String encode = visualKitEntity.getEnCode();
        // 名称长度验证
        if (fullName.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }
        QueryWrapper<VisualKitEntity> query = new QueryWrapper<>();
        //重名验证
        if (Boolean.TRUE.equals(fullNameCheck)) {
            query.lambda().eq(VisualKitEntity::getFullName, fullName);
            if (!this.list(query).isEmpty()) {
                throw new DataException(MsgCode.EXIST001.get());
            }
        }
        //编码验证
        if (Boolean.TRUE.equals(encodeCheck)) {
            query.clear();
            query.lambda().eq(VisualKitEntity::getEnCode, encode);
            if (!this.list(query).isEmpty()) {
                throw new DataException(MsgCode.EXIST002.get());
            }
        }
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public void create(VisualKitForm form) {
        clearFormDataTable(form);
        VisualKitEntity entity = JsonUtil.getJsonToBean(form, VisualKitEntity.class);
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.BDTJ), code -> this.isExistByEnCode(code, null)));
            form.setEnCode(entity.getEnCode());
        }
        this.saveCheck(entity, true, true);
        this.baseMapper.create(entity);
        form.setId(entity.getId());
    }

    @Override
    public boolean update(String id, VisualKitForm form) {
        clearFormDataTable(form);
        VisualKitEntity entity = JsonUtil.getJsonToBean(form, VisualKitEntity.class);
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.BDTJ), code -> this.isExistByEnCode(code, id)));
            form.setEnCode(entity.getEnCode());
        }
        VisualKitEntity byId = this.getById(id);
        this.saveCheck(entity, !byId.getFullName().equals(form.getFullName()), !byId.getEnCode().equals(form.getEnCode()));
        return this.baseMapper.update(id, entity);
    }

    @Override
    public List<KitTreeVo> selectorList() {
        List<DictionaryDataEntity> typeList = dictionaryDataMapper.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        QueryWrapper<VisualKitEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualKitEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(VisualKitEntity::getSortCode)
                .orderByDesc(VisualKitEntity::getCreatorTime);
        List<VisualKitEntity> list = this.list(queryWrapper);
        Map<String, List<VisualKitEntity>> collect = list.stream().collect(Collectors.groupingBy(VisualKitEntity::getCategory));
        List<KitTreeVo> resList = new ArrayList<>();
        for (Map.Entry<String, List<VisualKitEntity>> entry : collect.entrySet()) {
            String key = entry.getKey();
            DictionaryDataEntity dataEntity = typeList.stream().filter(t -> t.getId().equals(key)).findFirst().orElse(new DictionaryDataEntity());
            KitTreeVo kitTreeVo = new KitTreeVo();
            kitTreeVo.setId(key);
            kitTreeVo.setFullName(dataEntity.getFullName());
            kitTreeVo.setEnCode(dataEntity.getEnCode());
            kitTreeVo.setHasChildren(true);
            List<VisualKitEntity> visualKitEntities = collect.get(key);
            List<KitTreeVo> jsonToList = JsonUtil.getJsonToList(visualKitEntities, KitTreeVo.class);
            jsonToList.forEach(t ->
                t.setParentId(key)
            );
            kitTreeVo.setChildren(jsonToList);
            resList.add(kitTreeVo);
        }
        return resList;
    }

    @Override
    public void actionsCopy(String id) {
        VisualKitEntity entity = this.getById(id);
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        String fullName = entity.getFullName() + ".副本" + copyNum;
        if (fullName.length() > 80) {
            throw new DataException(MsgCode.PRI006.get());
        }
        VisualKitForm form = JsonUtil.getJsonToBean(entity, VisualKitForm.class);
        form.setId(RandomUtil.uuId());
        form.setFullName(fullName);
        form.setEnCode(entity.getEnCode() + copyNum);
        form.setEnabledMark(0);
        this.create(form);
    }

    @Override
    public String importData(VisualKitEntity entity, Integer type) {
        QueryWrapper<VisualKitEntity> queryWrapper = new QueryWrapper<>();
        StringJoiner stringJoiner = new StringJoiner("、");
        if (this.getById(entity.getId()) != null) {
            if (Objects.equals(type, 0)) {
                stringJoiner.add("ID");
            } else {
                entity.setId(RandomUtil.uuId());
            }
        }
        queryWrapper.clear();
        queryWrapper.lambda().eq(VisualKitEntity::getEnCode, entity.getEnCode());
        if (this.count(queryWrapper) > 0) {
            stringJoiner.add(MsgCode.IMP009.get());
        }
        queryWrapper.clear();
        queryWrapper.lambda().eq(VisualKitEntity::getFullName, entity.getFullName());
        if (this.count(queryWrapper) > 0) {
            stringJoiner.add(MsgCode.IMP008.get());
        }
        if (stringJoiner.length() > 0 && ObjectUtil.equal(type, 1)) {
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
        } else if (ObjectUtil.equal(type, 0) && stringJoiner.length() > 0) {
            return stringJoiner.toString() + MsgCode.IMP007.get();
        }
        entity.setEnabledMark(0);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getLoginUserId());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        this.save(entity);
        return "";
    }

    /**
     * 清空formdata内的表信息
     *
     * @param form
     */
    private void clearFormDataTable(VisualKitForm form) {
        Map<String, Object> formMap = JsonUtil.stringToMap(form.getFormData());
        JSONArray formJsonArray = JsonUtil.getJsonToJsonArray(String.valueOf(formMap.get("fields")));
        this.recuClearTable(formJsonArray);
        formMap.put("fields", formJsonArray);
        form.setFormData(JsonUtil.getObjectToString(formMap));
    }

    /**
     * 递归清除
     *
     * @param formJsonArray
     */
    private void recuClearTable(JSONArray formJsonArray) {
        for (Object o : formJsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            JSONObject config = jsonObject.getJSONObject("__config__");
            boolean hasChange = false;
            if (ObjectUtil.isNotEmpty(config.get("tableName"))) {
                config.put("tableName", "");
                hasChange = true;
            }
            if (ObjectUtil.isNotEmpty(config.get("relationTable"))) {
                config.put("relationTable", "");
                hasChange = true;
            }
            JSONArray childArray = config.getJSONArray("children");
            if (ObjectUtil.isNotEmpty(childArray)) {
                this.recuClearTable(childArray);
                config.put("children", childArray);
                hasChange = true;
            }
            if (hasChange) {
                jsonObject.put("__config__", config);
            }
        }
    }
}
