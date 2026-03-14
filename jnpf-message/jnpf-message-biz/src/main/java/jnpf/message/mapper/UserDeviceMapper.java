package jnpf.message.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.UserDeviceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface UserDeviceMapper extends SuperMapper<UserDeviceEntity> {

    default UserDeviceEntity getInfoByUserId(String userId) {
        QueryWrapper<UserDeviceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserDeviceEntity::getUserId, userId);
        return this.selectOne(queryWrapper);
    }

    default List<String> getCidList(String userId) {
        List<String> cidList = new ArrayList<>();
        QueryWrapper<UserDeviceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserDeviceEntity::getUserId, userId);
        if (this.selectList(queryWrapper) != null && !this.selectList(queryWrapper).isEmpty()) {
            cidList = this.selectList(queryWrapper).stream().map(t -> t.getClientId()).distinct().collect(Collectors.toList());
        }
        return cidList;
    }

    default UserDeviceEntity getInfoByClientId(String clientId) {
        QueryWrapper<UserDeviceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserDeviceEntity::getClientId, clientId);
        return this.selectOne(queryWrapper);
    }
}
