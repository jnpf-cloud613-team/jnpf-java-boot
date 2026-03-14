package jnpf.base.mapper;


import cn.dev33.satoken.context.SaHolder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.model.datainterface.DataInterfaceActionModel;
import jnpf.base.model.datainterface.PaginationDataInterface;
import jnpf.base.model.datainterface.PaginationDataInterfaceSelector;
import jnpf.base.util.interfaceutil.InterfaceUtil;
import jnpf.exception.DataException;
import jnpf.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:30
 */
@Mapper
public interface DataInterfaceMapper extends SuperMapper<DataInterfaceEntity> {

    default List<DataInterfaceEntity> getList(PaginationDataInterface pagination, Integer isSelector) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(pagination.getEnabledMark())) {
            queryWrapper.lambda().eq(DataInterfaceEntity::getEnabledMark, pagination.getEnabledMark());
        }
        //关键字
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(DataInterfaceEntity::getFullName, pagination.getKeyword())
                            .or().like(DataInterfaceEntity::getEnCode, pagination.getKeyword())
            );
        }
        // 是否分页
        if (pagination.getHasPage() != null && pagination.getHasPage() == 0) {
            queryWrapper.lambda().eq(DataInterfaceEntity::getHasPage, pagination.getHasPage());
        }
        if (isSelector == 1) {
            queryWrapper.lambda().eq(DataInterfaceEntity::getIsPostPosition, 0);
            if (ObjectUtil.isEmpty(pagination.getEnabledMark())) {
                queryWrapper.lambda().eq(DataInterfaceEntity::getEnabledMark, 1);
            }
        }
        //分类
        queryWrapper.lambda().eq(DataInterfaceEntity::getCategory, pagination.getCategory());
        // 类型
        String type = pagination.getType();
        if (StringUtil.isNotEmpty(type)) {
            if (type.contains(",")) {
                List<Integer> collect = Arrays.stream(type.split(",")).map(Integer::valueOf).collect(Collectors.toList());
                queryWrapper.lambda().in(DataInterfaceEntity::getType, collect);
            } else {
                queryWrapper.lambda().eq(DataInterfaceEntity::getType, Integer.valueOf(type));
            }
        }
        //排序
        queryWrapper.lambda().orderByAsc(DataInterfaceEntity::getSortCode)
                .orderByDesc(DataInterfaceEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(DataInterfaceEntity::getLastModifyTime);
        }
        Page<DataInterfaceEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DataInterfaceEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<DataInterfaceEntity> getList(PaginationDataInterfaceSelector pagination) {
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.equal(pagination.getSourceType(), 1)) {
            queryWrapper.lambda().and(t -> t.ne(DataInterfaceEntity::getType, 1).ne(DataInterfaceEntity::getHasPage, 1).ne(DataInterfaceEntity::getIsPostPosition, 1)
                    .or(tt -> tt.eq(DataInterfaceEntity::getType, 1).eq(DataInterfaceEntity::getAction, 3).ne(DataInterfaceEntity::getHasPage, 1).ne(DataInterfaceEntity::getIsPostPosition, 1))
            );
        } else if (ObjectUtil.equal(pagination.getSourceType(), 2)) {
            queryWrapper.lambda().and(t -> t.ne(DataInterfaceEntity::getType, 1).ne(DataInterfaceEntity::getIsPostPosition, 1)
                    .or(tt -> tt.eq(DataInterfaceEntity::getType, 1).eq(DataInterfaceEntity::getAction, 3).ne(DataInterfaceEntity::getIsPostPosition, 1))
            );
        } else if (ObjectUtil.equal(pagination.getSourceType(), 3)) {
            queryWrapper.lambda().and(t -> t.ne(DataInterfaceEntity::getType, 1).ne(DataInterfaceEntity::getHasPage, 1).ne(DataInterfaceEntity::getIsPostPosition, 1)
                    .or(tt -> tt.eq(DataInterfaceEntity::getType, 1).ne(DataInterfaceEntity::getAction, 3).ne(DataInterfaceEntity::getHasPage, 1).ne(DataInterfaceEntity::getIsPostPosition, 1))
            );
        }
        // 类型
        String type = pagination.getType();
        if (StringUtil.isNotEmpty(type)) {
            if (type.contains(",")) {
                List<Integer> collect = Arrays.stream(type.split(",")).map(Integer::valueOf).collect(Collectors.toList());
                queryWrapper.lambda().in(DataInterfaceEntity::getType, collect);
            } else {
                queryWrapper.lambda().eq(DataInterfaceEntity::getType, Integer.valueOf(type));
            }
        }
        //分类
        queryWrapper.lambda().eq(DataInterfaceEntity::getCategory, pagination.getCategory());
        queryWrapper.lambda().eq(DataInterfaceEntity::getEnabledMark, 1);
        //关键字查询
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(t -> {
                t.like(DataInterfaceEntity::getFullName, pagination.getKeyword()).or();
                t.like(DataInterfaceEntity::getEnCode, pagination.getKeyword());
            });
        }
        //排序
        queryWrapper.lambda().orderByAsc(DataInterfaceEntity::getSortCode)
                .orderByDesc(DataInterfaceEntity::getCreatorTime);
        queryWrapper.lambda().orderByDesc(DataInterfaceEntity::getLastModifyTime);
        Page<DataInterfaceEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DataInterfaceEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<DataInterfaceEntity> getList(boolean filterPage) {
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        if (filterPage) {
            queryWrapper.lambda().ne(DataInterfaceEntity::getHasPage, 1);
        }
        queryWrapper.lambda().eq(DataInterfaceEntity::getEnabledMark, 1)
                .orderByAsc(DataInterfaceEntity::getSortCode)
                .orderByDesc(DataInterfaceEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default DataInterfaceEntity getInfo(String id) {
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataInterfaceEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(DataInterfaceEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        this.setIgnoreLogicDelete().insertOrUpdate(entity);
        this.clearIgnoreLogicDelete();
    }

    default boolean update(DataInterfaceEntity entity, String id) throws DataException {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(DateUtil.getNowDate());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default boolean isExistByFullNameOrEnCode(String id, String fullName, String enCode) {
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(fullName)) {
            queryWrapper.lambda().eq(DataInterfaceEntity::getFullName, fullName);
        }
        if (StringUtil.isNotEmpty(enCode)) {
            queryWrapper.lambda().eq(DataInterfaceEntity::getEnCode, enCode);
        }
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(DataInterfaceEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<DataInterfaceEntity> getList(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        QueryWrapper<DataInterfaceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DataInterfaceEntity::getId, ids);
        return this.selectList(queryWrapper);
    }

    default DataInterfaceActionModel checkParams(Map<String, String> map) {
        String ymDate = ServletUtil.getRequest().getHeader(InterfaceUtil.YMDATE);
        String authorSignature = ServletUtil.getRequest().getHeader(Constants.AUTHORIZATION);
        if (StringUtils.isEmpty(ymDate)) {
            throw new IllegalArgumentException("header参数：YmDate未传值");
        }
        if (StringUtils.isEmpty(authorSignature)) {
            throw new IllegalArgumentException("header参数：" + Constants.AUTHORIZATION + "未传值");
        }
        DataInterfaceActionModel entity = new DataInterfaceActionModel();
        //判断是否多租户，取参数tenantId
        if (InterfaceUtil.checkParam(map, "tenantId")) {
            entity.setTenantId(map.get("tenantId"));
        }
        String tenantId = SaHolder.getRequest().getParam("tenantId");
        if (StringUtil.isNotEmpty(tenantId)) {
            entity.setTenantId(tenantId);
        }
        entity.setMap(map);
        return entity;
    }
}
