package jnpf.base.service.impl;


import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import jnpf.base.mapper.ModuleDataAuthorizeSchemeMapper;
import jnpf.base.service.ModuleDataAuthorizeSchemeService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据权限方案
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ModuleDataAuthorizeSchemeServiceImpl extends SuperServiceImpl<ModuleDataAuthorizeSchemeMapper, ModuleDataAuthorizeSchemeEntity> implements ModuleDataAuthorizeSchemeService {

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getEnabledMarkList(String enabledMark) {
        return this.baseMapper.getEnabledMarkList(enabledMark);
    }

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getList(String moduleId) {
        return this.baseMapper.getList(moduleId);
    }

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getList(String moduleId, Pagination pagination) {
        return this.baseMapper.getList(moduleId, pagination);
    }

    @Override
    public ModuleDataAuthorizeSchemeEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(ModuleDataAuthorizeSchemeEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ModuleDataAuthorizeSchemeEntity entity) {
        entity.setId(id);
        entity.setEnabledMark(1);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @Override
    public void delete(ModuleDataAuthorizeSchemeEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public Boolean isExistByFullName(String id, String fullName, String moduleId) {
        return this.baseMapper.isExistByFullName(id, fullName, moduleId);
    }

    @Override
    public Boolean isExistByEnCode(String id, String enCode, String moduleId) {
        return this.baseMapper.isExistByEnCode(id, enCode, moduleId);
    }

    @Override
    public Boolean isExistAllData(String moduleId) {
        return this.baseMapper.isExistAllData(moduleId);
    }

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getListByModuleId(List<String> ids, Integer type) {
        return this.baseMapper.getListByModuleId(ids, type);
    }

    @Override
    public List<ModuleDataAuthorizeSchemeEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }
}
