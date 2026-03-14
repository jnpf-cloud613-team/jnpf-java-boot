package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.PermissionConst;
import jnpf.emnus.SysParamEnum;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.model.organize.OrganizeListVO;
import jnpf.permission.model.organize.OrganizePagination;
import jnpf.util.*;

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
public interface OrganizeMapper extends SuperMapper<OrganizeEntity> {

    default List<OrganizeEntity> getList(OrganizePagination pagination) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        boolean flag = false;
        //关键词查询
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(t -> t.like(OrganizeEntity::getFullName, pagination.getKeyword()).or()
                    .like(OrganizeEntity::getEnCode, pagination.getKeyword()));
        }
        //是否查询子组织
        if (StringUtil.isNotEmpty(pagination.getParentId())) {
            if (Objects.equals(pagination.getParentId(), "0")) {
                queryWrapper.lambda().eq(OrganizeEntity::getParentId, "-1");
            } else {
                queryWrapper.lambda().eq(OrganizeEntity::getParentId, pagination.getParentId());
            }
        }
        //是否查询启用禁用
        if (Objects.equals(pagination.getEnabledMark(), 1)) {
            queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        }
        //排序
        queryWrapper.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(OrganizeEntity::getLastModifyTime);
        }

        //不分页
        if (Objects.equals(pagination.getDataType(), 1)) {
            return this.selectList(queryWrapper);
        }
        //分页
        Page<OrganizeEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<OrganizeEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default void create(OrganizeEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        // 设置组织树形id和名称
        setOrgTreeIdAndName(entity);
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(1);
        this.insert(entity);
    }

    default boolean update(String id, OrganizeEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(1);
        // 设置组织树形id和名称
        setOrgTreeIdAndName(entity);
        int i = this.updateById(entity);
        return i > 0;
    }

    default OrganizeEntity getInfo(String id) {
        return this.selectById(id);
    }

    default Map<String, Object> getAllOrgsTreeName() {
        Map<String, Object> map = new HashMap<>();
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        Map<String, String> collect = list.stream().collect(Collectors.toMap(OrganizeEntity::getId, OrganizeEntity::getFullName));
        for (OrganizeEntity org : list) {
            String organizeIdTree = org.getOrganizeIdTree();
            if (StringUtil.isEmpty(organizeIdTree)) {
                organizeIdTree = org.getId();
            }
            String[] split = organizeIdTree.split(",");
            StringJoiner names = new StringJoiner("/");
            for (String id : split) {
                if (collect.get(id) != null) {
                    names.add(collect.get(id));
                }
            }
            map.put(org.getId(), names.toString());
        }
        return map;
    }

    default Map<String, String> getAllOrgsTreeName(boolean enabledMark) {
        Map<String, String> map = new HashMap<>();
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        if (enabledMark) {
            queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        }
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        Map<String, String> collect = list.stream().collect(Collectors.toMap(OrganizeEntity::getId, OrganizeEntity::getFullName));
        for (OrganizeEntity org : list) {
            String organizeIdTree = org.getOrganizeIdTree();
            if (StringUtil.isEmpty(organizeIdTree)) {
                organizeIdTree = org.getId();
            }
            String[] split = organizeIdTree.split(",");
            StringJoiner names = new StringJoiner("/");
            for (String id : split) {
                if (collect.get(id) != null) {
                    names.add(collect.get(id));
                    continue;
                }
                names = new StringJoiner(names.toString().substring(0, names.toString().length() - 1));
            }
            map.put(org.getId(), names.toString());
        }
        return map;
    }

    default List<OrganizeEntity> getListAll(List<String> idAll, String keyWord) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        List<OrganizeEntity> list = new ArrayList<>();
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(keyWord)) {
            flag = true;
            queryWrapper.lambda().and(t -> t.like(OrganizeEntity::getFullName, keyWord).or().like(OrganizeEntity::getEnCode, keyWord));
        }
        // 排序
        queryWrapper.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(OrganizeEntity::getLastModifyTime);
        }
        if (!idAll.isEmpty()) {
            queryWrapper.lambda().in(OrganizeEntity::getId, idAll);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default List<OrganizeEntity> getListByIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(OrganizeEntity::getId, list);
        }
        return this.selectList(queryWrapper);
    }


    default boolean isExistByFullName(OrganizeEntity entity, boolean isCheck, boolean isFilter) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getFullName, entity.getFullName());
        if (!isCheck) {
            if (isFilter) {
                queryWrapper.lambda().ne(OrganizeEntity::getId, entity.getId());
            }
            List<OrganizeEntity> entityList = this.selectList(queryWrapper);
            if (!entityList.isEmpty()) {
                for (OrganizeEntity organizeEntity : entityList) {
                    if (organizeEntity != null && organizeEntity.getParentId().equals(entity.getParentId())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(OrganizeEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    /**
     * 递归获取组织列表
     *
     * @return
     */
    default void recursionOrg(String id, List<OrganizeEntity> list) {
        OrganizeEntity info = getInfo(id);
        if (info != null) {
            list.add(info);
            recursionOrg(info.getParentId(), list);
        }
    }

    default List<OrganizeEntity> getParentList(String parentId) {
        List<OrganizeEntity> list = new ArrayList<>();
        if (StringUtil.isNotEmpty(parentId)) {
            recursionOrg(parentId, list);
            // 倒序排放
            Collections.reverse(list);
        }
        return list;
    }

    default List<OrganizeEntity> getListByParentIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        LambdaQueryWrapper<OrganizeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrganizeEntity::getParentId, idList);
        wrapper.eq(OrganizeEntity::getEnabledMark, 1);
        return this.selectList(wrapper);
    }

    default void setOrgTreeIdAndName(OrganizeEntity entity) {
        StringJoiner orgIdTree = new StringJoiner(",");
        StringJoiner orgNameTree = new StringJoiner("/");
        List<OrganizeEntity> parentList = getParentList(entity.getParentId());
        if (CollUtil.isNotEmpty(parentList)) {
            parentList.stream().forEach(t -> {
                orgIdTree.add(t.getId());
                orgNameTree.add(t.getFullName());
            });
        }
        orgIdTree.add(entity.getId());
        orgNameTree.add(entity.getFullName());
        entity.setOrganizeIdTree(orgIdTree.toString());
        entity.setOrgNameTree(orgNameTree.toString());
    }

    default List<OrganizeEntity> getDepsByParentId(String id) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getParentId, id);
        queryWrapper.lambda().eq(OrganizeEntity::getCategory, PermissionConst.DEPARTMENT);
        queryWrapper.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<OrganizeEntity> getList(boolean filterEnabledMark) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default OrganizeEntity getInfoByFullName(String fullName) {
        if (StringUtil.isEmpty(fullName)) {
            return null;
        }
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getFullName, fullName);
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        return !list.isEmpty() ? list.get(0) : null;
    }

    default List<OrganizeEntity> getList(String keyword, boolean filterEnabledMark) {
        return new LinkedList<>(getOrgMaps(keyword, filterEnabledMark, null).values());
    }


    /**
     * 获取组织信息
     *
     * @return OrgId, OrgEntity
     */
    default Map<String, OrganizeEntity> getOrgMapsAll(SFunction<OrganizeEntity, ?>... columns) {
        return getOrgMaps(null, false, null, columns);
    }

    /**
     * 获取组织信息
     *
     * @param keyword
     * @param filterEnabledMark
     * @param category
     * @return OrgId, OrgEntity
     */
    default Map<String, OrganizeEntity> getOrgMaps(String keyword, boolean filterEnabledMark, String category, SFunction<OrganizeEntity, ?>... columns) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().and(t -> t.like(OrganizeEntity::getFullName, keyword).or().like(OrganizeEntity::getEnCode, keyword.toLowerCase()));
        }
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        }
        if (StringUtil.isNotEmpty(category)) {
            queryWrapper.lambda().eq(OrganizeEntity::getCategory, category);
        }
        Map<String, OrganizeEntity> orgMaps = getBaseOrgMaps(queryWrapper, ImmutableMap.of(OrganizeEntity::getSortCode, true, OrganizeEntity::getCreatorTime, false), null, false);

        Map<String, OrganizeEntity> entityList = new LinkedHashMap<>();
        if (StringUtil.isNotEmpty(keyword)) {
            getParentOrganize(orgMaps, orgMaps, entityList);
            orgMaps.clear();
            orgMaps = entityList;
        }
        return orgMaps;
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
    default Map<String, OrganizeEntity> getBaseOrgMaps(QueryWrapper<OrganizeEntity> queryWrapper, Map<SFunction<OrganizeEntity, ?>, Boolean> orderBy, List<SFunction<OrganizeEntity, ?>> groupBy, boolean bigColumnHas, SFunction<OrganizeEntity, ?>... columns) {
        if (queryWrapper == null) {
            queryWrapper = new QueryWrapper<>();
        }
        LambdaQueryWrapper<OrganizeEntity> lambdaQueryWrapper = queryWrapper.lambda();

        List<SFunction<OrganizeEntity, ?>> columnList;
        List<SFunction<OrganizeEntity, ?>> bigColumnList = null;
        //没有指定查询字段就返回全部字段
        if (columns == null || columns.length == 0) {
            columnList = Arrays.asList(OrganizeEntity::getId, OrganizeEntity::getParentId, OrganizeEntity::getCategory, OrganizeEntity::getEnCode, OrganizeEntity::getFullName, OrganizeEntity::getManagerId, OrganizeEntity::getSortCode, OrganizeEntity::getEnabledMark, OrganizeEntity::getOrganizeIdTree, OrganizeEntity::getCreatorTime, OrganizeEntity::getCreatorUserId, OrganizeEntity::getLastModifyTime, OrganizeEntity::getLastModifyUserId, OrganizeEntity::getDeleteMark, OrganizeEntity::getDeleteTime, OrganizeEntity::getDeleteUserId, OrganizeEntity::getTenantId);
            //把长文本字段分开查询, 默认带有排序， 数据量大的情况长文本字段参与排序速度非常慢
            if (bigColumnHas) {
                bigColumnList = Arrays.asList(OrganizeEntity::getId, OrganizeEntity::getDescription, OrganizeEntity::getPropertyJson);
            }
        } else {
            columnList = new ArrayList<>(Arrays.asList(columns));
            //指定字段中没有ID， 强制添加ID字段
            if (!columnList.contains((SFunction<OrganizeEntity, ?>) OrganizeEntity::getId)) {
                columnList.add(OrganizeEntity::getId);
            }
        }
        lambdaQueryWrapper.select(columnList);
        QueryWrapper<OrganizeEntity> bigColumnQuery = null;
        if (bigColumnList != null) {
            //获取大字段不参与排序
            bigColumnQuery = queryWrapper.clone();
        }
        //排序
        if (orderBy != null && !orderBy.isEmpty()) {
            orderBy.forEach((k, v) -> lambdaQueryWrapper.orderBy(true, v, k));
        }
        //分组
        if (groupBy != null && !groupBy.isEmpty()) {
            lambdaQueryWrapper.groupBy(groupBy);
        }
        List<OrganizeEntity> list = this.selectList(queryWrapper);

        Map<String, OrganizeEntity> orgMaps = new LinkedHashMap<>(list.size(), 1);
        list.forEach(t -> orgMaps.put(t.getId(), t));

        if (bigColumnList != null) {
            //获取大字段数据
            bigColumnQuery.lambda().select(bigColumnList);
            List<OrganizeEntity> listBigFields = this.selectList(bigColumnQuery);
            listBigFields.forEach(t -> {
                OrganizeEntity organizeEntity = orgMaps.get(t.getId());
                if (organizeEntity != null) {
                    organizeEntity.setPropertyJson(t.getPropertyJson());
                    organizeEntity.setDescription(t.getDescription());
                }
            });
        }
        return orgMaps;
    }

    /**
     * 获取父级集合
     *
     * @param list       需要遍历的集合
     * @param entityList 结果集
     */
    default void getParentOrganize(Map<String, OrganizeEntity> list, Map<String, OrganizeEntity> searchList, Map<String, OrganizeEntity> entityList) {
        Map<String, OrganizeEntity> list1 = new LinkedHashMap<>();
        searchList.forEach((id, entity) -> {
            entityList.put(id, entity);
            OrganizeEntity info = list.get(id);
            if (info == null) {
                info = getInfo(id);
            }
            if (Objects.nonNull(info)) {
                if (StringUtil.isNotEmpty(info.getParentId()) && !"-1".equals(info.getParentId())) {
                    OrganizeEntity organizeEntity = list.get(info.getParentId());
                    if (organizeEntity == null) {
                        organizeEntity = getInfo(info.getParentId());
                    }
                    if (organizeEntity != null) {
                        list1.put(organizeEntity.getId(), organizeEntity);
                        getParentOrganize(list, list1, entityList);
                    }
                } else if (StringUtil.isNotEmpty(info.getParentId()) && "-1".equals(info.getParentId())) {
                    entityList.put(id, info);
                }
            }
        });
    }

    default List<OrganizeEntity> getOrgEntityList(List<String> idList, boolean enable) {
        if (!idList.isEmpty()) {
            QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(OrganizeEntity::getId, idList);
            if (enable) {
                queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
            }
            return this.selectList(queryWrapper);
        }
        return Collections.emptyList();
    }

    default Map<String, Object> getOrgMap() {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(OrganizeEntity::getId, OrganizeEntity::getFullName);
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        return list.stream().collect(Collectors.toMap(OrganizeEntity::getId, OrganizeEntity::getFullName));
    }

    default Map<String, Object> getOrgEncodeAndName(String category) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(OrganizeEntity::getId, OrganizeEntity::getFullName, OrganizeEntity::getEnCode);
        if (StringUtil.isNotEmpty(category)) {
            queryWrapper.lambda().eq(OrganizeEntity::getCategory, category);
        }
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        return list.stream().collect(Collectors.toMap(o -> o.getFullName() + "/" + o.getEnCode(), OrganizeEntity::getId, (v1, v2) -> v2));
    }

    default Map<String, Object> getOrgNameAndId(String category) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(OrganizeEntity::getId, OrganizeEntity::getFullName);
        if (StringUtil.isNotEmpty(category)) {
            queryWrapper.lambda().eq(OrganizeEntity::getCategory, category);
        }
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        Map<String, Object> allOrgMap = new HashMap<>();
        for (OrganizeEntity entity : list) {
            allOrgMap.put(entity.getFullName(), entity.getId());
        }
        return allOrgMap;
    }


    default OrganizeEntity getByFullName(String fullName) {
        OrganizeEntity organizeEntity = new OrganizeEntity();
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getFullName, fullName);
        queryWrapper.lambda().select(OrganizeEntity::getId);
        List<OrganizeEntity> list = this.selectList(queryWrapper);
        if (!list.isEmpty()) {
            organizeEntity = list.get(0);
        }
        return organizeEntity;
    }


    default void getOrganizeIdTree(String organizeId, List<String> organizeParentIdList) {
        OrganizeEntity entity = getInfo(organizeId);
        if (entity != null) {
            organizeParentIdList.add(entity.getId());
            if (StringUtil.isNotEmpty(entity.getParentId())) {
                getOrganizeIdTree(entity.getParentId(), organizeParentIdList);
            }
        }
    }

    default void getOrganizeId(String organizeId, List<OrganizeEntity> organizeList) {
        OrganizeEntity entity = getInfo(organizeId);
        if (entity != null) {
            organizeList.add(entity);
            if (StringUtil.isNotEmpty(entity.getParentId())) {
                getOrganizeId(entity.getParentId(), organizeList);
            }
        }
    }

    default List<OrganizeEntity> getOrganizeName(List<String> id) {
        List<OrganizeEntity> list = new ArrayList<>();
        if (!id.isEmpty()) {
            QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
            List<List<String>> lists = Lists.partition(id, 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> ids : lists) {
                    t.or().in(OrganizeEntity::getId, ids);
                }
            });
            queryWrapper.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default Map<String, OrganizeEntity> getOrganizeName(List<String> id, String keyword, boolean filterEnabledMark, String category) {
        Map<String, OrganizeEntity> list = Collections.emptyMap();
        if (!id.isEmpty()) {
            QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
            List<List<String>> lists = Lists.partition(id, 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> ids : lists) {
                    t.or().in(OrganizeEntity::getId, ids);
                }
            });
            if (StringUtil.isNotEmpty(keyword)) {
                queryWrapper.lambda().and(t -> t.like(OrganizeEntity::getFullName, keyword).or().like(OrganizeEntity::getEnCode, keyword));
            }
            if (StringUtil.isNotEmpty(category)) {
                queryWrapper.lambda().eq(OrganizeEntity::getCategory, category);
            }
            if (filterEnabledMark) {
                queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
            }
            list = getBaseOrgMaps(queryWrapper, ImmutableMap.of(OrganizeEntity::getSortCode, true, OrganizeEntity::getCreatorTime, false), null, false);
        }
        return list;
    }

    default Map<String, String> getInfoList() {
        Map<String, OrganizeEntity> orgs = getOrgMaps(null, false, null, OrganizeEntity::getFullName);
        Map<String, String> infoMap = new LinkedHashMap<>(orgs.size(), 1);
        orgs.forEach((k, v) -> infoMap.put(k, v.getFullName()));
        return infoMap;
    }

    default List<String> getUnderOrganizations(String organizeId, boolean filterEnabledMark) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().ne(OrganizeEntity::getId, organizeId);
        queryWrapper.lambda().like(OrganizeEntity::getOrganizeIdTree, organizeId);
        queryWrapper.lambda().select(OrganizeEntity::getId);
        return this.selectList(queryWrapper).stream().map(OrganizeEntity::getId).collect(Collectors.toList());
    }

    default List<String> getUnderOrganizationss(String organizeId) {
        return this.getUnderOrganizations(organizeId, false);
    }

    default List<OrganizeEntity> getListByParentId(String id) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getParentId, id);
        queryWrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }


    default String getFullNameByOrgIdTree(Map<String, String> idNameMaps, String orgIdTree, String regex) {
        if (idNameMaps == null || idNameMaps.isEmpty()) {
            Map<String, OrganizeEntity> orgs = getOrgMaps(null, false, null, OrganizeEntity::getFullName);
            idNameMaps = new LinkedHashMap<>(orgs.size(), 1);
        }
        String fullName = "";
        if (StringUtil.isNotEmpty(orgIdTree)) {
            String[] split = orgIdTree.split(",");
            StringBuilder orgName = new StringBuilder();
            String tmpName;
            for (String orgId : split) {
                if (StringUtil.isEmpty(orgIdTree)) {
                    continue;
                }
                if ((tmpName = idNameMaps.get(orgId)) != null) {
                    orgName.append(regex).append(tmpName);
                }
            }
            if (orgName.length() > 0) {
                fullName = orgName.toString().replaceFirst(regex, "");
            }
        }
        return fullName;
    }

    default String getOrganizeIdTree(OrganizeEntity entity) {
        List<String> list = new ArrayList<>();
        this.getOrganizeIdTree(entity.getParentId(), list);
        // 倒序排放
        Collections.reverse(list);

        StringJoiner organizeIdTree = new StringJoiner(",");
        for (String organizeParentId : list) {
            organizeIdTree.add(organizeParentId);
        }
        return organizeIdTree.toString();
    }

    default List<OrganizeEntity> getOrganizeByParentId(String parentId) {
        QueryWrapper<OrganizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrganizeEntity::getParentId, parentId);
        return this.selectList(queryWrapper);
    }

    default List<OrganizeEntity> getAllChild(String id) {
        QueryWrapper<OrganizeEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1).like(OrganizeEntity::getOrganizeIdTree, id);
        return this.selectList(wrapper);
    }

    default List<OrganizeEntity> getDepartmentAll(String organizeId) {
        OrganizeEntity organizeCompany = getOrganizeCompany(organizeId);
        if (null == organizeCompany) {
            return new ArrayList<>();
        }
        String id = organizeCompany.getId();
        List<OrganizeEntity> organizeList = new ArrayList<>();
        organizeList.add(organizeCompany);
        List<OrganizeEntity> organizeEntityList = getListByParentId(id).stream().filter(t -> PermissionConst.DEPARTMENT.equals(t.getCategory())).collect(Collectors.toList());
        for (OrganizeEntity entity : organizeEntityList) {
            QueryWrapper<OrganizeEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(OrganizeEntity::getEnabledMark, 1).like(OrganizeEntity::getOrganizeIdTree, entity.getId());
            organizeList.addAll(this.selectList(wrapper));
        }
        return organizeList;
    }

    default OrganizeEntity getOrganizeCompany(String organizeId) {
        OrganizeEntity entity = getInfo(organizeId);
        if (entity == null) {
            return null;
        }
        List<String> categoryList = ImmutableList.of("group", "company", "subsidiary", "unit");
        if (categoryList.contains(entity.getCategory())) {
            return entity;
        } else {
            return PermissionConst.DEPARTMENT.equals(entity.getCategory()) ? getOrganizeCompany(entity.getParentId()) : null;
        }
    }

    default void getOrganizeDepartmentAll(String organizeId, List<OrganizeEntity> organizeList) {
        List<OrganizeEntity> organizeEntityList = getListByParentId(organizeId);
        for (OrganizeEntity entity : organizeEntityList) {
            if (!PermissionConst.COMPANY.equals(entity.getCategory())) {
                organizeList.add(entity);
                getOrganizeDepartmentAll(entity.getId(), organizeList);
            }
        }
    }

    default List<String> getOrgIdTree(OrganizeEntity entity) {
        List<String> orgIds = new ArrayList<>();
        if (entity != null) {
            String organizeIdTree = entity.getOrganizeIdTree();
            if (StringUtil.isNotEmpty(organizeIdTree)) {
                String[] split = organizeIdTree.split(",");
                orgIds.addAll(Arrays.asList(split));
            }
        }
        return orgIds;
    }

    default List<OrganizeListVO> selectedList(List<String> idStrList) {
        if (CollUtil.isEmpty(idStrList)) return Collections.emptyList();
        List<String> idList = new ArrayList<>();

        for (String idStr : idStrList) {
            String[] split = idStr.split("--");
            idList.add(split[0]);
        }
        List<OrganizeEntity> listByIds = this.getListByIds(idList);
        List<OrganizeListVO> listVo = new ArrayList<>();
        for (String idStr : idStrList) {
            String[] split = idStr.split("--");
            String id = split[0];
            String type = split.length > 1 ? split[1] : "";
            SysParamEnum sysParamEnum = SysParamEnum.get(type);
            String suffix = sysParamEnum != null ? sysParamEnum.getSuffix() : "";
            OrganizeEntity organizeEntity = listByIds.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
            if (organizeEntity != null) {
                OrganizeListVO vo = JsonUtil.getJsonToBean(organizeEntity, OrganizeListVO.class);
                vo.setId(idStr);
                vo.setOrgNameTree(organizeEntity.getOrgNameTree() + suffix);
                listVo.add(vo);
            }
        }
        return listVo;
    }

    default List<OrganizeEntity> organizeCondition(List<String> idStrList) {
        if (CollUtil.isEmpty(idStrList)) return Collections.emptyList();

        List<String> idList = new ArrayList<>();
        List<String> parenList = new ArrayList<>();//用于查子组织
        List<String> ancestorsList = new ArrayList<>();//用于查孙子组织

        UserInfo userInfo = UserProvider.getUser();
        List<String> currentOrgIds = userInfo.getOrganizeIds();

        for (String idStr : idStrList) {
            if (DataInterfaceVarConst.ORG.equals(idStr)) {
                idList.addAll(currentOrgIds);
            } else if (DataInterfaceVarConst.ORGANDSUB.equals(idStr)) {
                idList.addAll(currentOrgIds);
                parenList.addAll(currentOrgIds);
            } else if (DataInterfaceVarConst.ORGANIZEANDPROGENY.equals(idStr)) {
                ancestorsList.addAll(currentOrgIds);
            } else {
                String[] split = idStr.split("--");
                idList.add(split[0]);
                if (split.length > 1) {
                    if (SysParamEnum.SUBORG.getCode().equalsIgnoreCase(split[1])) {
                        parenList.add(split[0]);
                    }
                    if (SysParamEnum.PROGENYORG.getCode().equalsIgnoreCase(split[1])) {
                        ancestorsList.add(split[0]);
                    }
                }
            }
        }
        if (CollUtil.isEmpty(idList) && CollUtil.isEmpty(parenList) && CollUtil.isEmpty(ancestorsList)) {
            return Collections.emptyList();
        }
        QueryWrapper<OrganizeEntity> query = new QueryWrapper<>();

        query.lambda().eq(OrganizeEntity::getEnabledMark, 1);
        query.lambda().and(t -> {
            if (CollUtil.isNotEmpty(idList)) {
                List<List<String>> lists = Lists.partition(idList, 1000);
                for (List<String> thisList : lists) {
                    t.in(OrganizeEntity::getId, thisList).or();
                }
            }
            if (CollUtil.isNotEmpty(parenList)) {
                List<List<String>> lists = Lists.partition(parenList, 1000);
                for (List<String> thisList : lists) {
                    t.in(OrganizeEntity::getParentId, thisList).or();
                }
            }
            if (CollUtil.isNotEmpty(ancestorsList)) {
                for (String thisId : ancestorsList) {
                    t.like(OrganizeEntity::getOrganizeIdTree, thisId).or();
                }
            }
        });
        query.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        return this.selectList(query);
    }

    default String getNameByIdStr(String idStr) {
        StringJoiner sj = new StringJoiner(",");
        if (StringUtil.isNotEmpty(idStr)) {
            try {
                List<String> ids = JsonUtil.getJsonToList(idStr, String.class);
                List<OrganizeEntity> listByIds = this.getListByIds(ids);
                for (OrganizeEntity item : listByIds) {
                    sj.add(item.getOrgNameTree());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sj.toString();
    }

    default List<OrganizeEntity> getProgeny(List<String> idList, Integer enabledMark) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<OrganizeEntity> query = new QueryWrapper<>();
        if (enabledMark != null) {
            query.lambda().eq(OrganizeEntity::getEnabledMark, enabledMark);
        }
        query.lambda().and(t -> {
            for (String thisId : idList) {
                t.like(OrganizeEntity::getOrganizeIdTree, thisId).or();
            }
        });
        query.lambda().orderByAsc(OrganizeEntity::getSortCode).orderByAsc(OrganizeEntity::getCreatorTime);
        return this.selectList(query);
    }

}
