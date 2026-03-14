package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;


/**
 * 字典分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface DictionaryTypeMapper extends SuperMapper<DictionaryTypeEntity> {

    default List<DictionaryTypeEntity> getList() {
        QueryWrapper<DictionaryTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(DictionaryTypeEntity::getSortCode)
                .orderByDesc(DictionaryTypeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default DictionaryTypeEntity getInfoByEnCode(String enCode) {
        QueryWrapper<DictionaryTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryTypeEntity::getEnCode, enCode);
        return this.selectOne(queryWrapper);
    }

    default DictionaryTypeEntity getInfo(String id) {
        QueryWrapper<DictionaryTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryTypeEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<DictionaryTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryTypeEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DictionaryTypeEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<DictionaryTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DictionaryTypeEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DictionaryTypeEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(DictionaryTypeEntity entity) {
        //判断id是否为空,为空则为新建
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
        }
        this.insert(entity);
    }

    default boolean update(String id, DictionaryTypeEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        int i = this.updateById(entity);
        return i > 0;
    }
}
