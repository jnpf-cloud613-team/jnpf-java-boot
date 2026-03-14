package jnpf.service.impl;

import jnpf.base.Pagination;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.CustomerEntity;
import jnpf.mapper.CustomerMapper;
import jnpf.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户信息
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 14:09:05
 */
@Service

public class CustomerServiceImpl extends SuperServiceImpl<CustomerMapper, CustomerEntity> implements CustomerService {


    @Override
    public List<CustomerEntity> getList(Pagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public CustomerEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(CustomerEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, CustomerEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(CustomerEntity entity) {
        this.baseMapper.delete(entity);
    }
}
