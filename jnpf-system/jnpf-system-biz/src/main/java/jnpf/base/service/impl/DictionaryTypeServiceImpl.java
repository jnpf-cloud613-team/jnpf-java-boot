package jnpf.base.service.impl;


import jnpf.base.mapper.DictionaryDataMapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.base.mapper.DictionaryTypeMapper;
import jnpf.base.service.DictionaryTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class DictionaryTypeServiceImpl extends SuperServiceImpl<DictionaryTypeMapper, DictionaryTypeEntity> implements DictionaryTypeService {


    private final DictionaryDataMapper dictionaryDataMapper;

    @Override
    public List<DictionaryTypeEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public DictionaryTypeEntity getInfoByEnCode(String enCode) {
        return this.baseMapper.getInfoByEnCode(enCode);
    }

    @Override
    public DictionaryTypeEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
       return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public void create(DictionaryTypeEntity entity) {
       this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, DictionaryTypeEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public boolean delete(DictionaryTypeEntity entity) {
        List<DictionaryTypeEntity> dictionaryTypeEntityList = list().stream().filter(t -> entity.getId().equals(t.getParentId())).collect(Collectors.toList());
        //没有子分类的时候才能删
        if (dictionaryTypeEntityList.isEmpty() && dictionaryDataMapper.getList(entity.getId()).isEmpty()){
                this.removeById(entity.getId());
                return true;
            }

        return false;
    }
}
