package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ProductEntity;
import jnpf.model.product.ProductPagination;
import jnpf.util.StringUtil;

import java.util.List;

/**
 *
 * 销售订单
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 10:40:59
 */
public interface ProductMapper extends SuperMapper<ProductEntity> {

    default List<ProductEntity> getList(ProductPagination productPagination) {
        QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(productPagination.getCode())) {
            queryWrapper.lambda().and(t -> t.like(ProductEntity::getEnCode, productPagination.getCode()));
        }
        if (StringUtil.isNotEmpty(productPagination.getCustomerName())) {
            queryWrapper.lambda().and(t -> t.like(ProductEntity::getCustomerName, productPagination.getCustomerName()));
        }
        if (StringUtil.isNotEmpty(productPagination.getContactTel())) {
            queryWrapper.lambda().and(t -> t.like(ProductEntity::getContactTel, productPagination.getContactTel()));
        }
        //排序
        queryWrapper.lambda().orderByDesc(ProductEntity::getCreatorTime);
        Page<ProductEntity> page = new Page<>(productPagination.getCurrentPage(), productPagination.getPageSize());
        IPage<ProductEntity> userIPage = this.selectPage(page, queryWrapper);
        return productPagination.setData(userIPage.getRecords(), userIPage.getTotal());
    }

    default ProductEntity getInfo(String id) {
        QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

}
