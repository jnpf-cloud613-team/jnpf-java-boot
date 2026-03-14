package jnpf.service;

import jnpf.base.service.SuperService;
import jnpf.entity.ProductGoodsEntity;
import jnpf.model.productgoods.ProductGoodsPagination;

import java.util.List;

/**
 *
 * 产品商品
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 15:57:50
 */
public interface ProductGoodsService extends SuperService<ProductGoodsEntity> {

    List<ProductGoodsEntity> getGoodList(String type);

    List<ProductGoodsEntity> getList(ProductGoodsPagination productgoodsPagination);

    ProductGoodsEntity getInfo(String id);

    void delete(ProductGoodsEntity entity);

    void create(ProductGoodsEntity entity);

    boolean update( String id, ProductGoodsEntity entity);

}
