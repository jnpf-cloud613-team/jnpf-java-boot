package jnpf.base.service.impl;

import jnpf.base.Page;
import jnpf.base.entity.DataInterfaceVariateEntity;
import jnpf.base.mapper.DataInterfaceVariateMapper;
import jnpf.base.service.DataInterfaceVariateService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DataInterfaceVariateServiceImpl extends SuperServiceImpl<DataInterfaceVariateMapper, DataInterfaceVariateEntity> implements DataInterfaceVariateService {

    @Override
    public List<DataInterfaceVariateEntity> getList(String id, Page page) {
        return this.baseMapper.getList(id, page);
    }

    @Override
    public DataInterfaceVariateEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(DataInterfaceVariateEntity entity) {
        return this.baseMapper.isExistByFullName(entity);
    }

    @Override
    public boolean create(DataInterfaceVariateEntity entity) {
        return this.baseMapper.create(entity);
    }

    @Override
    public boolean update(DataInterfaceVariateEntity entity) {
        return this.updateById(entity);
    }

    @Override
    public boolean delete(DataInterfaceVariateEntity entity) {
        return this.removeById(entity);
    }

    @Override
    public List<DataInterfaceVariateEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }

    @Override
    public void update(Map<String, String> map, List<DataInterfaceVariateEntity> variateEntities) {
        this.baseMapper.update(map, variateEntities);
    }

    @Override
    public DataInterfaceVariateEntity getInfoByFullName(String fullName) {
        return this.baseMapper.getInfoByFullName(fullName);
    }
}
