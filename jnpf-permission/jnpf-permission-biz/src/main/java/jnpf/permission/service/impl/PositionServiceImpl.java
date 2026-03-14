package jnpf.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.Pagination;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.constant.CodeConst;
import jnpf.constant.PermissionConst;
import jnpf.emnus.SysParamEnum;
import jnpf.model.BaseSystemInfo;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.mapper.*;
import jnpf.permission.model.permission.PermissionModel;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.position.PositionListVO;
import jnpf.permission.model.position.PositionPagination;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.PositionService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends SuperServiceImpl<PositionMapper, PositionEntity> implements PositionService {

    private final  RedisUtil redisUtil;
    private final  CacheKeyUtil cacheKeyUtil;
    private final  SysconfigService sysconfigApi;
    private final  CodeNumService codeNumService;
    private final  AuthorizeMapper authorizeMapper;
    private final  UserRelationMapper userRelationMapper;
    private final  RoleRelationMapper roleRelationMapper;
    private final  OrganizeMapper organizeMapper;

    @Override
    public List<PositionEntity> getList(PositionPagination pagination) {
        return this.getBaseMapper().getList(pagination);
    }


    @Override
    public boolean isExistByFullName(PositionEntity entity, boolean isFilter) {
        return this.getBaseMapper().isExistByFullName(entity, isFilter);
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.getBaseMapper().isExistByEnCode(enCode, id);
    }

    @Override
    public void create(PositionEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.GW), code -> this.isExistByEnCode(code, null)));
        }
        this.baseMapper.create(entity);
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
    }

    @Override
    public boolean update(String id, PositionEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.GW), code -> this.isExistByEnCode(code, id)));
        }
        if (Objects.equals(entity.getIsCondition(), 0)) {
            entity.setConditionJson("");
        }
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
        return this.baseMapper.update(id, entity);
    }

    @Override
    public PositionEntity getInfo(String id) {
        return this.baseMapper.selectById(id);
    }

    @Override
    @DSTransactional
    public void delete(PositionEntity entity) {
        this.removeById(entity.getId());
        userRelationMapper.deleteAllByObjId(entity.getId());
        authorizeMapper.deleteByObjIds(Arrays.asList(entity.getId()));
        roleRelationMapper.deleteAllByObjId(Arrays.asList(entity.getId()));
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
    }

    @Override
    public void deleteByOrgId(String orgId) {
        this.baseMapper.deleteByOrgId(orgId);
    }

    @Override
    public boolean checkLevel(PositionEntity entity) {
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        Integer positionLevel = sysInfo.getPositionLevel();
        PositionEntity pEntity = this.getInfo(entity.getParentId());
        Integer thisLevel = 1;
        if (pEntity != null) {
            String[] parents = pEntity.getPositionIdTree().split(",");
            //创建修改不同判断
            if (StringUtil.isEmpty(entity.getId())) {
                thisLevel += parents.length;
            } else {
                Integer childMax = 1;
                List<PositionEntity> allChild = this.getAllChild(entity.getId());
                for (PositionEntity item : allChild) {
                    String[] trees = item.getPositionIdTree().split(entity.getId() + ",");
                    if (trees.length >= 2) {
                        String[] childs = trees[1].split(",");
                        Integer length = childs.length + 1;
                        if (length > childMax) {
                            childMax = length;
                        }
                    }
                }
                thisLevel = parents.length + childMax;
            }
        }
        return thisLevel <= positionLevel;
    }


    @Override
    public List<PositionEntity> getParentList(String parentId) {
        return this.baseMapper.getParentList(parentId);
    }

    @Override
    public List<PositionEntity> getByParentId(String parentId) {
        return this.baseMapper.getByParentId(parentId);
    }

    @Override
    public List<PositionEntity> getListByOrgIds(List<String> orgIds) {
        return this.baseMapper.getListByOrgIds(orgIds);
    }

    @Override
    public List<PositionEntity> getListByOrgIdOneLevel(List<String> orgIds) {
        return this.baseMapper.getListByOrgIdOneLevel(orgIds);
    }

    @Override
    public List<PositionEntity> getList(boolean filterEnabledMark) {
        return this.baseMapper.getList(filterEnabledMark);
    }

    @Override
    public List<PositionEntity> getAllChild(String id) {
        return this.baseMapper.getAllChild(id);
    }

    @Override
    public List<PositionEntity> getPosList(List<String> idList) {
        return this.baseMapper.getPosList(idList);
    }

    @Override
    public List<PositionEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList);
    }

    @Override
    public List<PositionEntity> getListByIds(Pagination pagination, List<String> idList) {
        return this.baseMapper.getListByIds(pagination, idList);
    }

    @Override
    public Map<String, String> getPosMap() {
        return this.baseMapper.getPosMap();
    }

    @Override
    public Map<String, String> getPosFullNameMap() {
        if (redisUtil.exists(cacheKeyUtil.getPositionList())) {
            return new HashMap<>(redisUtil.getMap(cacheKeyUtil.getPositionList()));
        } else {
            QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(PositionEntity::getId, PositionEntity::getFullName, PositionEntity::getOrganizeId);
            List<PositionEntity> list = this.list(queryWrapper);
            Map<String, String> infoMap = new HashMap<>(list.size());
            Map<String, Object> allOrgsTreeName = organizeMapper.getAllOrgsTreeName();
            for (PositionEntity t : list) {
                String fullName = t.getFullName();
                if (Objects.nonNull(allOrgsTreeName.get(t.getOrganizeId()))) {
                    fullName = allOrgsTreeName.get(t.getOrganizeId()) + "/" + t.getFullName();
                }
                infoMap.put(t.getId(), fullName);
            }
            redisUtil.insert(cacheKeyUtil.getPositionList(), infoMap);
            return infoMap;
        }
    }


    @Override
    public Map<String, Object> getPosEncodeAndName() {
        return getPosEncodeAndName(false);
    }

    @Override
    public Map<String, Object> getPosEncodeAndName(boolean enabledMark) {
        return this.baseMapper.getPosEncodeAndName(enabledMark);
    }

    @Override
    @DSTransactional
    public boolean first(String id) {
        return this.baseMapper.first(id);
    }

    @Override
    @DSTransactional
    public boolean next(String id) {
        return this.baseMapper.next(id);
    }

    @Override
    public List<PositionEntity> getPositionName(List<String> id, boolean filterEnabledMark) {
        return this.baseMapper.getPositionName(id, filterEnabledMark);
    }

    @Override
    public List<PositionEntity> getPositionName(List<String> id, String keyword) {
        return this.baseMapper.getPositionName(id, keyword);
    }

    @Override
    public List<PositionEntity> getListByOrganizeId(List<String> organizeIds, boolean enabledMark) {
        return this.baseMapper.getPositionName(organizeIds, enabledMark);
    }

    @Override
    public List<PositionEntity> getListByOrgIdAndUserId(String organizeId, String userId) {
        // 用户绑定的所有岗位
        List<String> positionIds = userRelationMapper.getListByUserIdAndObjType(userId, PermissionConst.POSITION).stream()
                .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        if (!positionIds.isEmpty()) {
            List<PositionEntity> positionEntities = this.listByIds(positionIds);
            return positionEntities.stream().filter(p -> p.getOrganizeId().equals(organizeId)).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<PositionEntity> getListByFullName(String fullName, String enCode) {
        return this.baseMapper.getListByFullName(fullName, enCode);
    }

    @Override
    public List<PermissionModel> getListByOrganizeIds(List<String> organizeIds, boolean needCode, boolean enabledMark) {
        List<PermissionModel> permissionList = new LinkedList<>();
        for (String organizeId : organizeIds) {
            OrganizeEntity info = organizeMapper.getInfo(organizeId);
            if (info != null) {
                PermissionModel parentModel = new PermissionModel();
                List<PositionEntity> list = this.getListByOrganizeId(Collections.singletonList(organizeId), enabledMark);
                list.forEach(t -> {
                    if (needCode) {
                        t.setFullName(t.getFullName() + "/" + t.getEnCode());
                    }
                });
                List<PermissionModel> positionModels = JsonUtil.getJsonToList(list, PermissionModel.class);
                parentModel.setChildren(ImmutableList.copyOf(positionModels));
                parentModel.setHasChildren(true);
                parentModel.setFullName(info.getFullName());
                parentModel.setId(info.getId());
                permissionList.add(parentModel);
            }
        }
        return permissionList;
    }

    @Override
    public List<PositionListVO> selectedList(List<String> idStrList) {
        if (CollUtil.isEmpty(idStrList)) return Collections.emptyList();
        List<String> idList = new ArrayList<>();

        for (String idStr : idStrList) {
            String[] split = idStr.split("--");
            idList.add(split[0]);
        }
        Map<String, Object> allOrgsTreeName = organizeMapper.getAllOrgsTreeName();

        List<PositionEntity> listByIds = this.getListByIds(idList);
        List<PositionListVO> listVo = new ArrayList<>();
        for (String idStr : idStrList) {
            String[] split = idStr.split("--");
            String id = split[0];
            String type = split.length > 1 ? split[1] : "";
            SysParamEnum sysParamEnum = SysParamEnum.get(type);
            String suffix = sysParamEnum != null ? sysParamEnum.getSuffix() : "";
            PositionEntity positionEntity = listByIds.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
            if (positionEntity != null) {
                PositionListVO vo = JsonUtil.getJsonToBean(positionEntity, PositionListVO.class);
                vo.setId(idStr);
                vo.setOrgNameTree(allOrgsTreeName.get(positionEntity.getOrganizeId()) + "/" + positionEntity.getFullName() + suffix);
                listVo.add(vo);
            }
        }
        return listVo;
    }

    @Override
    public List<PositionEntity> positionCondition(List<String> idStrList) {
        return this.baseMapper.positionCondition(idStrList);
    }

    @Override
    public List<PositionEntity> getListByDutyUser(String userId) {
        return this.baseMapper.getListByDutyUser(userId);
    }

    @Override
    public List<PositionEntity> getListByParentIds(List<String> idList) {
        return this.baseMapper.getListByParentIds(idList);
    }

    @Override
    public void linkUpdate(String id, PosConModel posConModel) {
        this.baseMapper.linkUpdate(id, posConModel);
    }

    @Override
    public String getNameByIdStr(String idStr) {
        StringJoiner sj = new StringJoiner(",");
        if (StringUtil.isNotEmpty(idStr)) {
            try {
                List<String> ids = JsonUtil.getJsonToList(idStr, String.class);
                List<PositionEntity> listByIds = this.getListByIds(ids);
                for (PositionEntity item : listByIds) {
                    OrganizeEntity orgInfo = organizeMapper.getInfo(item.getOrganizeId());
                    sj.add(orgInfo.getOrgNameTree() + "/" + item.getFullName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sj.toString();
    }

    @Override
    public List<PositionEntity> getProgeny(List<String> idList, Integer enabledMark) {
        return this.baseMapper.getProgeny(idList, enabledMark);
    }
}
