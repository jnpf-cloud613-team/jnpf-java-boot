package jnpf.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.PaginationTime;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.EmailSendEntity;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 邮件发送
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface EmailSendMapper extends SuperMapper<EmailSendEntity> {

    default List<EmailSendEntity> getDraftList(PaginationTime paginationTime) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailSendEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailSendEntity::getCreatorUserId, userId).eq(EmailSendEntity::getState, -1);
        //日期范围（近7天、近1月、近3月、自定义）
        if (!ObjectUtil.isEmpty(paginationTime.getStartTime()) && !ObjectUtil.isEmpty(paginationTime.getEndTime())) {
            queryWrapper.lambda().between(EmailSendEntity::getCreatorTime, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        //关键字（用户、IP地址、功能名称）
        String keyWord = paginationTime.getKeyword() != null ? paginationTime.getKeyword() : null;
        //关键字（发件人、主题）
        if (!StringUtils.isEmpty(keyWord)) {
            String word = keyWord;
            queryWrapper.lambda().and(
                    t -> t.like(EmailSendEntity::getSender, word)
                            .or().like(EmailSendEntity::getSubject, word)
            );
        }
        //排序
        if (StringUtils.isEmpty(paginationTime.getSidx())) {
            queryWrapper.lambda().orderByDesc(EmailSendEntity::getCreatorTime);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(paginationTime.getSort()) ? queryWrapper.orderByAsc(paginationTime.getSidx()) : queryWrapper.orderByDesc(paginationTime.getSidx());
        }
        Page<EmailSendEntity> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<EmailSendEntity> userIPage = this.selectPage(page, queryWrapper);
        return paginationTime.setData(userIPage.getRecords(), page.getTotal());
    }


    default List<EmailSendEntity> getSentList(PaginationTime paginationTime) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailSendEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailSendEntity::getCreatorUserId, userId).ne(EmailSendEntity::getState, -1);
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(paginationTime.getStartTime()) && ObjectUtil.isNotEmpty(paginationTime.getEndTime())) {
            queryWrapper.lambda().between(EmailSendEntity::getCreatorTime, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        //关键字（用户、IP地址、功能名称）
        String keyWord = paginationTime.getKeyword() != null ? String.valueOf(paginationTime.getKeyword()) : null;
        //关键字（发件人、主题）
        if (!StringUtils.isEmpty(keyWord)) {
            queryWrapper.lambda().and(
                    t -> t.like(EmailSendEntity::getSender, keyWord)
                            .or().like(EmailSendEntity::getSubject, keyWord)
            );
        }
        //排序
        String sort = paginationTime.getSort() != null ? paginationTime.getSort() : null;
        if (!StringUtils.isEmpty(sort)) {
            queryWrapper.lambda().orderByDesc(EmailSendEntity::getCreatorTime);
        }
        Page<EmailSendEntity> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<EmailSendEntity> userIPage = this.selectPage(page, queryWrapper);
        return paginationTime.setData(userIPage.getRecords(), page.getTotal());
    }

}
