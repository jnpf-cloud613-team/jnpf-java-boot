package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.util.*;

import java.util.ArrayList;
import java.util.List;


/**
 * 字典数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface DictionaryDataMapper extends SuperMapper<DictionaryDataEntity> {

    default List<DictionaryDataEntity> getList(String dictionaryTypeId, Boolean enable) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId);
        if (Boolean.TRUE.equals(enable)) {
            queryWrapper.lambda().eq(DictionaryDataEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(DictionaryDataEntity::getSortCode)
                .orderByDesc(DictionaryDataEntity::getCreatorTime)
                .orderByDesc(DictionaryDataEntity::getLastModifyTime);
        return this.selectList(queryWrapper);
    }

    default List<DictionaryDataEntity> getList(String dictionaryTypeId) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId);
        queryWrapper.lambda().orderByAsc(DictionaryDataEntity::getSortCode)
                .orderByDesc(DictionaryDataEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<DictionaryDataEntity> getDicList(String dictionaryTypeId) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(
                t -> t.eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId)
                        .or().eq(DictionaryDataEntity::getEnCode, dictionaryTypeId)
        );
        queryWrapper.lambda().select(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName, DictionaryDataEntity::getEnCode);
        return this.selectList(queryWrapper);
    }

    default List<DictionaryDataEntity> geDicList(String dictionaryTypeId) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(
                t -> t.eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId)
                        .or().eq(DictionaryDataEntity::getEnCode, dictionaryTypeId)
        );
        queryWrapper.lambda().select(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName, DictionaryDataEntity::getEnabledMark);
        return this.selectList(queryWrapper);
    }

    default Boolean isExistSubset(String parentId) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getParentId, parentId);
        return !this.selectList(queryWrapper).isEmpty();
    }

    default DictionaryDataEntity getInfo(String id) {
        if (id == null) {
            return null;
        }
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default DictionaryDataEntity getSwapInfo(String value, String dictionaryTypeId) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId).and(
                t -> t.eq(DictionaryDataEntity::getId, value)
                        .or().eq(DictionaryDataEntity::getEnCode, value)
        );
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String dictionaryTypeId, String fullName, String id) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getFullName, fullName).eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DictionaryDataEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String dictionaryTypeId, String enCode, String id) {
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryDataEntity::getEnCode, enCode).eq(DictionaryDataEntity::getDictionaryTypeId, dictionaryTypeId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DictionaryDataEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void delete(DictionaryDataEntity entity) {
        this.deleteById(entity.getId());
    }

    default void create(DictionaryDataEntity entity) {
        //判断id是否为空,为空则为新建
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
            entity.setSimpleSpelling(PinYinUtil.getFirstSpell(entity.getFullName()).toUpperCase());
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
        }
        this.insert(entity);
    }

    default boolean update(String id, DictionaryDataEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        int i = this.updateById(entity);
        return i > 0;
    }

    default boolean first(String id) {
        boolean isOk = false;
        //获取要上移的那条数据的信息
        DictionaryDataEntity upEntity = this.selectById(id);
        Long upSortCode = upEntity.getSortCode() == null ? 0 : upEntity.getSortCode();
        //查询上几条记录
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DictionaryDataEntity::getDictionaryTypeId, upEntity.getDictionaryTypeId())
                .eq(DictionaryDataEntity::getParentId, upEntity.getParentId())
                .lt(DictionaryDataEntity::getSortCode, upSortCode)
                .orderByDesc(DictionaryDataEntity::getSortCode);
        List<DictionaryDataEntity> downEntity = this.selectList(queryWrapper);
        if (!downEntity.isEmpty()) {
            //交换两条记录的sort值
            Long temp = upEntity.getSortCode();
            upEntity.setSortCode(downEntity.get(0).getSortCode());
            downEntity.get(0).setSortCode(temp);
            updateById(downEntity.get(0));
            updateById(upEntity);
            isOk = true;
        }
        return isOk;
    }

    default boolean next(String id) {
        boolean isOk = false;
        //获取要下移的那条数据的信息
        DictionaryDataEntity downEntity = this.selectById(id);
        Long upSortCode = downEntity.getSortCode() == null ? 0 : downEntity.getSortCode();
        //查询下几条记录
        QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DictionaryDataEntity::getDictionaryTypeId, downEntity.getDictionaryTypeId())
                .eq(DictionaryDataEntity::getParentId, downEntity.getParentId())
                .gt(DictionaryDataEntity::getSortCode, upSortCode)
                .orderByAsc(DictionaryDataEntity::getSortCode);
        List<DictionaryDataEntity> upEntity = this.selectList(queryWrapper);
        if (!upEntity.isEmpty()) {
            //交换两条记录的sort值
            Long temp = downEntity.getSortCode();
            downEntity.setSortCode(upEntity.get(0).getSortCode());
            upEntity.get(0).setSortCode(temp);
            updateById(upEntity.get(0));
            updateById(downEntity);
            isOk = true;
        }
        return isOk;
    }

    default List<DictionaryDataEntity> getDictionName(List<String> id) {
        List<DictionaryDataEntity> dictionList = new ArrayList<>();
        if (id != null && !id.isEmpty()) {
            QueryWrapper<DictionaryDataEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().and(
                    t -> t.in(DictionaryDataEntity::getEnCode, id)
                            .or().in(DictionaryDataEntity::getId, id)
            );
            queryWrapper.lambda().orderByAsc(DictionaryDataEntity::getParentId);
            dictionList = this.selectList(queryWrapper);
        }
        return dictionList;
    }

    default List<DictionaryDataEntity> getListByTypeDataCode(String typeCode) {
        MPJLambdaWrapper<DictionaryDataEntity> queryWrapper = JoinWrappers.lambda(DictionaryDataEntity.class);
        queryWrapper.leftJoin(DictionaryTypeEntity.class, DictionaryTypeEntity::getId, DictionaryDataEntity::getDictionaryTypeId);
        queryWrapper.selectAll(DictionaryDataEntity.class);
        queryWrapper.eq(DictionaryTypeEntity::getEnCode, typeCode);
        queryWrapper.orderByAsc(DictionaryDataEntity::getSortCode)
                .orderByDesc(DictionaryDataEntity::getCreatorTime);
        return this.selectJoinList(DictionaryDataEntity.class, queryWrapper);

    }

    default List<DictionaryDataEntity> getByTypeCodeEnable(String typeCode) {
        MPJLambdaWrapper<DictionaryDataEntity> queryWrapper = JoinWrappers.lambda(DictionaryDataEntity.class);
        queryWrapper.leftJoin(DictionaryTypeEntity.class, DictionaryTypeEntity::getId, DictionaryDataEntity::getDictionaryTypeId);
        queryWrapper.selectAll(DictionaryDataEntity.class);
        queryWrapper.eq(DictionaryTypeEntity::getEnCode, typeCode);
        queryWrapper.eq(DictionaryDataEntity::getEnabledMark, 1);
        queryWrapper.orderByAsc(DictionaryDataEntity::getSortCode)
                .orderByDesc(DictionaryDataEntity::getCreatorTime);
        return this.selectJoinList(DictionaryDataEntity.class, queryWrapper);
    }

}
