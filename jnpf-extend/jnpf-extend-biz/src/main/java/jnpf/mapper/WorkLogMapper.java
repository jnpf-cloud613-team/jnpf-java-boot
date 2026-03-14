package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.WorkLogEntity;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 工作日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface WorkLogMapper extends SuperMapper<WorkLogEntity> {

    default List<WorkLogEntity> getSendList(Pagination pageModel) {
        QueryWrapper<WorkLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(WorkLogEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        //app搜索
        if (StringUtil.isNotEmpty(pageModel.getKeyword())) {
            queryWrapper.lambda().like(WorkLogEntity::getTitle, pageModel.getKeyword());
        }
        //排序
        if (StringUtils.isEmpty(pageModel.getSidx())) {
            queryWrapper.lambda().orderByDesc(WorkLogEntity::getCreatorTime);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(pageModel.getSort()) ? queryWrapper.orderByAsc(pageModel.getSidx()) : queryWrapper.orderByDesc(pageModel.getSidx());
        }
        Page<WorkLogEntity> page = new Page<>(pageModel.getCurrentPage(), pageModel.getPageSize());
        IPage<WorkLogEntity> iPage = this.selectPage(page, queryWrapper);
        return pageModel.setData(iPage.getRecords(), page.getTotal());
    }

    default List<WorkLogEntity> getReceiveList(Pagination pageModel) {
        QueryWrapper<WorkLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().like(WorkLogEntity::getToUserId, UserProvider.getUser().getUserId());
        //app搜索
        if (StringUtil.isNotEmpty(pageModel.getKeyword())) {
            queryWrapper.lambda().like(WorkLogEntity::getTitle, pageModel.getKeyword());
        }
        //排序
        if (StringUtils.isEmpty(pageModel.getSidx())) {
            queryWrapper.lambda().orderByDesc(WorkLogEntity::getCreatorTime);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(pageModel.getSort()) ? queryWrapper.orderByAsc(pageModel.getSidx()) : queryWrapper.orderByDesc(pageModel.getSidx());
        }
        Page<WorkLogEntity> page = new Page<>(pageModel.getCurrentPage(), pageModel.getPageSize());
        IPage<WorkLogEntity> iPage = this.selectPage(page, queryWrapper);
        return pageModel.setData(iPage.getRecords(), page.getTotal());
    }

    default WorkLogEntity getInfo(String id) {
        QueryWrapper<WorkLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(WorkLogEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

}
