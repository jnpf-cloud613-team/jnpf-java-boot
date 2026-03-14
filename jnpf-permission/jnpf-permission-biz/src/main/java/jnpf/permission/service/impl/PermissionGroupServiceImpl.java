package jnpf.permission.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.CodeConst;
import jnpf.constant.PermissionConst;
import jnpf.emnus.SysParamEnum;
import jnpf.permission.entity.*;
import jnpf.permission.mapper.*;
import jnpf.permission.model.permissiongroup.PaginationPermissionGroup;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.PermissionGroupService;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl extends SuperServiceImpl<PermissionGroupMapper, PermissionGroupEntity> implements PermissionGroupService {

    private final  CodeNumService codeNumService;
    private final  AuthorizeMapper authorizeMapper;
    private final  PositionMapper positionMapper;
    private final  OrganizeMapper organizeMapper;
    private final  RoleRelationMapper roleRelationMapper;
    private final  UserRelationMapper userRelationMapper;

    @Override
    public List<PermissionGroupEntity> list(PaginationPermissionGroup pagination) {
        return this.baseMapper.list(pagination);
    }

    @Override
    public List<PermissionGroupEntity> list(boolean filterEnabledMark, List<String> ids) {
        return this.baseMapper.list(filterEnabledMark, ids);
    }

    @Override
    public PermissionGroupEntity info(String id) {
        return this.baseMapper.info(id);
    }

    @Override
    public boolean create(PermissionGroupEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.QXJH), code -> this.isExistByEnCode(code, null)));
        }
        return this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, PermissionGroupEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.QXJH), code -> this.isExistByEnCode(code, null)));
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public boolean delete(PermissionGroupEntity entity) {
        return this.baseMapper.delete(entity);
    }

    @Override
    public boolean isExistByFullName(String id, PermissionGroupEntity entity) {
        return this.baseMapper.isExistByFullName(id, entity);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<PermissionGroupEntity> list(List<String> ids) {
        return this.baseMapper.list(ids);
    }

    @Override
    public PermissionGroupEntity permissionMember(String id) {
        return this.baseMapper.permissionMember(id);
    }

    @Override
    public List<PermissionGroupEntity> getPermissionGroupByObjectId(String objectId, String objectType) {
        List<PermissionGroupEntity> permissionGroupEntities = this.list(true, null).stream()
                .filter(t -> StringUtil.isNotEmpty(t.getPermissionMember())).collect(Collectors.toList());
        String id = objectId + "--" + objectType;
        return permissionGroupEntities.stream()
                .filter(entity -> entity.getPermissionMember().contains(id)).collect(Collectors.toList());
    }

    @Override
    public List<String> setAuthByIds(String id, List<String> ids) {
        List<AuthorizeEntity> hasAuthList = authorizeMapper.getListByObjectId(Arrays.asList(id));
        Set<String> orgIds = new HashSet<>();
        Set<String> posIds = new HashSet<>();
        Set<String> roleIds = new HashSet<>();
        Set<String> objectIds = new HashSet<>();
        for (String s : ids) {
            if (s.contains("--")) {
                String[] split = s.split("--");
                String thisId = split[0];
                String thisType = split[1];
                if (SysParamEnum.ORG.getCode().equals(thisType)) {
                    orgIds.add(thisId);
                }
                if (SysParamEnum.POS.getCode().equals(thisType)) {
                    posIds.add(thisId);
                }
                if (SysParamEnum.ROLE.getCode().equals(thisType)) {
                    roleIds.add(thisId);
                }
            }
        }
        List<PositionEntity> posList = positionMapper.getListByIds(new ArrayList<>(posIds));
        for (PositionEntity item : posList) {
            posIds.addAll(Arrays.asList(item.getPositionIdTree().split(",")));
            orgIds.add(item.getOrganizeId());
        }

        List<OrganizeEntity> orgList = organizeMapper.getListByIds(new ArrayList<>(orgIds));
        for (OrganizeEntity item : orgList) {
            orgIds.addAll(Arrays.asList(item.getOrganizeIdTree().split(",")));
        }
        objectIds.addAll(posIds);
        objectIds.addAll(orgIds);
        objectIds.addAll(roleIds);
        addAuthList(posIds, hasAuthList, PermissionConst.POSITION);
        addAuthList(orgIds, hasAuthList, PermissionConst.ORGANIZE);
        addAuthList(roleIds, hasAuthList, PermissionConst.ROLE);

        Set<String> userIds = new HashSet<>();
        List<String> roleUser = roleRelationMapper.getListByRoleId(new ArrayList<>(roleIds), null).stream()
                .map(RoleRelationEntity::getObjectId).collect(Collectors.toList());
        userIds.addAll(roleUser);
        List<String> orgUser = userRelationMapper.getListByObjectIdAll(new ArrayList<>(objectIds)).stream()
                .map(UserRelationEntity::getUserId).collect(Collectors.toList());
        userIds.addAll(orgUser);
        return new ArrayList<>(userIds);
    }

    /**
     * 添加权限
     *
     * @param roleIds
     * @param hasAuthList
     * @param role
     */
    private void addAuthList(Set<String> roleIds, List<AuthorizeEntity> hasAuthList, String role) {
        for (String objectId : roleIds) {
            List<AuthorizeEntity> thisAuth = authorizeMapper.getListByObjectId(objectId, null);
            Set<String> list2Keys = thisAuth.stream()
                    .map(item -> item.getItemId() + "|" + item.getItemType())
                    .collect(Collectors.toSet());
            hasAuthList.stream().forEach(t -> {
                if (!list2Keys.contains(t.getItemId() + "|" + t.getItemType())) {
                    AuthorizeEntity authorizeEntity = new AuthorizeEntity();
                    authorizeEntity.setId(RandomUtil.uuId());
                    authorizeEntity.setObjectId(objectId);
                    authorizeEntity.setObjectType(role);
                    authorizeEntity.setItemType(t.getItemType());
                    authorizeEntity.setItemId(t.getItemId());
                    authorizeMapper.insert(authorizeEntity);
                }
            });
        }
    }
}
