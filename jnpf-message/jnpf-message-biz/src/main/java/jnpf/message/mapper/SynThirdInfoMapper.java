package jnpf.message.mapper;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.SuperBaseEntity;
import jnpf.base.mapper.SuperMapper;

import jnpf.base.model.synthird.PaginationSynThirdInfo;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.model.SynThirdInfoVo;
import jnpf.message.util.SynThirdConsts;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第三方工具对象同步表
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/23 17:25
 */
public interface SynThirdInfoMapper extends SuperMapper<SynThirdInfoEntity> {

    default List<SynThirdInfoEntity> getList(String thirdType, String dataType) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getDataType, Integer.valueOf(dataType)));
        queryWrapper.lambda().orderByAsc(SynThirdInfoEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<SynThirdInfoEntity> getList(String thirdType, String dataType, String enableMark) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getDataType, Integer.valueOf(dataType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getEnabledMark, Integer.valueOf(enableMark)));
        queryWrapper.lambda().orderByAsc(SynThirdInfoEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default SynThirdInfoEntity getInfo(String id) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SynThirdInfoEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(SynThirdInfoEntity entity) {
        String sysObjId = entity.getSysObjId();
        Integer thirdType = entity.getThirdType();
        Integer dataType = entity.getDataType();
        LambdaQueryWrapper<SynThirdInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SynThirdInfoEntity::getSysObjId, sysObjId);
        wrapper.eq(SynThirdInfoEntity::getThirdType, thirdType);
        wrapper.eq(SynThirdInfoEntity::getDataType, dataType);
        SynThirdInfoEntity one = this.selectOne(wrapper);
        if (BeanUtil.isNotEmpty(one)) {
            BeanUtil.copyProperties(entity, one, "id");
            this.updateById(one);
            return;
        }
        this.insert(entity);
    }

    default SynThirdInfoEntity getInfoBySysObjId(String thirdType, String dataType, String id) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getDataType, Integer.valueOf(dataType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getSysObjId, id));
        return this.selectOne(queryWrapper);
    }

    default List<SynThirdInfoVo> getListJoin(PaginationSynThirdInfo paginationSynThirdInfo) {
        MPJLambdaWrapper<SynThirdInfoEntity> wrapper = JoinWrappers.lambda(SynThirdInfoEntity.class);
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getThirdType
                , Integer.valueOf(paginationSynThirdInfo.getThirdType())));
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getDataType
                , Integer.valueOf(paginationSynThirdInfo.getType())));
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getEnabledMark
                , Integer.valueOf(paginationSynThirdInfo.getResultType())));
        if (StringUtil.isNotEmpty(paginationSynThirdInfo.getKeyword())) {
            String keyword = paginationSynThirdInfo.getKeyword();
            wrapper.and(t -> {
                t.like(SynThirdInfoEntity::getDescription, keyword);
                t.or().like(SynThirdInfoEntity::getThirdName, keyword);
                if (paginationSynThirdInfo.getType().equals(SynThirdConsts.DATA_TYPE_ORG)) {
                    t.or().like(OrganizeEntity::getFullName, keyword);
                }
            });

        }
        wrapper.selectAs(SynThirdInfoEntity::getId, SynThirdInfoVo::getId)
                .selectAs(SynThirdInfoEntity::getDataType, SynThirdInfoVo::getDataType)
                .selectAs(SynThirdInfoEntity::getEnabledMark, SynThirdInfoVo::getEnabledMark)
                .selectAs(SynThirdInfoEntity::getCreatorTime, SynThirdInfoVo::getCreatorTime)
                .selectAs(SynThirdInfoEntity::getLastModifyTime, SynThirdInfoVo::getLastModifyTime)
                .selectAs(SynThirdInfoEntity::getThirdType, SynThirdInfoVo::getThirdType)
                .selectAs(SynThirdInfoEntity::getThirdName, SynThirdInfoVo::getThirdName)
                .selectAs(SynThirdInfoEntity::getDescription, SynThirdInfoVo::getDescription)
                .selectAs(SynThirdInfoEntity::getSysObjId, SynThirdInfoVo::getSysObjId);


        if (paginationSynThirdInfo.getType().equals(SynThirdConsts.DATA_TYPE_ORG)) {
            wrapper.leftJoin(OrganizeEntity.class, OrganizeEntity::getId, SynThirdInfoEntity::getSysObjId);
            wrapper.selectAs(OrganizeEntity::getFullName, SynThirdInfoVo::getSystemObjectName);
        } else {
            wrapper.leftJoin(UserEntity.class, UserEntity::getId, SynThirdInfoEntity::getSysObjId);
            wrapper.selectAs(UserEntity::getRealName, SynThirdInfoVo::getSystemObjectName);
        }
        Page<SynThirdInfoVo> page = new Page<>(paginationSynThirdInfo.getCurrentPage(), paginationSynThirdInfo.getPageSize());
        Page<SynThirdInfoVo> synThirdInfoVoPage = this.selectJoinPage(page, SynThirdInfoVo.class, wrapper);
        return paginationSynThirdInfo.setData(synThirdInfoVoPage.getRecords(), page.getTotal());
    }

    default List<SynThirdInfoEntity> syncThirdInfoByType(String thirdToSysType, String dataTypeOrg, String sysToThirdType) {

        HashMap<String, String> typeMap = new HashMap<>();
        typeMap.put(sysToThirdType, thirdToSysType);
        typeMap.put(thirdToSysType, sysToThirdType);

        List<SynThirdInfoEntity> synThirdInfoList = this.getList(thirdToSysType, dataTypeOrg);
        List<SynThirdInfoEntity> synThirdInfoDingList = this.getList(typeMap.get(thirdToSysType), dataTypeOrg);

        // 记录已经存在的组合
        HashMap<String, Boolean> existingMap = new HashMap<>();
        synThirdInfoList.forEach(k -> {
            String tag = k.getThirdType() + "-" + k.getDataType() + "-" + k.getSysObjId() + "-" + k.getThirdObjId();
            existingMap.put(tag, true);
        });
        synThirdInfoDingList.forEach(k -> {
            String tag = k.getThirdType() + "-" + k.getDataType() + "-" + k.getSysObjId() + "-" + k.getThirdObjId();
            existingMap.put(tag, true);
        });

        HashMap<String, SynThirdInfoEntity> mapSource = new HashMap<>();
        HashMap<String, SynThirdInfoEntity> mapTarget = new HashMap<>();
        String tag = "";
        for (SynThirdInfoEntity entity : synThirdInfoList) {
            tag = entity.getSysObjId() + "-" + entity.getThirdObjId();
            mapSource.put(tag, entity);
        }
        for (SynThirdInfoEntity entity : synThirdInfoDingList) {
            tag = entity.getSysObjId() + "-" + entity.getThirdObjId();
            mapTarget.put(tag, entity);
        }

        // 同步记录
        List<SynThirdInfoEntity> synThirdInfoAddList = new ArrayList<>();
        SynThirdInfoEntity addEntity = null;
        if (mapSource.size() == 0 && mapTarget.size() == 0) {
            return new ArrayList<>();
        } else if (mapSource.size() > 0 && mapTarget.size() == 0) {
            for (Map.Entry<String, SynThirdInfoEntity> item : mapSource.entrySet()) {
                SynThirdInfoEntity synThirdInfoEntity = item.getValue();
                addEntity = JsonUtil.getJsonToBean(synThirdInfoEntity, SynThirdInfoEntity.class);
                addEntity.setId(RandomUtil.uuId());
                addEntity.setThirdType(Integer.valueOf(typeMap.get(thirdToSysType)));
                synThirdInfoAddList.add(addEntity);
            }

        } else if (mapSource.size() == 0 && mapTarget.size() > 0) {
            for (Map.Entry<String, SynThirdInfoEntity> item : mapTarget.entrySet()) {
                SynThirdInfoEntity synThirdInfoEntity = item.getValue();
                addEntity = JsonUtil.getJsonToBean(synThirdInfoEntity, SynThirdInfoEntity.class);
                addEntity.setId(RandomUtil.uuId());
                addEntity.setThirdType(Integer.valueOf(thirdToSysType));
                synThirdInfoAddList.add(addEntity);
            }
        } else {
            for (Map.Entry<String, SynThirdInfoEntity> item : mapSource.entrySet()) {
                if (!mapTarget.containsKey(item.getKey())) {
                    SynThirdInfoEntity synThirdInfoEntity = item.getValue();
                    addEntity = JsonUtil.getJsonToBean(synThirdInfoEntity, SynThirdInfoEntity.class);
                    addEntity.setId(RandomUtil.uuId());
                    addEntity.setThirdType(Integer.valueOf(typeMap.get(thirdToSysType)));
                    synThirdInfoAddList.add(addEntity);
                }
            }
            for (Map.Entry<String, SynThirdInfoEntity> item : mapTarget.entrySet()) {
                if (!mapSource.containsKey(item.getKey())) {
                    SynThirdInfoEntity synThirdInfoEntity = item.getValue();
                    addEntity = JsonUtil.getJsonToBean(synThirdInfoEntity, SynThirdInfoEntity.class);
                    addEntity.setId(RandomUtil.uuId());
                    addEntity.setThirdType(Integer.valueOf(thirdToSysType));
                    synThirdInfoAddList.add(addEntity);
                }
            }

        }

        ArrayList<SynThirdInfoEntity> addList = new ArrayList<>();
        if (!synThirdInfoAddList.isEmpty()) {
            // 过滤
            synThirdInfoAddList.forEach(k -> {
                String addTag = k.getThirdType() + "-" + k.getDataType() + "-" + k.getSysObjId() + "-" + k.getThirdObjId();
                if (existingMap.get(addTag) == null) {
                    addList.add(k);
                }
            });
            this.insert(addList);
        }
        // 查找对应的数据
        synThirdInfoList = this.getList(thirdToSysType, dataTypeOrg);
        return synThirdInfoList;
    }

    default List<String> selectAllFail() {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getEnabledMark, "2"));
        List<SynThirdInfoEntity> lists = this.selectList(queryWrapper);
        return lists.stream().map(t -> t.getId()).collect(Collectors.toList());
    }

    default boolean getBySysObjId(String id, String thirdType) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SynThirdInfoEntity::getEnabledMark, 1);
        queryWrapper.lambda().eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType));
        queryWrapper.lambda().eq(SynThirdInfoEntity::getSysObjId, id);
        List<SynThirdInfoEntity> list = this.selectList(queryWrapper);
        return list != null && !list.isEmpty();
    }

    default String getSysByThird(String valueOf) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNotNull(SynThirdInfoEntity::getSysObjId);
        queryWrapper.lambda().eq(SynThirdInfoEntity::getThirdObjId, valueOf);
        List<SynThirdInfoEntity> list = this.selectList(queryWrapper);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getSysObjId();
        }
        return null;
    }

    default String getSysByThird(String valueOf, Integer type) {

        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNotNull(SynThirdInfoEntity::getSysObjId);
        queryWrapper.lambda().eq(SynThirdInfoEntity::getThirdType, type);
        queryWrapper.lambda().eq(SynThirdInfoEntity::getThirdObjId, valueOf);
        List<SynThirdInfoEntity> list = this.selectList(queryWrapper);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getSysObjId();
        }
        return null;
    }

    default SynThirdInfoEntity getInfoByThirdObjId(String thirdType, String dataType, String thirdObjId) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getDataType, Integer.valueOf(dataType)));
        queryWrapper.lambda().and(t -> t.eq(SynThirdInfoEntity::getThirdObjId, thirdObjId));
        return this.selectOne(queryWrapper);
    }
}
