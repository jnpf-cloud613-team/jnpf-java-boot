package jnpf.permission.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.CodeConst;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.AuthorizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.StandingEntity;
import jnpf.permission.mapper.AuthorizeMapper;
import jnpf.permission.mapper.PositionMapper;
import jnpf.permission.mapper.RoleMapper;
import jnpf.permission.mapper.StandingMapper;
import jnpf.permission.model.standing.StandingModel;
import jnpf.permission.model.standing.StandingPagination;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.StandingService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 身份管理impl
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:24:09
 */
@Service
@RequiredArgsConstructor
public class StandingServiceImpl extends SuperServiceImpl<StandingMapper, StandingEntity> implements StandingService {

    private final  CodeNumService codeNumService;
    private final  RoleMapper roleMapper;
    private final  PositionMapper positionMapper;
    private final  AuthorizeMapper authorizeMapper;

    @Override
    public List<StandingEntity> getList(StandingPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public void crete(StandingEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.SF), code -> this.isExistByEnCode(code, null)));
        }
        this.baseMapper.crete(entity);
    }

    @Override
    public Boolean update(String id, StandingEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.SF), code -> this.isExistByEnCode(code, null)));
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public StandingEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void delete(StandingEntity entity) {
        this.baseMapper.deleteById(entity);
        //绑定关系删除
        authorizeMapper.deleteByItemIds(Arrays.asList(entity.getId()));
    }

    @Override
    public Boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<StandingEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList);
    }

    @Override
    public List<RoleEntity> getRolePage(StandingPagination pagination) {
        MPJLambdaWrapper<RoleEntity> queryWrapper = JoinWrappers.lambda(RoleEntity.class);
        queryWrapper.leftJoin(AuthorizeEntity.class, AuthorizeEntity::getObjectId, RoleEntity::getId);
        queryWrapper.selectAll(RoleEntity.class);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(RoleEntity::getEnCode, pagination.getKeyword())
                            .or().like(RoleEntity::getFullName, pagination.getKeyword())
            );
        }
        queryWrapper.eq(AuthorizeEntity::getItemId, pagination.getId());
        queryWrapper.eq(AuthorizeEntity::getObjectType, PermissionConst.ROLE);
        queryWrapper.isNotNull(RoleEntity::getId);

        queryWrapper.orderByAsc(RoleEntity::getGlobalMark).orderByDesc(AuthorizeEntity::getId);
        Page<RoleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<RoleEntity> data = roleMapper.selectJoinPage(page, RoleEntity.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    @Override
    public List<PositionEntity> getPosPage(StandingPagination pagination) {
        MPJLambdaWrapper<PositionEntity> queryWrapper = JoinWrappers.lambda(PositionEntity.class);
        queryWrapper.leftJoin(AuthorizeEntity.class, AuthorizeEntity::getObjectId, PositionEntity::getId);
        queryWrapper.selectAll(PositionEntity.class);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(PositionEntity::getEnCode, pagination.getKeyword())
                            .or().like(PositionEntity::getFullName, pagination.getKeyword())
            );
        }
        queryWrapper.eq(AuthorizeEntity::getItemId, pagination.getId());
        queryWrapper.eq(AuthorizeEntity::getObjectType, PermissionConst.POSITION);
        queryWrapper.isNotNull(PositionEntity::getId);

        queryWrapper.orderByDesc(AuthorizeEntity::getId);
        Page<PositionEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<PositionEntity> data = positionMapper.selectJoinPage(page, PositionEntity.class, queryWrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    @Override
    public List<StandingModel> getByObjectIds(List<String> objectIds) {
        //获取用户所有权限
        List<AuthorizeEntity> authorizeList = authorizeMapper.getListByObjectId(objectIds);
        List<AuthorizeEntity> standingList = authorizeList.stream().filter(t -> PermissionConst.STAND.equals(t.getItemType())).collect(Collectors.toList());
        List<String> standingIds = standingList.stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        Map<String, List<AuthorizeEntity>> collect = standingList.stream().collect(Collectors.groupingBy(AuthorizeEntity::getItemId));
        List<StandingEntity> listByIds = this.getListByIds(standingIds);
        List<StandingModel> list = JsonUtil.getJsonToList(listByIds, StandingModel.class);
        for (StandingModel item : list) {
            List<AuthorizeEntity> all = collect.get(item.getId());
            List<String> posIds = all.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
            List<String> roleIds = all.stream().filter(t -> PermissionConst.ROLE.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
            List<String> listPos = new ArrayList<>(posIds);
            listPos.retainAll(objectIds);
            item.setPosIds(listPos);
            List<String> listRole = new ArrayList<>(roleIds);
            listRole.retainAll(objectIds);
            item.setRoleIds(listRole);
        }
        return list;
    }
}
