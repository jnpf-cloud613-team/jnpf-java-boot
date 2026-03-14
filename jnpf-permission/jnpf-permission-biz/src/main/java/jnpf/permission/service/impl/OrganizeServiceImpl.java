package jnpf.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.constant.CodeConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.emnus.OrgTypeEnum;
import jnpf.exception.DataException;
import jnpf.model.BaseSystemInfo;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.mapper.OrganizeMapper;
import jnpf.permission.mapper.PositionMapper;
import jnpf.permission.model.organize.OrganizeListVO;
import jnpf.permission.model.organize.OrganizePagination;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.RoleRelationService;
import jnpf.permission.service.UserRelationService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织机构
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class OrganizeServiceImpl extends SuperServiceImpl<OrganizeMapper, OrganizeEntity> implements OrganizeService {

    private final CacheKeyUtil cacheKeyUtil;
    private final RedisUtil redisUtil;
    private final SysconfigService sysconfigApi;
    private final CodeNumService codeNumService;
    private final PositionMapper positionMapper;
    private final UserRelationService userRelationService;
    private final RoleRelationService roleRelationService;

    @Override
    public List<OrganizeEntity> getList(OrganizePagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public void create(OrganizeEntity entity) {
        // 自动生成编码
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.ZZ), code -> this.isExistByEnCode(code, null)));
        }
        String positionId = RandomUtil.uuId();
        entity.setDutyPosition(positionId);
        this.baseMapper.create(entity);

        //生成默认岗位
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        PositionEntity positionEntity = new PositionEntity();
        positionEntity.setId(positionId);
        positionEntity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.GW), code -> positionMapper.isExistByEnCode(code, null)));
        positionEntity.setFullName(sysInfo.getSysPositionName());
        positionEntity.setOrganizeId(entity.getId());
        positionEntity.setDefaultMark(1);
        positionMapper.create(positionEntity);
        redisUtil.remove(cacheKeyUtil.getOrganizeInfoList());
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_ORG_TREE);
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
    }

    @Override
    public boolean update(String id, OrganizeEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.ZZ), code -> this.isExistByEnCode(code, id)));
        }
        boolean updateById = this.baseMapper.update(id, entity);
        redisUtil.remove(cacheKeyUtil.getOrganizeInfoList());
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_ORG_TREE);
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
        return updateById;
    }

    @Override
    public OrganizeEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public List<OrganizeEntity> delete(String orgId) {
        OrganizeEntity organizeEntity = this.getInfo(orgId);
        if (organizeEntity == null) {
            throw new DataException(MsgCode.FA003.get());
        }
        List<OrganizeEntity> orgList = this.baseMapper.getAllChild(orgId);
        Map<Integer, List<OrganizeEntity>> map = new HashMap<>();
        for (OrganizeEntity org : orgList) {
            String organizeIdTree = org.getOrganizeIdTree();
            int length = organizeIdTree.split(",").length;
            List<OrganizeEntity> organizeEntityList = map.get(length) != null ? map.get(length) : new ArrayList<>();
            organizeEntityList.add(org);
            map.put(length, organizeEntityList);
        }

        List<String> orgIdS = orgList.stream().map(OrganizeEntity::getId).collect(Collectors.toList());
        List<PositionEntity> posList = positionMapper.getListByOrgIds(orgIdS);
        List<String> posIds = posList.stream().map(PositionEntity::getId).collect(Collectors.toList());
        List<String> objectIds = new ArrayList<>();
        objectIds.addAll(orgIdS);
        objectIds.addAll(posIds);
        List<UserRelationEntity> userList = userRelationService.getListByObjectIdAll(objectIds);
        if (CollUtil.isNotEmpty(userList)) {
            throw new DataException(MsgCode.PS040.get(MsgCode.PS003.get()));
        }

        //删除组织，和岗位
        for (OrganizeEntity item : orgList) {
            this.removeById(item.getId());
            positionMapper.deleteByOrgId(item.getId());

        }
        List<Integer> collect = map.keySet()
                .stream().sorted(Comparator.comparing(Integer::byteValue).reversed())
                .collect(Collectors.toList());
        List<OrganizeEntity> organizeEntities = new ArrayList<>();
        for (Integer integer : collect) {
            organizeEntities.addAll(map.get(integer));
        }

        //删除关系
        for (UserRelationEntity ure : userList) {
            userRelationService.removeById(ure);
        }
        List<RoleRelationEntity> roleList = roleRelationService.getListByObjectId(objectIds, null);
        for (RoleRelationEntity rre : roleList) {
            roleRelationService.removeById(rre);
        }
        redisUtil.remove(cacheKeyUtil.getOrganizeInfoList());
        redisUtil.remove(cacheKeyUtil.getPositionList());
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_ORG_TREE);
        redisUtil.remove(TenantHolder.getDatasourceId() + CacheKeyUtil.SYS_POS);
        return organizeEntities;
    }

    @Override
    public boolean checkLevel(OrganizeEntity entity) {
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        Integer orgLevel = sysInfo.getOrgLevel();
        OrganizeEntity pEntity = this.getInfo(entity.getParentId());
        int thisLevel = 1;
        if (pEntity != null) {
            String[] parents = pEntity.getOrganizeIdTree().split(",");
            //创建修改不同判断
            if (StringUtil.isEmpty(entity.getId())) {
                thisLevel += parents.length;
            } else {
                int childMax = 1;
                List<OrganizeEntity> allChild = this.baseMapper.getAllChild(entity.getId());
                for (OrganizeEntity item : allChild) {
                    String[] trees = item.getOrganizeIdTree().split(entity.getId() + ",");
                    if (trees.length >= 2) {
                        String[] childs = trees[1].split(",");
                        int length = childs.length + 1;
                        if (length > childMax) {
                            childMax = length;
                        }
                    }
                }
                thisLevel = parents.length + childMax;
            }
        }
        return thisLevel <= orgLevel;
    }

    @Override
    public boolean checkOrgType(OrganizeEntity entity) {
        List<OrganizeEntity> listChild = this.getListByParentId(entity.getId());
        OrgTypeEnum orgTypeEnum = OrgTypeEnum.get(entity.getCategory());
        //有一个子组织类型不在范围内就false
        for (OrganizeEntity item : listChild) {
            if (!orgTypeEnum.getChildType().contains(item.getCategory())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, Object> getAllOrgsTreeName() {
        return this.baseMapper.getAllOrgsTreeName();
    }

    @Override
    public Map<String, String> getAllOrgsTreeName(boolean enabledMark) {
        return this.baseMapper.getAllOrgsTreeName(enabledMark);
    }

    @Override
    public List<OrganizeEntity> getListAll(List<String> idAll, String keyWord) {
        return this.baseMapper.getListAll(idAll, keyWord);
    }

    @Override
    public List<OrganizeEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList);
    }


    @Override
    public boolean isExistByFullName(OrganizeEntity entity, boolean isCheck, boolean isFilter) {
        return this.baseMapper.isExistByFullName(entity, isCheck, isFilter);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<OrganizeEntity> getParentList(String parentId) {
        return this.baseMapper.getParentList(parentId);
    }

    @Override
    public List<OrganizeEntity> getListByParentIds(List<String> idList) {
        return this.baseMapper.getListByParentIds(idList);
    }

    @Override
    public void setOrgTreeIdAndName(OrganizeEntity entity) {
        this.baseMapper.setOrgTreeIdAndName(entity);
    }

    @Override
    public List<OrganizeEntity> getDepsByParentId(String id) {
        return this.baseMapper.getDepsByParentId(id);
    }

    @Override
    public List<OrganizeEntity> getList(boolean filterEnabledMark) {
        return this.baseMapper.getList(filterEnabledMark);
    }

    @Override
    public OrganizeEntity getInfoByFullName(String fullName) {
        return this.baseMapper.getInfoByFullName(fullName);
    }

    @Override
    public List<OrganizeEntity> getList(String keyword, boolean filterEnabledMark) {
        return this.baseMapper.getList(keyword, filterEnabledMark);
    }


    /**
     * 获取组织信息
     *
     * @return OrgId, OrgEntity
     */
    @Override
    public Map<String, OrganizeEntity> getOrgMapsAll(SFunction<OrganizeEntity, ?>... columns) {
        return this.baseMapper.getOrgMapsAll(columns);
    }

    /**
     * 获取组织信息
     *
     * @param keyword
     * @param filterEnabledMark
     * @param category
     * @return OrgId, OrgEntity
     */
    @Override
    public Map<String, OrganizeEntity> getOrgMaps(String keyword, boolean filterEnabledMark, String category, SFunction<OrganizeEntity, ?>... columns) {
        return this.baseMapper.getOrgMaps(keyword, filterEnabledMark, category, columns);
    }


    /**
     * 组织基础过滤
     *
     * @param queryWrapper
     * @param orderBy      Map<Column, isAsc>
     * @param groupBy      Column
     * @param columns      query
     * @return
     */
    public Map<String, OrganizeEntity> getBaseOrgMaps(QueryWrapper<OrganizeEntity> queryWrapper, Map<SFunction<OrganizeEntity, ?>, Boolean> orderBy, List<SFunction<OrganizeEntity, ?>> groupBy, boolean bigColumnHas, SFunction<OrganizeEntity, ?>... columns) {
        return this.baseMapper.getBaseOrgMaps(queryWrapper, orderBy, groupBy, bigColumnHas, columns);
    }

    @Override
    public List<OrganizeEntity> getOrgEntityList(List<String> idList, Boolean enable) {
        return this.baseMapper.getOrgEntityList(idList, enable);
    }

    @Override
    public Map<String, Object> getOrgMap() {
        return this.baseMapper.getOrgMap();
    }

    @Override
    public Map<String, Object> getOrgEncodeAndName(String category) {
        return this.baseMapper.getOrgEncodeAndName(category);
    }

    @Override
    public Map<String, Object> getOrgNameAndId(String category) {
        return this.baseMapper.getOrgNameAndId(category);
    }


    @Override
    public OrganizeEntity getByFullName(String fullName) {
        return this.baseMapper.getByFullName(fullName);
    }


    @Override
    public void getOrganizeIdTree(String organizeId, List<String> organizeParentIdList) {
        this.baseMapper.getOrganizeIdTree(organizeId, organizeParentIdList);
    }

    @Override
    public void getOrganizeId(String organizeId, List<OrganizeEntity> organizeList) {
        this.baseMapper.getOrganizeId(organizeId, organizeList);
    }

    @Override
    public String allowDelete(String orgId) {
        // 组织底下是否有组织
        List<OrganizeEntity> list = getListByParentId(orgId);
        if (Objects.nonNull(list) && !list.isEmpty()) {
            return MsgCode.PS003.get();
        }
        // 组织底下是否有岗位
        List<PositionEntity> positonList = positionMapper.getListByOrganizeId(Collections.singletonList(orgId), false);
        List<String> positonIds = positonList.stream().map(PositionEntity::getId).collect(Collectors.toList());

        // 组织底下是否有用户
        if (Boolean.TRUE.equals(userRelationService.existByObj(PermissionConst.ORGANIZE, orgId))) {
            return MsgCode.PS005.get();
        }
        // 组织底下是否有角色
        List<RoleRelationEntity> listByObjectId = roleRelationService.getListByObjectId(positonIds, PermissionConst.POSITION);
        if (!listByObjectId.isEmpty()) {
            return MsgCode.PS006.get();
        }
        return null;
    }

    @Override
    public List<OrganizeEntity> getOrganizeName(List<String> id) {
        return this.baseMapper.getOrganizeName(id);
    }

    @Override
    public Map<String, OrganizeEntity> getOrganizeName(List<String> id, String keyword, boolean filterEnabledMark, String category) {
        return this.baseMapper.getOrganizeName(id, keyword, filterEnabledMark, category);
    }


    @Override
    public List<String> getUnderOrganizations(String organizeId, boolean filterEnabledMark) {
        return this.baseMapper.getUnderOrganizations(organizeId, filterEnabledMark);
    }

    @Override
    public List<String> getUnderOrganizationss(String organizeId) {
        return this.baseMapper.getUnderOrganizationss(organizeId);
    }

    @Override
    public List<OrganizeEntity> getListByParentId(String id) {
        return this.baseMapper.getListByParentId(id);
    }

    @Override
    public List<OrganizeEntity> getAllOrgByUserId(String userId) {
        List<String> ids = new ArrayList<>();
        userRelationService.getAllOrgRelationByUserId(userId).forEach(r -> ids.add(r.getObjectId()));
        return this.listByIds(ids);
    }

    @Override
    public String getFullNameByOrgIdTree(Map<String, String> idNameMaps, String orgIdTree, String regex) {
        if (idNameMaps == null || idNameMaps.isEmpty()) {
            idNameMaps = this.getInfoList();
        }
        return this.baseMapper.getFullNameByOrgIdTree(idNameMaps, orgIdTree, regex);
    }

    @Override
    public String getOrganizeIdTree(OrganizeEntity entity) {
        return this.baseMapper.getOrganizeIdTree(entity);
    }

    @Override
    public List<OrganizeEntity> getOrganizeByParentId(String parentId) {
        return this.baseMapper.getOrganizeByParentId(parentId);
    }


    @Override
    public List<OrganizeEntity> getDepartmentAll(String organizeId) {
        return this.baseMapper.getDepartmentAll(organizeId);
    }

    @Override
    public OrganizeEntity getOrganizeCompany(String organizeId) {
        return this.baseMapper.getOrganizeCompany(organizeId);
    }

    @Override
    public void getOrganizeDepartmentAll(String organizeId, List<OrganizeEntity> organizeList) {
        this.baseMapper.getOrganizeDepartmentAll(organizeId, organizeList);
    }

    @Override
    public List<String> getOrgIdTree(OrganizeEntity entity) {
        return this.baseMapper.getOrgIdTree(entity);
    }

    @Override
    public List<String> upWardRecursion(List<String> orgIDs, String orgID) {
        this.getOrgIDs(orgIDs, orgID);
        return orgIDs;
    }

    private void getOrgIDs(List<String> orgIDs, String orgID) {
        OrganizeEntity info = this.getInfo(orgID);
        if (info != null) {
            this.getOrgIDs(orgIDs, info.getParentId());
            orgIDs.add(info.getId());
        }
    }

    @Override
    public Map<String, String> getInfoList() {
        if (redisUtil.exists(cacheKeyUtil.getOrganizeInfoList())) {
            return new HashMap<>(redisUtil.getMap(cacheKeyUtil.getOrganizeInfoList()));
        } else {
            Map<String, String> infoMap = this.baseMapper.getInfoList();
            redisUtil.insert(cacheKeyUtil.getOrganizeInfoList(), infoMap);
            return infoMap;
        }
    }

    @Override
    public List<OrganizeListVO> selectedList(List<String> idStrList) {
        return this.baseMapper.selectedList(idStrList);
    }

    @Override
    public List<OrganizeEntity> organizeCondition(List<String> idStrList) {
        return this.baseMapper.organizeCondition(idStrList);
    }

    @Override
    public String getNameByIdStr(String idStr) {
        return this.baseMapper.getNameByIdStr(idStr);
    }

    @Override
    public List<OrganizeEntity> getProgeny(List<String> idList, Integer enabledMark) {
        return this.baseMapper.getProgeny(idList, enabledMark);
    }
}
