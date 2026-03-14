package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.CustomerEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * 客户信息
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 14:09:05
 */
public interface CustomerMapper extends SuperMapper<CustomerEntity> {

    default List<CustomerEntity> getList(Pagination pagination) {
        QueryWrapper<CustomerEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(CustomerEntity::getAddress, pagination.getKeyword())
                            .or().like(CustomerEntity::getName, pagination.getKeyword())
                            .or().like(CustomerEntity::getCode, pagination.getKeyword())
            );
        }
        Page<CustomerEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<CustomerEntity> userIPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userIPage.getRecords(), userIPage.getTotal());
    }

    default CustomerEntity getInfo(String id) {
        QueryWrapper<CustomerEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CustomerEntity::getId, id);
        return this.selectOne(queryWrapper);

    }

    default void create(CustomerEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        this.insert(entity);
    }

    default boolean update(String id, CustomerEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        return this.updateById(entity) > 0;
    }

    default void delete(CustomerEntity entity) {
        if (entity != null) {
            this.deleteById(entity.getId());
        }
    }

}
