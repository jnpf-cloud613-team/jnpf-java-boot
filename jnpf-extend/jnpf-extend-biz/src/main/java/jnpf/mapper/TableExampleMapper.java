package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.TableExampleEntity;
import jnpf.model.tableexample.PaginationTableExample;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 表格示例数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface TableExampleMapper extends SuperMapper<TableExampleEntity> {

    default List<TableExampleEntity> getList() {
        QueryWrapper<TableExampleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(TableExampleEntity::getProjectType).orderByAsc(TableExampleEntity::getSortCode);
        return this.selectList(queryWrapper);
    }

    default List<TableExampleEntity> getList(String keyword) {
        QueryWrapper<TableExampleEntity> queryWrapper = new QueryWrapper<>();
        //关键字查询
        if (!StringUtils.isEmpty(keyword)) {
            queryWrapper.lambda().and(t -> t.like(TableExampleEntity::getCustomerName, keyword)
                    .or().like(TableExampleEntity::getProjectName, keyword));
        }
        queryWrapper.lambda().orderByAsc(TableExampleEntity::getProjectType).orderByAsc(TableExampleEntity::getSortCode);
        return this.selectList(queryWrapper);
    }

    default List<TableExampleEntity> getList(String typeId, PaginationTableExample paginationTableExample) {
        QueryWrapper<TableExampleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TableExampleEntity::getProjectType, typeId);
        //关键字（项目编码、项目名称、客户名称）
        String keyWord = paginationTableExample.getKeyword() != null ? paginationTableExample.getKeyword() : null;
        if (!StringUtils.isEmpty(keyWord)) {
            queryWrapper.lambda().and(
                    t -> t.like(TableExampleEntity::getProjectCode, keyWord)
                            .or().like(TableExampleEntity::getProjectName, keyWord)
                            .or().like(TableExampleEntity::getCustomerName, keyWord)
            );
        }
        //标签查询
        String sign = paginationTableExample.getFSign() != null ? paginationTableExample.getFSign() : null;
        if (!StringUtils.isEmpty(sign)) {
            String[] arraySign = sign.split(",");
            for (int i = 0; i < arraySign.length; i++) {
                String item = arraySign[i];
                if (i == 0) {
                    queryWrapper.lambda().like(TableExampleEntity::getProjectCode, item);
                } else {
                    queryWrapper.lambda().or(t -> t.like(TableExampleEntity::getProjectCode, item));
                }
            }
        }
        //排序
        if (StringUtils.isEmpty(paginationTableExample.getSidx())) {
            queryWrapper.lambda().orderByDesc(TableExampleEntity::getRegisterDate);
        } else {
            queryWrapper = "asc".equalsIgnoreCase(paginationTableExample.getSort()) ? queryWrapper.orderByAsc(paginationTableExample.getSidx()) : queryWrapper.orderByDesc(paginationTableExample.getSidx());
        }
        return this.selectList(queryWrapper);
    }

    default List<TableExampleEntity> getList(PaginationTableExample paginationTableExample) {
        QueryWrapper<TableExampleEntity> queryWrapper = new QueryWrapper<>();
        //关键字（项目编码、项目名称、客户名称）
        String keyWord = paginationTableExample.getKeyword() != null ? paginationTableExample.getKeyword() : null;
        if (!StringUtils.isEmpty(keyWord)) {
            queryWrapper.lambda().and(
                    t -> t.like(TableExampleEntity::getProjectCode, keyWord)
                            .or().like(TableExampleEntity::getProjectName, keyWord)
                            .or().like(TableExampleEntity::getCustomerName, keyWord)
            );
        }
        //标签查询
        String sign = paginationTableExample.getFSign() != null ? paginationTableExample.getFSign() : null;
        if (!StringUtils.isEmpty(sign)) {
            String[] arraySign = sign.split(",");
            for (int i = 0; i < arraySign.length; i++) {
                String item = arraySign[i];
                if (i == 0) {
                    queryWrapper.lambda().like(TableExampleEntity::getProjectCode, item);
                } else {
                    queryWrapper.lambda().or(t -> t.like(TableExampleEntity::getProjectCode, item));
                }
            }
        }
        //排序
        if (StringUtils.isEmpty(paginationTableExample.getSidx())) {
            queryWrapper.lambda().orderByDesc(TableExampleEntity::getRegisterDate);
        } else {
            queryWrapper = "asc".equalsIgnoreCase(paginationTableExample.getSort()) ? queryWrapper.orderByAsc(paginationTableExample.getSidx()) : queryWrapper.orderByDesc(paginationTableExample.getSidx());
        }
        Page<TableExampleEntity> page = new Page<>(paginationTableExample.getCurrentPage(), paginationTableExample.getPageSize());
        IPage<TableExampleEntity> exampleEntityIPage = this.selectPage(page, queryWrapper);
        return paginationTableExample.setData(exampleEntityIPage.getRecords(), page.getTotal());
    }

    default TableExampleEntity getInfo(String id) {
        QueryWrapper<TableExampleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TableExampleEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void delete(TableExampleEntity entity) {
        this.deleteById(entity.getId());
    }

    default void create(TableExampleEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setSortCode(RandomUtil.parses());
        entity.setRegisterDate(new Date());
        entity.setRegistrant(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default boolean update(String id, TableExampleEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return this.updateById(entity) > 0;
    }

    default boolean rowEditing(TableExampleEntity entity) {
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return this.updateById(entity) > 0;
    }

}
