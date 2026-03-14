package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.entity.ProvinceEntity;
import jnpf.base.model.province.PaginationProvince;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 行政区划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ProvinceMapper extends SuperMapper<ProvinceEntity> {

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ProvinceEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ProvinceEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<ProvinceEntity> getList(String parentId) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceEntity::getParentId, parentId);
        // 排序
        queryWrapper.lambda().orderByDesc(ProvinceEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }


    default List<ProvinceEntity> getList(String parentId, PaginationProvince page) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        // 模糊查询
        if (Objects.nonNull(page) && StringUtil.isNotEmpty(page.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(ProvinceEntity::getFullName, page.getKeyword()).or()
                            .like(ProvinceEntity::getEnCode, page.getKeyword())
            );
        }
        if (null!=page&&page.getEnabledMark() != null) {
            queryWrapper.lambda().eq(ProvinceEntity::getEnabledMark, page.getEnabledMark());
        }
        queryWrapper.lambda().eq(ProvinceEntity::getParentId, parentId);
        // 排序
        queryWrapper.lambda().orderByAsc(ProvinceEntity::getSortCode).orderByDesc(ProvinceEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(ProvinceEntity::getLastModifyTime);
        }
        return this.selectList(queryWrapper);
    }

    default List<ProvinceEntity> getAllList() {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByDesc(ProvinceEntity::getSortCode).orderByAsc(ProvinceEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ProvinceEntity> getProListBytype(String type) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(ProvinceEntity::getId, ProvinceEntity::getFullName).eq(ProvinceEntity::getType, type);
        return this.selectList(queryWrapper);
    }

    default List<ProvinceEntity> getProList(List<String> proIdList) {
        if (!proIdList.isEmpty()) {
            QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(ProvinceEntity::getId, ProvinceEntity::getFullName).in(ProvinceEntity::getId, proIdList);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }


    default ProvinceEntity getInfo(String id) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default ProvinceEntity getInfo(String fullName, List<String> parentId) {
        QueryWrapper<ProvinceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceEntity::getFullName, fullName);
        if (!parentId.isEmpty()) {
            queryWrapper.lambda().in(ProvinceEntity::getParentId, parentId);
        }
        return this.selectOne(queryWrapper);
    }

    default void create(ProvinceEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default boolean update(String id, ProvinceEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return SqlHelper.retBool(this.updateById(entity));
    }
}
