package jnpf.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.PaginationTime;
import jnpf.base.mapper.SuperMapper;
import jnpf.base.entity.EmailReceiveEntity;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 邮件接收
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface EmailReceiveMapper extends SuperMapper<EmailReceiveEntity> {

    default List<EmailReceiveEntity> getReceiveList(PaginationTime paginationTime) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailReceiveEntity::getCreatorUserId, userId);
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(paginationTime.getStartTime()) && ObjectUtil.isNotEmpty(paginationTime.getEndTime())) {
            queryWrapper.lambda().between(EmailReceiveEntity::getFdate, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        //关键字（用户、IP地址、功能名称）
        String keyWord = paginationTime.getKeyword() != null ? paginationTime.getKeyword() : null;
        //关键字（发件人、主题）
        if (!StringUtils.isEmpty(keyWord)) {
            String word = keyWord;
            queryWrapper.lambda().and(
                    t -> t.like(EmailReceiveEntity::getSender, word)
                            .or().like(EmailReceiveEntity::getSubject, word)
            );
        }
        //排序
        if (StringUtils.isEmpty(paginationTime.getSidx())) {
            queryWrapper.lambda().orderByDesc(EmailReceiveEntity::getFdate);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(paginationTime.getSort()) ? queryWrapper.orderByAsc(paginationTime.getSidx()) : queryWrapper.orderByDesc(paginationTime.getSidx());
        }
        Page<EmailReceiveEntity> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<EmailReceiveEntity> userIPage = this.selectPage(page, queryWrapper);
        return paginationTime.setData(userIPage.getRecords(), page.getTotal());
    }

    default List<EmailReceiveEntity> getDashboardReceiveList() {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailReceiveEntity::getCreatorUserId, userId).eq(EmailReceiveEntity::getIsRead, 0).orderByDesc(EmailReceiveEntity::getCreatorTime);
        Page<EmailReceiveEntity> page = new Page<>(1, 20);
        IPage<EmailReceiveEntity> iPage = this.selectPage(page, queryWrapper);
        return iPage.getRecords();
    }

    default List<EmailReceiveEntity> getStarredList(PaginationTime paginationTime) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailReceiveEntity::getCreatorUserId, userId).eq(EmailReceiveEntity::getStarred, 1);
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(paginationTime.getStartTime()) && ObjectUtil.isNotEmpty(paginationTime.getEndTime())) {
            queryWrapper.lambda().between(EmailReceiveEntity::getCreatorTime, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        //关键字（用户、IP地址、功能名称）
        String keyWord = paginationTime.getKeyword() != null ? paginationTime.getKeyword() : null;
        //关键字（发件人、主题）
        if (!StringUtils.isEmpty(keyWord)) {
            String word = keyWord;
            queryWrapper.lambda().and(
                    t -> t.like(EmailReceiveEntity::getSender, word)
                            .or().like(EmailReceiveEntity::getSubject, word)
            );
        }
        //排序
        if (StringUtils.isEmpty(paginationTime.getSidx())) {
            queryWrapper.lambda().orderByDesc(EmailReceiveEntity::getCreatorTime);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(paginationTime.getSort()) ? queryWrapper.orderByAsc(paginationTime.getSidx()) : queryWrapper.orderByDesc(paginationTime.getSidx());
        }
        Page<EmailReceiveEntity> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<EmailReceiveEntity> userIPage = this.selectPage(page, queryWrapper);
        return paginationTime.setData(userIPage.getRecords(), page.getTotal());
    }


}
