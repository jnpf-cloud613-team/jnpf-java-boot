package jnpf.onlinedev.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.onlinedev.entity.VisualLogEntity;
import jnpf.onlinedev.model.log.VisualLogPage;

import java.util.List;

/**
 * 数据日志mapper
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 18:22:40
 */
public interface VisualLogMapper extends SuperMapper<VisualLogEntity> {

    default List<VisualLogEntity> getList(VisualLogPage pagination) {
        QueryWrapper<VisualLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualLogEntity::getModelId, pagination.getModelId());
        queryWrapper.lambda().eq(VisualLogEntity::getDataId, pagination.getDataId());
        //排序
        queryWrapper.lambda().orderByDesc(VisualLogEntity::getCreatorTime);
        Page<VisualLogEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<VisualLogEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }
}
