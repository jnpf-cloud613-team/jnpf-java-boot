package jnpf.service.impl;

import jnpf.base.Pagination;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.ProductEntryEntity;
import jnpf.mapper.ProductEntryMapper;
import jnpf.service.ProductEntryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 销售订单明细
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 10:40:59
 */
@Service
public class ProductEntryServiceImpl extends SuperServiceImpl<ProductEntryMapper, ProductEntryEntity> implements ProductEntryService {

    @Override
    public List<ProductEntryEntity> getProductentryEntityList(String id) {
        return this.baseMapper.getProductentryEntityList(id);
    }

    @Override
    public List<ProductEntryEntity> getProductentryEntityList(Pagination pagination) {
        return this.baseMapper.getProductentryEntityList(pagination);
    }

}
