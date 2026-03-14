package jnpf.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.permission.connector.UserInfoService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.mapper.UserMapper;
import jnpf.util.JsonUtil;
import jnpf.util.Md5Util;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户信息保存
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/7/28 14:38
 */
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    private final UserMapper userMapper;

    @Override
    public Boolean create(Map<String, Object> map) {
        UserEntity entity = JsonUtil.getJsonToBean(map, UserEntity.class);
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getAccount, entity.getAccount());
        UserEntity entity1 = userMapper.selectOne(queryWrapper);
        if (entity1 != null) {
            entity.setId(entity1.getId());
            if (StringUtil.isNotEmpty(entity.getPassword())) {
                entity.setPassword(Md5Util.getStringMd5(entity.getPassword() + entity1.getSecretkey().toLowerCase()));
            }
            return SqlHelper.retBool(userMapper.updateById(entity));
        } else {
            if (StringUtil.isEmpty(entity.getId())) {
                String userId = RandomUtil.uuId();
                entity.setId(userId);
            }
            entity.setSecretkey(RandomUtil.uuId());
            entity.setPassword(Md5Util.getStringMd5(entity.getPassword() + entity.getSecretkey().toLowerCase()));
            entity.setIsAdministrator(0);
            return userMapper.insertOrUpdate(entity);
        }
    }

    @Override
    public Boolean update(Map<String, Object> map) {
        return create(map);
    }

    @Override
    public Boolean delete(Map<String, Object> map) {
        UserEntity entity = JsonUtil.getJsonToBean(map, UserEntity.class);
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getAccount, entity.getAccount());
        UserEntity entity1 = userMapper.selectOne(queryWrapper);
        if (entity1 != null) {
            entity.setId(entity1.getId());
        }
        return SqlHelper.retBool(userMapper.deleteById(entity.getId()));
    }

    @Override
    public Map<String, Object> getInfo(String id) {
        UserEntity entity = userMapper.selectById(id);
        return JsonUtil.entityToMap(entity);
    }
}
