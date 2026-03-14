package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ProductclassifyEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * 产品分类
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 14:34:04
 */
public interface ProductclassifyMapper extends SuperMapper<ProductclassifyEntity> {


    default List<ProductclassifyEntity> getList() {
        QueryWrapper<ProductclassifyEntity> queryWrapper = new QueryWrapper<>();
        return selectList(queryWrapper);
    }

    default ProductclassifyEntity getInfo(String id) {
        QueryWrapper<ProductclassifyEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductclassifyEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(ProductclassifyEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        this.insert(entity);
    }

    default boolean update(String id, ProductclassifyEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        return this.updateById(entity) > 0;
    }

    default void delete(ProductclassifyEntity entity) {
        if (entity != null) {
            this.deleteById(entity.getId());
        }
    }

}
