package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.model.dblink.PaginationDbLink;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.List;

/**
 * 数据连接
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface DbLinkMapper extends SuperMapper<DbLinkEntity> {

    default List<DbLinkEntity> getList() {
        QueryWrapper<DbLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(DbLinkEntity::getSortCode)
                .orderByDesc(DbLinkEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<DbLinkEntity> getList(PaginationDbLink pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<DbLinkEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(DbLinkEntity::getFullName, pagination.getKeyword())
                            .or().like(DbLinkEntity::getHost, pagination.getKeyword())
            );
        }
        if (StringUtil.isNotEmpty(pagination.getDbType())) {
            flag = true;
            queryWrapper.lambda().eq(DbLinkEntity::getDbType, pagination.getDbType());
        }
        queryWrapper.lambda().orderByAsc(DbLinkEntity::getSortCode)
                .orderByDesc(DbLinkEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(DbLinkEntity::getLastModifyTime);
        }
        Page<DbLinkEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DbLinkEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default DbLinkEntity getInfo(String id) {
        QueryWrapper<DbLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DbLinkEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<DbLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DbLinkEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DbLinkEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(DbLinkEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default boolean update(String id, DbLinkEntity entity) {
        entity.setId(id);
        return SqlHelper.retBool(this.updateById(entity));
    }

}
