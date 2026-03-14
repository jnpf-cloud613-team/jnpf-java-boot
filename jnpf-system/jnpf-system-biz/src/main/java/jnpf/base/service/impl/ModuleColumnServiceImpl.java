package jnpf.base.service.impl;


import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleColumnEntity;
import jnpf.base.mapper.ModuleColumnMapper;
import jnpf.base.service.ModuleColumnService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 列表权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ModuleColumnServiceImpl extends SuperServiceImpl<ModuleColumnMapper, ModuleColumnEntity> implements ModuleColumnService {

    @Override
    public List<ModuleColumnEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleColumnEntity> getEnabledMarkList(String enabledMark) {
        return this.baseMapper.getEnabledMarkList(enabledMark);
    }

    @Override
    public List<ModuleColumnEntity> getList(String moduleId, Pagination pagination) {
        return this.baseMapper.getList(moduleId, pagination);
    }

    @Override
    public List<ModuleColumnEntity> getList(String moduleId) {
        return this.baseMapper.getList(moduleId);
    }

    @Override
    public ModuleColumnEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public ModuleColumnEntity getInfo(String id, String moduleId) {
        return this.baseMapper.getInfo(id, moduleId);
    }

    @Override
    public boolean isExistByFullName(String moduleId, String fullName, String id) {
        return this.baseMapper.isExistByFullName(moduleId, fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String moduleId, String enCode, String id) {
        return this.baseMapper.isExistByEnCode(moduleId, enCode, id);
    }

    @Override
    public void create(ModuleColumnEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    @DSTransactional
    public void create(List<ModuleColumnEntity> entitys) {
        this.baseMapper.create(entitys);
    }

    @Override
    public boolean update(String id, ModuleColumnEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @Override
    public void delete(ModuleColumnEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public List<ModuleColumnEntity> getListByModuleId(List<String> ids, Integer type) {
        return this.baseMapper.getListByModuleId(ids, type);
    }

    @Override
    public List<ModuleColumnEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }
}
