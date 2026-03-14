package jnpf.portal.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.mapper.SuperMapper;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.model.PortalPagination;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.List;

/**
 * base_portal
 * 版本： V3.0.0
 * 版权： 引迈信息技术有限公司
 * 作者： 管理员/admin
 * 日期： 2020-10-21 14:23:30
 */
public interface PortalMapper extends SuperMapper<PortalEntity> {

    default List<PortalEntity> getList(PortalPagination portalPagination) {
        return getList(portalPagination, new QueryWrapper<>());
    }

    default List<PortalEntity> getList(PortalPagination portalPagination, QueryWrapper<PortalEntity> queryWrapper) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        // 模糊查询
        if (!StringUtil.isEmpty(portalPagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(q -> q.like(PortalEntity::getFullName, portalPagination.getKeyword()).or()
                    .like(PortalEntity::getEnCode, portalPagination.getKeyword()));
        }
        // 分类（数据字典）
        if (StringUtil.isNotEmpty(portalPagination.getCategory())) {
            flag = true;
            queryWrapper.lambda().eq(PortalEntity::getCategory, portalPagination.getCategory());
        }
        // 类型(0-页面设计,1-自定义路径)
        if (portalPagination.getType() != null) {
            flag = true;
            queryWrapper.lambda().eq(PortalEntity::getType, portalPagination.getType());
        }
        // 锁定
        if (portalPagination.getEnabledLock() != null) {
            flag = true;
            queryWrapper.lambda().eq(PortalEntity::getEnabledLock, portalPagination.getEnabledLock());
        }
        // 发布状态
        if (portalPagination.getIsRelease() != null) {
            flag = true;
            queryWrapper.lambda().eq(PortalEntity::getState, portalPagination.getIsRelease());
        }
        // 系统id
        if (StringUtil.isNotEmpty(portalPagination.getSystemId())) {
            queryWrapper.lambda().eq(PortalEntity::getSystemId, portalPagination.getSystemId());
        }
        // 排序
        queryWrapper.lambda().orderByAsc(PortalEntity::getSortCode).orderByDesc(PortalEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(PortalEntity::getLastModifyTime);
        }
        // 分页
        Page<PortalEntity> page = new Page<>(portalPagination.getCurrentPage(), portalPagination.getPageSize());
        IPage<PortalEntity> userPage = this.selectPage(page, queryWrapper);
        return portalPagination.setData(userPage.getRecords(), page.getTotal());
    }

    default List<PortalEntity> getListBySystemId(String systemId) {
        QueryWrapper<PortalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PortalEntity::getSystemId, systemId);
        return this.selectList(queryWrapper);
    }

    default PortalEntity getInfo(String id) {
        QueryWrapper<PortalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PortalEntity::getId, id);
        return this.selectOne(queryWrapper);
    }


    default boolean isExistByFullName(String fullName, String id, String systemId) {
        QueryWrapper<PortalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PortalEntity::getFullName, fullName);
        queryWrapper.lambda().eq(PortalEntity::getSystemId, systemId);
        return isExistCommon(queryWrapper, id);
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<PortalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PortalEntity::getEnCode, enCode);
        return isExistCommon(queryWrapper, id);
    }

    default boolean isExistCommon(QueryWrapper<PortalEntity> queryWrapper, String id) {
        if (!StringUtil.isEmpty(id)) queryWrapper.lambda().ne(PortalEntity::getId, id);
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(PortalEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setState(0);
        entity.setEnabledMark(0);
        this.insert(entity);
    }

    default boolean update(String id, PortalEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return SqlHelper.retBool(this.updateById(entity));
    }
}
