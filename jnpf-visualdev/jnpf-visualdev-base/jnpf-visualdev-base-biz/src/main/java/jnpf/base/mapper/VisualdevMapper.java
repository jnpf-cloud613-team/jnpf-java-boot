package jnpf.base.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.PaginationVisualdev;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
public interface VisualdevMapper extends SuperMapper<VisualdevEntity> {

    default List<VisualdevEntity> getList(PaginationVisualdev paginationVisualdev) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<VisualdevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(VisualdevEntity::getId, VisualdevEntity::getCategory, VisualdevEntity::getEnCode, VisualdevEntity::getFullName,
                VisualdevEntity::getCreatorTime, VisualdevEntity::getCreatorUserId, VisualdevEntity::getLastModifyTime, VisualdevEntity::getLastModifyUserId,
                VisualdevEntity::getEnabledMark, VisualdevEntity::getSortCode, VisualdevEntity::getState, VisualdevEntity::getType, VisualdevEntity::getEnableFlow,
                VisualdevEntity::getWebType, VisualdevEntity::getVisualTables, VisualdevEntity::getPlatformRelease);

        if (!StringUtil.isEmpty(paginationVisualdev.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(t -> t.like(VisualdevEntity::getFullName, paginationVisualdev.getKeyword())
                    .or().like(VisualdevEntity::getEnCode, paginationVisualdev.getKeyword()));
        }

        if (ObjectUtil.isNotEmpty(paginationVisualdev.getType())) {
            queryWrapper.lambda().eq(VisualdevEntity::getType, paginationVisualdev.getType());
        }

        if (StringUtil.isNotEmpty(paginationVisualdev.getCategory())) {
            flag = true;
            queryWrapper.lambda().eq(VisualdevEntity::getCategory, paginationVisualdev.getCategory());
        }

        //---功能类型查询
        if (paginationVisualdev.getWebType() != null) {//普通表单
            flag = true;
            //2-表单：改成查询纯表单和列表
            if (Objects.equals(paginationVisualdev.getWebType(), 2)) {
                queryWrapper.lambda().in(VisualdevEntity::getWebType, Arrays.asList(1, 2));
            } else {
                queryWrapper.lambda().eq(VisualdevEntity::getWebType, paginationVisualdev.getWebType());
            }
        }
        if (StringUtil.isNotEmpty(paginationVisualdev.getSystemId())) {//普通表单
            queryWrapper.lambda().eq(VisualdevEntity::getSystemId, paginationVisualdev.getSystemId());
        }

        //状态
        if (StringUtil.isNotEmpty(paginationVisualdev.getIsRelease())) {
            flag = true;
            List<String> releaseList = Arrays.asList(paginationVisualdev.getIsRelease().split(","));
            if (releaseList.size() > 1) {
                List<Integer> jsonToList = JsonUtil.getJsonToList(releaseList, Integer.class);
                queryWrapper.lambda().in(VisualdevEntity::getState, jsonToList);
            } else if (releaseList.size() == 1) {
                queryWrapper.lambda().eq(VisualdevEntity::getState, paginationVisualdev.getIsRelease());
            }

        }

        // 排序
        queryWrapper.lambda().orderByAsc(VisualdevEntity::getSortCode).orderByDesc(VisualdevEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(VisualdevEntity::getLastModifyTime);
        }
        Page<VisualdevEntity> page = new Page<>(paginationVisualdev.getCurrentPage(), paginationVisualdev.getPageSize());
        IPage<VisualdevEntity> userPage = this.selectPage(page, queryWrapper);
        return paginationVisualdev.setData(userPage.getRecords(), page.getTotal());
    }

    default List<VisualdevEntity> getList() {
        QueryWrapper<VisualdevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(VisualdevEntity::getSortCode).orderByDesc(VisualdevEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<VisualdevEntity> getListBySystemId(String systemId) {
        QueryWrapper<VisualdevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevEntity::getSystemId, systemId);
        return this.selectList(queryWrapper);
    }

    default VisualdevEntity getInfo(String id) {
        if (StringUtils.isBlank(id)) return null;
        QueryWrapper<VisualdevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default Boolean getObjByEncode(String encode, Integer type) {
        if (StringUtil.isEmpty(encode)) return false;
        QueryWrapper<VisualdevEntity> visualWrapper = new QueryWrapper<>();
        visualWrapper.lambda().eq(VisualdevEntity::getEnCode, encode).eq(VisualdevEntity::getType, type);
        return this.selectCount(visualWrapper) > 0;
    }


    default Boolean getCountByName(String name, Integer type, String systemId) {
        QueryWrapper<VisualdevEntity> visualWrapper = new QueryWrapper<>();
        visualWrapper.lambda().eq(VisualdevEntity::getFullName, name).eq(VisualdevEntity::getType, type);
        if (StringUtil.isNotEmpty(systemId)) {
            visualWrapper.lambda().eq(VisualdevEntity::getSystemId, systemId);
        }
        return this.selectCount(visualWrapper) > 0;
    }


    default List<VisualdevEntity> selectorList(String systemId) {
        QueryWrapper<VisualdevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(
                VisualdevEntity::getId,
                VisualdevEntity::getFullName,
                VisualdevEntity::getWebType,
                VisualdevEntity::getType,
                VisualdevEntity::getSystemId,
                VisualdevEntity::getCategory);
        if (StringUtil.isNotEmpty(systemId)) {
            queryWrapper.lambda().eq(VisualdevEntity::getSystemId, systemId);
        }
        return this.selectList(queryWrapper);
    }


}
