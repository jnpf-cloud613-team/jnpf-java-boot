package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.ProductclassifyEntity;
import jnpf.mapper.ProductclassifyMapper;
import jnpf.service.ProductclassifyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品分类
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 14:34:04
 */
@Service
public class ProductclassifyServiceImpl extends SuperServiceImpl<ProductclassifyMapper, ProductclassifyEntity> implements ProductclassifyService {


    @Override
    public List<ProductclassifyEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public ProductclassifyEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(ProductclassifyEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ProductclassifyEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(ProductclassifyEntity entity) {
        this.baseMapper.delete(entity);
    }

}
