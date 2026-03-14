package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.BigDataEntity;
import jnpf.util.StringUtil;

import java.util.List;

/**
 * 大数据测试
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface BigDataMapper extends SuperMapper<BigDataEntity> {

    Integer maxCode();

    default List<BigDataEntity> getList(Pagination pagination) {
        QueryWrapper<BigDataEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(BigDataEntity::getFullName, pagination.getKeyword())
                            .or().like(BigDataEntity::getEnCode, pagination.getKeyword())
            );
        }
        //排序
        queryWrapper.lambda().orderByDesc(BigDataEntity::getCreatorTime, BigDataEntity::getId);
        Page<BigDataEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<BigDataEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

}
