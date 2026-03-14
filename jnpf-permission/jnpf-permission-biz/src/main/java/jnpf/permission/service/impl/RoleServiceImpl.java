package jnpf.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.CodeConst;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.AuthorizeEntity;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.mapper.AuthorizeMapper;
import jnpf.permission.mapper.RoleMapper;
import jnpf.permission.mapper.RoleRelationMapper;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.role.RolePagination;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.RoleService;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统角色
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends SuperServiceImpl<RoleMapper, RoleEntity> implements RoleService {

    private final  CodeNumService codeNumService;
    private final  AuthorizeMapper authorizeMapper;
    private final  RoleRelationMapper roleRelationMapper;

    @Override
    public List<RoleEntity> getList(RolePagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public Boolean isExistByFullName(String fullName, String id, String type) {
        return this.baseMapper.isExistByFullName(fullName, id, type);
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    /**
     * 设置编码
     *
     * @param entity
     */
    private void setEnCode(RoleEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            String codeType = CodeConst.YHJS;
            if (PermissionConst.ORGANIZE.equals(entity.getType())) {
                codeType = CodeConst.ZZJS;
            } else if (PermissionConst.POSITION.equals(entity.getType())) {
                codeType = CodeConst.GWJS;
            }
            final String codeTypeP = codeType;
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(codeTypeP), code -> isExistByEnCode(code, null)));
        }
    }

    @Override
    public void create(RoleEntity entity) {
        setEnCode(entity);
        this.baseMapper.create(entity);
    }

    @Override
    public Boolean update(String id, RoleEntity entity) {
        setEnCode(entity);
        if (Objects.equals(entity.getIsCondition(), 0)) {
            entity.setConditionJson("");
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public RoleEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public void delete(RoleEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
            QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(AuthorizeEntity::getObjectId, entity.getId());
            authorizeMapper.deleteByIds(authorizeMapper.selectList(queryWrapper));
            QueryWrapper<RoleRelationEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(RoleRelationEntity::getRoleId, entity.getId());
            roleRelationMapper.deleteByIds(roleRelationMapper.selectList(wrapper));
        }
    }

    @Override
    public RoleEntity getByEnCode(String enCode) {
        return this.baseMapper.getByEnCode(enCode);
    }

    @Override
    public List<RoleEntity> getList(boolean filterEnabledMark, String type, Integer isSystem) {
        return this.baseMapper.getList(filterEnabledMark, type, isSystem);
    }

    @Override
    public List<RoleEntity> getListByIds(List<String> id, String keyword, boolean filterEnabledMark) {
        return this.baseMapper.getListByIds(id, keyword, filterEnabledMark);
    }

    @Override
    public List<RoleEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList);
    }

    @Override
    public List<RoleEntity> getListByIds(Pagination pagination, List<String> idList) {
        return this.baseMapper.getListByIds(pagination, idList);
    }

    @Override
    public Map<String, Object> getRoleMap() {
        return this.baseMapper.getRoleMap();
    }

    @Override
    public Map<String, Object> getRoleNameAndIdMap() {
        return this.baseMapper.getRoleNameAndIdMap();
    }

    @Override
    public Map<String, Object> getRoleNameAndIdMap(boolean enabledMark) {
        return this.baseMapper.getRoleNameAndIdMap(enabledMark);
    }

    @Override
    public RoleEntity getInfoByFullName(String fullName) {
        return this.baseMapper.getInfoByFullName(fullName);
    }

    @Override
    public List<RoleEntity> getCurRolesByOrgId() {
        UserInfo user = UserProvider.getUser();
        //同组织下所有角色
        List<RoleRelationEntity> roleRelations = new ArrayList<>();
        roleRelations.addAll(roleRelationMapper.getListByObjectId(user.getOrganizeId(), PermissionConst.ORGANIZE));
        roleRelations.addAll(roleRelationMapper.getListByObjectId(user.getPositionId(), PermissionConst.POSITION));
        roleRelations.addAll(roleRelationMapper.getListByObjectId(user.getId(), PermissionConst.USER));
        if (CollUtil.isEmpty(roleRelations)) {
            return Collections.emptyList();
        }
        List<String> roleIds = roleRelations.stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        return this.baseMapper.getListByIds(roleIds, null, true);
    }

    @Override
    public List<RoleEntity> getList(List<String> idList, Pagination pagination, boolean filterEnabledMark) {
        return this.baseMapper.getList(idList, pagination, filterEnabledMark);
    }

    @Override
    public Map<String, Integer> roleUserCount() {
        Map<String, Integer> map = new HashMap<>();
        List<RoleEntity> list = this.baseMapper.getList(true, PermissionConst.USER, null);
        List<RoleRelationEntity> listByRoleId = roleRelationMapper.getListByRoleId("", PermissionConst.USER);
        Map<String, List<RoleRelationEntity>> roleGroup = listByRoleId.stream().collect(Collectors.groupingBy(RoleRelationEntity::getRoleId));
        for (RoleEntity role : list) {
            map.put(role.getFullName(), CollUtil.isEmpty(roleGroup.get(role.getId())) ? 0 : roleGroup.get(role.getId()).size());
        }
        return map;
    }

    @Override
    public List<RoleEntity> getUserRoles(String userId) {
        List<String> roleIds = roleRelationMapper.getListByObjectId(userId, PermissionConst.USER).stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        return this.getListByIds(roleIds);
    }

    @Override
    public void linkUpdate(String id, PosConModel posConModel) {
        this.baseMapper.linkUpdate(id, posConModel);
    }

}
