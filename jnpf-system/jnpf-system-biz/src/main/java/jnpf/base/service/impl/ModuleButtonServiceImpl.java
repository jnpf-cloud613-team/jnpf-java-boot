package jnpf.base.service.impl;


import jnpf.base.Pagination;
import jnpf.base.entity.ModuleButtonEntity;
import jnpf.base.mapper.ModuleButtonMapper;
import jnpf.base.service.ModuleButtonService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按钮权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ModuleButtonServiceImpl extends SuperServiceImpl<ModuleButtonMapper, ModuleButtonEntity> implements ModuleButtonService {

    @Override
    public List<ModuleButtonEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleButtonEntity> getEnabledMarkList(String enabledMark) {
        return this.baseMapper.getEnabledMarkList(enabledMark);
    }

    @Override
    public List<ModuleButtonEntity> getListByModuleIds(String moduleId) {
        return this.baseMapper.getListByModuleIds(moduleId);
    }

    @Override
    public List<ModuleButtonEntity> getListByModuleIds(String moduleId, Pagination pagination) {
        return this.baseMapper.getListByModuleIds(moduleId, pagination);
    }

    @Override
    public ModuleButtonEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public ModuleButtonEntity getInfo(String id, String moduleId) {
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
    public void create(ModuleButtonEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ModuleButtonEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @Override
    public void delete(ModuleButtonEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public List<ModuleButtonEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }
}
