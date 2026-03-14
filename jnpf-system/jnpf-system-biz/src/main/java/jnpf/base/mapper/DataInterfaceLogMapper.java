package jnpf.base.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.Pagination;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.DataInterfaceLogEntity;
import jnpf.base.model.interfaceoauth.PaginationIntrfaceLog;
import jnpf.util.*;

import java.util.Date;
import java.util.List;

/**
 * 数据接口调用日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-06-03
 */
public interface DataInterfaceLogMapper extends SuperMapper<DataInterfaceLogEntity> {

    default void create(String dateInterfaceId, Integer invokWasteTime) {
        DataInterfaceLogEntity entity = new DataInterfaceLogEntity();
        entity.setId(RandomUtil.uuId());
        entity.setInvokTime(DateUtil.getNowDate());
        entity.setUserId(UserProvider.getUser().getUserId());
        entity.setInvokId(dateInterfaceId);
        entity.setInvokIp(IpUtil.getIpAddr());
        entity.setInvokType("GET");
        entity.setInvokDevice(ServletUtil.getUserAgent());
        entity.setInvokWasteTime(invokWasteTime);
        this.insert(entity);
    }

    default void create(String dateInterfaceId, Integer invokWasteTime, String appId, String invokType) {
        DataInterfaceLogEntity entity = new DataInterfaceLogEntity();
        entity.setId(RandomUtil.uuId());
        entity.setInvokTime(DateUtil.getNowDate());
        entity.setUserId(UserProvider.getUser().getUserId());
        entity.setInvokId(dateInterfaceId);
        entity.setInvokIp(IpUtil.getIpAddr());
        entity.setInvokType(invokType);
        entity.setInvokDevice(ServletUtil.getUserAgent());
        entity.setInvokWasteTime(invokWasteTime);
        entity.setOauthAppId(appId);
        this.insert(entity);
    }

    default List<DataInterfaceLogEntity> getList(String invokId, Pagination pagination) {
        QueryWrapper<DataInterfaceLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataInterfaceLogEntity::getInvokId, invokId).orderByDesc(DataInterfaceLogEntity::getInvokTime);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(DataInterfaceLogEntity::getUserId, pagination.getKeyword())
                            .or().like(DataInterfaceLogEntity::getInvokIp, pagination.getKeyword())
                            .or().like(DataInterfaceLogEntity::getInvokDevice, pagination.getKeyword())
                            .or().like(DataInterfaceLogEntity::getInvokType, pagination.getKeyword())
            );
        }
        // 排序
        queryWrapper.lambda().orderByDesc(DataInterfaceLogEntity::getInvokTime);
        Page<DataInterfaceLogEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DataInterfaceLogEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default List<DataInterfaceLogEntity> getListByIds(String appId, List<String> invokIds, PaginationIntrfaceLog pagination) {
        MPJLambdaWrapper<DataInterfaceLogEntity> queryWrapper = JoinWrappers
                .lambda(DataInterfaceLogEntity.class)
                .leftJoin(DataInterfaceEntity.class, DataInterfaceEntity::getId, DataInterfaceLogEntity::getInvokId);
        queryWrapper.eq(DataInterfaceLogEntity::getOauthAppId, appId);
        queryWrapper.in(DataInterfaceLogEntity::getInvokId, invokIds);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.and(
                    t -> t.like(DataInterfaceEntity::getEnCode, pagination.getKeyword())
                            .or().like(DataInterfaceEntity::getFullName, pagination.getKeyword())
            );
        }
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            queryWrapper.between(DataInterfaceLogEntity::getInvokTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        // 排序
        queryWrapper.orderByDesc(DataInterfaceLogEntity::getInvokTime);
        Page<DataInterfaceLogEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DataInterfaceLogEntity> iPage = this.selectJoinPage(page, DataInterfaceLogEntity.class, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }
}
