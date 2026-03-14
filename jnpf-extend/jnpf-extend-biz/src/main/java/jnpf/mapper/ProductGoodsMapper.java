package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ProductGoodsEntity;
import jnpf.model.productgoods.ProductGoodsPagination;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 *
 * 产品商品
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 15:57:50
 */
public interface ProductGoodsMapper extends SuperMapper<ProductGoodsEntity> {

    default List<ProductGoodsEntity> getGoodList(String type) {
        QueryWrapper<ProductGoodsEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(ProductGoodsEntity::getType, type);
        }
        return this.selectList(queryWrapper);
    }

    default List<ProductGoodsEntity> getList(ProductGoodsPagination goodsPagination) {
        QueryWrapper<ProductGoodsEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(goodsPagination.getCode())) {
            queryWrapper.lambda().like(ProductGoodsEntity::getEnCode, goodsPagination.getCode());
        }
        if (StringUtil.isNotEmpty(goodsPagination.getFullName())) {
            queryWrapper.lambda().like(ProductGoodsEntity::getFullName, goodsPagination.getFullName());
        }
        if (StringUtil.isNotEmpty(goodsPagination.getClassifyId())) {
            queryWrapper.lambda().like(ProductGoodsEntity::getClassifyId, goodsPagination.getClassifyId());
        }
        if (StringUtil.isNotEmpty(goodsPagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ProductGoodsEntity::getFullName, goodsPagination.getKeyword())
                            .or().like(ProductGoodsEntity::getEnCode, goodsPagination.getKeyword())
                            .or().like(ProductGoodsEntity::getProductSpecification, goodsPagination.getKeyword())
            );
        }
        //排序
        if (StringUtil.isEmpty(goodsPagination.getSidx())) {
            queryWrapper.lambda().orderByDesc(ProductGoodsEntity::getId);
        } else {
            queryWrapper = "asc".equalsIgnoreCase(goodsPagination.getSort()) ? queryWrapper.orderByAsc(goodsPagination.getSidx()) : queryWrapper.orderByDesc(goodsPagination.getSidx());
        }
        Page<ProductGoodsEntity> page = new Page<>(goodsPagination.getCurrentPage(), goodsPagination.getPageSize());
        IPage<ProductGoodsEntity> userIPage = this.selectPage(page, queryWrapper);
        return goodsPagination.setData(userIPage.getRecords(), userIPage.getTotal());
    }

    default ProductGoodsEntity getInfo(String id) {
        QueryWrapper<ProductGoodsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductGoodsEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(ProductGoodsEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        this.insert(entity);
    }

    default boolean update(String id, ProductGoodsEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        return this.updateById(entity)>0;
    }

    default void delete(ProductGoodsEntity entity) {
        if (entity != null) {
            this.deleteById(entity.getId());
        }
    }

}
