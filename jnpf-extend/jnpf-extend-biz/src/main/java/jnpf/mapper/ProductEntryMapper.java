package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ProductEntryEntity;
import jnpf.util.StringUtil;

import java.util.List;

/**
 *
 * base_productentry
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 10:40:59
 */
public interface ProductEntryMapper extends SuperMapper<ProductEntryEntity> {
    
   default List<ProductEntryEntity> getProductentryEntityList(String id) {
        QueryWrapper<ProductEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductEntryEntity::getProductId, id);
        return this.selectList(queryWrapper);
    }

   default List<ProductEntryEntity> getProductentryEntityList(Pagination pagination) {
        QueryWrapper<ProductEntryEntity> queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(pagination.getKeyword())){
            queryWrapper.lambda().and(
                    t->t.like(ProductEntryEntity::getProductName, pagination.getKeyword())
                            .or().like(ProductEntryEntity::getProductCode, pagination.getKeyword())
                            .or().like(ProductEntryEntity::getProductSpecification, pagination.getKeyword())
            );
        }
        Page<ProductEntryEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ProductEntryEntity> userIPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userIPage.getRecords(), userIPage.getTotal());
    }

}
