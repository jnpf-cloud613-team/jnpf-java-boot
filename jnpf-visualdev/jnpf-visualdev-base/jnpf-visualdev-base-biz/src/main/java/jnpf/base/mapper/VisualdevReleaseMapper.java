package jnpf.base.mapper;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.model.PaginationVisualdev;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;

import java.util.List;
import java.util.Objects;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
public interface VisualdevReleaseMapper extends SuperMapper<VisualdevReleaseEntity> {

    default List<VisualdevEntity> getPageList(PaginationVisualdev paginationVisualdev) {
        QueryWrapper<VisualdevReleaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(
                VisualdevReleaseEntity::getId,
                VisualdevReleaseEntity::getFullName,
                VisualdevReleaseEntity::getType,
                VisualdevReleaseEntity::getEnableFlow,
                VisualdevReleaseEntity::getEnCode);
        //1-集成助手，查询列表， 2-流程表单查询，纯表单和列表
        if (paginationVisualdev.getWebType() != null) {
            List<Integer> webType = ImmutableList.of(1, 2, 4);
            if (Objects.equals(paginationVisualdev.getWebType(), 1)) {
                webType = ImmutableList.of(2);
            } else if (Objects.equals(paginationVisualdev.getWebType(), 2)) {
                webType = ImmutableList.of(1, 2);
            }
            queryWrapper.lambda().in(VisualdevReleaseEntity::getWebType, webType);
        }
        if (!StringUtil.isEmpty(paginationVisualdev.getKeyword())) {
            queryWrapper.lambda().like(VisualdevReleaseEntity::getFullName, paginationVisualdev.getKeyword());
        }
        if (ObjectUtil.isNotEmpty(paginationVisualdev.getType())) {
            queryWrapper.lambda().eq(VisualdevReleaseEntity::getType, paginationVisualdev.getType());
        }
        if (StringUtil.isNotEmpty(paginationVisualdev.getCategory())) {
            queryWrapper.lambda().eq(VisualdevReleaseEntity::getCategory, paginationVisualdev.getCategory());
        }
        if (StringUtil.isNotEmpty(paginationVisualdev.getSystemId())) {
            queryWrapper.lambda().eq(VisualdevReleaseEntity::getSystemId, paginationVisualdev.getSystemId());
        }
        if (Objects.equals(paginationVisualdev.getEnableFlow(), 1)) {
            queryWrapper.lambda().and(
                    t -> t.isNull(VisualdevReleaseEntity::getEnableFlow).or()
                            .eq(VisualdevReleaseEntity::getEnableFlow, 1)
            );
        }
        // 排序
        queryWrapper.lambda().orderByAsc(VisualdevReleaseEntity::getSortCode).orderByDesc(VisualdevReleaseEntity::getCreatorTime);
        Page<VisualdevReleaseEntity> page = new Page<>(paginationVisualdev.getCurrentPage(), paginationVisualdev.getPageSize());
        IPage<VisualdevReleaseEntity> userPage = this.selectPage(page, queryWrapper);
        List<VisualdevEntity> list = JsonUtil.getJsonToList(userPage.getRecords(), VisualdevEntity.class);
        return paginationVisualdev.setData(list, page.getTotal());
    }

    default long beenReleased(String id) {
        QueryWrapper<VisualdevReleaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevReleaseEntity::getId, id);
        return this.selectCount(queryWrapper);
    }

    default List<VisualdevReleaseEntity> selectorList(String systemId) {
        QueryWrapper<VisualdevReleaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(
                VisualdevReleaseEntity::getId,
                VisualdevReleaseEntity::getFullName,
                VisualdevReleaseEntity::getWebType,
                VisualdevReleaseEntity::getType,
                VisualdevReleaseEntity::getWebAddress,
                VisualdevReleaseEntity::getAppAddress,
                VisualdevReleaseEntity::getSystemId,
                VisualdevReleaseEntity::getCategory);
        if (StringUtil.isNotEmpty(systemId)) {
            queryWrapper.lambda().eq(VisualdevReleaseEntity::getSystemId, systemId);
        }
        return this.selectList(queryWrapper);
    }

    default List<VisualdevReleaseEntity> selectByIds(List<String> ids, SFunction<VisualdevReleaseEntity, ?>... columns) {
        QueryWrapper<VisualdevReleaseEntity> queryWrapper = new QueryWrapper<>();
        if (columns != null) {
            queryWrapper.lambda().select(columns);
        } else {
            queryWrapper.lambda().select(
                    VisualdevReleaseEntity::getId,
                    VisualdevReleaseEntity::getFullName,
                    VisualdevReleaseEntity::getWebType,
                    VisualdevReleaseEntity::getType,
                    VisualdevReleaseEntity::getCategory);
        }
        if (CollUtil.isNotEmpty(ids)) {
            queryWrapper.lambda().in(VisualdevReleaseEntity::getId, ids);
        }
        return this.selectList(queryWrapper);
    }

    default List<VisualdevEntity> getListBySystem(PaginationVisualdev paginationVisualdev) {
        QueryWrapper<VisualdevReleaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(
                VisualdevReleaseEntity::getId,
                VisualdevReleaseEntity::getFullName,
                VisualdevReleaseEntity::getEnCode);
        queryWrapper.lambda().eq(VisualdevReleaseEntity::getType, 1);
        queryWrapper.lambda().eq(VisualdevReleaseEntity::getWebType, 2);
        if (!StringUtil.isEmpty(paginationVisualdev.getKeyword())) {
            queryWrapper.lambda().like(VisualdevReleaseEntity::getFullName, paginationVisualdev.getKeyword());
        }
        if (StringUtil.isNotEmpty(paginationVisualdev.getSystemId())) {
            queryWrapper.lambda().eq(VisualdevReleaseEntity::getSystemId, paginationVisualdev.getSystemId());
        }
        // 排序
        queryWrapper.lambda().orderByAsc(VisualdevReleaseEntity::getSortCode).orderByDesc(VisualdevReleaseEntity::getCreatorTime);
        Page<VisualdevReleaseEntity> page = new Page<>(paginationVisualdev.getCurrentPage(), paginationVisualdev.getPageSize());
        IPage<VisualdevReleaseEntity> userPage = this.selectPage(page, queryWrapper);
        List<VisualdevEntity> list = JsonUtil.getJsonToList(userPage.getRecords(), VisualdevEntity.class);
        return paginationVisualdev.setData(list, page.getTotal());
    }
}
