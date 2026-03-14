package jnpf.base.service.impl;


import jnpf.base.entity.ModuleDataAuthorizeEntity;
import jnpf.base.mapper.ModuleDataAuthorizeMapper;
import jnpf.base.service.ModuleDataAuthorizeService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据权限配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ModuleDataAuthorizeServiceImpl extends SuperServiceImpl<ModuleDataAuthorizeMapper, ModuleDataAuthorizeEntity> implements ModuleDataAuthorizeService {

    @Override
    public List<ModuleDataAuthorizeEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleDataAuthorizeEntity> getList(String moduleId) {
        return this.baseMapper.getList(moduleId);
    }

    @Override
    public ModuleDataAuthorizeEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(ModuleDataAuthorizeEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ModuleDataAuthorizeEntity entity) {
        entity.setId(id);
        entity.setEnabledMark(1);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @Override
    public void delete(ModuleDataAuthorizeEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public boolean isExistByEnCode(String moduleId, String enCode, String id) {
        return this.baseMapper.isExistByEnCode(moduleId, enCode, id);
    }

    @Override
    public boolean isExistByFullName(String moduleId, String fullName, String id) {
        return this.baseMapper.isExistByFullName(moduleId, fullName, id);
    }

}
