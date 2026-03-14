package jnpf.base.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleFormEntity;
import jnpf.base.mapper.ModuleFormMapper;
import jnpf.base.service.ModuleFormService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表单权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ModuleFormServiceImpl extends SuperServiceImpl<ModuleFormMapper, ModuleFormEntity> implements ModuleFormService {

    @Override
    public List<ModuleFormEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleFormEntity> getEnabledMarkList(String enabledMark) {
        return this.baseMapper.getEnabledMarkList(enabledMark);
    }

    @Override
    public List<ModuleFormEntity> getList(String moduleId, Pagination pagination) {
        return this.baseMapper.getList(moduleId, pagination);
    }

    @Override
    public List<ModuleFormEntity> getList(String moduleId) {
        return this.baseMapper.getList(moduleId);
    }

    @Override
    public ModuleFormEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public ModuleFormEntity getInfo(String id, String moduleId) {
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
    public void create(ModuleFormEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    @DSTransactional
    public void create(List<ModuleFormEntity> entitys) {
        this.baseMapper.create(entitys);
    }

    @Override
    public boolean update(String id, ModuleFormEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @Override
    public void delete(ModuleFormEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public List<ModuleFormEntity> getListByModuleId(List<String> ids, Integer type) {
        return this.baseMapper.getListByModuleId(ids, type);
    }

    @Override
    public List<ModuleFormEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }

}
