package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.InterfaceOauthEntity;
import jnpf.base.mapper.InterfaceOauthMapper;
import jnpf.base.model.interfaceoauth.PaginationOauth;
import jnpf.base.service.InterfaceOauthService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.DataException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 接口认证服务
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/8 9:50
 */
@Service
public class InterfaceOauthServiceImpl extends SuperServiceImpl<InterfaceOauthMapper, InterfaceOauthEntity> implements InterfaceOauthService {

    @Override
    public boolean isExistByAppName(String appName, String id) {
        return this.baseMapper.isExistByAppName(appName, id);
    }

    @Override
    public boolean isExistByAppId(String appId, String id) {
        return this.baseMapper.isExistByAppId(appId, id);
    }

    @Override
    public List<InterfaceOauthEntity> getList(PaginationOauth pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public InterfaceOauthEntity getInfo(String id) {
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(InterfaceOauthEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(InterfaceOauthEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(InterfaceOauthEntity entity, String id) throws DataException {
        return this.baseMapper.update(entity, id);
    }

    @Override
    public void delete(InterfaceOauthEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public InterfaceOauthEntity getInfoByAppId(String appId) {
        return this.baseMapper.getInfoByAppId(appId);
    }
}
