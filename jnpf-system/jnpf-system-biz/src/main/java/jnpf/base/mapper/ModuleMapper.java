package jnpf.base.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.module.ModulePagination;
import jnpf.base.model.module.ModuleSelectorVo;
import jnpf.constant.JnpfConst;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统功能
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleMapper extends SuperMapper<ModuleEntity> {

    default void create(ModuleEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default ModuleEntity getInfo(String id) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<ModuleEntity> getList(boolean filterFlowWork, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode)
                .orderByDesc(ModuleEntity::getCreatorTime);
        // 移除工作流程菜单
        if (filterFlowWork) {
            List<String> moduleCode = JnpfConst.MODULE_CODE;
            queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleCode);
        }
        if (!moduleAuthorize.isEmpty()) {
            queryWrapper.lambda().notIn(ModuleEntity::getId, moduleAuthorize);
            queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleAuthorize);
        }
        if (!moduleUrlAddressAuthorize.isEmpty()) {
            queryWrapper.lambda().and(t -> t.notIn(ModuleEntity::getUrlAddress, moduleUrlAddressAuthorize).or().isNull(ModuleEntity::getUrlAddress));
        }
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> getList() {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode)
                .orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> getListTenant(String urlAddress) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNotNull(ModuleEntity::getUrlAddress);
        if (urlAddress != null) {
            queryWrapper.lambda().ne(ModuleEntity::getUrlAddress, urlAddress);
        }
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode)
                .orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> getListByParentId(String id) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getParentId, id);
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode)
                .orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default ModuleEntity getInfo(String id, String systemId) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getId, id);
        queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);
        return this.selectOne(queryWrapper);
    }

    default ModuleEntity getInfo(String id, String systemId, String parentId) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getId, id);
        queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);
        queryWrapper.lambda().eq(ModuleEntity::getParentId, parentId);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(ModuleEntity entity, String category, String systemId) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getFullName, entity.getFullName()).eq(ModuleEntity::getCategory, category);
        if (!StringUtil.isEmpty(entity.getId())) {
            queryWrapper.lambda().ne(ModuleEntity::getId, entity.getId());
        }
        queryWrapper.lambda().eq(ModuleEntity::getParentId, entity.getParentId());
        // 通过系统id查询
        queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);

        List<ModuleEntity> entityList = this.selectList(queryWrapper);
        return !entityList.isEmpty();
    }

    default boolean isExistByEnCode(ModuleEntity entity, String category, String systemId) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getEnCode, entity.getEnCode()).eq(ModuleEntity::getCategory, category);
        if (!StringUtil.isEmpty(entity.getId())) {
            queryWrapper.lambda().ne(ModuleEntity::getId, entity.getId());
        }

        List<ModuleEntity> entityList = this.selectList(queryWrapper);
        return !entityList.isEmpty();
    }

    default boolean isExistByAddress(ModuleEntity entity, String category, String systemId) {
        if (JnpfConst.WEB.equals(entity.getCategory())) {
            //目录、大屏、外链(非_self) 不需要验证
            boolean isLinkAndSelf = Objects.equals(7, entity.getType()) && "_self".equals(entity.getLinkTarget());
            if (isLinkAndSelf || !Arrays.asList(1, 6, 7).contains(entity.getType())) {
                QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(ModuleEntity::getUrlAddress, entity.getUrlAddress()).eq(ModuleEntity::getCategory, category);
                if (!StringUtil.isEmpty(entity.getId())) {
                    queryWrapper.lambda().ne(ModuleEntity::getId, entity.getId());
                }
                // 通过系统id查询
                queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);
                List<ModuleEntity> entityList = this.selectList(queryWrapper);
                return !entityList.isEmpty();
            }
        }
        return false;
    }

    default void deleteBySystemId(String systemId) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);
        this.deleteByIds(this.selectList(queryWrapper));
    }

    default List<ModuleEntity> getModuleList(String visualId) {
        QueryWrapper<ModuleEntity> moduleWrapper = new QueryWrapper<>();
        moduleWrapper.lambda().eq(ModuleEntity::getModuleId, visualId).or().like(ModuleEntity::getPropertyJson, visualId);
        return this.selectList(moduleWrapper);
    }

    default List<ModuleEntity> getModuleBySystemIds(List<String> ids, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize, Integer type) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        if (moduleAuthorize != null && !moduleAuthorize.isEmpty()) {
            queryWrapper.lambda().notIn(ModuleEntity::getId, moduleAuthorize);
            queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleAuthorize);
        }
        if (moduleUrlAddressAuthorize != null && !moduleUrlAddressAuthorize.isEmpty()) {
            queryWrapper.lambda().and(t -> t.notIn(ModuleEntity::getUrlAddress, moduleUrlAddressAuthorize).or().isNull(ModuleEntity::getUrlAddress));
        }
        queryWrapper.lambda().in(ModuleEntity::getSystemId, ids);
        if (type == 1) {
            queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode).orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> getModuleByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> list : lists) {
                t.in(ModuleEntity::getId, list).or();
            }
        });
        queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode).orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> getListByEnCode(List<String> enCodeList) {
        if (enCodeList.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ModuleEntity::getEnCode, enCodeList);
        queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

    default List<ModuleEntity> findModuleAdmin(int mark, String id, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize) {
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(ModuleEntity::getId, id);
        }
        if (mark == 1) {
            queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, mark);
        }
        if (moduleAuthorize != null && !moduleAuthorize.isEmpty()) {
            queryWrapper.lambda().notIn(ModuleEntity::getId, moduleAuthorize);
            queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleAuthorize);
        }
        if (moduleUrlAddressAuthorize != null && !moduleUrlAddressAuthorize.isEmpty()) {
            queryWrapper.lambda().and(t -> t.notIn(ModuleEntity::getUrlAddress, moduleUrlAddressAuthorize).or().isNull(ModuleEntity::getUrlAddress));
        }
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode).orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default void getParentModule(List<ModuleEntity> data, Map<String, ModuleEntity> moduleEntityMap) {
        data.forEach(t -> {
            ModuleEntity moduleEntity = t;
            while (moduleEntity != null) {
                if (!moduleEntityMap.containsKey(moduleEntity.getId())) {
                    moduleEntityMap.put(moduleEntity.getId(), moduleEntity);
                }
                moduleEntity = this.getInfo(moduleEntity.getParentId());
            }
        });
    }

    default List<ModuleEntity> getListByUrlAddress(List<String> ids, List<String> urlAddressList) {
        urlAddressList = urlAddressList.stream().filter(StringUtil::isNotEmpty).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ModuleEntity::getId, ids);
        if (!urlAddressList.isEmpty()) {
            queryWrapper.lambda().or().in(ModuleEntity::getUrlAddress, urlAddressList);
        }
        List<String> moduleCode = JnpfConst.MODULE_CODE;
        queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleCode);
        return this.selectList(queryWrapper);
    }

    default List<ModuleSelectorVo> getFormMenuList(ModulePagination pagination) {
        MPJLambdaWrapper<ModuleEntity> wrapper = JoinWrappers.lambda(ModuleEntity.class);
        wrapper.selectAs(ModuleEntity::getId, ModuleSelectorVo::getId);
        wrapper.selectAs(ModuleEntity::getPropertyJson, ModuleSelectorVo::getPropertyJson);
        wrapper.selectAs(ModuleEntity::getFullName, ModuleSelectorVo::getFullName);
        wrapper.selectAs(ModuleEntity::getEnCode, ModuleSelectorVo::getEnCode);
        wrapper.selectAs(ModuleEntity::getType, ModuleSelectorVo::getType);
        wrapper.selectAs(SystemEntity::getFullName, ModuleSelectorVo::getSystemName);
        wrapper.leftJoin(SystemEntity.class, SystemEntity::getId, ModuleEntity::getSystemId);
        wrapper.eq(ModuleEntity::getEnabledMark, 1);
        List<Integer> typeList = ImmutableList.of(3, 9);
        wrapper.in(ModuleEntity::getType, typeList);
        wrapper.eq(ModuleEntity::getCategory, JnpfConst.WEB);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            wrapper.and(t -> {
                t.like(SystemEntity::getFullName, pagination.getKeyword()).or();
                t.like(ModuleEntity::getFullName, pagination.getKeyword()).or();
                t.like(ModuleEntity::getEnCode, pagination.getKeyword()).or();
            });
        }
        if (ObjectUtil.isNotEmpty(pagination.getSystemId())) {
            wrapper.eq(SystemEntity::getId, pagination.getSystemId());
        }
        // 过滤掉开发平台
        wrapper.ne(SystemEntity::getEnCode, JnpfConst.MAIN_SYSTEM_CODE);
        wrapper.orderByDesc(ModuleEntity::getCreatorTime);
        Page<ModuleSelectorVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<ModuleSelectorVo> data = this.selectJoinPage(page, ModuleSelectorVo.class, wrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    default List<ModuleSelectorVo> getPageList(ModulePagination pagination) {
        MPJLambdaWrapper<ModuleEntity> wrapper = JoinWrappers.lambda(ModuleEntity.class);
        wrapper.selectAs(ModuleEntity::getId, ModuleSelectorVo::getId);
        wrapper.selectAs(ModuleEntity::getPropertyJson, ModuleSelectorVo::getPropertyJson);
        wrapper.selectAs(ModuleEntity::getFullName, ModuleSelectorVo::getFullName);
        wrapper.selectAs(ModuleEntity::getEnCode, ModuleSelectorVo::getEnCode);
        wrapper.selectAs(ModuleEntity::getType, ModuleSelectorVo::getType);
        wrapper.eq(ModuleEntity::getEnabledMark, 1);
        if (pagination.getType() != null) {
            wrapper.eq(ModuleEntity::getType, pagination.getType());
        }

        if (Objects.equals(pagination.getType(), 3)) {
            wrapper.notLike(ModuleEntity::getPropertyJson, "\"webType\":4");
        }
        if (StringUtil.isNotEmpty(pagination.getCategory())) {
            wrapper.eq(ModuleEntity::getCategory, pagination.getCategory());
        }
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            wrapper.and(t -> {
                t.like(SystemEntity::getFullName, pagination.getKeyword()).or();
                t.like(ModuleEntity::getFullName, pagination.getKeyword()).or();
                t.like(ModuleEntity::getEnCode, pagination.getKeyword()).or();
            });
        }

        if (ObjectUtil.isNotEmpty(pagination.getSystemId())) {
            wrapper.eq(ModuleEntity::getSystemId, pagination.getSystemId());
        }
        wrapper.orderByAsc(ModuleEntity::getSortCode).orderByDesc(ModuleEntity::getCreatorTime);
        return this.selectJoinList(ModuleSelectorVo.class, wrapper);
    }

}
