package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.ContractEntity;
import jnpf.util.StringUtil;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 9:47
 */
public interface ContractMapper extends SuperMapper<ContractEntity> {

    default List<ContractEntity> getlist(Pagination pagination) {
        //通过UserProvider获取用户信息
        QueryWrapper<ContractEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ContractEntity::getContractName, pagination.getKeyword())
                            .or().like(ContractEntity::getMytelePhone, pagination.getKeyword())
            );
        }
        //排序
        if (!StringUtil.isEmpty(pagination.getSidx())) {
            queryWrapper = "ASC".equals(pagination.getSort()) ? queryWrapper.orderByAsc(pagination.getSidx()) : queryWrapper.orderByDesc(pagination.getSidx());
        }
        Page<ContractEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ContractEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

}
