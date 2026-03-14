package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.ProductGoodsEntity;
import jnpf.mapper.ProductGoodsMapper;
import jnpf.model.productgoods.ProductGoodsPagination;
import jnpf.service.ProductGoodsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品商品
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 15:57:50
 */
@Service
public class ProductGoodsServiceImpl extends SuperServiceImpl<ProductGoodsMapper, ProductGoodsEntity> implements ProductGoodsService {


    @Override
    public List<ProductGoodsEntity> getGoodList(String type) {
        return this.baseMapper.getGoodList(type);
    }

    @Override
    public List<ProductGoodsEntity> getList(ProductGoodsPagination goodsPagination) {
        return this.baseMapper.getList(goodsPagination);
    }

    @Override
    public ProductGoodsEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(ProductGoodsEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ProductGoodsEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(ProductGoodsEntity entity) {
        this.baseMapper.delete(entity);
    }

}
