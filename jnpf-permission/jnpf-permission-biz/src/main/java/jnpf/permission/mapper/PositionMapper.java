package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.emnus.SysParamEnum;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.position.PositionPagination;
import jnpf.util.*;

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

public interface PositionMapper extends SuperMapper<PositionEntity> {

    default List<PositionEntity> getList(PositionPagination pagination) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        boolean flag = false;
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(PositionEntity::getFullName, pagination.getKeyword())
                            .or().like(PositionEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (StringUtil.isNotEmpty(pagination.getOrganizeId())) {
            queryWrapper.lambda().eq(PositionEntity::getOrganizeId, pagination.getOrganizeId());
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.lambda().eq(PositionEntity::getEnabledMark, pagination.getEnabledMark());
        }
        if (pagination.getDefaultMark() != null) {
            if (Objects.equals(pagination.getDefaultMark(), 0)) {
                queryWrapper.lambda().and(t -> t.eq(PositionEntity::getDefaultMark, 0)
                        .or().isNull(PositionEntity::getDefaultMark));
            } else {
                queryWrapper.lambda().eq(PositionEntity::getDefaultMark, pagination.getDefaultMark());
            }
        }

        if (Objects.equals(pagination.getDataType(), 1)) {
            //排序
            queryWrapper.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
            if (flag) {
                queryWrapper.lambda().orderByDesc(PositionEntity::getLastModifyTime);
            }
            List<PositionEntity> list = selectList(queryWrapper);
            PositionEntity positionEntity = list.stream().filter(t -> Objects.equals(t.getDefaultMark(), 1)).findFirst().orElse(null);
            if (positionEntity != null) {
                list.remove(positionEntity);
                list.add(0, positionEntity);
            }
            return list;
        }

        long count = this.selectCount(queryWrapper);
        //排序
        queryWrapper.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(PositionEntity::getLastModifyTime);
        }
        Page<PositionEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize(), count, false);
        page.setOptimizeCountSql(false);
        IPage<PositionEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default boolean isExistByFullName(PositionEntity entity, boolean isFilter) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        if (entity != null) {
            queryWrapper.lambda().eq(PositionEntity::getFullName, entity.getFullName());
        }
        //是否需要过滤
        if (isFilter&&entity!=null) {
            queryWrapper.lambda().ne(PositionEntity::getId, entity.getId());
        }
        List<PositionEntity> entityList = this.selectList(queryWrapper);
        for (PositionEntity positionEntity : entityList) {
            //如果组织id相同则代表已存在
            if (entity != null && entity.getOrganizeId().equals(positionEntity.getOrganizeId())) {
                return true;
            }
        }
        return false;
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PositionEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(PositionEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(PositionEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        setTreeAtt(entity);
        entity.setEnabledMark(1);
        if (entity.getSortCode() == null) {
            entity.setSortCode(0l);
        }
        this.insert(entity);
    }

    default boolean update(String id, PositionEntity entity) {
        entity.setId(id);
        setTreeAtt(entity);
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(1);
        int i = this.updateById(entity);
        return i > 0;
    }

    default PositionEntity getInfo(String id) {
        return this.selectById(id);
    }

    default void deleteByOrgId(String orgId) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(PositionEntity::getId);
        queryWrapper.lambda().eq(PositionEntity::getOrganizeId, orgId);
        this.deleteByIds(this.selectList(queryWrapper));
    }

    /**
     * 递归获取父级列表
     *
     * @return
     */
    default void recursionOrg(String id, List<PositionEntity> list) {
        PositionEntity info = this.selectById(id);
        if (info != null) {
            list.add(info);
            recursionOrg(info.getParentId(), list);
        }
    }

    default List<PositionEntity> getParentList(String parentId) {
        List<PositionEntity> list = new ArrayList<>();
        if (StringUtil.isNotEmpty(parentId)) {
            recursionOrg(parentId, list);
            // 倒序排放
            Collections.reverse(list);
        }
        return list;
    }

    default List<PositionEntity> getByParentId(String parentId) {
        if (StringUtil.isEmpty(parentId)) return Collections.emptyList();
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PositionEntity::getParentId, parentId);
        return this.selectList(queryWrapper);
    }

    /**
     * 设置树形属性
     *
     * @param entity
     */
    default void setTreeAtt(PositionEntity entity) {
        String treeIds = entity.getId();
        if (StringUtil.isNotEmpty(entity.getParentId())) {
            StringJoiner postJ = new StringJoiner(",");
            List<PositionEntity> parentList = getParentList(entity.getParentId());
            parentList.stream().forEach(t -> postJ.add(t.getId()));
            postJ.add(entity.getId());
            treeIds = postJ.toString();
        }
        entity.setPositionIdTree(treeIds);
    }

    default List<PositionEntity> getListByOrgIds(List<String> orgIds) {
        if (CollUtil.isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PositionEntity::getOrganizeId, orgIds);
        queryWrapper.lambda().eq(PositionEntity::getEnabledMark, 1);
        return selectList(queryWrapper);
    }

    default List<PositionEntity> getListByOrgIdOneLevel(List<String> orgIds) {
        if (CollUtil.isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PositionEntity::getOrganizeId, orgIds);
        queryWrapper.lambda().and(t -> t.isNull(PositionEntity::getParentId).or().eq(PositionEntity::getParentId, "-1"));
        queryWrapper.lambda().eq(PositionEntity::getEnabledMark, 1);
        return selectList(queryWrapper);
    }

    default List<PositionEntity> getList(boolean filterEnabledMark) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(PositionEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<PositionEntity> getAllChild(String id) {
        QueryWrapper<PositionEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(PositionEntity::getEnabledMark, 1).like(PositionEntity::getPositionIdTree, id);
        return this.selectList(wrapper);
    }

    default List<PositionEntity> getPosList(List<String> idList) {
        if (!idList.isEmpty()) {
            QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(PositionEntity::getId, idList)
                    .select(PositionEntity::getId, PositionEntity::getFullName, PositionEntity::getEnabledMark);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

    default List<PositionEntity> getListByIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(PositionEntity::getId, list);
        }
        return this.selectList(queryWrapper);
    }

    default List<PositionEntity> getListByIds(Pagination pagination, List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(PositionEntity::getId, PositionEntity::getOrganizeId, PositionEntity::getFullName,
                PositionEntity::getEnCode, PositionEntity::getDescription);
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(PositionEntity::getId, list);
        }
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(PositionEntity::getFullName, pagination.getKeyword())
                            .or().like(PositionEntity::getEnCode, pagination.getKeyword())
            );
        }
        Page<PositionEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<PositionEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default Map<String, String> getPosMap() {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(PositionEntity::getId, PositionEntity::getFullName);
        return this.selectList(queryWrapper).stream().collect(Collectors.toMap(PositionEntity::getId, PositionEntity::getFullName, (p1, p2) -> p1));
    }


    default Map<String, Object> getPosEncodeAndName() {
        return getPosEncodeAndName(false);
    }

    default Map<String, Object> getPosEncodeAndName(boolean enabledMark) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        if (enabledMark) {
            queryWrapper.lambda().eq(PositionEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().select(PositionEntity::getId, PositionEntity::getFullName, PositionEntity::getEnCode);
        return this.selectList(queryWrapper).stream().collect(Collectors.toMap(p -> p.getFullName() + "/" + p.getEnCode(), PositionEntity::getId, (p1, p2) -> p1));
    }


    default boolean first(String id) {
        boolean isOk = false;
        //获取要上移的那条数据的信息
        PositionEntity upEntity = this.selectById(id);
        Long upSortCode = upEntity.getSortCode() == null ? 0 : upEntity.getSortCode();
        //查询上几条记录
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .lt(PositionEntity::getSortCode, upSortCode)
                .eq(PositionEntity::getOrganizeId, upEntity.getOrganizeId())
                .orderByDesc(PositionEntity::getSortCode);
        List<PositionEntity> downEntity = this.selectList(queryWrapper);
        if (!downEntity.isEmpty()) {
            //交换两条记录的sort值
            Long temp = upEntity.getSortCode();
            upEntity.setSortCode(downEntity.get(0).getSortCode());
            downEntity.get(0).setSortCode(temp);
            this.updateById(downEntity.get(0));
            this.updateById(upEntity);
            isOk = true;
        }
        return isOk;
    }

    default boolean next(String id) {
        boolean isOk = false;
        //获取要下移的那条数据的信息
        PositionEntity downEntity = this.selectById(id);
        Long upSortCode = downEntity.getSortCode() == null ? 0 : downEntity.getSortCode();
        //查询下几条记录
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .gt(PositionEntity::getSortCode, upSortCode)
                .eq(PositionEntity::getOrganizeId, downEntity.getOrganizeId())
                .orderByAsc(PositionEntity::getSortCode);
        List<PositionEntity> upEntity = this.selectList(queryWrapper);
        if (!upEntity.isEmpty()) {
            //交换两条记录的sort值
            Long temp = downEntity.getSortCode();
            downEntity.setSortCode(upEntity.get(0).getSortCode());
            upEntity.get(0).setSortCode(temp);
            this.updateById(upEntity.get(0));
            this.updateById(downEntity);
            isOk = true;
        }
        return isOk;
    }

    default List<PositionEntity> getPositionName(List<String> id, boolean filterEnabledMark) {
        List<PositionEntity> roleList = new ArrayList<>();
        if (!id.isEmpty()) {
            QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(PositionEntity::getId, id);
            roleList = this.selectList(queryWrapper);
        }
        return roleList;
    }

    default List<PositionEntity> getPositionName(List<String> id, String keyword) {
        List<PositionEntity> roleList = new ArrayList<>();
        if (!id.isEmpty()) {
            QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(PositionEntity::getId, id);
            //关键字（名称、编码）
            if (!StringUtil.isEmpty(keyword)) {
                queryWrapper.lambda().and(
                        t -> t.like(PositionEntity::getFullName, keyword)
                                .or().like(PositionEntity::getEnCode, keyword)
                );
            }
            roleList = this.selectList(queryWrapper);
        }
        return roleList;
    }

    default List<PositionEntity> getListByOrganizeId(List<String> organizeIds, boolean enabledMark) {
        if (organizeIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PositionEntity::getOrganizeId, organizeIds);
        if (enabledMark) {
            queryWrapper.lambda().eq(PositionEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }


    default List<PositionEntity> getListByFullName(String fullName, String enCode) {
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PositionEntity::getFullName, fullName).eq(PositionEntity::getEnCode, enCode);
        return this.selectList(queryWrapper);
    }

    default List<PositionEntity> positionCondition(List<String> idStrList) {
        if (CollUtil.isEmpty(idStrList)) return Collections.emptyList();

        List<String> idList = new ArrayList<>();
        List<String> parenList = new ArrayList<>();//用于查子岗位
        List<String> ancestorsList = new ArrayList<>();//用于查子孙组织

        UserInfo userInfo = UserProvider.getUser();
        List<String> currentPosIds = userInfo.getPositionIds();

        for (String idStr : idStrList) {
            if (DataInterfaceVarConst.POSITIONID.equals(idStr)) {
                idList.addAll(currentPosIds);
            } else if (DataInterfaceVarConst.POSITIONANDSUB.equals(idStr)) {
                idList.addAll(currentPosIds);
                parenList.addAll(currentPosIds);
            } else if (DataInterfaceVarConst.POSITIONANDPROGENY.equals(idStr)) {
                ancestorsList.addAll(currentPosIds);
            } else {
                String[] split = idStr.split("--");
                idList.add(split[0]);
                if (split.length > 1) {
                    if (SysParamEnum.SUBPOS.getCode().equalsIgnoreCase(split[1])) {
                        parenList.add(split[0]);
                    }
                    if (SysParamEnum.PROGENYPOS.getCode().equalsIgnoreCase(split[1])) {
                        ancestorsList.add(split[0]);
                    }
                }
            }
        }
        if (CollUtil.isEmpty(idList) && CollUtil.isEmpty(parenList) && CollUtil.isEmpty(ancestorsList)) {
            return Collections.emptyList();
        }
        QueryWrapper<PositionEntity> query = new QueryWrapper<>();

        query.lambda().eq(PositionEntity::getEnabledMark, 1);
        query.lambda().and(t -> {
            if (CollUtil.isNotEmpty(idList)) {
                List<List<String>> lists = Lists.partition(idList, 1000);
                for (List<String> thisList : lists) {
                    t.in(PositionEntity::getId, thisList).or();
                }
            }
            if (CollUtil.isNotEmpty(parenList)) {
                List<List<String>> lists = Lists.partition(parenList, 1000);
                for (List<String> thisList : lists) {
                    t.in(PositionEntity::getParentId, thisList).or();
                }
            }
            if (CollUtil.isNotEmpty(ancestorsList)) {
                for (String thisId : ancestorsList) {
                    t.like(PositionEntity::getPositionIdTree, thisId).or();
                }
            }
        });
        query.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
        return this.selectList(query);
    }

    default List<PositionEntity> getListByDutyUser(String userId) {
        LambdaQueryWrapper<PositionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PositionEntity::getDutyUser, userId);
        wrapper.eq(PositionEntity::getEnabledMark, "1");
        return this.selectList(wrapper);
    }

    default List<PositionEntity> getListByParentIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        LambdaQueryWrapper<PositionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PositionEntity::getParentId, idList);
        return this.selectList(wrapper);
    }

    default void linkUpdate(String id, PosConModel posConModel) {
        //联动修改互斥对象
        List<String> muEList = new ArrayList<>();
        if (posConModel.getMutualExclusionFlag()) {
            muEList.addAll(posConModel.getMutualExclusion());
        }
        //muEList 互斥对象。除了这个列表外其他角色里不能包含该互斥
        QueryWrapper<PositionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().like(PositionEntity::getConditionJson, id);
        if (CollUtil.isNotEmpty(muEList)) {
            queryWrapper.lambda().or().in(PositionEntity::getId, muEList);
        }

        List<PositionEntity> list = this.selectList(queryWrapper);
        for (PositionEntity item : list) {
            if (muEList.contains(item.getId())) {
                //添加
                item.setIsCondition(1);
                PosConModel psModel = StringUtil.isEmpty(item.getConditionJson()) ? new PosConModel() : JsonUtil.getJsonToBean(item.getConditionJson(), PosConModel.class);
                List<Integer> constraintType = psModel.getConstraintType() == null ? new ArrayList<>() : psModel.getConstraintType();
                if (!constraintType.contains(0)) {
                    constraintType.add(0);
                    psModel.setConstraintType(constraintType);
                }
                List<String> mutualExclusion = psModel.getMutualExclusion() == null ? new ArrayList<>() : psModel.getMutualExclusion();
                if (!mutualExclusion.contains(id)) {
                    mutualExclusion.add(id);
                    psModel.setMutualExclusion(mutualExclusion);
                    item.setConditionJson(JsonUtil.getObjectToString(psModel));
                }
                this.update(item.getId(), item);
            } else {
                //移除
                if (Objects.equals(item.getIsCondition(), 1)) {
                    PosConModel psModel = JsonUtil.getJsonToBean(item.getConditionJson(), PosConModel.class);
                    psModel.init();
                    if (psModel.getMutualExclusionFlag()) {
                        List<String> mutualExclusion = psModel.getMutualExclusion();
                        if (mutualExclusion.contains(id)) {
                            mutualExclusion.remove(id);
                            if (mutualExclusion.isEmpty()) {
                                List<Integer> constraintType = psModel.getConstraintType();
                                constraintType.remove(Integer.valueOf(0));
                                psModel.setConstraintType(constraintType);
                                if (constraintType.isEmpty()) {
                                    item.setIsCondition(0);
                                }
                            }
                            item.setConditionJson(JsonUtil.getObjectToString(psModel));
                            this.update(item.getId(), item);
                        }
                    }
                }
            }
        }
    }

    default List<PositionEntity> getProgeny(List<String> idList, Integer enabledMark) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<PositionEntity> query = new QueryWrapper<>();
        if (enabledMark != null) {
            query.lambda().eq(PositionEntity::getEnabledMark, enabledMark);
        }
        query.lambda().and(t -> {
            for (String thisId : idList) {
                t.like(PositionEntity::getPositionIdTree, thisId).or();
            }
        });
        query.lambda().orderByAsc(PositionEntity::getSortCode).orderByAsc(PositionEntity::getCreatorTime);
        return this.selectList(query);
    }
}
