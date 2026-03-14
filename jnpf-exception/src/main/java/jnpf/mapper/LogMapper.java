package jnpf.mapper;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.LogEntity;
import jnpf.enums.LogSortEnum;
import jnpf.model.PaginationLogModel;
import jnpf.util.*;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 系统日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface LogMapper extends SuperMapper<LogEntity> {

    default List<LogEntity> getList(int category, PaginationLogModel paginationTime, Boolean filterUser) {
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(LogEntity::getId, LogEntity::getUserId, LogEntity::getUserName, LogEntity::getType, LogEntity::getLevels,
                LogEntity::getIpAddress, LogEntity::getIpAddressName, LogEntity::getRequestUrl, LogEntity::getRequestMethod, LogEntity::getRequestDuration,
                LogEntity::getPlatForm, LogEntity::getBrowser, LogEntity::getCreatorTime, LogEntity::getCreatorUserId, LogEntity::getDescription, LogEntity::getLoginMark,
                LogEntity::getLoginType, LogEntity::getModuleName, LogEntity::getModuleId, LogEntity::getObjectId);
        queryWrapper.lambda().eq(LogEntity::getType, category);
        if (Boolean.TRUE.equals(filterUser)) {
            //用户Id
            String userId = userInfo.getUserId();
            String userAccount = userInfo.getUserAccount();
            queryWrapper.lambda().and(
                    t -> t.eq(LogEntity::getUserId, userId)
                            .or().eq(LogEntity::getUserId, userAccount)
            );
        }
        //日期范围（近7天、近1月、近3月、自定义）
        if (!ObjectUtil.isEmpty(paginationTime.getStartTime()) && !ObjectUtil.isEmpty(paginationTime.getEndTime())) {
            queryWrapper.lambda().between(LogEntity::getCreatorTime, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        //关键字（用户、IP地址、功能名称）
        String keyWord = paginationTime.getKeyword();
        if (!StringUtil.isEmpty(keyWord)) {
            if (category == 1) {
                queryWrapper.lambda().and(
                        t -> t.like(LogEntity::getUserName, keyWord)
                                .or().like(LogEntity::getIpAddress, keyWord)
                );
            } else if (category == 5 || category == 4) {
                queryWrapper.lambda().and(
                        t -> t.like(LogEntity::getUserName, keyWord)
                                .or().like(LogEntity::getIpAddress, keyWord)
                                .or().like(LogEntity::getRequestUrl, keyWord)
                );
            } else if (category == 3) {
                queryWrapper.lambda().and(
                        t -> t.like(LogEntity::getUserName, keyWord)
                                .or().like(LogEntity::getIpAddress, keyWord)
                                .or().like(LogEntity::getRequestUrl, keyWord)
                                .or().like(LogEntity::getModuleName, keyWord)
                );
            }
        }
        // 请求方式
        if (StringUtil.isNotEmpty(paginationTime.getRequestMethod())) {
            queryWrapper.lambda().eq(LogEntity::getRequestMethod, paginationTime.getRequestMethod());
        }
        // 类型
        if (paginationTime.getLoginType() != null) {
            queryWrapper.lambda().eq(LogEntity::getLoginType, paginationTime.getLoginType());
        }
        // 状态
        if (paginationTime.getLoginMark() != null) {
            queryWrapper.lambda().eq(LogEntity::getLoginMark, paginationTime.getLoginMark());
        }
        if (StringUtil.isNotEmpty(paginationTime.getDataInterFaceId())) {
            String s =
                    paginationTime.getDataInterFaceId() +
                            "/Actions/Preview";
            String ss =
                    paginationTime.getDataInterFaceId() +
                            "/Actions/Response";
            queryWrapper.lambda().and(query ->
                    query.like(LogEntity::getRequestUrl, s).or().like(LogEntity::getRequestUrl, ss)
            );
        }
        //排序
        queryWrapper.lambda().orderByDesc(LogEntity::getCreatorTime);
        Page<LogEntity> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<LogEntity> userPage = this.selectPage(page, queryWrapper);
        return paginationTime.setData(userPage.getRecords(), page.getTotal());
    }

    default LogEntity getInfo(String id) {
        QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LogEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean delete(String[] ids) {
        if (ids.length > 0) {
            QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(LogEntity::getId, ids);
            return this.delete(queryWrapper) > 0;
        }
        return false;
    }

    default void writeLogAsync(String userId, String userName, String abstracts, long requestDuration) {
        writeLogAsync(userId, userName, abstracts, null, 1, null, requestDuration);
    }

    default void writeLogAsync(String userId, String userName, String abstracts, UserInfo userInfo, int loginMark, Integer loginType, long requestDuration) {
        LogEntity entity = new LogEntity();
        String ipAddr = IpUtil.getIpAddr();
        entity.setIpAddress(ipAddr);
        entity.setIpAddressName(IpUtil.getIpCity(ipAddr));
        // 请求设备
        UserAgent userAgent = UserAgentUtil.parse(ServletUtil.getUserAgent());
        if (userAgent != null) {
            entity.setPlatForm(userAgent.getPlatform().getName() + " " + userAgent.getOsVersion());
            entity.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
        }
        if (loginType != null) {
            entity.setLoginType(1);
        } else {
            entity.setLoginType(0);
        }
        entity.setLoginMark(loginMark);
        entity.setRequestDuration(Integer.parseInt(String.valueOf(requestDuration)));
        entity.setId(RandomUtil.uuId());
        entity.setUserId(userId);
        entity.setUserName(userName);
        entity.setDescription(abstracts);
        entity.setRequestUrl(ServletUtil.getServletPath());
        entity.setRequestMethod(ServletUtil.getRequest().getMethod());
        entity.setType(LogSortEnum.LOGIN.getCode());
        this.insert(entity);
    }

    default void writeLogAsync(LogEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void deleteHandleLog(String type, Integer userOnline, String dataInterfaceId) {
        QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LogEntity::getType, Integer.valueOf(type));
        if (ObjectUtil.equals(userOnline, 1)) {
            queryWrapper.lambda().eq(LogEntity::getCreatorUserId, UserProvider.getLoginUserId());
        }
        if (StringUtil.isNotEmpty(dataInterfaceId)) {
            String s = dataInterfaceId +
                    "/Actions/Preview";
            String ss = dataInterfaceId +
                    "/Actions/Response";
            queryWrapper
                    .lambda()
                    .and(query -> query
                            .like(LogEntity::getRequestUrl, s).or().like(LogEntity::getRequestUrl, ss));
        }
        queryWrapper.lambda().select(LogEntity::getId);
        List<String> ids = this.selectList(queryWrapper).stream().map(LogEntity::getId).collect(Collectors.toList());
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            QueryWrapper<LogEntity> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.lambda().in(LogEntity::getId, list);
            this.delete(deleteWrapper);
        }
    }

    default Set<String> queryList() {
        QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LogEntity::getType, 3);
        return !this.selectList(queryWrapper).isEmpty() ? this.selectList(queryWrapper).stream().map(t -> t.getModuleName()).collect(Collectors.toSet()) : new HashSet<>(16);
    }

}
